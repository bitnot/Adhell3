package com.fusionjack.adhell3;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.fusionjack.adhell3.dialogfragment.AdhellTurnOnDialogFragment;
import com.fusionjack.adhell3.fragments.AppsFragment;
import com.fusionjack.adhell3.fragments.BlockerFragment;
import com.fusionjack.adhell3.fragments.DomainsFragment;
import com.fusionjack.adhell3.fragments.OthersFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.roughike.bottombar.BottomBar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final String BACK_STACK_TAB_TAG = "tab_fragment";
    protected DeviceAdminInteractor mAdminInteractor;
    private FragmentManager fragmentManager;
    private AdhellTurnOnDialogFragment turnOnDialogFragment;

    @Override
    public void onBackPressed() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count <= 1) {
            finish();
        } else {
            fragmentManager.popBackStackImmediate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragmentManager = getSupportFragmentManager();
        mAdminInteractor = DeviceAdminInteractor.getInstance();
        if (!mAdminInteractor.isContentBlockerSupported()) {
            Log.i(TAG, "Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        turnOnDialogFragment = AdhellTurnOnDialogFragment.newInstance("Adhell Turn On");
        turnOnDialogFragment.setCancelable(false);
        setContentView(R.layout.activity_main);
        BottomBar bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setTabTitleTextAppearance(R.style.bottomBarTextView);
        bottomBar.setOnTabSelectListener(tabId -> {
            if (mAdminInteractor.isActiveAdmin() && mAdminInteractor.isKnoxEnabled()) {
                onTabSelected(tabId);
            }
        });

        AsyncTask.execute(() -> {
            AdhellAppIntegrity adhellAppIntegrity = new AdhellAppIntegrity();
            adhellAppIntegrity.checkDefaultPolicyExists();
            adhellAppIntegrity.checkAdhellStandardPackage();
            adhellAppIntegrity.fillPackageDb();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                fragmentManager.popBackStack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mAdminInteractor.isContentBlockerSupported()) {
            Log.i(TAG, "Device not supported");
            AdhellFactory.getInstance().createNotSupportedDialog(this);
            return;
        }

        if (!mAdminInteractor.isActiveAdmin()) {
            Log.d(TAG, "Admin is not active. Request enabling");
            if (!turnOnDialogFragment.isVisible()) {
                turnOnDialogFragment.show(fragmentManager, "dialog_fragment_turn_on_adhell");
            }
            return;
        }

        if (!mAdminInteractor.isKnoxEnabled()) {
            Log.d(TAG, "Knox disabled");

            Log.d(TAG, "Checking if internet connection exists");
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                Log.d(TAG, "Is internet connection exists: " + isConnected);
                if (!isConnected) {
                    AdhellFactory.getInstance().createNoInternetConnectionDialog(this);
                }
            }

            if (!turnOnDialogFragment.isVisible()) {
                turnOnDialogFragment.show(fragmentManager, "dialog_fragment_turn_on_adhell");
            }
        }
        Log.d(TAG, "Everything is okay");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying activity");
        LogUtils.getInstance().close();
    }

    private void onTabSelected(int tabId) {
        fragmentManager.popBackStack(BACK_STACK_TAB_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment replacing;
        switch (tabId) {
            case R.id.blockerTab:
                replacing = new BlockerFragment();
                break;
            case R.id.appsManagementTab:
                replacing = new AppsFragment();
                break;
            case R.id.domainsTab:
                replacing = new DomainsFragment();
                break;
            case R.id.othersTab:
                replacing = new OthersFragment();
                break;
            default:
                replacing = new BlockerFragment();
        }

        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, replacing)
                .addToBackStack(BACK_STACK_TAB_TAG)
                .commit();
    }
}

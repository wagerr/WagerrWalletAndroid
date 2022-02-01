package com.wagerrwallet.presenter.activities;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.platform.HTTPServer;
import com.wagerrwallet.R;
import com.wagerrwallet.core.BRCorePeer;
import com.wagerrwallet.presenter.activities.settings.WebViewActivity;
import com.wagerrwallet.presenter.activities.util.BRActivity;
import com.wagerrwallet.presenter.customviews.BRButton;
import com.wagerrwallet.presenter.customviews.BRNotificationBar;
import com.wagerrwallet.presenter.customviews.BRSwapSearchBar;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.tools.animation.BRAnimator;
import com.wagerrwallet.tools.animation.BRDialog;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.manager.FontManager;
import com.wagerrwallet.tools.manager.InternetManager;
import com.wagerrwallet.tools.manager.SwapManager;
import com.wagerrwallet.tools.manager.SyncManager;
import com.wagerrwallet.tools.manager.SwapManager;
import com.wagerrwallet.tools.sqlite.CurrencyDataSource;
import com.wagerrwallet.tools.threads.executor.BRExecutor;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.wagerrwallet.wallet.abstracts.OnSwapListModified;
import com.wagerrwallet.wallet.abstracts.SyncListener;
import com.wagerrwallet.wallet.wallets.util.CryptoUriParser;

import java.math.BigDecimal;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.wagerrwallet.tools.animation.BRAnimator.t1Size;
import static com.wagerrwallet.tools.animation.BRAnimator.t2Size;

/**
 * Created by byfieldj on 1/16/18.
 * <p>
 * <p>
 * This activity will display pricing and transaction information for any currency the user has access to
 * (BTC, BCH, ETH)
 */

public class SwapActivity extends BRActivity implements InternetManager.ConnectionReceiverListener, OnSwapListModified, SyncManager.OnProgressUpdate {
    private static final String TAG = SwapActivity.class.getName();
    BRText mCurrencyTitle;
    BRText mCurrencyPriceUsd;
    BRText mBalancePrimary;
    BRText mBalanceSecondary;
    Toolbar mToolbar;
    ImageButton mBackButton;
    BRButton mBuyButton;
    BRText mBalanceLabel;
    BRText mProgressLabel;
    ProgressBar mProgressBar;

    public ViewFlipper barFlipper;
    private BRSwapSearchBar searchBar;
    private ImageButton mSearchIcon;
    private ImageButton mSwap;
    private ConstraintLayout toolBarConstraintLayout;
    private String showCurrency;

    private BRNotificationBar mNotificationBar;

    private static SwapActivity app;

    private InternetManager mConnectionReceiver;
    private TestLogger logger;
    public boolean isSearchBarVisible = false;

    public static SwapActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_swap);

        showCurrency = "";
        showCurrency = getIntent().getStringExtra("currency");

        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalancePrimary = findViewById(R.id.balance_primary);
        mBalanceSecondary = findViewById(R.id.balance_secondary);
        mToolbar = findViewById(R.id.bread_bar);
        mBackButton = findViewById(R.id.back_icon);
        mBuyButton = findViewById(R.id.buy_button);
        barFlipper = findViewById(R.id.tool_bar_flipper);
        searchBar = findViewById(R.id.search_bar);
        mSearchIcon = findViewById(R.id.search_icon);
        toolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mSwap = findViewById(R.id.swap);
        mBalanceLabel = findViewById(R.id.balance_label);
        mProgressLabel = findViewById(R.id.syncing_label);
        mProgressBar = findViewById(R.id.sync_progress);
        mNotificationBar = findViewById(R.id.notification_bar);

        if (Utils.isEmulatorOrDebug(this)) {
            if (logger != null) logger.interrupt();
            logger = new TestLogger(); //Sync logger
            logger.start();
        }

        setUpBarFlipper();

        BRAnimator.init(this);
        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);//make it the size it should be after animation to get the X
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);//make it the size it should be after animation to get the X

        BRAnimator.init(this);
        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);//make it the size it should be after animation to get the X
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);//make it the size it should be after animation to get the X


        mBuyButton.setHasShadow(false);
        mBuyButton.setVisibility(View.VISIBLE);
        mBuyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showSendSwapFragment(SwapActivity.this, showCurrency);
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });

        mSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
                barFlipper.setDisplayedChild(1); //search bar
                searchBar.onShow(true);
            }
        });

        mBalancePrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });
        mBalanceSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });

        SwapManager.getInstance().init(this);

        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        Log.e(TAG, "---START WalletActivity OnCreate UpdateUI");
        updateUi();
        Log.e(TAG, "---END WalletActivity OnCreate UpdateUI");
//        exchangeTest();

        boolean cryptoPreferred = BRSharedPrefs.isCryptoPreferred(this);

        if (cryptoPreferred) {
            setPriceTags(cryptoPreferred, false);
            //swap();   // buggy when restoring activity
        }

        // Check if the "Twilight" screen altering app is currently running
        if (checkIfScreenAlteringAppIsRunning("com.urbandroid.lux")) {
            BRDialog.showSimpleDialog(this, getString(R.string.Dialog_screenAlteringTitle), getString(R.string.Dialog_screenAlteringMessage));
        }

        if ( showCurrency!=null && !showCurrency.equals("") )   {
            int interval = 1500; // 1,5 Second
            Handler handler = new Handler();
            Runnable runnable = new Runnable(){
                public void run() {
                    mBuyButton.performClick();
                }
            };
            handler.postDelayed(runnable, interval);
        }
    }

    public boolean isSearchActive() {
        return isSearchBarVisible;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnectionReceiver != null)
            unregisterReceiver(mConnectionReceiver);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //since we have one instance of activity at all times, this is needed to know when a new intent called upon this activity
        handleUrlClickIfNeeded(intent);
    }

    private void handleUrlClickIfNeeded(Intent intent) {
        Uri data = intent.getData();
        if (data != null && !data.toString().isEmpty()) {
            //handle external click with crypto scheme
            CryptoUriParser.processRequest(this, data.toString(), WalletsMaster.getInstance(this).getCurrentWallet(this));
        }
    }

    private void updateUi() {
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (wallet == null) {
            Log.e(TAG, "updateUi: wallet is null");
            return;
        }

//        String fiatIso = BRSharedPrefs.getPreferredFiatIso(this);

        String fiatExchangeRate = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), wallet.getFiatExchangeRate(this));
        String fiatBalance = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), wallet.getFiatBalance(this));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(this, wallet.getIso(this), new BigDecimal(wallet.getCachedBalance(this)));

        mCurrencyTitle.setText(wallet.getName(this));
        mCurrencyPriceUsd.setText(String.format("%s per %s", fiatExchangeRate, wallet.getIso(this)));
        mBalancePrimary.setText(fiatBalance);
        mBalanceSecondary.setText(cryptoBalance);
        mToolbar.setBackgroundColor(Color.parseColor(wallet.getUiConfiguration().colorHex));
        mBuyButton.setColor(Color.parseColor(wallet.getUiConfiguration().colorHex));

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                SwapManager.getInstance().updateSwapList(SwapActivity.this);
            }
        });

    }

    // This method checks if a screen altering app(such as Twightlight) is currently running
    // If it is, notify the user that the BRD app will not function properly and they should
    // disable it
    private boolean checkIfScreenAlteringAppIsRunning(String packageName) {

        // Use the ActivityManager API if sdk version is less than 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Get the Activity Manager
            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

            // Get a list of running tasks, we are only interested in the last one,
            // the top most so we give a 1 as parameter so we only get the topmost.
            List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
            Log.d(TAG, "Process list count -> " + processes.size());


            String processName = "";
            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {

                // Get the info we need for comparison.
                processName = processInfo.processName;
                Log.d(TAG, "Process package name -> " + processName);

                // Check if it matches our package name
                if (processName.equals(packageName)) return true;


            }


        }
        // Use the UsageStats API for sdk versions greater than Lollipop
        else {
            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                String currentPackageName = "";
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                    currentPackageName = usageStats.getPackageName();


                    if (currentPackageName.equals(packageName)) {
                        return true;
                    }


                }


            }

        }


        return false;
    }

    private void swap() {
        if (!BRAnimator.isClickAllowed()) return;
        boolean b = !BRSharedPrefs.isCryptoPreferred(this);
        setPriceTags(b, true);
        BRSharedPrefs.setIsCryptoPreferred(this, b);
    }

    private void setPriceTags(final boolean cryptoPreferred, boolean animate) {
        //mBalanceSecondary.setTextSize(!cryptoPreferred ? t1Size : t2Size);
        //mBalancePrimary.setTextSize(!cryptoPreferred ? t2Size : t1Size);
        ConstraintSet set = new ConstraintSet();
        set.clone(toolBarConstraintLayout);
        if (animate)
            TransitionManager.beginDelayedTransition(toolBarConstraintLayout);
        int px8 = Utils.getPixelsFromDps(this, 8);
        int px16 = Utils.getPixelsFromDps(this, 16);
//
//        //align first item to parent right
//        set.connect(!cryptoPreferred ? R.id.balance_secondary : R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, px16);
//        //align swap symbol after the first item
//        set.connect(R.id.swap, ConstraintSet.START, !cryptoPreferred ? R.id.balance_secondary : R.id.balance_primary, ConstraintSet.START, px8);
//        //align second item after swap symbol
//        set.connect(!cryptoPreferred ? R.id.balance_secondary : R.id.balance_primary, ConstraintSet.START, mSwap.getId(), ConstraintSet.END, px8);
//

        // CRYPTO on RIGHT
        if (cryptoPreferred) {

            // Align crypto balance to the right parent
            set.connect(R.id.balance_secondary, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, px8);
            set.connect(R.id.balance_secondary, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, -px8);

            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, px8);

            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, px8);

            mBalancePrimary.setPadding(0, 0, 0, Utils.getPixelsFromDps(this, 6));
            mBalanceSecondary.setPadding(0, 0, 0, Utils.getPixelsFromDps(this, 4));
            mSwap.setPadding(0, 0, 0, Utils.getPixelsFromDps(this, 2));

            Log.d(TAG, "CryptoPreferred " + cryptoPreferred);

            mBalanceSecondary.setTextSize(t1Size);
            mBalancePrimary.setTextSize(t2Size);

            set.applyTo(toolBarConstraintLayout);

        }

        // CRYPTO on LEFT
        else {

            // Align primary to right of parent
            set.connect(R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, px8);

            // Align swap icon to left of usd balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_primary, ConstraintSet.START, px8);


            // Align secondary currency to the left of swap icon
            set.connect(R.id.balance_secondary, ConstraintSet.END, R.id.swap, ConstraintSet.START, px8);

            mBalancePrimary.setPadding(0, 0, 0, Utils.getPixelsFromDps(this, 2));
            mBalanceSecondary.setPadding(0, 0, 0, Utils.getPixelsFromDps(this, 4));
            mSwap.setPadding(0, 0, 0, Utils.getPixelsFromDps(this, 2));

            //mBalancePrimary.setPadding(0,0, 0, Utils.getPixelsFromDps(this, -4));

            Log.d(TAG, "CryptoPreferred " + cryptoPreferred);

            mBalanceSecondary.setTextSize(t2Size);
            mBalancePrimary.setTextSize(t1Size);


            set.applyTo(toolBarConstraintLayout);

        }


        if (!cryptoPreferred) {
            mBalanceSecondary.setTextColor(getResources().getColor(R.color.currency_subheading_color, null));
            mBalancePrimary.setTextColor(getResources().getColor(R.color.white, null));
            mBalanceSecondary.setTypeface(FontManager.get(this, "CircularPro-Book.otf"));

        } else {
            mBalanceSecondary.setTextColor(getResources().getColor(R.color.white, null));
            mBalancePrimary.setTextColor(getResources().getColor(R.color.currency_subheading_color, null));
            mBalanceSecondary.setTypeface(FontManager.get(this, "CircularPro-Bold.otf"));

        }

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                }, toolBarConstraintLayout.getLayoutTransition().getDuration(LayoutTransition.CHANGE_APPEARING));
    }

    @Override
    protected void onResume() {
        super.onResume();

        app = this;

        WalletsMaster.getInstance(app).initWallets(app);

        setupNetworking();

        SwapManager.getInstance().adapter.updateData();
        SwapManager.getInstance().onResume(this);

        CurrencyDataSource.getInstance(this).addOnDataChangedListener(new CurrencyDataSource.OnDataChanged() {
            @Override
            public void onChanged() {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                });
            }
        });
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        wallet.addSwapListModifiedListener(this);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                long balance = wallet.getWallet().getBalance();
                wallet.setCashedBalance(app, balance);
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                });

            }
        });

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (wallet.getPeerManager().getConnectStatus() != BRCorePeer.ConnectStatus.Connected)
                    wallet.connectWallet(SwapActivity.this);
            }
        });

        wallet.addSyncListeners(new SyncListener() {
            @Override
            public void syncStopped(String err) {

            }

            @Override
            public void syncStarted() {
                SyncManager.getInstance().startSyncing(SwapActivity.this, wallet, SwapActivity.this);
            }
        });

        SyncManager.getInstance().startSyncing(this, wallet, this);

        handleUrlClickIfNeeded(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        SyncManager.getInstance().stopSyncing();
    }

    private void setUpBarFlipper() {
        barFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        barFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    public void resetFlipper() {
        barFlipper.setDisplayedChild(0);
    }

    private void setupNetworking() {
        if (mConnectionReceiver == null) mConnectionReceiver = InternetManager.getInstance();
        IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionReceiver, mNetworkStateFilter);
        InternetManager.addConnectionListener(this);
    }


    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged");
        if (isConnected) {
            if (barFlipper != null && barFlipper.getDisplayedChild() == 2) {
                barFlipper.setDisplayedChild(0);
            }
            final BaseWalletManager wm = WalletsMaster.getInstance(SwapActivity.this).getCurrentWallet(SwapActivity.this);
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    final double progress = wm.getPeerManager()
                            .getSyncProgress(BRSharedPrefs.getStartHeight(SwapActivity.this,
                                    BRSharedPrefs.getCurrentWalletIso(SwapActivity.this)));
//                    Log.e(TAG, "run: " + progress);
                    if (progress < 1 && progress > 0) {
                        SyncManager.getInstance().startSyncing(SwapActivity.this, wm, SwapActivity.this);
                    }
                }
            });

        } else {
            if (barFlipper != null)
                barFlipper.setDisplayedChild(2);

        }
    }


    @Override
    public void onBackPressed() {
        int c = getFragmentManager().getBackStackEntryCount();
        if (c > 0) {
            super.onBackPressed();
            return;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        if (!isDestroyed()) {
            finish();
        }
    }

    @Override
    public void swapListModified(String hash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });

    }

    @Override
    public boolean onProgressUpdated(double progress) {
        mProgressBar.setProgress((int) (progress * 100));
        if (progress == 1) {
            mProgressBar.setVisibility(View.GONE);
            mProgressLabel.setVisibility(View.GONE);
            mBalanceLabel.setVisibility(View.VISIBLE);
            mProgressBar.invalidate();
            return false;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressLabel.setVisibility(View.VISIBLE);
        mBalanceLabel.setVisibility(View.GONE);
        mProgressBar.invalidate();
        return true;
    }


    //test logger
    class TestLogger extends Thread {
        private static final String TAG = "TestLogger";

        @Override
        public void run() {
            super.run();

            while (true) {
                StringBuilder builder = new StringBuilder();
                for (BaseWalletManager w : WalletsMaster.getInstance(SwapActivity.this).getAllWallets()) {
                    builder.append("   " + w.getIso(SwapActivity.this));
                    String connectionStatus = "";
                    if (w.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Connected)
                        connectionStatus = "Connected";
                    else if (w.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Disconnected)
                        connectionStatus = "Disconnected";
                    else if (w.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Connecting)
                        connectionStatus = "Connecting";

                    double progress = w.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(SwapActivity.this, w.getIso(SwapActivity.this)));

                    builder.append(" - " + connectionStatus + " " + progress * 100 + "%     ");

                }

                Log.e(TAG, "testLog: " + builder.toString());

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
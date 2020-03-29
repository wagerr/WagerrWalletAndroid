package com.wagerrwallet.presenter.activities;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.platform.HTTPServer;
import com.wagerrwallet.R;
import com.wagerrwallet.core.BRCoreAddress;
import com.wagerrwallet.core.BRCorePeer;
import com.wagerrwallet.presenter.activities.settings.BetSettings;
import com.wagerrwallet.presenter.activities.settings.SettingsActivity;
import com.wagerrwallet.presenter.activities.settings.WebViewActivity;
import com.wagerrwallet.presenter.activities.util.BRActivity;
import com.wagerrwallet.presenter.customviews.BRButton;
import com.wagerrwallet.presenter.customviews.BRDialogView;
import com.wagerrwallet.presenter.customviews.BREventSearchBar;
import com.wagerrwallet.presenter.customviews.BRNotificationBar;
import com.wagerrwallet.presenter.customviews.BRSearchBar;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.BetMappingEntity;
import com.wagerrwallet.presenter.entities.CryptoRequest;
import com.wagerrwallet.presenter.entities.EventTxUiHolder;
import com.wagerrwallet.tools.animation.BRAnimator;
import com.wagerrwallet.tools.animation.BRDialog;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.manager.FontManager;
import com.wagerrwallet.tools.manager.InternetManager;
import com.wagerrwallet.tools.manager.SyncManager;
import com.wagerrwallet.tools.manager.EventTxManager;
import com.wagerrwallet.tools.sqlite.BetMappingTxDataStore;
import com.wagerrwallet.tools.sqlite.CurrencyDataSource;
import com.wagerrwallet.tools.threads.executor.BRExecutor;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.wagerrwallet.wallet.abstracts.OnBalanceChangedListener;
import com.wagerrwallet.wallet.abstracts.OnEventTxListModified;
import com.wagerrwallet.wallet.abstracts.OnTxListModified;
import com.wagerrwallet.wallet.abstracts.SyncListener;
import com.wagerrwallet.wallet.wallets.util.CryptoUriParser;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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

public class EventsActivity extends BRActivity implements InternetManager.ConnectionReceiverListener, OnEventTxListModified, SyncManager.OnProgressUpdate {
    private static final String TAG = EventsActivity.class.getName();
    BRText mCurrencyTitle;
    BRText mCurrencyPriceUsd;
    BRText mBalancePrimary;
    BRText mBalanceSecondary;
    Toolbar mToolbar;
    ImageButton mBackButton;
    //BRButton mSendButton;
    //BRButton mReceiveButton;
    //BRButton mBuyButton;
    BRText mBalanceLabel;
    BRText mProgressLabel;
    BRText mLabelEmpty;
    ProgressBar mProgressBar;

    public ViewFlipper barFlipper;
    private BREventSearchBar searchBar;
    private ImageButton mSearchIcon;
    private ImageButton mSettingsIcon;
    private ImageButton mSwap;
    private ConstraintLayout toolBarConstraintLayout;

    private Spinner mSpinnerSport;
    private Spinner mSpinnerTournament;
    public long[] filterSwitches = new long[2];

    private BRNotificationBar mNotificationBar;

    private static EventsActivity app;

    private InternetManager mConnectionReceiver;
    private TestLogger logger;
    public boolean isSearchBarVisible = false;

    public static EventsActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalancePrimary = findViewById(R.id.balance_primary);
        mBalanceSecondary = findViewById(R.id.balance_secondary);
        mToolbar = findViewById(R.id.bread_bar);
        mBackButton = findViewById(R.id.back_icon);
        //mSendButton = findViewById(R.id.send_button);
        //mReceiveButton = findViewById(R.id.receive_button);
        //mBuyButton = findViewById(R.id.buy_button);
        barFlipper = findViewById(R.id.tool_bar_flipper);
        searchBar = findViewById(R.id.search_bar);
        mSearchIcon = findViewById(R.id.search_icon);
        mSettingsIcon = findViewById(R.id.settings_icon);
        toolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mSwap = findViewById(R.id.swap);
        mBalanceLabel = findViewById(R.id.balance_label);
        mProgressLabel = findViewById(R.id.syncing_label);
        mProgressBar = findViewById(R.id.sync_progress);
        mNotificationBar = findViewById(R.id.notification_bar);
        mLabelEmpty = findViewById(R.id.label_empty);

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

        mSpinnerSport = findViewById(R.id.spinner_sport);
        mSpinnerSport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BetMappingEntity bme = (BetMappingEntity)parent.getSelectedItem();
                if (bme!=null) {
                    filterSwitches[0] = bme.getMappingID();
                }

                if (EventTxManager.getInstance().adapter != null)
                    EventTxManager.getInstance().adapter.filter( filterSwitches, true, searchBar.getSearchQuery());

                updateTournaments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mSpinnerTournament = findViewById(R.id.spinner_tournament);
        mSpinnerTournament.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BetMappingEntity bme = (BetMappingEntity)parent.getSelectedItem();
                if (bme!=null) {
                    filterSwitches[1] = bme.getMappingID();
                }

                if (EventTxManager.getInstance().adapter != null)
                    EventTxManager.getInstance().adapter.filter( filterSwitches, true, searchBar.getSearchQuery());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

/*
        mSendButton.setHasShadow(false);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Activity app = WalletActivity.this;
//                BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
//                CryptoUriParser.processRequest(WalletActivity.this, "bitcoin:?r=https://bitpay.com/i/HUsFqTFirmVtgE4PhLzcRx", wm);
                BRAnimator.showSendFragment(EventsActivity.this, null);

            }
        });

        mSendButton.setHasShadow(false);
        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showReceiveFragment(EventsActivity.this, true);

            }
        });
*/
//        BaseWalletManager wm = WalletsMaster.getInstance(this).getCurrentWallet(this);
//        Log.d(TAG, "Current wallet ISO -> " + wm.getIso(this));

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

        mSettingsIcon.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Intent intent = new Intent(EventsActivity.this, BetSettings.class);
                 startActivity(intent);
                 overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
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

        EventTxManager.getInstance().init(this);

        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        updateUi();
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

    }

    public boolean isSearchActive() {
        boolean ret = false;
        if ( searchBar!=null && searchBar.isShown() )    {
            ret = true;
        }
        return ret;
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

    public void updateSports() {
        Date date = new Date();
        long timeStamp = (date.getTime()/1000) + WalletWagerrManager.BET_CUTTOFF_SECONDS;

        BetMappingTxDataStore bmds = BetMappingTxDataStore.getInstance(this);
        List<BetMappingEntity> sports = bmds.getAllSports(this, "wgr", timeStamp );
        int currPosition = 0;
        BetMappingEntity bmeS = (BetMappingEntity)mSpinnerSport.getSelectedItem();

        // add item 0
        sports.add(0, new BetMappingEntity("",0, BetMappingEntity.MappingNamespaceType.SPORT, -1, "Sport",0,0,"wgr"));
        currPosition = findItemIndex( bmeS, sports);

        ArrayAdapter<BetMappingEntity> dataAdapter = getDataAdapter(sports);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSport.setAdapter(dataAdapter);
        mSpinnerSport.setSelection(currPosition);
    }

    public void updateTournaments() {
        Date date = new Date();
        long timeStamp = (date.getTime()/1000) + WalletWagerrManager.BET_CUTTOFF_SECONDS;

        BetMappingTxDataStore bmds = BetMappingTxDataStore.getInstance(this);
        int currPosition = 0;
        BetMappingEntity bmeS = (BetMappingEntity)mSpinnerSport.getSelectedItem();
        BetMappingEntity bmeT = (BetMappingEntity)mSpinnerTournament.getSelectedItem();

        List<BetMappingEntity> tournaments = new ArrayList<>();
        long sportID = -1;
        if (bmeS!=null && bmeS.getMappingID()!=-1 && bmeT!=null) {
            sportID = bmeS.getMappingID();
            tournaments = bmds.getAllTournaments(this, "wgr", sportID, timeStamp );
            // add item 0
            tournaments.add(0, new BetMappingEntity("",0, BetMappingEntity.MappingNamespaceType.TOURNAMENT, -1, "League",0,0,"wgr"));
            currPosition = findItemIndex( bmeT, tournaments);
        }
        else {
            tournaments.add(0, new BetMappingEntity("",0, BetMappingEntity.MappingNamespaceType.TOURNAMENT, -1, "League",0,0,"wgr"));
        }

        ArrayAdapter<BetMappingEntity> dataAdapter = getDataAdapter(tournaments);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerTournament.setAdapter(dataAdapter);
        mSpinnerTournament.setSelection(currPosition);
    }

    protected int findItemIndex( BetMappingEntity item, List<BetMappingEntity> list )    {
        int ret = 0, i = 0;

        if (item==null)     return 0;

        for(BetMappingEntity obj : list)   {
            if (item.getMappingID()==obj.getMappingID())    {
                ret = i;
                break;
            }
            i++;
        }
        return ret;
    }

    public ArrayAdapter<BetMappingEntity> getDataAdapter(List<BetMappingEntity> list) {
        ArrayAdapter<BetMappingEntity> dataAdapter = new ArrayAdapter<BetMappingEntity>(this,
                android.R.layout.simple_spinner_item, list)
        {
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View v = null;
                if ( position < super.getCount() ) {
                    v = super.getView(position, convertView, parent);
                    ((TextView) v).setTypeface(sTypeFace(getApplicationContext()));//Typeface for normal view
                    ((TextView) v).setTextColor(Color.parseColor("#c20c23"));
                    ((TextView) v).setBackgroundColor(Color.parseColor("#fafafa"));
                }
                return v;
            }
            public View getDropDownView(int position, View convertView, ViewGroup parent)
            {
                View v = null;
                if ( position < super.getCount() ) {
                    v = super.getDropDownView(position, convertView, parent);
                    ((TextView) v).setTypeface(sTypeFace(getApplicationContext()));//Typeface for dropdown view
                    ((TextView) v).setBackgroundColor(Color.parseColor("#fafafa"));
                    ((TextView) v).setTextColor(Color.parseColor("#c20c23"));
                }
                return v;
            }
        };
        return dataAdapter;
    }

    public static Typeface sTypeFace(Context mCnxt) {
        Typeface mtypeface = FontManager.get(mCnxt,"CircularPro-Book.otf");
        return mtypeface;
    }

    private void handleUrlClickIfNeeded(Intent intent) {
        Uri data = intent.getData();

        BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
        Boolean isSyncing =  wallet.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(app, "WGR" ))<1;
        if (isSyncing)    {
            BRDialog.showCustomDialog(app, "Error", "Wallet is still syncing", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismiss();
                }
            }, null, null, 0);
        }
        else    {
            if (data != null && !data.toString().isEmpty()) {
                long eventID = parseRequest( data.toString() );
                EventTxUiHolder item = EventTxManager.getInstance().adapter.findByEventID( eventID );

                if ( item == null )   {
                    BRDialog.showCustomDialog(app, "Error", "Event is no longer valid", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismiss();
                        }
                    }, null, null, 0);
                }
                else    {
                    BRAnimator.showEventDetails(app, item, 0);
                }
                //handle external click with crypto scheme
                //CryptoUriParser.processRequest(this, data.toString(), WalletsMaster.getInstance(this).getCurrentWallet(this));
            }
        }
    }

    public static long parseRequest( String str) {
        if (str == null || str.isEmpty()) return 0;

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();
        String[] keyValue = u.getQuery().split("=", 2);
        long ret = Long.parseLong( keyValue[1] );

        return ret;
    }

    private void updateUi() {
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (wallet == null) {
            Log.e(TAG, "events updateUi: wallet is null");
            return;
        }

        mLabelEmpty.setVisibility( (EventTxManager.getInstance().adapter.getItemCount()>0) ? View.GONE : View.VISIBLE);

        // update spinners
        updateSports();
        updateTournaments();

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.getPixelsFromDps(this, 65), 1.5f
        );

        LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.getPixelsFromDps(this, 65), 1.5f
        );
        param.gravity = Gravity.CENTER;
        param2.gravity = Gravity.CENTER;

        param.setMargins(Utils.getPixelsFromDps(this, 8), Utils.getPixelsFromDps(this, 8), Utils.getPixelsFromDps(this, 8), 0);
        param2.setMargins(0, Utils.getPixelsFromDps(this, 8), Utils.getPixelsFromDps(this, 8), 0);

        //mSendButton.setLayoutParams(param);
        //mReceiveButton.setLayoutParams(param2);
        //mBuyButton.setVisibility(View.GONE);

//        String fiatIso = BRSharedPrefs.getPreferredFiatIso(this);

        updateBalance();

        mToolbar.setBackgroundColor(Color.parseColor(wallet.getUiConfiguration().colorHex));
        //mSendButton.setColor(Color.parseColor(wallet.getUiConfiguration().colorHex));
        //mBuyButton.setColor(Color.parseColor(wallet.getUiConfiguration().colorHex));
        //mReceiveButton.setColor(Color.parseColor(wallet.getUiConfiguration().colorHex));

        EventTxManager.getInstance().updateTxList(EventsActivity.this);
    }

    protected void updateBalance()  {
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);

        String fiatExchangeRate = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), wallet.getFiatExchangeRate(this));
        String fiatBalance = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), wallet.getFiatBalance(this));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(this, wallet.getIso(this), new BigDecimal(wallet.getCachedBalance(this)));

        mCurrencyTitle.setText(wallet.getName(this));
        mCurrencyPriceUsd.setText(String.format("%s per %s", fiatExchangeRate, wallet.getIso(this)));
        mBalancePrimary.setText(fiatBalance);
        mBalanceSecondary.setText(cryptoBalance);
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
            set.connect(R.id.balance_secondary, ConstraintSet.TOP, R.id.currency_usd_price, ConstraintSet.BOTTOM, -px8);

            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, px8);

            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, px8);

            mBalancePrimary.setPadding(0, 0, 0, Utils.getPixelsFromDps(this, 6));
            mBalanceSecondary.setPadding(0, Utils.getPixelsFromDps(this, 6), 0, Utils.getPixelsFromDps(this, 4));
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
            mBalanceSecondary.setPadding(0, Utils.getPixelsFromDps(this, 6), 0, Utils.getPixelsFromDps(this, 4));
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

        EventTxManager.getInstance().onResume(this);

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
        wallet.addEventTxListModifiedListener(this);
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
                    wallet.connectWallet(EventsActivity.this);
            }
        });


        /*
        wallet.addSyncListeners(new SyncListener() {
            @Override
            public void syncStopped(String err) {

            }

            @Override
            public void syncStarted() {
                SyncManager.getInstance().startSyncing(EventsActivity.this, wallet, EventsActivity.this);
            }
        });

        SyncManager.getInstance().startSyncing(this, wallet, this);
        */
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
            final BaseWalletManager wm = WalletsMaster.getInstance(EventsActivity.this).getCurrentWallet(EventsActivity.this);
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    final double progress = wm.getPeerManager()
                            .getSyncProgress(BRSharedPrefs.getStartHeight(EventsActivity.this,
                                    BRSharedPrefs.getCurrentWalletIso(EventsActivity.this)));
//                    Log.e(TAG, "run: " + progress);
                    if (progress < 1 && progress > 0) {
                        SyncManager.getInstance().startSyncing(EventsActivity.this, wm, EventsActivity.this);
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
    public void eventTxListModified(String hash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });

    }

    @Override
    public boolean onProgressUpdated(double progress) {
        /* disabled here
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
        */
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
                for (BaseWalletManager w : WalletsMaster.getInstance(EventsActivity.this).getAllWallets()) {
                    builder.append("   " + w.getIso(EventsActivity.this));
                    String connectionStatus = "";
                    if (w.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Connected)
                        connectionStatus = "Connected";
                    else if (w.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Disconnected)
                        connectionStatus = "Disconnected";
                    else if (w.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Connecting)
                        connectionStatus = "Connecting";

                    double progress = w.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(EventsActivity.this, w.getIso(EventsActivity.this)));

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

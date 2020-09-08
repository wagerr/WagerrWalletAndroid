package com.wagerrwallet.presenter.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;
import com.wagerrwallet.R;
import com.wagerrwallet.WagerrApp;
import com.wagerrwallet.core.BRCoreTransaction;
import com.wagerrwallet.presenter.activities.EventsActivity;
import com.wagerrwallet.presenter.activities.settings.BetSettings;
import com.wagerrwallet.presenter.activities.settings.WebViewActivity;
import com.wagerrwallet.presenter.customviews.BRDialogView;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.BetEntity;
import com.wagerrwallet.presenter.entities.BetEventEntity;
import com.wagerrwallet.presenter.entities.CryptoRequest;
import com.wagerrwallet.presenter.entities.CurrencyEntity;
import com.wagerrwallet.presenter.entities.EventTxUiHolder;
import com.wagerrwallet.presenter.entities.TxUiHolder;
import com.wagerrwallet.tools.animation.BRAnimator;
import com.wagerrwallet.tools.animation.BRDialog;
import com.wagerrwallet.tools.manager.BRClipboardManager;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.manager.SendManager;
import com.wagerrwallet.tools.sqlite.BetEventTxDataStore;
import com.wagerrwallet.tools.util.BRConstants;
import com.wagerrwallet.tools.util.BRDateUtil;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;

/**
 * Created by byfieldj on 2/26/18.
 * <p>
 * Reusable dialog fragment that display details about a particular transaction
 */

public class FragmentEventDetails extends DialogFragment implements View.OnClickListener {

    private static final String EXTRA_TX_ITEM = "event_item";
    private static final String TAG = "FragmentEventDetails";
    private static final int NORMAL_SIZE = 24;
    private static final int BIG_SIZE = 32;
    private static final long UNIT_MULTIPLIER = 100000000L;     // so far in full WGR units

    private EventTxUiHolder mTransaction;

    private BRText mTxEventHeader;
    private BRText mTxEventDate;
    private BRText mTxEventId;
    private BRText mTxHomeTeam;
    private BRText mTxAwayTeam;
    //private BRText mTxHomeResult;
    //private BRText mTxAwayResult;
    private BRText mTxHomeOdds;
    private BRText mTxDrawOdds;
    private BRText mTxAwayOdds;

    private SeekBar seekBar;

    private BRText mTxSpreadPoints;
    private BRText mTxSpreadHomeOdds;
    private BRText mTxSpreadAwayOdds;

    private BRText mTxTotalPoints;
    private BRText mTxTotalOverOdds;
    private BRText mTxTotalUnderOdds;

    private EditText mTxAmount;
    private BRText mTxCurrency;
    private BRText mTxStatus;
    private BRText mTxDate;
    private BRText mTxLastUpdated;
    private BRText mTxLastDate;
    private BRText mToFrom;
    private BRText mToFromAddress;
    private BRText mMemoText;

    private BRText mStartingBalance;
    private BRText mEndingBalance;
    private BRText mExchangeRate;
    private BRText mConfirmedInBlock;
    private BRText mTransactionId;
    private BRText mShowHide;
    private BRText mAmountWhenSent;
    private BRText mAmountNow;
    private BRText mTxNoBetBalance;

    private ImageButton mCloseButton;
    private RelativeLayout mDetailsContainer;

    private LinearLayout mMainLayout;

    private RelativeLayout mBetSliderContainer;
    private LinearLayout mBetSliderLayout;
    private RelativeLayout mMoneyLineContainer;
    private LinearLayout mMoneyLineLayout;
    private LinearLayout mSpreadsLayout;
    private LinearLayout mTotalsLayout;
    private LinearLayout mDetailsLayout;
    private RelativeLayout mSpreadsContainer;
    private RelativeLayout mTotalsContainer;
    private ImageButton mAcceptBet;
    private ImageButton mCancelBet;
    private View mCurrentSelectedBetOption = null;
    private ImageButton faq;
    private BRText mPotentialReward;

    private ImageButton betSmartHomeButton;
    private ImageButton betSmartAwayButton;

    // layout management
    private boolean bHasMoneyLine = true;
    private boolean bHasSpreads = false;
    private boolean bHasTotals = false;
    private boolean bBarSliding = false;
    LinearLayout rlToPutBelowBetSlider = null;
    LinearLayout rlToPutBelowPrevious = null;
    RelativeLayout rlLastContainer = null;

    boolean mDetailsShowing = false;

    private int mInterval = 3000;
    private Handler mHandler;
    private boolean updatingNode;

    // refresh to update odds
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                Context app = WagerrApp.getBreadContext();
                mTransaction = BetEventTxDataStore.getInstance(app).getTransactionByEventId(app, "wgr", mTransaction.getEventID());
                updateUi();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.event_details, container, false);

        faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(getActivity(), BRConstants.betSlip);
            }
        });

        mMainLayout = rootView.findViewById(R.id.dynamic_container);
        mTxEventHeader = rootView.findViewById(R.id.tx_eventheader);
        mTxEventDate= rootView.findViewById(R.id.tx_eventdate);
        mTxEventId= rootView.findViewById(R.id.tx_eventid);
        final long evID = mTransaction.getEventID();
        mTxEventId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format( WagerrApp.HOST_EXPLORER + "/#/bet/event/%d", evID)));
                startActivity(browserIntent);
                getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        mTxHomeTeam = rootView.findViewById(R.id.tx_home);
        mTxAwayTeam= rootView.findViewById(R.id.tx_away);
        //mTxHomeResult = rootView.findViewById(R.id.tx_home_result);
        //mTxAwayResult= rootView.findViewById(R.id.tx_away_result);
        mTxHomeOdds = rootView.findViewById(R.id.tx_home_odds);
        mTxDrawOdds= rootView.findViewById(R.id.tx_draw_odds);
        mTxAwayOdds= rootView.findViewById(R.id.tx_away_odds);
        mTxAmount = (EditText) rootView.findViewById(R.id.tx_amount);
        mTxCurrency = rootView.findViewById(R.id.tx_currency);

        mMoneyLineContainer = rootView.findViewById(R.id.odds_container);
        mMoneyLineLayout =  rootView.findViewById(R.id.odds_layout);

        betSmartHomeButton = rootView.findViewById(R.id.betsmart_button_home);
        betSmartAwayButton = rootView.findViewById(R.id.betsmart_button_away);

        betSmartHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strTeam = "", strSport = "";
                try {
                    strTeam = URLEncoder.encode(mTransaction.getTxHomeTeam(), "UTF-8");
                    strSport = URLEncoder.encode(mTransaction.getTxSport(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                Toast.makeText(getActivity(), "launching web view", Toast.LENGTH_LONG).show();
                CreateWebFragment( getActivity(), String.format("https://betsmart.app/teaser-team/?name=%s&sport=%s&mode=light&source=wagerr", strTeam, strSport));
            }
        });

        betSmartAwayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strTeam = "", strSport = "";
                try {
                    strTeam = URLEncoder.encode(mTransaction.getTxAwayTeam(), "UTF-8");
                    strSport = URLEncoder.encode(mTransaction.getTxSport(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                Toast.makeText(getActivity(), "launching web view", Toast.LENGTH_LONG).show();
                CreateWebFragment( getActivity(), String.format("https://betsmart.app/teaser-team/?name=%s&sport=%s&mode=light&source=wagerr", strTeam, strSport));
            }
        });

        final BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());

        seekBar = rootView.findViewById(R.id.bet_seekBar);
        int min = getContext().getResources().getInteger(R.integer.min_bet_amount);
        int max = Math.min( (int)(walletManager.getWallet().getBalance()/UNIT_MULTIPLIER),
                getContext().getResources().getInteger(R.integer.max_bet_amount));
        seekBar.setMax(max-min);

        int defVal = Math.min( BRSharedPrefs.getDefaultBetAmount(getContext()), max );
        mTxAmount.setText("" + defVal);
        updateSeekBar(defVal, 0);
        seekBar.setProgress( defVal - min);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
               @Override
               public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                   int min = getContext().getResources().getInteger(R.integer.min_bet_amount);
                   int max = Math.min( (int)(walletManager.getWallet().getBalance()/UNIT_MULTIPLIER),
                           getContext().getResources().getInteger(R.integer.max_bet_amount));
                   seekBar.setMax(max-min);
                   int posX = seekBar.getThumb().getBounds().centerX();
                   int coinAmount = progress + min;
                   updateSeekBar(coinAmount, 0);
                   if (bBarSliding) {
                       mTxAmount.setText("" + coinAmount);
                   }
                   //textView.setY(100); just added a value set this properly using screen with height aspect ratio , if you do not set it by default it will be there below seek bar
               }
               @Override
               public void onStartTrackingTouch(SeekBar seekBar) {
                   bBarSliding = true;
               }

               @Override
               public void onStopTrackingTouch(SeekBar seekBar) {
                   bBarSliding = false;
               }
        });

        mTxAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                int minvalue = getContext().getResources().getInteger(R.integer.min_bet_amount);
                int value = minvalue;
                int balance = (int)(walletManager.getWallet().getBalance()/UNIT_MULTIPLIER);
                int maxvalue = Math.min(getContext().getResources().getInteger(R.integer.max_bet_amount), balance );

                if (!hasFocus) {
                    try {
                        value = Integer.parseInt(mTxAmount.getText().toString());
                        if ( value < minvalue )    value = minvalue;
                        if ( value > maxvalue )    {
                            value = maxvalue;
                            // add message
                            BRDialog.showCustomDialog(getContext(), "Warning", String.format("Transaction reduced to maximum of %d WGR", maxvalue), getContext().getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);

                        }
                        mTxAmount.setText(String.valueOf(value));
                    }
                    catch (NumberFormatException e)     {
                    }
                    seekBar.setProgress( value - minvalue);
                }
            }
        });

        mTxAmount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mTxAmount.clearFocus();
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });


        mTxAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mCurrentSelectedBetOption==null)    return;
                String oddTx = ((BRText)mCurrentSelectedBetOption).getText().toString();
                float odds = 0;
                int value = getContext().getResources().getInteger(R.integer.min_bet_amount);
                int minvalue = value;
                Float fValue = 0.0f;
                try {
                    odds = Float.parseFloat( oddTx );
                    fValue = Float.parseFloat(mTxAmount.getText().toString());
                    value = Math.max(minvalue, fValue.intValue());
                } catch (NumberFormatException e) {
                }
                setRewardAmount(value, odds);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

        mSpreadsContainer = rootView.findViewById(R.id.spreads_container);
        mSpreadsLayout = rootView.findViewById(R.id.spreads_layout);
        mTxSpreadPoints= rootView.findViewById(R.id.tx_spread_points);
        mTxSpreadHomeOdds = rootView.findViewById(R.id.tx_spreads_home_odds);
        mTxSpreadAwayOdds= rootView.findViewById(R.id.tx_spreads_away_odds);


        mTotalsContainer = rootView.findViewById(R.id.totals_container);
        mTotalsLayout = rootView.findViewById(R.id.totals_layout);
        mTxTotalPoints = rootView.findViewById(R.id.tx_total_points);
        mTxTotalOverOdds= rootView.findViewById(R.id.tx_over_odds);
        mTxTotalUnderOdds= rootView.findViewById(R.id.tx_under_odds);
        mAmountNow = rootView.findViewById(R.id.amount_now);
        mAmountWhenSent = rootView.findViewById(R.id.amount_when_sent);

        mTxNoBetBalance = rootView.findViewById(R.id.tx_no_bet_balance);
        boolean canBet = (((float)walletManager.getWallet().getBalance())/UNIT_MULTIPLIER) > getContext().getResources().getInteger(R.integer.min_bet_amount);

        if (canBet) {
            mTxHomeOdds.setOnClickListener(this);
            mTxDrawOdds.setOnClickListener(this);
            mTxAwayOdds.setOnClickListener(this);
            mTxSpreadHomeOdds.setOnClickListener(this);
            mTxSpreadAwayOdds.setOnClickListener(this);
            mTxTotalOverOdds.setOnClickListener(this);
            mTxTotalUnderOdds.setOnClickListener(this);
        }
        else {
            mTxNoBetBalance.setVisibility(View.VISIBLE);
            mTxNoBetBalance.setText(String.format("Minimum bet is %d plus fees", getContext().getResources().getInteger(R.integer.min_bet_amount)));
        }
        mTxStatus = rootView.findViewById(R.id.tx_status);
        mTxDate = rootView.findViewById(R.id.tx_date);
        mTxLastUpdated = rootView.findViewById(R.id.tx_last_updated);
        mTxLastDate = rootView.findViewById(R.id.tx_last_date);
        mToFrom = rootView.findViewById(R.id.tx_to_from);
        mToFromAddress = rootView.findViewById(R.id.tx_to_from_address);
        //mMemoText = rootView.findViewById(R.id.memo);
        mStartingBalance = rootView.findViewById(R.id.tx_starting_balance);
        mEndingBalance = rootView.findViewById(R.id.tx_ending_balance);
        mExchangeRate = rootView.findViewById(R.id.exchange_rate);
        mConfirmedInBlock = rootView.findViewById(R.id.confirmed_in_block_number);
        mTransactionId = rootView.findViewById(R.id.transaction_id);
        mShowHide = rootView.findViewById(R.id.show_hide_details);
        mDetailsContainer = rootView.findViewById(R.id.details_container);
        mDetailsLayout = rootView.findViewById(R.id.details_layout);
        mCloseButton = rootView.findViewById(R.id.close_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mBetSliderContainer = rootView.findViewById(R.id.bet_container);
        mBetSliderLayout = rootView.findViewById(R.id.bet_layout);
        mAcceptBet = rootView.findViewById(R.id.bet_send);
        mAcceptBet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AcceptBet();
            }
        });
        mCancelBet= rootView.findViewById(R.id.bet_cancel);
        mCancelBet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CancelBet();
            }
        });
        mPotentialReward = rootView.findViewById(R.id.tx_potential);

        mShowHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mDetailsShowing) {
                    mDetailsContainer.setVisibility(View.VISIBLE);
                    mDetailsShowing = true;
                    mShowHide.setText("Hide Details");
                } else {
                    mDetailsContainer.setVisibility(View.GONE);
                    mDetailsShowing = false;
                    mShowHide.setText("Show Details");
                }
            }
        });

        updateUi();
        return rootView;
    }

    protected void CreateWebFragment(Activity app, String theUrl)   {
        FragmentWebView fragmentWebView = (FragmentWebView) app.getFragmentManager().findFragmentByTag(FragmentWebView.class.getName());

        if(fragmentWebView != null && fragmentWebView.isAdded()){
            Log.e(TAG, "showEventDetails: Already showing");

       //     return;
        }


        fragmentWebView = new FragmentWebView();
        Bundle args = new Bundle();
        args.putString("url", theUrl);
        fragmentWebView.setArguments(args);

        fragmentWebView.show( app.getFragmentManager(), FragmentWebView.class.getName());
        app.getFragmentManager().beginTransaction()
                .addToBackStack(null)
                .commit();

        /*app.getFragmentManager().beginTransaction()
                .replace( ((ViewGroup)getView().getParent()).getId(), fragmentWebView, FragmentWebView.class.getName())
                //.add(fragmentWebView, FragmentWebView.class.getName())
                .addToBackStack(null)
                .commit();
*/
        //app.getFragmentManager().beginTransaction().show(fragmentWebView).commit();
        //app.getFragmentManager().beginTransaction().hide(this).commit();
    }
    protected void updateSeekBar( int amount, int posX ) {
        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        BigDecimal cryptoAmount = new BigDecimal((long)amount*UNIT_MULTIPLIER);
        BigDecimal fiatAmount = walletManager.getFiatForSmallestCrypto(getActivity(), cryptoAmount.abs(), null);
        String fiatAmountStr = CurrencyUtils.getFormattedAmount(getContext(), BRSharedPrefs.getPreferredFiatIso(getContext()), fiatAmount);

        if (mCurrentSelectedBetOption!=null) {
            String oddTx = ((BRText)mCurrentSelectedBetOption).getText().toString();
            long stake;
            Float odds;
            try {
                odds = Float.parseFloat( oddTx );
                stake = amount;
                setRewardAmount(stake, odds);
            }
            catch (NumberFormatException e) {
                mPotentialReward.setText("---");
            }
        }
        mTxCurrency.setText(" WGR (" + fiatAmountStr +")" );

    }

    protected void setRewardAmount(long stake, float odds)  {
        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());

        try {
            boolean oddsSetting = BRSharedPrefs.getFeatureEnabled(WagerrApp.getBreadContext(), BetSettings.FEATURE_DISPLAY_ODDS, false);
            boolean americanSetting = BRSharedPrefs.getFeatureEnabled(WagerrApp.getBreadContext(), BetSettings.FEATURE_DISPLAY_AMERICAN, false);

            if (americanSetting)    {
                odds = AmericanToDecimal( odds );
            }
            long rewardAmount = stake + ((oddsSetting)?(long)((stake * (odds - 1)) * 0.94):(long)(stake * (odds-1)));
            BigDecimal rewardCryptoAmount = new BigDecimal((long)rewardAmount*UNIT_MULTIPLIER);
            BigDecimal rewardFiatAmount = walletManager.getFiatForSmallestCrypto(getActivity(), rewardCryptoAmount.abs(), null);
            String rewardFiatAmountStr = CurrencyUtils.getFormattedAmount(getContext(), BRSharedPrefs.getPreferredFiatIso(getContext()), rewardFiatAmount);
            mPotentialReward.setText("" + rewardAmount + "  WGR (" + rewardFiatAmountStr +")" );
        }
        catch (NumberFormatException e) {
            mPotentialReward.setText("---");
        }
    }

    public float AmericanToDecimal(float odd)   {
        if ( odd > 0 )  {
            return (float)Math.round( ((odd/100) + 1)*100 )/100;
        }
        else    {
            return (float)Math.round( ((100/-odd) + 1)*100 )/100;
        }
    }

    protected void AcceptBet()  {
        mTxAmount.clearFocus();
        int min = getContext().getResources().getInteger(R.integer.min_bet_amount);
        BetEntity.BetTxType betType = (mTransaction.getType()== BetEventEntity.BetTxType.PEERLESS)? BetEntity.BetTxType.PEERLESS:BetEntity.BetTxType.CHAIN_LOTTO;
        long amount = (seekBar.getProgress() + min) * UNIT_MULTIPLIER;
        //amount = 1000000;   // 0.01 WGR for testing
        Date date = new Date();
        long timeStampLimit = (date.getTime()/1000) + WalletWagerrManager.BET_CUTTOFF_SECONDS;
        if (mTransaction.getEventTimestamp()<timeStampLimit)    {
            BRDialog.showCustomDialog(getContext(), "Error", "Event is closed for betting", getContext().getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismiss();
                }
            }, null, null, 0);
        }
        else {
            final BaseWalletManager wallet = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
            BRCoreTransaction tx = wallet.getWallet().createBetTransaction(amount, betType.getNumber(), (int) mTransaction.getEventID(), getSelectedOutcome());

            CryptoRequest item = new CryptoRequest(tx, null, false, "", "", new BigDecimal(amount));
            SendManager.sendTransaction(getActivity(), item, wallet);
            //BRAnimator.showFragmentEvent = mTransaction;
            dismiss();  // close fragment
        }
    }

    protected void CancelBet()  {
        mTxAmount.clearFocus();
        BRText txPrev = (mCurrentSelectedBetOption!=null)?(BRText) mCurrentSelectedBetOption:null;
        if (txPrev!=null)   {
            txPrev.setTextSize(NORMAL_SIZE);
        }
        mBetSliderContainer.setVisibility(View.GONE);
        mCurrentSelectedBetOption = null;
    }

    protected int getSelectedOutcome() {
        BetEntity.BetOutcome outcome = BetEntity.BetOutcome.UNKNOWN;
        if (mCurrentSelectedBetOption!=null) {
            switch (mCurrentSelectedBetOption.getId()) {
                case R.id.tx_home_odds:
                    outcome = BetEntity.BetOutcome.MONEY_LINE_HOME_WIN;
                    break;

                case R.id.tx_draw_odds:
                    outcome = BetEntity.BetOutcome.MONEY_LINE_DRAW;
                    break;

                case R.id.tx_away_odds:
                    outcome = BetEntity.BetOutcome.MONEY_LINE_AWAY_WIN;
                    break;

                case R.id.tx_spreads_home_odds:
                    outcome = BetEntity.BetOutcome.SPREADS_HOME;
                    break;

                case R.id.tx_spreads_away_odds:
                    outcome = BetEntity.BetOutcome.SPREADS_AWAY;
                    break;

                case R.id.tx_over_odds:
                    outcome = BetEntity.BetOutcome.TOTAL_OVER;
                    break;

                case R.id.tx_under_odds:
                    outcome = BetEntity.BetOutcome.TOTAL_UNDER;
                    break;
            }
        }
        return outcome.getNumber();
    }

    @Override
    public void onClick(View v) {
        int idContainer = 0;
        BRText txCur = (BRText)v;
        BRText txPrev = (mCurrentSelectedBetOption!=null)?(BRText) mCurrentSelectedBetOption:null;

        if ("N/A".equals(txCur.getText().toString()) )    {
            return;
        }

        if ( (txPrev==null || txCur.getId()!=txPrev.getId()) ) {
            txCur.setTextSize(BIG_SIZE);
            mBetSliderContainer.setVisibility(View.VISIBLE);
        }

        mMainLayout.removeAllViews();
        mMainLayout.addView(mMoneyLineLayout);

        switch(v.getId()) {
            case R.id.tx_home_odds:
            case R.id.tx_draw_odds:
            case R.id.tx_away_odds:
                mMainLayout.addView(mBetSliderLayout);
                mMainLayout.addView(mSpreadsLayout);
                mMainLayout.addView(mTotalsLayout);
                break;

            case R.id.tx_spreads_home_odds:
            case R.id.tx_spreads_away_odds:
                mMainLayout.addView(mSpreadsLayout);
                mMainLayout.addView(mBetSliderLayout);
                mMainLayout.addView(mTotalsLayout);
                break;

            case R.id.tx_over_odds:
            case R.id.tx_under_odds:
                mMainLayout.addView(mSpreadsLayout);
                mMainLayout.addView(mTotalsLayout);
                mMainLayout.addView(mBetSliderLayout);
                break;
        }
        mMainLayout.addView(mDetailsLayout);

        if (txPrev!=null)   {
            txPrev.setTextSize(NORMAL_SIZE);
        }

        if ( txPrev==null || txCur.getId()!=txPrev.getId()) {
            mCurrentSelectedBetOption = v;
            int min = getContext().getResources().getInteger(R.integer.min_bet_amount);
            int coinAmount = seekBar.getProgress()+min;
            updateSeekBar(coinAmount,0);
        }
        else {
            mBetSliderContainer.setVisibility(View.GONE);
            mCurrentSelectedBetOption = null;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void setTransaction(EventTxUiHolder item) {
        this.mTransaction = item;
    }

    public EventTxUiHolder getTransaction() {
        return this.mTransaction;
    }

    public void updateUi() {

        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        // Set mTransction fields
        if (mTransaction != null) {

            boolean sent = true; // mTransaction.getSent() > 0;
            String amountWhenSent;
            String amountNow;
            String exchangeRateFormatted;

            /*
            //user prefers crypto (or fiat)
            boolean isCryptoPreferred = BRSharedPrefs.isCryptoPreferred(getActivity());
            String cryptoIso = walletManager.getIso(getActivity());
            String fiatIso = BRSharedPrefs.getPreferredFiatIso(getContext());

            String iso = isCryptoPreferred ? cryptoIso : fiatIso;

            BigDecimal cryptoAmount = new BigDecimal(0);

            BigDecimal fiatAmountNow = walletManager.getFiatForSmallestCrypto(getActivity(), cryptoAmount.abs(), null);

            BigDecimal fiatAmountWhenSent;
            fiatAmountWhenSent = new BigDecimal(0);
            amountWhenSent = CurrencyUtils.getFormattedAmount(getActivity(), fiatIso, fiatAmountWhenSent);//always fiat amount

            amountNow = CurrencyUtils.getFormattedAmount(getActivity(), fiatIso, fiatAmountNow);//always fiat amount

            mAmountWhenSent.setText(amountWhenSent);
            mAmountNow.setText(amountNow);

            //BigDecimal tmpStartingBalance = new BigDecimal(mTransaction.getBalanceAfterTx()).subtract(cryptoAmount.abs()).subtract(new BigDecimal(mTransaction.getFee()).abs());

            //BigDecimal startingBalance = isCryptoPreferred ? walletManager.getCryptoForSmallestCrypto(getActivity(), tmpStartingBalance) : walletManager.getFiatForSmallestCrypto(getActivity(), tmpStartingBalance, null);

            //BigDecimal endingBalance = isCryptoPreferred ? walletManager.getCryptoForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx())) : walletManager.getFiatForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx()), null);

            //mStartingBalance.setText(CurrencyUtils.getFormattedAmount(getActivity(), iso, startingBalance == null ? null : startingBalance.abs()));
            //mEndingBalance.setText(CurrencyUtils.getFormattedAmount(getActivity(), iso, endingBalance == null ? null : endingBalance.abs()));
*/
            EventTxUiHolder item = mTransaction;
            mTxHomeTeam.setText( (item.getTxHomeTeam()!=null)?item.getTxHomeTeam():"Home N/A" );
            mTxAwayTeam.setText( (item.getTxAwayTeam()!=null)?item.getTxAwayTeam():"Away N/A" );
            mTxHomeOdds.setText( (item.getHomeOdds()>0)?item.getTxHomeOdds():"N/A" );

            String strDraw = (item.getDrawOdds()>0)?item.getTxDrawOdds():"N/A";
            mTxDrawOdds.setText( strDraw );
            if ( "N/A".equals(strDraw)) {
                mTxDrawOdds.setVisibility(View.INVISIBLE);
            }
            mTxAwayOdds.setText( (item.getAwayOdds()>0)?item.getTxAwayOdds():"N/A" );
            rlLastContainer = mMoneyLineContainer;      // default...

            bHasSpreads = (item.getSpreadPoints()>0);
            if (bHasSpreads) {
                String txSpreadFormat = (item.getHomeOdds()>item.getAwayOdds())?"+%s/-%s":"-%s/+%s";
                String txSpreadPoints = String.format(txSpreadFormat, item.getTxSpreadPoints(), item.getTxSpreadPoints() );
                mTxSpreadPoints.setText(txSpreadPoints);
                mTxSpreadHomeOdds.setText((item.getSpreadHomeOdds() > 0) ? item.getTxSpreadHomeOdds() : "N/A");
                mTxSpreadAwayOdds.setText((item.getSpreadAwayOdds() > 0) ? item.getTxSpreadAwayOdds() : "N/A");
                mSpreadsContainer.setVisibility(View.VISIBLE);
                rlLastContainer = mSpreadsContainer;
            }
            else    {
                mSpreadsContainer.setVisibility(View.GONE);
            }

            bHasTotals = (item.getTotalPoints()>0);
            if (bHasTotals) {
                mTxTotalPoints.setText(item.getTxTotalPoints());
                mTxTotalOverOdds.setText((item.getOverOdds() > 0) ? item.getTxOverOdds() : "N/A");
                mTxTotalUnderOdds.setText((item.getUnderOdds() > 0) ? item.getTxUnderOdds() : "N/A");
                mTotalsContainer.setVisibility(View.VISIBLE);
                rlLastContainer = mTotalsContainer;
            }
            else    {
                mTotalsContainer.setVisibility(View.GONE);
            }

/*
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, rlLastContainer.getId());
            mShowHide.setLayoutParams(params);
*/
            //String homeScore = (item.getHomeScore()!=-1)?String.valueOf(item.getHomeScore()):"---";
            //mTxHomeResult.setText( homeScore );

            //String awayScore = (item.getAwayScore()!=-1)?String.valueOf(item.getAwayScore()):"---";
            //mTxAwayResult.setText( awayScore );

            mTxEventHeader.setText(item.getTxEventHeader());
            mTxEventDate.setText( item.getTxEventDate() );
            mTxEventId.setText( String.format("Event #%d",item.getEventID()) );

            // timestamp is 0 if it's not confirmed in a block yet so make it now
            mTxDate.setText(BRDateUtil.getEventDate(mTransaction.getTimestamp() == 0 ? System.currentTimeMillis() : (mTransaction.getTimestamp() * 1000)));
            mTxLastDate.setText(BRDateUtil.getEventDate(mTransaction.getLastUpdated() == 0 ? System.currentTimeMillis() : (mTransaction.getLastUpdated() * 1000)));

            // Set the transaction id
            mTransactionId.setText(mTransaction.getTxHash());

            // Allow the transaction id to be copy-able
            mTransactionId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // Get the default color based on theme
                    final int color = mTransactionId.getCurrentTextColor();

                    mTransactionId.setTextColor(getContext().getColor(R.color.light_gray));
                    String id = mTransaction.getTxHash();
                    BRClipboardManager.putClipboard(getContext(), id);
                    //Toast.makeText(getContext(), getString(R.string.Receive_copied), Toast.LENGTH_LONG).show();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mTransactionId.setTextColor(color);

                        }
                    }, 200);
                }
            });

            // Set the transaction block number
            String confirmedBlock = String.valueOf(mTransaction.getBlockheight());
            if ( confirmedBlock.equals(BRConstants.INT23_MAX))  confirmedBlock = "Unconfirmed";
            mConfirmedInBlock.setText(confirmedBlock);
/*
            mToFrom.setText(sent ? "To " : "Via ");

            mToFromAddress.setText("to"); //showing only the destination address

            // Allow the to/from address to be copyable
            mToFromAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // Get the default color based on theme
                    final int color = mToFromAddress.getCurrentTextColor();

                    mToFromAddress.setTextColor(getContext().getColor(R.color.light_gray));
                    String address = mToFromAddress.getText().toString();
                    BRClipboardManager.putClipboard(getContext(), address);
                    Toast.makeText(getContext(), getString(R.string.Receive_copied), Toast.LENGTH_LONG).show();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mToFromAddress.setTextColor(color);

                        }
                    }, 200);


                }
            });

            mTxAmount.setText(CurrencyUtils.getFormattedAmount(getActivity(), walletManager.getIso(getActivity()), cryptoAmount));//this is always crypto amount


            if (!sent)
                mTxAmount.setTextColor(getContext().getColor(R.color.transaction_amount_received_color));

            // timestamp is 0 if it's not confirmed in a block yet so make it now
            mTxDate.setText(BRDateUtil.getLongDate(mTransaction.getTimestamp() == 0 ? System.currentTimeMillis() : (mTransaction.getTimestamp() * 1000)));


*/
        } else {

            Toast.makeText(getContext(), "Error getting transaction data", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler = new Handler();
        startRepeatingTask();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRepeatingTask();
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }
    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }
}

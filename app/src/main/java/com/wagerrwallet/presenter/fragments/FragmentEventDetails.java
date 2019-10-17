package com.wagerrwallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
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
import com.wagerrwallet.core.BRCoreTransaction;
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
import com.wagerrwallet.tools.util.BRDateUtil;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

import java.math.BigDecimal;
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
    private BRText mPotentialReward;


    // layout management
    private boolean bHasMoneyLine = true;
    private boolean bHasSpreads = false;
    private boolean bHasTotals = false;
    LinearLayout rlToPutBelowBetSlider = null;
    LinearLayout rlToPutBelowPrevious = null;
    RelativeLayout rlLastContainer = null;

    boolean mDetailsShowing = false;

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

        mMainLayout = rootView.findViewById(R.id.dynamic_container);
        mTxEventHeader = rootView.findViewById(R.id.tx_eventheader);
        mTxEventDate= rootView.findViewById(R.id.tx_eventdate);
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

        final BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());

        seekBar = rootView.findViewById(R.id.bet_seekBar);
        updateSeekBar(getContext().getResources().getInteger(R.integer.min_bet_amount), 0);
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
                   //textView.setY(100); just added a value set this properly using screen with height aspect ratio , if you do not set it by default it will be there below seek bar
               }
               @Override
               public void onStartTrackingTouch(SeekBar seekBar) {
                   //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
               }

               @Override
               public void onStopTrackingTouch(SeekBar seekBar) {
                   //textView.setText("Covered: " + progress + "/" + seekBar.getMax());
                   //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
               }
        });

        mTxAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                int value = getContext().getResources().getInteger(R.integer.min_bet_amount);
                int minvalue = value;
                Float fValue = 0.0f;
                if (!hasFocus) {
                    try {
                        fValue = Float.parseFloat(mTxAmount.getText().toString());
                        value = Math.max( minvalue, fValue.intValue() );
                        mTxAmount.setText(String.valueOf(value));
                    }
                    catch (NumberFormatException e)     {
                    }
                    seekBar.setProgress(value-minvalue);
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
        boolean canBet = ((int)(walletManager.getWallet().getBalance()/UNIT_MULTIPLIER) > getContext().getResources().getInteger(R.integer.min_bet_amount));

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
            mTxNoBetBalance.setText(String.format("Minimum bet is %d", getContext().getResources().getInteger(R.integer.min_bet_amount)));
        }
        mTxStatus = rootView.findViewById(R.id.tx_status);
        mTxDate = rootView.findViewById(R.id.tx_date);
        mTxLastUpdated = rootView.findViewById(R.id.tx_last_updated);
        mTxLastDate = rootView.findViewById(R.id.tx_last_date);
        mToFrom = rootView.findViewById(R.id.tx_to_from);
        mToFromAddress = rootView.findViewById(R.id.tx_to_from_address);
        mMemoText = rootView.findViewById(R.id.memo);
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
                long rewardAmount = stake + (long)((stake * (odds - 1)) * 0.94);
                BigDecimal rewardCryptoAmount = new BigDecimal((long)rewardAmount*UNIT_MULTIPLIER);
                BigDecimal rewardFiatAmount = walletManager.getFiatForSmallestCrypto(getActivity(), rewardCryptoAmount.abs(), null);
                String rewardFiatAmountStr = CurrencyUtils.getFormattedAmount(getContext(), BRSharedPrefs.getPreferredFiatIso(getContext()), rewardFiatAmount);
                mPotentialReward.setText("" + rewardAmount + "  WGR (" + rewardFiatAmountStr +")" );
            }
            catch (NumberFormatException e) {
                mPotentialReward.setText("---");
            }
        }
        mTxAmount.setText("" + amount);
        mTxCurrency.setText(" WGR (" + fiatAmountStr +")" );

        //mTxAmount.setX(seekBar.getX() + posX);
    }

    protected void AcceptBet()  {
        int min = getContext().getResources().getInteger(R.integer.min_bet_amount);
        BetEntity.BetTxType betType = (mTransaction.getType()== BetEventEntity.BetTxType.PEERLESS)? BetEntity.BetTxType.PEERLESS:BetEntity.BetTxType.CHAIN_LOTTO;
        long amount = (seekBar.getProgress() + min) * UNIT_MULTIPLIER;
        //amount = 1000000;   // 0.01 WGR for testing
        Date date = new Date();
        long timeStampLimit = (date.getTime()/1000) - WalletWagerrManager.BET_CUTTOFF_SECONDS;
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
            BRAnimator.showFragmentEvent = mTransaction;
            dismiss();  // close fragment
        }
    }

    protected void CancelBet()  {
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
            mTxHomeOdds.setText( (item.getTxHomeOdds()!=null)?item.getTxHomeOdds():"N/A" );
            mTxDrawOdds.setText( (item.getTxDrawOdds()!=null)?item.getTxDrawOdds():"N/A" );
            mTxAwayOdds.setText( (item.getTxAwayOdds()!=null)?item.getTxAwayOdds():"N/A" );
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

            bHasTotals = (item.getTotalPoints()>0);
            if (bHasTotals) {
                mTxTotalPoints.setText(item.getTxTotalPoints());
                mTxTotalOverOdds.setText((item.getOverOdds() > 0) ? item.getTxOverOdds() : "N/A");
                mTxTotalUnderOdds.setText((item.getUnderOdds() > 0) ? item.getTxUnderOdds() : "N/A");
                mTotalsContainer.setVisibility(View.VISIBLE);
                rlLastContainer = mTotalsContainer;
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
                    Toast.makeText(getContext(), getString(R.string.Receive_copied), Toast.LENGTH_LONG).show();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mTransactionId.setTextColor(color);

                        }
                    }, 200);
                }
            });

            // Set the transaction block number
            mConfirmedInBlock.setText(String.valueOf(mTransaction.getBlockheight()));


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

    }
}

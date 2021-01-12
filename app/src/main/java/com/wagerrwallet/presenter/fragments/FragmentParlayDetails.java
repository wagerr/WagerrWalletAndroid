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
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import com.wagerrwallet.R;
import com.wagerrwallet.WagerrApp;
import com.wagerrwallet.core.BRCoreTransaction;
import com.wagerrwallet.presenter.activities.settings.BetSettings;
import com.wagerrwallet.presenter.customviews.BRDialogView;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.BetEntity;
import com.wagerrwallet.presenter.entities.BetEventEntity;
import com.wagerrwallet.presenter.entities.CryptoRequest;
import com.wagerrwallet.presenter.entities.EventTxUiHolder;
import com.wagerrwallet.presenter.entities.ParlayBetEntity;
import com.wagerrwallet.presenter.entities.ParlayLegEntity;
import com.wagerrwallet.presenter.interfaces.WagerrParlayLegNotification;
import com.wagerrwallet.tools.animation.BRAnimator;
import com.wagerrwallet.tools.animation.BRDialog;
import com.wagerrwallet.tools.manager.BRClipboardManager;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.manager.SendManager;
import com.wagerrwallet.tools.sqlite.BetEventTxDataStore;
import com.wagerrwallet.tools.util.BRConstants;
import com.wagerrwallet.tools.util.BRDateUtil;
import com.wagerrwallet.tools.util.CurrencyUtils;
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

public class FragmentParlayDetails extends DialogFragment  {

    private static final String EXTRA_TX_ITEM = "event_item";
    private static final String TAG = "FragmentParlayDetails";
    private static final int NORMAL_SIZE = 24;
    private static final int BIG_SIZE = 32;
    private static final long UNIT_MULTIPLIER = 100000000L;     // so far in full WGR units

    private ParlayBetEntity mTransaction;

    private RelativeLayout mLegLayout[] = new RelativeLayout[5];
    private BRText mTxEventHeader[] = new BRText[5];
    private BRText mTxEventDate[] = new BRText[5];
    private BRText mTxEventId[] = new BRText[5];
    private BRText mTxHomeTeam[] = new BRText[5];
    private BRText mTxAwayTeam[] = new BRText[5];

    private BRText mTxOdds[] = new BRText[5];
    private BRText mTxOutcome[] = new BRText[5];
    private BRText mTxOutcomePoints[] = new BRText[5];
    private ImageButton mRemoveLeg[] = new ImageButton[5];

    private SeekBar seekBar;
    private EditText mTxAmount;
    private BRText mTxCurrency;

    private ImageButton faq;
    private ImageButton mCloseButton;
    private RelativeLayout mBetContainer;
    private RelativeLayout mBetWarningContainer;

    private ImageButton mAcceptBet;
    private ImageButton mCancelBet;

    private BRText mPotentialReward;
    private BRText mTotalOdd;

    private boolean bBarSliding = false;

    private int mInterval = 3000;
    private Handler mHandler;

    public WagerrParlayLegNotification mListener;

    // refresh to update odds
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if (mTransaction==null) {
                    final BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
                    mTransaction = ((WalletWagerrManager)walletManager).getParlay();
                }

                if (didLegsExpire( true )) {
                    updateUi();
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    protected boolean didLegsExpire( boolean bOrChange )  {
        boolean redraw = false;
        for(int i = 0; i < mTransaction.getLegCount(); i++) {
            ParlayLegEntity leg = mTransaction.get(i);
            if (!leg.isValid()) {
                mTransaction.removeAt(i);
                redraw = true;
            }
            else    {
                BetEventEntity betEvent = BetEventTxDataStore.getInstance(getActivity()).getTransactionByEventId(getActivity(), "wgr", leg.getEvent().getEventID());
                if ( leg.updateEvent( (EventTxUiHolder)betEvent ) && bOrChange )    {
                    redraw = true;
                }
            }
        }
        if (redraw) {
            mListener.onLegChanged();
        }
        return redraw;
    }

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

        View rootView = inflater.inflate(R.layout.parlay_details, container, false);
        final BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());

        if (mTransaction==null) {
            mTransaction = ((WalletWagerrManager) walletManager).getParlay();
        }

        faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(getActivity(), BRConstants.betSlip);
            }
        });

        mCloseButton = rootView.findViewById( R.id.close_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mBetContainer = rootView.findViewById(R.id.bet_container);
        mBetWarningContainer = rootView.findViewById(R.id.bet_container_warning);
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
        mTotalOdd = rootView.findViewById(R.id.tx_odd_total);

        mLegLayout[0] = rootView.findViewById(R.id.layout_leg_1);
        mLegLayout[1] = rootView.findViewById(R.id.layout_leg_2);
        mLegLayout[2] = rootView.findViewById(R.id.layout_leg_3);
        mLegLayout[3] = rootView.findViewById(R.id.layout_leg_4);
        mLegLayout[4] = rootView.findViewById(R.id.layout_leg_5);

        int nLegs = mTransaction.getLegCount();
        // leg 1
        if (nLegs>0) {
            mLegLayout[0].setVisibility(View.VISIBLE);
            mTxEventHeader[0] = rootView.findViewById(R.id.tx_eventheader_1);
            mTxEventDate[0] = rootView.findViewById(R.id.tx_eventdate_1);
            /* mTxEventId= rootView.findViewById(R.id.tx_eventid);
            final long evID = mTransaction.getEventID();
            mTxEventId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://explorer.wagerr.com/#/bet/event/%d", evID)));
                    startActivity(browserIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });
             */
            mTxHomeTeam[0] = rootView.findViewById(R.id.tx_home_1);
            mTxAwayTeam[0] = rootView.findViewById(R.id.tx_away_1);
            mTxOutcome[0] = rootView.findViewById(R.id.tx_outcome_1);
            mTxOutcomePoints[0] = rootView.findViewById(R.id.tx_outcome_1b);
            mTxOdds[0] = rootView.findViewById(R.id.tx_odd_1);
            mRemoveLeg[0] = rootView.findViewById(R.id.leg_remove_1);
        }

        if (nLegs>1) {
            mLegLayout[1].setVisibility(View.VISIBLE);
            mTxEventHeader[1] = rootView.findViewById(R.id.tx_eventheader_2);
            mTxEventDate[1] = rootView.findViewById(R.id.tx_eventdate_2);
            /* mTxEventId= rootView.findViewById(R.id.tx_eventid);
            final long evID = mTransaction.getEventID();
            mTxEventId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://explorer.wagerr.com/#/bet/event/%d", evID)));
                    startActivity(browserIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });
             */
            mTxHomeTeam[1] = rootView.findViewById(R.id.tx_home_2);
            mTxAwayTeam[1] = rootView.findViewById(R.id.tx_away_2);
            mTxOutcome[1] = rootView.findViewById(R.id.tx_outcome_2);
            mTxOutcomePoints[1] = rootView.findViewById(R.id.tx_outcome_2b);
            mTxOdds[1] = rootView.findViewById(R.id.tx_odd_2);
            mRemoveLeg[1] = rootView.findViewById(R.id.leg_remove_2);
        }

        if (nLegs>2) {
            mLegLayout[2].setVisibility(View.VISIBLE);
            mTxEventHeader[2] = rootView.findViewById(R.id.tx_eventheader_3);
            mTxEventDate[2] = rootView.findViewById(R.id.tx_eventdate_3);
            /* mTxEventId= rootView.findViewById(R.id.tx_eventid);
            final long evID = mTransaction.getEventID();
            mTxEventId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://explorer.wagerr.com/#/bet/event/%d", evID)));
                    startActivity(browserIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });
             */
            mTxHomeTeam[2] = rootView.findViewById(R.id.tx_home_3);
            mTxAwayTeam[2] = rootView.findViewById(R.id.tx_away_3);
            mTxOutcome[2] = rootView.findViewById(R.id.tx_outcome_3);
            mTxOutcomePoints[2] = rootView.findViewById(R.id.tx_outcome_3b);
            mTxOdds[2] = rootView.findViewById(R.id.tx_odd_3);
            mRemoveLeg[2] = rootView.findViewById(R.id.leg_remove_3);
        }

        if (nLegs>3) {
            mLegLayout[3].setVisibility(View.VISIBLE);
            mTxEventHeader[3] = rootView.findViewById(R.id.tx_eventheader_4);
            mTxEventDate[3] = rootView.findViewById(R.id.tx_eventdate_4);
            /* mTxEventId= rootView.findViewById(R.id.tx_eventid);
            final long evID = mTransaction.getEventID();
            mTxEventId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://explorer.wagerr.com/#/bet/event/%d", evID)));
                    startActivity(browserIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });
             */
            mTxHomeTeam[3] = rootView.findViewById(R.id.tx_home_4);
            mTxAwayTeam[3] = rootView.findViewById(R.id.tx_away_4);
            mTxOutcome[3] = rootView.findViewById(R.id.tx_outcome_4);
            mTxOutcomePoints[3] = rootView.findViewById(R.id.tx_outcome_4b);
            mTxOdds[3] = rootView.findViewById(R.id.tx_odd_4);
            mRemoveLeg[3] = rootView.findViewById(R.id.leg_remove_4);
        }

        if (nLegs>4) {
            mLegLayout[4].setVisibility(View.VISIBLE);
            mTxEventHeader[4] = rootView.findViewById(R.id.tx_eventheader_5);
            mTxEventDate[4] = rootView.findViewById(R.id.tx_eventdate_5);
            /* mTxEventId= rootView.findViewById(R.id.tx_eventid);
            final long evID = mTransaction.getEventID();
            mTxEventId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://explorer.wagerr.com/#/bet/event/%d", evID)));
                    startActivity(browserIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });
             */
            mTxHomeTeam[4] = rootView.findViewById(R.id.tx_home_5);
            mTxAwayTeam[4] = rootView.findViewById(R.id.tx_away_5);
            mTxOutcome[4] = rootView.findViewById(R.id.tx_outcome_5);
            mTxOutcomePoints[4] = rootView.findViewById(R.id.tx_outcome_5b);
            mTxOdds[4] = rootView.findViewById(R.id.tx_odd_5);
            mRemoveLeg[4] = rootView.findViewById(R.id.leg_remove_5);
        }

        for(int i = 0; i < mTransaction.getLegCount(); i++) {
            final int n = i;
            mRemoveLeg[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTransaction.removeAt(n);
                    mListener.onLegChanged();
                    updateUi();
                }
            });
        }

        mTxAmount = rootView.findViewById(R.id.tx_amount);
        mTxCurrency = rootView.findViewById(R.id.tx_currency);
        seekBar = rootView.findViewById(R.id.bet_seekBar);
        int min = getContext().getResources().getInteger(R.integer.min_bet_amount);
        int max = Math.min( (int)(walletManager.getWallet().getBalance()/UNIT_MULTIPLIER),
                getContext().getResources().getInteger(R.integer.max_bet_amount_parlay));
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
                           getContext().getResources().getInteger(R.integer.max_bet_amount_parlay));
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
                int maxvalue = Math.min(getContext().getResources().getInteger(R.integer.max_bet_amount_parlay), balance );

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
                int value = getContext().getResources().getInteger(R.integer.min_bet_amount);
                int minvalue = value;
                Float fValue = 0.0f;
                try {
                    fValue = Float.parseFloat(mTxAmount.getText().toString());
                    value = Math.max(minvalue, fValue.intValue());
                } catch (NumberFormatException e) {
                }
                setRewardAmount(value);
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

        updateUi();
        return rootView;
    }

    protected void updateSeekBar( int amount, int posX ) {
        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        BigDecimal cryptoAmount = new BigDecimal((long)amount*UNIT_MULTIPLIER);
        BigDecimal fiatAmount = walletManager.getFiatForSmallestCrypto(getActivity(), cryptoAmount.abs(), null);
        String fiatAmountStr = CurrencyUtils.getFormattedAmount(getContext(), BRSharedPrefs.getPreferredFiatIso(getContext()), fiatAmount);

        long stake;
        try {
            stake = amount;
            setRewardAmount(stake);
        }
        catch (NumberFormatException e) {
            mPotentialReward.setText("---");
        }
        mTxCurrency.setText(" WGR (" + fiatAmountStr +")" );

    }

    // combined odd is Product(individual leg odd) (in numeric format)
    // bEffective = true ignores setting
    protected float getCombinedOdd(boolean bEffective)   {
        float odds = 1;
        boolean settingOdds = BRSharedPrefs.getFeatureEnabled(WagerrApp.getBreadContext(), BetSettings.FEATURE_DISPLAY_ODDS, false);
        if (bEffective) {   // ignore setting
            settingOdds = false;
        }
        for(int i = 0; i < mTransaction.getLegCount(); i++) {
            ParlayLegEntity leg = mTransaction.get(i);
            float chainOdds = leg.getOdd() / BetEventEntity.ODDS_MULTIPLIER;
            float legodds = (settingOdds) ? chainOdds : (float)((chainOdds-1)*0.94)+1;
            odds *= legodds;
        }

        return odds;
    }

    protected void setRewardAmount(long stake)  {
        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());

        try {
            float odds = getCombinedOdd(true);
            long rewardAmount = stake + (long)(stake * (odds-1));
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
        BetEntity.BetTxType betType = BetEntity.BetTxType.PARLAY;
        long amount = (seekBar.getProgress() + min) * UNIT_MULTIPLIER;
        //amount = 1000000;   // 0.01 WGR for testing
        long arrEventID[] = new long[5];
        int arrOutcome[] = new int[5];

        if (didLegsExpire( false ))    {
            BRDialog.showCustomDialog(getContext(), "Error", "One or more event legs are closed for betting, list will refresh.", getContext().getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismiss();
                }
            }, null, null, 0);
            // refresh and cancel
            updateUi();
            return;
        }

        for(int i = 0; i < mTransaction.getLegCount(); i++) {
            ParlayLegEntity leg = mTransaction.get(i);
            arrEventID[i] = leg.getEvent().getEventID();
            arrOutcome[i] = leg.getOutcome().getNumber();
        }

        final BaseWalletManager wallet = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        BRCoreTransaction tx = wallet.getWallet().createParlayBetTransaction(amount, betType.getNumber(), mTransaction.getLegCount(),
                (int) arrEventID[0], (int) arrOutcome[0],
                (int) arrEventID[1], (int) arrOutcome[1],
                (int) arrEventID[2], (int) arrOutcome[2],
                (int) arrEventID[3], (int) arrOutcome[3],
                (int) arrEventID[4], (int) arrOutcome[4]
                );

        CryptoRequest item = new CryptoRequest(tx, null, false, "", "", new BigDecimal(amount));
        SendManager.sendTransaction(getActivity(), item, wallet);
        //BRAnimator.showFragmentEvent = mTransaction;
        mTransaction.clearLegs();
        mListener.onLegChanged();
        mListener.onSendParlayBet();
        dismiss();  // close fragment
    }

    protected void CancelBet()  {
        dismiss();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            if (mListener == null) {
                this.mListener = (WagerrParlayLegNotification) context;
            }
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement WagerrParlayLegNotification");
        }
    }

    public void updateUi() {

        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        // Set mTransction fields
        if (mTransaction != null) {
            int nLegCount = mTransaction.getLegCount();

            if (nLegCount==0)   {
                dismiss();
                return;
            }

            for(int i = 0; i < nLegCount; i++) {
                ParlayLegEntity leg = mTransaction.get(i);
                mTxEventHeader[i].setText( leg.getEvent().getTxEventHeader() );
                mTxEventDate[i].setText( leg.getEvent().getTxEventDate());
                mLegLayout[i].setVisibility(View.VISIBLE);
                mTxHomeTeam[i].setText( leg.getEvent().getTxHomeTeam() );
                mTxAwayTeam[i].setText( leg.getEvent().getTxAwayTeam() );
                mTxOutcome[i].setText( leg.getOutcome().toString() );
                if ( leg.getOutcome().hasPoints())  {
                    mTxOutcomePoints[i].setText( leg.getEvent().getPointsForOutcome(leg.getOutcome()));
                }
                else    {
                    mTxOutcomePoints[i].setVisibility(View.GONE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mTxOutcome[i].getLayoutParams();
                    params.setMargins(0,8,18,0);
                    mTxOutcome[i].setLayoutParams(params);
                }
                mTxOdds[i].setText( leg.getOddTx() );
                mTxOdds[i].setBackgroundColor( leg.getOddColor( getContext() ) );
            }

            // hide rest
            for(int i = mTransaction.getLegCount(); i < ParlayBetEntity.MAX_LEGS; i++) {
                mLegLayout[i].setVisibility(View.GONE);
            }

            mBetContainer.setVisibility( (nLegCount>1) ? View.VISIBLE : View.GONE);
            mBetWarningContainer.setVisibility( (nLegCount>1) ? View.GONE : View.VISIBLE );
            mTotalOdd.setText(  mTransaction.get(0).getEvent().getOddTx(getCombinedOdd(false)) );
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

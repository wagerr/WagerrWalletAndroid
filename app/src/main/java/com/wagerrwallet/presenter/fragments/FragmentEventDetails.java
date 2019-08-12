package com.wagerrwallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;
import com.wagerrwallet.R;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.CurrencyEntity;
import com.wagerrwallet.presenter.entities.EventTxUiHolder;
import com.wagerrwallet.presenter.entities.TxUiHolder;
import com.wagerrwallet.tools.manager.BRClipboardManager;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.util.BRDateUtil;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;

/**
 * Created by byfieldj on 2/26/18.
 * <p>
 * Reusable dialog fragment that display details about a particular transaction
 */

public class FragmentEventDetails extends DialogFragment {

    private static final String EXTRA_TX_ITEM = "event_item";
    private static final String TAG = "FragmentEventDetails";

    private EventTxUiHolder mTransaction;

    private BRText mTxEventHeader;
    private BRText mTxEventDate;
    private BRText mTxHomeTeam;
    private BRText mTxAwayTeam;
    private BRText mTxHomeResult;
    private BRText mTxAwayResult;
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

    private BRText mTxAmount;
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

    private ImageButton mCloseButton;
    private RelativeLayout mDetailsContainer;

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

        mTxEventHeader = rootView.findViewById(R.id.tx_eventheader);
        mTxEventDate= rootView.findViewById(R.id.tx_eventdate);
        mTxHomeTeam = rootView.findViewById(R.id.tx_home);
        mTxAwayTeam= rootView.findViewById(R.id.tx_away);
        mTxHomeResult = rootView.findViewById(R.id.tx_home_result);
        mTxAwayResult= rootView.findViewById(R.id.tx_away_result);
        mTxHomeOdds = rootView.findViewById(R.id.tx_home_odds);
        mTxDrawOdds= rootView.findViewById(R.id.tx_draw_odds);
        mTxAwayOdds= rootView.findViewById(R.id.tx_away_odds);
        mTxAmount = rootView.findViewById(R.id.tx_amount);

        seekBar = rootView.findViewById(R.id.bet_seekBar);
        seekBar.setMin(getContext().getResources().getInteger(R.integer.min_bet_amount));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
               @Override
               public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                   int val = (progress * (seekBar.getWidth() - 2 * seekBar.getThumbOffset())) / seekBar.getMax();
                   mTxAmount.setText("" + progress);
                   mTxAmount.setX(seekBar.getX() + val + seekBar.getThumbOffset() / 2);
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

        mTxSpreadPoints= rootView.findViewById(R.id.tx_spread_points);
        mTxSpreadHomeOdds = rootView.findViewById(R.id.tx_spreads_home_odds);
        mTxSpreadAwayOdds= rootView.findViewById(R.id.tx_spreads_away_odds);

        mTxTotalPoints = rootView.findViewById(R.id.tx_total_points);
        mTxTotalOverOdds= rootView.findViewById(R.id.tx_over_odds);
        mTxTotalUnderOdds= rootView.findViewById(R.id.tx_under_odds);

        mAmountNow = rootView.findViewById(R.id.amount_now);
        mAmountWhenSent = rootView.findViewById(R.id.amount_when_sent);

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
        mCloseButton = rootView.findViewById(R.id.close_button);

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void setTransaction(EventTxUiHolder item) {

        this.mTransaction = item;

    }

    private void updateUi() {

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
            mTxHomeOdds.setText( (item.getTxHomeOdds()!=null)?item.getTxHomeOdds():"H odd" );
            mTxDrawOdds.setText( (item.getTxDrawOdds()!=null)?item.getTxDrawOdds():"D odd" );
            mTxAwayOdds.setText( (item.getTxAwayOdds()!=null)?item.getTxAwayOdds():"A odd" );

            String homeScore = (item.getHomeScore()!=-1)?String.valueOf(item.getHomeScore()):"";
            mTxHomeResult.setText( homeScore );

            String awayScore = (item.getAwayScore()!=-1)?String.valueOf(item.getAwayScore()):"";
            mTxAwayResult.setText( awayScore );

            mTxEventHeader.setText(item.getTxEventHeader());
            mTxEventDate.setText( item.getTxEventDate() );

            // timestamp is 0 if it's not confirmed in a block yet so make it now
            mTxDate.setText(BRDateUtil.getLongDate(mTransaction.getTimestamp() == 0 ? System.currentTimeMillis() : (mTransaction.getTimestamp() * 1000)));
            mTxLastDate.setText(BRDateUtil.getLongDate(mTransaction.getLastUpdated() == 0 ? System.currentTimeMillis() : (mTransaction.getLastUpdated() * 1000)));

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

            int maxBetAmount = Math.min( (int)(walletManager.getWallet().getBalance()/100000000),
                                         getContext().getResources().getInteger(R.integer.max_bet_amount));
            seekBar.setMax(maxBetAmount);

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

package com.wagerrwallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.wagerrwallet.R;
import com.wagerrwallet.core.BRCoreTransaction;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.BRTransactionEntity;
import com.wagerrwallet.presenter.entities.BetEntity;
import com.wagerrwallet.presenter.entities.BetResultEntity;
import com.wagerrwallet.presenter.entities.CurrencyEntity;
import com.wagerrwallet.presenter.entities.EventTxUiHolder;
import com.wagerrwallet.presenter.entities.TxUiHolder;
import com.wagerrwallet.tools.crypto.WagerrOpCodeManager;
import com.wagerrwallet.tools.manager.BRClipboardManager;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.sqlite.BetEventTxDataStore;
import com.wagerrwallet.tools.sqlite.BetResultTxDataStore;
import com.wagerrwallet.tools.util.BRConstants;
import com.wagerrwallet.tools.util.BRDateUtil;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;

/**
 * Created by byfieldj on 2/26/18.
 * <p>
 * Reusable dialog fragment that display details about a particular transaction
 */

public class FragmentTxDetails extends DialogFragment {

    private static final String EXTRA_TX_ITEM = "tx_item";
    private static final String TAG = "FragmentTxDetails";

    private TxUiHolder mTransaction;

    private BRText mTxAction;
    private BRText mTxAmount;
    private BRText mTxStatus;
    private BRText mTxDate;
    private BRText mToFrom;
    private BRText mToFromAddress;
    private BRText mToFromAddress2;
    private BRText mToFromAddress3;
    private BRText mMemoText;
    private BRText mLinkOpenInExplorer;

    private BRText mStartingBalance;
    private BRText mEndingBalance;
    private BRText mExchangeRate;
    private BRText mConfirmedInBlock;
    private BRText mTransactionId;
    private BRText mShowHide;
    private BRText mAmountWhenSent;
    private BRText mAmountNow;
    private BRText mLabelNow;

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

        View rootView = inflater.inflate(R.layout.transaction_details, container, false);

        mAmountNow = rootView.findViewById(R.id.amount_now);
        mLabelNow = rootView.findViewById(R.id.label_now);
        mAmountWhenSent = rootView.findViewById(R.id.amount_when_sent);
        mTxAction = rootView.findViewById(R.id.tx_action);
        mTxAmount = rootView.findViewById(R.id.tx_amount);

        mTxStatus = rootView.findViewById(R.id.tx_status);
        mTxDate = rootView.findViewById(R.id.tx_date);
        mToFrom = rootView.findViewById(R.id.tx_to_from);
        mToFromAddress = rootView.findViewById(R.id.tx_to_from_address);
        mToFromAddress2 = rootView.findViewById(R.id.tx_to_from_address2);
        mToFromAddress3 = rootView.findViewById(R.id.tx_to_from_address3);
        //mMemoText = rootView.findViewById(R.id.memo);
        mLinkOpenInExplorer = rootView.findViewById(R.id.link_open_in_explorer);
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

    public void setTransaction(TxUiHolder item) {

        this.mTransaction = item;

    }

    private void updateUi() {

        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        // Set mTransction fields
        if (mTransaction != null) {

            boolean sent = mTransaction.getSent() > 0;
            String amountWhenSent;
            String amountNow;
            String exchangeRateFormatted;

            if (!mTransaction.isValid()) {
                mTxStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            //user prefers crypto (or fiat)
            boolean isCryptoPreferred = BRSharedPrefs.isCryptoPreferred(getActivity());
            String cryptoIso = walletManager.getIso(getActivity());
            String fiatIso = BRSharedPrefs.getPreferredFiatIso(getContext());

            String iso = isCryptoPreferred ? cryptoIso : fiatIso;

            BigDecimal cryptoAmount = new BigDecimal(mTransaction.getAmount());

            BigDecimal fiatAmountNow = walletManager.getFiatForSmallestCrypto(getActivity(), cryptoAmount.abs(), null);

            BigDecimal fiatAmountWhenSent;
            TxMetaData metaData = KVStoreManager.getInstance().getTxMetaData(getActivity(), mTransaction.getTxHash());
            if (metaData == null || metaData.exchangeRate == 0 || Utils.isNullOrEmpty(metaData.exchangeCurrency)) {
                fiatAmountWhenSent = new BigDecimal(0);
                amountWhenSent = CurrencyUtils.getFormattedAmount(getActivity(), fiatIso, fiatAmountWhenSent);//always fiat amount
            } else {

                CurrencyEntity ent = new CurrencyEntity(metaData.exchangeCurrency, null, (float) metaData.exchangeRate, walletManager.getIso(getActivity()));
                fiatAmountWhenSent = walletManager.getFiatForSmallestCrypto(getActivity(), cryptoAmount.abs(), ent);
                amountWhenSent = CurrencyUtils.getFormattedAmount(getActivity(), ent.code, fiatAmountWhenSent);//always fiat amount

            }

            amountNow = CurrencyUtils.getFormattedAmount(getActivity(), fiatIso, fiatAmountNow);//always fiat amount

            mAmountWhenSent.setText(amountWhenSent);
            mAmountNow.setText(amountNow);

            BigDecimal tmpStartingBalance = new BigDecimal(mTransaction.getBalanceAfterTx()).subtract(cryptoAmount.abs()).subtract(new BigDecimal(mTransaction.getFee()).abs());

            BigDecimal startingBalance = isCryptoPreferred ? walletManager.getCryptoForSmallestCrypto(getActivity(), tmpStartingBalance).multiply(new BigDecimal(BRConstants.ONE_BITCOIN)) : walletManager.getFiatForSmallestCrypto(getActivity(), tmpStartingBalance, null);

            BigDecimal endingBalance = isCryptoPreferred ? walletManager.getCryptoForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx())).multiply(new BigDecimal(BRConstants.ONE_BITCOIN)) : walletManager.getFiatForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx()), null);

            mStartingBalance.setText(CurrencyUtils.getFormattedAmount(getActivity(), iso, startingBalance == null ? null : startingBalance.abs()));
            mEndingBalance.setText(CurrencyUtils.getFormattedAmount(getActivity(), iso, endingBalance == null ? null : endingBalance.abs()));

            String txSent = "", txToLabel = "", txToAddressLabel = "", txToAddressLabel2="", txToAddressLabel3="";
            long eventID = 0;
            if (mTransaction.getBetEntity()==null)  {
                if (mTransaction.isCoinbase() && mTransaction.getBlockHeight() != Integer.MAX_VALUE) {       //  payout reward
                    BetResultTxDataStore brds = BetResultTxDataStore.getInstance(getContext());
                    BetResultEntity br = brds.getByBlockHeight(getContext(), walletManager.getIso(getContext()), mTransaction.getBlockHeight() - 1);
                    if (br != null) {
                        eventID = br.getEventID();
                        EventTxUiHolder ev = BetEventTxDataStore.getInstance(getContext()).getTransactionByEventId(getContext(), "wgr", eventID);
                        if (ev != null) {
                            String txResult = (ev.getHomeScore()<0)?"Pending":String.format("%s - %s", ev.getTxHomeScore(), ev.getTxAwayScore());
                            txToAddressLabel = String.format("%s - %s", ev.getTxHomeTeam(), ev.getTxAwayTeam());
                            txToAddressLabel2 = String.format("Result: %s ", (ev.getHomeScore()<0)?"Pending":txResult);
                            txToAddressLabel3 = String.format("Event #%s", String.valueOf(ev.getEventID()));
                        } else {
                            txToAddressLabel = String.format("Event #%d: info not avalable", eventID);
                        }
                        txSent = String.format("PAYOUT Event #%d", eventID);
                    } else {
                        txToAddressLabel = String.format("Result not avalable at height %d", mTransaction.getBlockHeight() - 1);
                        txSent = "PAYOUT";
                    }
                }
                else {      // normal tx
                    txSent = sent ? "Sent" : "Received";
                    txToLabel = sent ? "To " : "Via ";
                    eventID = 0;
                    txToAddressLabel = walletManager.decorateAddress(getActivity(), mTransaction.getToRecipient(walletManager, !sent));
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
                }
            }
            else {
                EventTxUiHolder ev = BetEventTxDataStore.getInstance(getContext()).getTransactionByEventId(getContext(), "wgr", mTransaction.getBetEntity().getEventID());
                if (ev!=null) {
                    BetEntity.BetOutcome outcome = mTransaction.getBetEntity().getOutcome();
                    String sTotals = ( outcome == BetEntity.BetOutcome.TOTAL_UNDER || outcome == BetEntity.BetOutcome.TOTAL_OVER)?ev.getTxTotalPoints():"";
                    txToLabel = "";
                    eventID = ev.getEventID();

                    String txResult = (ev.getHomeScore()<0)?"Pending":String.format("%s - %s", ev.getTxHomeScore(), ev.getTxAwayScore());
                    String txWin =
                    txSent = String.format("BET: %s%s", outcome.toString(), sTotals);
                    txToAddressLabel = String.format("%s : %s vs %s", ev.getTxEventShortDate(), ev.getTxHomeTeam(), ev.getTxAwayTeam());
                    txToAddressLabel2 = String.format("Result: %s ", (ev.getHomeScore()<0)?"Pending":txResult);
                    txToAddressLabel3 = String.format("Event #%s", String.valueOf(ev.getEventID()));
                }
            }

            mTxAction.setText(txSent);
            mToFrom.setText(txToLabel);
            mToFromAddress.setText(txToAddressLabel);
            mToFromAddress2.setText(txToAddressLabel2);
            mToFromAddress3.setText(txToAddressLabel3);
            final long evID = eventID;
            mToFromAddress3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://explorer.wagerr.com/#/bet/event/%d", evID)));
                    startActivity(browserIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });

            mTxAmount.setText(CurrencyUtils.getFormattedAmount(getActivity(), walletManager.getIso(getActivity()), cryptoAmount));//this is always crypto amount

            if (!sent)
                mTxAmount.setTextColor(getContext().getColor(R.color.transaction_amount_received_color));

            // Set the memo text if one is available
/* disable memo upon request
            String memo;
            TxMetaData txMetaData = KVStoreManager.getInstance().getTxMetaData(getActivity(), mTransaction.getTxHash());

            if (txMetaData != null) {
                Log.d(TAG, "TxMetaData not null");
                if (txMetaData.comment != null) {
                    Log.d(TAG, "Comment not null");
                    memo = txMetaData.comment;
                    mMemoText.setText(memo);
                } else {
                    Log.d(TAG, "Comment is null");
                    mMemoText.setText("");
                }

                String metaIso = Utils.isNullOrEmpty(txMetaData.exchangeCurrency) ? "USD" : txMetaData.exchangeCurrency;

                exchangeRateFormatted = CurrencyUtils.getFormattedAmount(getActivity(), metaIso, new BigDecimal(txMetaData.exchangeRate));
                mExchangeRate.setText(exchangeRateFormatted);
            } else {
                mMemoText.setText("");
            }
*/
            // timestamp is 0 if it's not confirmed in a block yet so make it now
            mTxDate.setText(BRDateUtil.getMidDate(mTransaction.getTimeStamp() == 0 ? System.currentTimeMillis() : (mTransaction.getTimeStamp() * 1000)));

            // Set the transaction id
            mTransactionId.setText(mTransaction.getTxHashHexReversed());

            // Allow the transaction id to be copy-able
            mTransactionId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // Get the default color based on theme
                    final int color = mTransactionId.getCurrentTextColor();

                    mTransactionId.setTextColor(getContext().getColor(R.color.light_gray));
                    String id = mTransaction.getTxHashHexReversed();
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

            mLinkOpenInExplorer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://explorer.wagerr.com/#/tx/%s", mTransaction.getTxHashHexReversed())));
                    startActivity(browserIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });

            // Set the transaction block number
            String confirmedBlock = String.valueOf(mTransaction.getBlockHeight());
            if ( confirmedBlock.equals(BRConstants.INT23_MAX))  confirmedBlock = "Unconfirmed";
            mConfirmedInBlock.setText(confirmedBlock);

        } else {
            Toast.makeText(getContext(), "Error getting transaction data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }
}

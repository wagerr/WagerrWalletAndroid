package com.wagerrwallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;
import com.wagerrwallet.R;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.BetEntity;
import com.wagerrwallet.presenter.entities.BetResultEntity;
import com.wagerrwallet.presenter.entities.CurrencyEntity;
import com.wagerrwallet.presenter.entities.EventTxUiHolder;
import com.wagerrwallet.presenter.entities.SwapUiHolder;
import com.wagerrwallet.presenter.entities.TxUiHolder;
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

import java.math.BigDecimal;

/**
 * Created by byfieldj on 2/26/18.
 * <p>
 * Reusable dialog fragment that display details about a particular transaction
 */

public class FragmentSwapDetails extends DialogFragment implements View.OnClickListener {

    private static final String EXTRA_TX_ITEM = "swap_item";
    private static final String TAG = "FragmentSwapDetails";

    private SwapUiHolder mTransaction;

    private BRText mHeaderTxId;
    private BRText mTxId;
    private BRText mLabelStatus;
    private BRText mTimestamp;
    private BRText mDepositAmount;
    private BRText mReceivedAmount;
    private BRText mReceiveAddress;
    private BRText mRefundAddress;

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

        View rootView = inflater.inflate(R.layout.swap_details, container, false);

        mHeaderTxId = rootView.findViewById(R.id.tx_id);

        mTxId = rootView.findViewById(R.id.value_tx_id);
        mLabelStatus = rootView.findViewById(R.id.label_transaction_state);
        mTimestamp = rootView.findViewById(R.id.value_timestamp);
        mDepositAmount = rootView.findViewById(R.id.value_deposit_amount);
        mReceivedAmount = rootView.findViewById(R.id.value_receive_amount);
        mReceiveAddress = rootView.findViewById(R.id.value_receive_address);
        mRefundAddress = rootView.findViewById(R.id.value_refund_address);
        mDetailsContainer = rootView.findViewById(R.id.details_container);
        mCloseButton = rootView.findViewById(R.id.close_button);

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mTxId.setOnClickListener(this);
        mReceiveAddress.setOnClickListener(this);
        mRefundAddress.setOnClickListener(this);

        updateUi();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void setTransaction(SwapUiHolder item) {
        this.mTransaction = item;
    }

    private void updateUi() {

        BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        // Set mTransction fields
        if (mTransaction != null) {

            mHeaderTxId.setText("ID: " + mTransaction.getTransactionId());
            mTxId.setText(mTransaction.getTransactionId());
            mLabelStatus.setText(mTransaction.getTransactionState().toString());
            mTimestamp.setText(mTransaction.getTimestamp());
            mDepositAmount.setText(mTransaction.getDepositAmount());
            mReceivedAmount.setText(mTransaction.getReceivingAmount());
            mReceiveAddress.setText(mTransaction.getReceiveWallet());
            mRefundAddress.setText(mTransaction.getRefundWallet());

        } else {
            Toast.makeText(getContext(), "Error getting transaction data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onClick(View v) {
        BRText txCur = (BRText)v;

        // Get the default color based on theme
        final int color = txCur.getCurrentTextColor();

        txCur.setTextColor(getContext().getColor(R.color.light_gray));
        String address = txCur.getText().toString();
        BRClipboardManager.putClipboard(getContext(), address);
        Toast.makeText(getContext(), getString(R.string.Receive_copied), Toast.LENGTH_LONG).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                txCur.setTextColor(color);
            }
        }, 200);

    }
}

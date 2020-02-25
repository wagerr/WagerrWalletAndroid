package com.wagerrwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.platform.tools.KVStoreManager;
import com.wagerrwallet.R;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.SwapUiHolder;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.threads.executor.BRExecutor;
import com.wagerrwallet.tools.util.BRDateUtil;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/27/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class SwapListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = SwapListAdapter.class.getName();

    private final Context mContext;
    private final int txResId;
    private final int promptResId;
    private List<SwapUiHolder> backUpFeed;
    private List<SwapUiHolder> itemFeed;
    //    private Map<String, TxMetaData> mds;
    public boolean[] filterSwitches = { false, false, false };
    public String filterQuery="";

    private final int txType = 0;
    private final int promptType = 1;
    private boolean updatingReverseTxHash;
    private boolean updatingData;

//    private boolean updatingMetadata;

    public SwapListAdapter(Context mContext, List<SwapUiHolder> items) {
        this.txResId = R.layout.swap_item;
        this.promptResId = R.layout.prompt_item;
        this.mContext = mContext;
        items = new ArrayList<>();
        init(items);
//        updateMetadata();
    }

    public void setItems(List<SwapUiHolder> items) {
        init(items);
    }

    private void init(List<SwapUiHolder> items) {
        if (items == null) items = new ArrayList<>();
        if (itemFeed == null) itemFeed = new ArrayList<>();
        if (backUpFeed == null) backUpFeed = new ArrayList<>();
        this.itemFeed = items;
        this.backUpFeed = items;
    }

    public void updateData() {
        if (updatingData) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                long s = System.currentTimeMillis();
                List<SwapUiHolder> newItems = new ArrayList<>(itemFeed);
                SwapUiHolder item;
                for (int i = 0; i < newItems.size(); i++) {
                    item = newItems.get(i);
                }
                backUpFeed = newItems;
                String log = String.format("newItems: %d, took: %d", newItems.size(), (System.currentTimeMillis() - s));
                Log.e(TAG, "updateData: " + log);
                updatingData = false;
            }
        });

    }


    public List<SwapUiHolder> getItems() {
        return itemFeed;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflate the layout
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        return new SwapHolder(inflater.inflate(txResId, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case txType:
                holder.setIsRecyclable(false);
                setTexts((SwapHolder) holder, position);
                break;
            case promptType:
                //setPrompt((PromptHolder) holder);
                break;
        }

    }

    @Override
    public int getItemViewType(int position) {
        return txType;
    }

    @Override
    public int getItemCount() {
        return itemFeed.size();
    }

    private void setTexts(final SwapHolder convertView, int position) {
        BaseWalletManager wallet = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        SwapUiHolder item = itemFeed.get(position);

        convertView.transactionTimestamp.setText(item.getTimestamp());
        convertView.transactionAmount.setText(item.getReceivingAmount());
        convertView.transactionState.setText(item.getTransactionState().toString());
        convertView.transactionId.setText("ID: " + item.getTransactionId());
    }

    public void resetFilter() {
        itemFeed = backUpFeed;
        notifyDataSetChanged();
    }

    public void filterBy(String query, boolean[] switches) {
        filter(query, switches);
    }

    private void filter(final String query, final boolean[] switches) {
        BaseWalletManager wallet = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);

        long start = System.currentTimeMillis();
        String lowerQuery = query.toLowerCase().trim();
        // undesired behavior: no filter = rebuild whole list instead return
        //if (Utils.isNullOrEmpty(lowerQuery) && !switches[0] && !switches[1] && !switches[2] && !switches[3])
        //    return;
        int switchesON = 0;
        for (boolean i : switches) if (i) switchesON++;

        final List<SwapUiHolder> filteredList = new ArrayList<>();
        SwapUiHolder item;
        for (int i = 0; i < backUpFeed.size(); i++) {
            item = backUpFeed.get(i);
            boolean matchesId = item.getTransactionId() != null && item.getTransactionId().toLowerCase().contains(lowerQuery);

            if (matchesId) {
                if (switchesON == 0) {
                    filteredList.add(item);
                } else {
                    boolean willAdd = true;
                    if (switches[0] && (item.getTransactionState() != SwapUiHolder.TransactionState.completed && item.getTransactionState() != SwapUiHolder.TransactionState.notcompleted)) {
                        willAdd = false;
                    }
                    if (switches[1] && item.getTransactionState() == SwapUiHolder.TransactionState.notcompleted ) {
                        willAdd = false;
                    }
                    if (switches[2] && item.getTransactionState() == SwapUiHolder.TransactionState.completed ) {
                        willAdd = false;
                    }
                    if (willAdd) filteredList.add(item);
                }
            }
        }
        filterSwitches = switches;
        filterQuery = query;
        itemFeed = filteredList;
        notifyDataSetChanged();

        Log.e(TAG, "filter: " + query + " took: " + (System.currentTimeMillis() - start));
    }

    private class SwapHolder extends RecyclerView.ViewHolder {
        public RelativeLayout mainLayout;
        public ConstraintLayout constraintLayout;

        public BRText transactionTimestamp;
        public BRText transactionAmount;
        public BRText transactionState;
        public BRText transactionId;

        public SwapHolder(View view) {
            super(view);

            transactionTimestamp = view.findViewById(R.id.tx_timestamp);
            transactionAmount = view.findViewById(R.id.tx_amount);
            transactionState = view.findViewById(R.id.tx_state);
            transactionId = view.findViewById(R.id.tx_id);
        }
    }

}
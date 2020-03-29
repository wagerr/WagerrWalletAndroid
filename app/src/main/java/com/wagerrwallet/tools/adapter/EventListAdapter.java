package com.wagerrwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.platform.tools.KVStoreManager;
import com.wagerrwallet.R;
import com.wagerrwallet.presenter.activities.EventsActivity;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.EventTxUiHolder;
import com.wagerrwallet.presenter.fragments.FragmentWebView;
import com.wagerrwallet.tools.animation.BRAnimator;
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
 *
 * (c) Wagerr Betting platform 2019
 *
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

public class EventListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = EventListAdapter.class.getName();

    private final Context mContext;
    private final int txResId;
    private final int promptResId;
    private List<EventTxUiHolder> backUpFeed;
    private List<EventTxUiHolder> itemFeed;
    //    private Map<String, TxMetaData> mds;
    public long[] filterSwitches = new long[2];

    private final int txType = 0;
    private final int promptType = 1;
    private boolean updatingReverseTxHash;
    private boolean updatingData;

//    private boolean updatingMetadata;

    public EventListAdapter(Context mContext, List<EventTxUiHolder> items) {
        this.txResId = R.layout.eventtx_item;
        this.promptResId = R.layout.prompt_item;
        this.mContext = mContext;
        items = new ArrayList<>();
        init(items);
//        updateMetadata();
    }

    public void setItems(List<EventTxUiHolder> items) {
        init(items);
    }

    private void init(List<EventTxUiHolder> items) {
        if (items == null) items = new ArrayList<>();
        if (itemFeed == null) itemFeed = new ArrayList<>();
        if (backUpFeed == null) backUpFeed = new ArrayList<>();
        this.itemFeed = items;
        this.backUpFeed = items;

    }

    public EventTxUiHolder findByEventID( long eventID ) {
        for(EventTxUiHolder item : itemFeed)    {
            if (item.getEventID() == eventID)   {
                return item;
            }
        }
        return null;
    }

    public void updateData() {
        if (updatingData) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                long s = System.currentTimeMillis();
                List<EventTxUiHolder> newItems = new ArrayList<>(itemFeed);
                EventTxUiHolder item;
                for (int i = 0; i < newItems.size(); i++) {
                    item = newItems.get(i);
                    BRAnimator.updateEventDetails(((Activity) mContext), item);
                }
                backUpFeed = newItems;
                String log = String.format("newItems: %d, took: %d", newItems.size(), (System.currentTimeMillis() - s));
                Log.e(TAG, "updateData: " + log);
                updatingData = false;
            }
        });

    }


    public List<EventTxUiHolder> getItems() {
        return itemFeed;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflate the layout
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        return new EventHolder(inflater.inflate(txResId, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case txType:
                holder.setIsRecyclable(false);
                setTexts((EventHolder) holder, position);
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

    private void setTexts(final EventHolder convertView, int position) {
        BaseWalletManager wallet = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        EventTxUiHolder item = itemFeed.get(position);

        String commentString = "";
        boolean received = false;

        BigDecimal cryptoAmount = new BigDecimal(0);
        boolean isCryptoPreferred = BRSharedPrefs.isCryptoPreferred(mContext);
        String preferredIso = isCryptoPreferred ? wallet.getIso(mContext) : BRSharedPrefs.getPreferredFiatIso(mContext);

        BigDecimal amount = isCryptoPreferred ? cryptoAmount : wallet.getFiatForSmallestCrypto(mContext, cryptoAmount, null);

        int blockHeight = (int)item.getBlockheight();
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : BRSharedPrefs.getLastBlockHeight(mContext, wallet.getIso(mContext)) - blockHeight + 1;

        //if it's 0 we use the current time.
        long timeStamp = item.getTimestamp() == 0 ? System.currentTimeMillis() : item.getTimestamp() * 1000;

        String shortDate = BRDateUtil.getShortDate(timeStamp);

        convertView.transactionHomeTeam.setText( (item.getTxHomeTeam()!=null)?item.getTxHomeTeam():"Home Team N/A" );
        convertView.transactionAwayTeam.setText( (item.getTxAwayTeam()!=null)?item.getTxAwayTeam():"Away Team N/A" );
        convertView.transactionHomeOdds.setText( (item.getTxHomeOdds()!=null)?item.getTxHomeOdds():"N/A" );
        convertView.transactionDrawOdds.setText( (item.getTxDrawOdds()!=null)?item.getTxDrawOdds():"N/A" );
        convertView.transactionAwayOdds.setText( (item.getTxAwayOdds()!=null)?item.getTxAwayOdds():"N/A" );

        String homeScore = item.getTxHomeScore();
        convertView.transactionHomeResult.setText( homeScore );

        String awayScore = item.getTxAwayScore();
        convertView.transactionAwayResult.setText( awayScore );

        convertView.transactionHeader.setText(item.getTxEventHeader());
        convertView.transactionEventDate.setText( item.getTxEventDate() );

        convertView.betSmartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText((Activity)mContext, "launching web view from activity", Toast.LENGTH_LONG).show();
                CreateWebFragment( (Activity)mContext, String.format("https://betsmart.app/teaser-event?id=%d&mode=light&source=wagerr", item.getEventID() ));
            }
        });

    }

    public void CreateWebFragment(Activity app, String theUrl)   {
        FragmentWebView fragmentWebView = (FragmentWebView) app.getFragmentManager().findFragmentByTag(FragmentWebView.class.getName());

        if(fragmentWebView != null && fragmentWebView.isAdded()){
            Log.e(TAG, "showWebView: Already showing");
            return;
        }

        fragmentWebView = new FragmentWebView();
        Bundle args = new Bundle();
        args.putString("url", theUrl);
        fragmentWebView.setArguments(args);

        fragmentWebView.show( app.getFragmentManager(), FragmentWebView.class.getName());
        app.getFragmentManager().beginTransaction()
                .addToBackStack(null)
                .commit();

        /*
        FragmentWebView fragmentWebView = new FragmentWebView();
        Bundle args = new Bundle();
        args.putString("url", String.format("https://betsmart.app/teaser-event?id=%d&mode=light&source=wagerr", item.getEventID()));
        fragmentWebView.setArguments(args);
        app.getFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentWebView, FragmentWebView.class.getName())
                .addToBackStack(FragmentWebView.class.getName()).commit();
        */
    }

    private void showTransactionProgress(EventHolder holder, int progress) {


        if (progress < 100) {
            holder.transactionProgress.setVisibility(View.VISIBLE);
            holder.transactionProgress.setProgress(progress);

            RelativeLayout.LayoutParams detailParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            detailParams.addRule(RelativeLayout.RIGHT_OF, holder.transactionProgress.getId());
            detailParams.addRule(RelativeLayout.CENTER_VERTICAL);
            detailParams.setMargins(Utils.getPixelsFromDps(mContext, 16), Utils.getPixelsFromDps(mContext, 36), 0, 0);
            //holder.transactionDetail.setLayoutParams(detailParams);
        } else {
            holder.transactionProgress.setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams startingParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            startingParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            startingParams.addRule(RelativeLayout.CENTER_VERTICAL);
            startingParams.setMargins(Utils.getPixelsFromDps(mContext, 16), 0, 0, 0);
            //holder.transactionDetail.setLayoutParams(startingParams);
            holder.setIsRecyclable(true);
        }
    }

    private void showTransactionFailed(EventHolder holder, EventTxUiHolder tx, boolean received) {

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.RIGHT_OF, holder.transactionFailed.getId());
        params.setMargins(16, 0, 0, 0);
        params.addRule(RelativeLayout.CENTER_VERTICAL, holder.transactionFailed.getId());
        //holder.transactionDetail.setLayoutParams(params);
        //holder.transactionDetail.setText("sending to ");

    }

    public void filterBy(String query) {
        filter(filterSwitches, true, query);
    }

    public void resetFilter() {
        itemFeed = backUpFeed;
        notifyDataSetChanged();
    }

    public void filter(final long[] switches, boolean bNotify, String query) {
        long start = System.currentTimeMillis();
        int switchesON = 0;
        for (long i : switches) if (i>=0) switchesON++;
        filterSwitches = switches;

        final List<EventTxUiHolder> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        EventTxUiHolder item;
        for (int i = 0; i < backUpFeed.size(); i++) {
            item = backUpFeed.get(i);
            // team match
            boolean matchesTeam =  item.getTxHomeTeam().toLowerCase().contains(lowerQuery)
                                || item.getTxAwayTeam().toLowerCase().contains(lowerQuery);
            boolean matchesEventId = Utils.isInteger(lowerQuery) && item.getEventID()==Integer.parseInt(lowerQuery);

            if ( matchesTeam || matchesEventId ) {
                if (switchesON == 0) {
                    filteredList.add(item);
                } else {
                    if ((switches[0] == -1 || switches[0] == item.getSportID())
                    && (switches[1] == -1 || switches[1] == item.getTournamentID())) {
                        filteredList.add(item);
                    }
                }
            }
        }
        filterSwitches = switches;
        itemFeed = filteredList;
        if (bNotify)    notifyDataSetChanged();

        Log.e(TAG, "filter event list took: " + (System.currentTimeMillis() - start));
    }

    // wagerr: change for Event row UI items
    private class EventHolder extends RecyclerView.ViewHolder {
        public RelativeLayout mainLayout;
        public ConstraintLayout constraintLayout;
        public TextView amount;
        public TextView account;
        public TextView status;
        public TextView status_2;
        public TextView timestamp;
        public TextView comment;
        public TextView spread_points;
        public TextView spread_home_odds;
        public TextView spread_away_odds;
        public TextView total_points;
        public TextView total_over_odds;
        public TextView total_under_odds;
        public ImageView arrowIcon;

        public ImageButton betSmartButton;
        public BRText transactionHeader;        // sport - tournament - round
        public BRText transactionEventDate;
        public BRText transactionHomeTeam;
        public BRText transactionHomeResult;
        public BRText transactionAwayTeam;
        public BRText transactionAwayResult;
        public BRText transactionHomeOdds;
        public BRText transactionAwayOdds;
        public BRText transactionDrawOdds;
        public Button transactionFailed;
        public ProgressBar transactionProgress;


        public EventHolder(View view) {
            super(view);

            betSmartButton = view.findViewById(R.id.betsmart_button);
            transactionHeader = view.findViewById(R.id.tx_eventheader);
            transactionHeader.setLineSpacing(0, 1.0f);
            transactionEventDate = view.findViewById(R.id.tx_eventdate);
            transactionHomeTeam = view.findViewById(R.id.tx_home);
            transactionAwayTeam = view.findViewById(R.id.tx_away);
            transactionHomeResult = view.findViewById(R.id.tx_home_result);
            transactionAwayResult = view.findViewById(R.id.tx_away_result);
            transactionHomeOdds = view.findViewById(R.id.tx_home_odds);
            transactionAwayOdds = view.findViewById(R.id.tx_away_odds);
            transactionDrawOdds = view.findViewById(R.id.tx_draw_odds);
            transactionFailed = view.findViewById(R.id.tx_failed_button);
            transactionProgress = view.findViewById(R.id.tx_progress);

        }
    }

}
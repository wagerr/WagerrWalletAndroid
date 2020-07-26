package com.platform.entities;

import com.wagerrwallet.presenter.entities.BetEntity;
import com.wagerrwallet.presenter.entities.BetEventEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by MIP 7/01/20.
 * Copyright (c) 2020 Wagerr LTD
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class TxExplorerPayoutInfo {

    public class TxExplorerPayoutLeg  {
        public int event_id;
        public int outcome;
        public String legResultType;
        public TxExplorerPayoutLockedEvent lockedEvent;

        public String getPriceTx( float price )     {
            return BetEventEntity.getOddText(BetEventEntity.getOddsStatic(price));
        }

        public String getDescription()  {
            BetEntity.BetOutcome betOutcome = BetEntity.BetOutcome.fromValue( outcome );
            String ret = String.format("#%d - %s, %s - %s\n", event_id, (betOutcome.toString()), lockedEvent.home, lockedEvent.away );
            String sign;

            switch (betOutcome) {
                case MONEY_LINE_HOME_WIN:
                    ret += String.format(" (Price: %s)", getPriceTx( (float)lockedEvent.homeOdds/BetEventEntity.ODDS_MULTIPLIER ));
                    break;
                case MONEY_LINE_AWAY_WIN:
                    ret += String.format(" (Price: %s)", getPriceTx( (float)lockedEvent.awayOdds/BetEventEntity.ODDS_MULTIPLIER ));
                    break;
                case MONEY_LINE_DRAW:
                    ret += String.format(" (Price: %s)", getPriceTx( (float)lockedEvent.drawOdds/BetEventEntity.ODDS_MULTIPLIER ));
                    break;
                case SPREADS_HOME:
                    sign = (lockedEvent.spreadPoints>0) ? "+": "-";
                    ret += String.format(" (Price: %s, Spread: %s%s )", getPriceTx( (float)lockedEvent.spreadHomeOdds/BetEventEntity.ODDS_MULTIPLIER ), sign, BetEventEntity.getTextSpreadPoints( lockedEvent.spreadPoints ) );
                    break;
                case SPREADS_AWAY:
                    sign = (lockedEvent.spreadPoints>0) ? "-": "+";
                    ret += String.format(" (Price: %s, Spread: %s%s )", getPriceTx( (float)lockedEvent.spreadAwayOdds/BetEventEntity.ODDS_MULTIPLIER ), sign, BetEventEntity.getTextSpreadPoints( lockedEvent.spreadPoints ) );
                    break;
                case TOTAL_OVER:
                    ret += String.format(" (Price: %s, Total: %s )", getPriceTx( (float)lockedEvent.totalOverOdds/BetEventEntity.ODDS_MULTIPLIER ), BetEventEntity.getTextTotalPoints( lockedEvent.totalPoints ) );
                    break;
                case TOTAL_UNDER:
                    ret += String.format(" (Price: %s, Total: %s )", getPriceTx( (float)lockedEvent.totalUnderOdds/BetEventEntity.ODDS_MULTIPLIER ), BetEventEntity.getTextTotalPoints( lockedEvent.totalPoints ) );
                    break;
                case UNKNOWN:
                ret += " Unknown";
            }
            return ret;
        }
        public long getLegPrice()   {
            BetEntity.BetOutcome betOutcome = BetEntity.BetOutcome.fromValue( outcome );
            long ret = 0;

            switch (betOutcome) {
                case MONEY_LINE_HOME_WIN:
                    ret = lockedEvent.homeOdds;
                    break;
                case MONEY_LINE_AWAY_WIN:
                    ret = lockedEvent.awayOdds;
                    break;
                case MONEY_LINE_DRAW:
                    ret = lockedEvent.drawOdds;
                    break;
                case SPREADS_HOME:
                    ret = lockedEvent.spreadHomeOdds;
                    break;
                case SPREADS_AWAY:
                    ret = lockedEvent.spreadAwayOdds;
                    break;
                case TOTAL_OVER:
                    ret = lockedEvent.totalOverOdds;
                    break;
                case TOTAL_UNDER:
                    ret = lockedEvent.totalUnderOdds;
                    break;
                case UNKNOWN:
                    ret = 0;
            }
            return ret;
        }
    }

    public class TxExplorerPayoutLockedEvent    {
        public long homeOdds;
        public long awayOdds;
        public long drawOdds;
        public long spreadPoints;
        public long spreadHomeOdds;
        public long spreadAwayOdds;
        public long totalPoints;
        public long totalOverOdds;
        public long totalUnderOdds;
        public int starting;
        public String home;
        public String away;
        public String tournament;
        public String eventResultType;
        public long homeScore;
        public long awayScore;
    }

    public int betBlockHeight;
    public String betTxHash;
    public int betTxOut;
    public List<TxExplorerPayoutLeg> legs;
    public String address;
    public int amount;
    public int time;
    public String completed;
    public String betResultType;
    public Double payout;
    public String payoutTxHash;
    public int payoutTxOut;

    public void PopulateFromJsonObject(JSONObject o)    {
        try {
            betBlockHeight = o.getInt("betBlockHeight");
            betTxHash = o.getString("betTxHash");
            betTxOut = o.getInt("betTxOut");
            address = o.getString("address");
            amount = o.getInt("amount");
            time = o.getInt("time");
            completed = o.getString("completed");
            betResultType = o.getString("betResultType");
            payout = o.getDouble("payout");
            payoutTxHash = o.getString("payoutTxHash");
            payoutTxOut = o.getInt("payoutTxOut");

            legs = new ArrayList<>();
            JSONArray legsArray = o.getJSONArray("legs");
            for (int i = 0; i < legsArray.length(); i++) {
                if ( i == 5 )   break;      // too many legs, unsupported
                JSONObject jsonLeg = legsArray.getJSONObject(i);
                TxExplorerPayoutLeg leg = new TxExplorerPayoutLeg();
                leg.event_id = jsonLeg.getInt("event-id");
                leg.outcome = jsonLeg.getInt("outcome");
                leg.legResultType = jsonLeg.getString("legResultType");
                TxExplorerPayoutLockedEvent lockedEvent = new TxExplorerPayoutLockedEvent();
                JSONObject jsonLockedEvent = jsonLeg.getJSONObject("lockedEvent");
                lockedEvent.homeOdds = jsonLockedEvent.getLong("homeOdds");
                lockedEvent.awayOdds = jsonLockedEvent.getLong("awayOdds");
                lockedEvent.drawOdds = jsonLockedEvent.getLong("drawOdds");
                lockedEvent.spreadPoints = jsonLockedEvent.getLong("spreadPoints");
                lockedEvent.spreadHomeOdds = jsonLockedEvent.getLong("spreadHomeOdds");
                lockedEvent.spreadAwayOdds = jsonLockedEvent.getLong("spreadAwayOdds");
                lockedEvent.totalPoints = jsonLockedEvent.getLong("totalPoints");
                lockedEvent.totalOverOdds = jsonLockedEvent.getLong("totalOverOdds");
                lockedEvent.totalUnderOdds = jsonLockedEvent.getLong("totalUnderOdds");
                lockedEvent.starting = jsonLockedEvent.getInt("starting");
                lockedEvent.home = jsonLockedEvent.getString("home");
                lockedEvent.away = jsonLockedEvent.getString("away");
                lockedEvent.tournament = jsonLockedEvent.getString("tournament");
                lockedEvent.eventResultType = jsonLockedEvent.getString("eventResultType");
                lockedEvent.homeScore = jsonLockedEvent.getLong("homeScore");
                lockedEvent.awayScore = jsonLockedEvent.getLong("awayScore");
                leg.lockedEvent = lockedEvent;
                legs.add(leg);
            }
        }
        catch (JSONException e) {

        }
    }

    public String getParlayPrice()  {
        String ret = "";
        float price = 1;
        if (legs.size()==0) return ret;
        for (TxExplorerPayoutLeg leg : legs)  {
            price *= (float)leg.getLegPrice() / (float)BetEventEntity.ODDS_MULTIPLIER;
        }
        return BetEventEntity.getOddText( BetEventEntity.getOddsStatic( price) );
    }
}


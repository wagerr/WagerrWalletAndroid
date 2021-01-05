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
 * Created by MIP 6/12/20.
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
public class TxExplorerInfo {

    public class TxExplorerLegInfo  {
        public double price;
        public double spread;
        public double total;
        public String market;
        public String homeTeam;
        public String awayTeam;
        public int homeScore;
        public int awayScore;
        public String betResult;
        public int outcome = -1;

        public String getPriceTx()  {
            return BetEventEntity.getOddText(BetEventEntity.getOddsStatic((float)price));
        }

        public String getResultIcon()   {
            return String.format("<img src=\"%s\">", (outcome > 0) ? betResult : "pending");
        }

        public BetEntity.BetOutcome getOutcome()    {
            return BetEntity.BetOutcome.fromValue(outcome);
        }
        public String getHomeScoreTx()  {
            return String.format("%d", homeScore / BetEventEntity.RESULT_MULTIPLIER);
        }
        public String getAwayScoreTx()  {
            return String.format("%d", awayScore / BetEventEntity.RESULT_MULTIPLIER);
        }
    }

    public String address;
    public double value;
    public double price;
    public String spread;
    public double total;
    public String market;
    public String eventId;
    public double betValue;
    public double betValueUSD;
    public String betResultType = "";
    public int isParlay;
    public String homeTeam;
    public String awayTeam;
    public int homeScore = -1;
    public int awayScore = -1;
    public List<TxExplorerLegInfo> legs = new ArrayList<>();

    public void PopulateFromJsonObject(JSONObject o)    {
        try {
            address = o.getString("address");
            value = o.getDouble("value");
            if (o.has("price"))    price = o.getDouble("price");
            if (o.has("Spread"))    spread = o.getString("Spread");
            if (o.has("Total"))     total = o.getDouble("Total");
            market = o.getString("market");
            betValue = o.getDouble("betValue");
            betValueUSD = o.getDouble("betValueUSD");
            isParlay = o.getInt("isParlay");
            if (o.has("homeScore"))     homeScore = o.getInt("homeScore");
            if (o.has("awayScore"))     awayScore = o.getInt("awayScore");
            if (o.has("homeTeam"))     homeTeam = o.getString("homeTeam");
            if (o.has("awayTeam"))     awayTeam = o.getString("awayTeam");
            if (o.has("betResultType"))     betResultType = o.getString("betResultType");

            if ( isParlay == 1 )    {
                JSONArray legsArray = o.getJSONArray("legs");
                for (int i = 0; i < legsArray.length(); i++) {
                    if ( i == 5 )   break;      // too many legs, unsupported
                    JSONObject jsonLeg = legsArray.getJSONObject(i);
                    TxExplorerLegInfo leg = new TxExplorerLegInfo();
                    leg.price = price = jsonLeg.getDouble("price");
                    if (jsonLeg.has("Spread"))    leg.spread = jsonLeg.getDouble("Spread");
                    if (jsonLeg.has("Total"))     leg.total = jsonLeg.getDouble("Total");
                    if (jsonLeg.has("homeTeam"))    leg.homeTeam = jsonLeg.getString("homeTeam");
                    if (jsonLeg.has("awayTeam"))     leg.awayTeam= jsonLeg.getString("awayTeam");
                    leg.market = o.getString("market");
                    if (o.has("homeScore"))     leg.homeScore = o.getInt("homeScore");
                    if (o.has("awayScore"))     leg.awayScore = o.getInt("awayScore");
                    if (o.has("betResult"))     leg.betResult = o.getString("betResult");
                    if (o.has("outcome"))       leg.outcome = o.getInt("outcome");
                    legs.add(leg);
                }
            }
        }
        catch (JSONException e) {

        }
    }

    public String getResultIcon()   {
        return String.format("<img src=\"%s\">", (betResultType.equals("")) ? "pending" : betResultType);
    }

    public String getPriceTx()  {
        return BetEventEntity.getOddText(BetEventEntity.getOddsStatic((float)price));
    }
    public String getHomeScoreTx()  {
        return String.format("%d", homeScore / BetEventEntity.RESULT_MULTIPLIER);
    }
    public String getAwayScoreTx()  {
        return String.format("%d", awayScore / BetEventEntity.RESULT_MULTIPLIER);
    }

    public String getParlayPrice()  {
        String ret = "";
        float price = 1;
        if (legs.size()==0) return ret;
        for (TxExplorerLegInfo leg : legs)  {
            price *= leg.price;
        }
        return BetEventEntity.getOddText( price );
    }
}


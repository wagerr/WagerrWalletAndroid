package com.platform.entities;

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
    }

    public String address;
    public double value;
    public double price;
    public double spread;
    public double total;
    public String market;
    public String eventId;
    public double betValue;
    public double betValueUSD;
    public int isParlay;
    public List<TxExplorerLegInfo> legs = new ArrayList<>();

    public void PopulateFromJsonObject(JSONObject o)    {
        try {
            address = o.getString("address");
            value = o.getDouble("value");
            if (o.has("price"))    price = o.getDouble("price");
            if (o.has("Spread"))    spread = o.getDouble("Spread");
            if (o.has("Total"))     total = o.getDouble("Total");
            market = o.getString("market");
            betValue = o.getDouble("betValue");
            betValueUSD = o.getDouble("betValueUSD");
            isParlay = o.getInt("isParlay");

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
                    legs.add(leg);
                }
            }
        }
        catch (JSONException e) {

        }
    }


}


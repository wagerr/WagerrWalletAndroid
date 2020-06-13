package com.platform.entities;

import org.json.JSONException;
import org.json.JSONObject;

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

    public String address;
    public double value;
    public double price;
    public double spread;
    public double total;
    public String market;
    public String eventId;
    public double betValue;
    public double betValueUSD;

    public void PopulateFromJsonObject(JSONObject o)    {
        try {
            address = o.getString("address");
            value = o.getDouble("value");
            price = o.getDouble("price");
            if (o.has("Spread"))    spread = o.getDouble("Spread");
            if (o.has("Total"))     total = o.getDouble("Total");
            market = o.getString("market");
            betValue = o.getDouble("betValue");
            betValueUSD = o.getDouble("betValueUSD");
        }
        catch (JSONException e) {

        }
    }
}

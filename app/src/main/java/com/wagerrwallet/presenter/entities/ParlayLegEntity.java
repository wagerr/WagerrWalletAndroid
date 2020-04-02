package com.wagerrwallet.presenter.entities;


import android.content.Context;

import com.wagerrwallet.R;
import com.wagerrwallet.tools.util.BRDateUtil;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

import java.util.Date;

/**
 * BreadWallet
 * <p>
 * Created by MIP on 01/30/20.
 * Copyright (c) 2020 Wagerr Ltd
 * <p>
 *
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

public class ParlayLegEntity {
    public static final String TAG = ParlayLegEntity.class.getName();

    private ParlayLegEntity() {
    }

    private EventTxUiHolder event;
    private BetEntity.BetOutcome outcome;
    private float odd;

    // extended constructor with support text for UI
    public ParlayLegEntity(EventTxUiHolder event, BetEntity.BetOutcome outcome, float odd) {

        this.event = event;
        this.outcome = outcome;
        this.odd = odd;
    }

    public EventTxUiHolder getEvent()
    {
        return event;
    }

    public BetEntity.BetOutcome getOutcome( )   {
        return outcome;
    }

    public int getOddColor( Context context)    {
        int home = context.getResources().getColor( R.color.red, null);
        int draw = context.getResources().getColor( R.color.gray, null);
        int away = context.getResources().getColor( R.color.black, null);

        switch (outcome)    {
            case MONEY_LINE_HOME_WIN:
            case SPREADS_HOME:
            case TOTAL_OVER:
                return home;

            case MONEY_LINE_AWAY_WIN:
            case SPREADS_AWAY:
            case TOTAL_UNDER:
                return away;

            case MONEY_LINE_DRAW:
                return draw;

            default:
                return home;
        }
    }

    public void updateEvent( EventTxUiHolder event )   {
        this.event = event;
        updateOdd();
    }

    public void updateOdd() {
        switch (outcome)    {
            case MONEY_LINE_HOME_WIN:
                odd = event.homeOdds;
                break;

            case SPREADS_HOME:
                odd = event.spreadHomeOdds;
                break;

            case TOTAL_OVER:
                odd = event.overOdds;
                break;

            case MONEY_LINE_AWAY_WIN:
                odd = event.awayOdds;
                break;

            case SPREADS_AWAY:
                odd = event.spreadAwayOdds;
                break;

            case TOTAL_UNDER:
                odd = event.underOdds;
                break;

            case MONEY_LINE_DRAW:
                odd = event.drawOdds;
                break;

            default:
                odd = 0;
        }
    }

    public float getOdd()   {
        return odd;
    }

    public boolean isValid( ) {
        Date date = new Date();
        long timeStampLimit = (date.getTime()/1000) + WalletWagerrManager.BET_CUTTOFF_SECONDS;
        return (getEvent().getEventTimestamp() >= timeStampLimit);
    }

}

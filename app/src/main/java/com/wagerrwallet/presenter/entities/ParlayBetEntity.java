package com.wagerrwallet.presenter.entities;


import java.util.ArrayList;
import java.util.List;

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

public class ParlayBetEntity {
    public static final String TAG = ParlayBetEntity.class.getName();
    public static final int MAX_LEGS = 5;

    public ParlayBetEntity() {
    }

    private List<ParlayLegEntity> legs = new ArrayList<>();
    private String strAmount;

    public int eventID[] = new int[MAX_LEGS];
    public BetEntity.BetOutcome outcome[] = new BetEntity.BetOutcome[MAX_LEGS];

    public ParlayLegEntity get( int index )
    {
        return legs.get( index );
    }

    public boolean add( ParlayLegEntity leg )  throws IndexOutOfBoundsException  {
        // validate max legs
        if (legs.size() == MAX_LEGS)    {
            throw new IndexOutOfBoundsException();
        }
        // validate rule only one leg per event
        if ( searchLegByEventID(leg.getEvent().getEventID()) > -1 )   {
            return false;
        }
        return legs.add(leg);
    }

    public void removeAt( int index )    {
        legs.remove(index);
    }
    public void removeByEventID( int eventID)    {
        int index = searchLegByEventID(eventID);
        if ( index > -1 ) {
            removeAt(index);
        }
    }

    public int getLegCount()    {
        return legs.size();
    }

    public void clearLegs() {
        legs.clear();
    }

    public enum BetInParlayResult {
        NOT_IN_LEG,
        EVENT_IN_LEG,
        OUTCOME_IN_LEG;
    }

    public BetInParlayResult checkBetInParlay( long eventID, BetEntity.BetOutcome outcome)    {
        int index = searchLegByEventID(eventID);
        if ( index > -1 ) {
            if (legs.get(index).getOutcome() == outcome)    {
                return BetInParlayResult.OUTCOME_IN_LEG;
            }
            else    {
                return BetInParlayResult.EVENT_IN_LEG;
            }
        }
        else {
            return BetInParlayResult.NOT_IN_LEG;
        }
    }

    public int searchLegByEventID( long eventID )   {
        int nIndex = 0;
        boolean found = false;
        for ( ParlayLegEntity currentLeg : legs )  {
            if (currentLeg.getEvent().getEventID() == eventID)  {
                found = true;
                break;
            }
            nIndex++;
        }
        return (found) ? nIndex : -1;
    }

    public String getAmount( )   {
        return strAmount;
    }

    public void setAmount( String amount )   {
        strAmount = amount;
    }

}

package com.wagerrwallet.presenter.entities;


import com.wagerrwallet.tools.util.BRDateUtil;

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
    public float getOdd()   {
        return odd;
    }
}

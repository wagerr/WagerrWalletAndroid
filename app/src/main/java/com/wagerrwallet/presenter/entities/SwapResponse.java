package com.wagerrwallet.presenter.entities;


/**
 * BreadWallet
 * <p>
 * Created by MIP (2020)
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 *
 * (c) Wagerr Betting platform 2020
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

public class SwapResponse {
    public static final String TAG = SwapResponse.class.getName();

    protected String transactionId;
    protected String depositWallet;
    protected String receivingAmount;

    public SwapResponse(String transactionId, String depositWallet, String receivingAmount) {
        this.transactionId = transactionId;
        this.depositWallet = depositWallet;
        this.receivingAmount = receivingAmount;
    }

    public SwapResponse()   {}

    public String getTransactionId() {
        return transactionId;
    }

    public String getDepositWallet() {
        return depositWallet;
    }

    public String getReceivingAmount() {
        return receivingAmount;
    }
}

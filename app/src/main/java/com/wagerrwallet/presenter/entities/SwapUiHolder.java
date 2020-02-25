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

public class SwapUiHolder {
    public static final String TAG = SwapUiHolder.class.getName();

    public enum TransactionState {
        awaiting("Awaiting Deposit"),
        swaping("Swaping"),
        withdraw("Withdraw"),
        completed("Completed"),
        notcompleted("Deposit Not Completed"),
        unknown("Unknown");

        private String type;
        TransactionState(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }

        public static TransactionState fromValue (String value) {
            // Just a linear search - easy, quick-enough.
            for (TransactionState txType : TransactionState.values())
                if (txType.type.equals(value))
                    return txType;
            return unknown;
        }
    }

    protected String transactionId;
    protected String depositCoin;
    protected String receiveCoin;
    protected String depositAmount;
    protected String receivingAmount;

    protected String refundWallet;
    protected String receiveWallet;
    protected String depositWallet;
    protected TransactionState transactionState;
    protected String timestamp;

    public SwapUiHolder(String transactionId, String depositCoin, String receiveCoin, String depositAmount, String receivingAmount,
                           String refundWallet, String receiveWallet, String depositWallet, TransactionState transactionState,
                           String timestamp) {
        this.transactionId = transactionId;
        this.depositCoin = depositCoin;
        this.receiveCoin = receiveCoin;
        this.depositAmount = depositAmount;
        this.receivingAmount = receivingAmount;
        this.refundWallet = refundWallet;
        this.receiveWallet = receiveWallet;
        this.depositWallet = depositWallet;
        this.transactionState = transactionState;
        this.timestamp = timestamp;
    }

    public SwapUiHolder()    {}

    public String getTransactionId() {
        return transactionId;
    }

    public String getDepositCoin() {
        return depositCoin;
    }

    public String getReceiveCoin() {
        return receiveCoin;
    }

    public String getDepositAmount() {
        return depositAmount;
    }

    public String getReceivingAmount() {
        return receivingAmount;
    }

    public String getRefundWallet() {
        return refundWallet;
    }

    public String getReceiveWallet() {
        return receiveWallet;
    }

    public String getDepositWallet() {
        return depositWallet;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public TransactionState getTransactionState() {
        return transactionState;
    }

}

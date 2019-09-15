package com.wagerrwallet.presenter.entities;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 1/13/16.
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

public class BetEntity {
    public static final String TAG = BetEntity.class.getName();

    public enum BetTxType {
        PEERLESS(0x03),
        CHAIN_LOTTO(0x07),
        UNKNOWN(-1);

        private int type;
        BetTxType(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static BetTxType fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (BetTxType txType : BetTxType.values())
                if (txType.type == value)
                    return txType;
            return UNKNOWN;
        }
    }

    public enum BetOutcome {
        MONEY_LINE_HOME_WIN(0x01),
        MONEY_LINE_AWAY_WIN(0x02),
        MONEY_LINE_DRAW(0x03),
        SPREADS_HOME(0x04),
        SPREADS_AWAY(0x05),
        TOTAL_OVER(0x06),
        TOTAL_UNDER(0x07),
        UNKNOWN(-1);

        private int type;
        BetOutcome(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static BetOutcome fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (BetOutcome outcome : BetOutcome.values())
                if (outcome.type == value)
                    return outcome;
            return UNKNOWN;
        }

        public String toString()    {
            String ret="Unknown";
            switch (this)  {
                case MONEY_LINE_HOME_WIN:
                    ret = "M.L. home";
                    break;
                case MONEY_LINE_AWAY_WIN:
                    ret = "M.L. away";
                    break;
                case MONEY_LINE_DRAW:
                    ret = "M.L. draw";
                    break;
                case SPREADS_HOME:
                    ret = "Sp. home";
                    break;
                case SPREADS_AWAY:
                    ret = "Sp. away";
                    break;
                case TOTAL_OVER:
                    ret = "Tot. Over";
                    break;
                case TOTAL_UNDER:
                    ret = "Tot. under";
                    break;
            }
            return ret;
        }
    }

    protected long blockheight;
    protected long timestamp;
    protected String txHash;
    protected String txISO;
    protected long version;
    protected BetTxType type;
    protected long eventID;
    protected BetOutcome outcome;
    protected long amount;


    // constructor for DB
    public BetEntity(String txHash, BetTxType type, long version,
                     long eventID, BetOutcome outcome, long amount,
                     long blockheight, long timestamp, String iso) {
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.txHash = txHash;
        this.txISO = iso;

        this.version = version;
        this.type = type;
        this.eventID = eventID;

        this.outcome = outcome;
        this.amount = amount;
    }

    protected BetEntity() {
    }

    public long getBlockheight() {
        return blockheight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getTxISO() {
        return txISO;
    }

    public long getVersion() {
        return version;
    }

    public BetTxType getType() {
        return type;
    }

    public long getEventID() {
        return eventID;
    }

    public BetOutcome getOutcome() {
        return outcome;
    }

    public long getAmount() {
        return amount;
    }
}

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

public class BetResultEntity {
    public static final String TAG = BetResultEntity.class.getName();

    public enum BetTxType {
        PEERLESS(0x04),
        CHAIN_LOTTO(0x08),
        UNKNOWN(-1);

        private int type;
        BetTxType(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static BetTxType fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (BetTxType resultType : BetTxType.values())
                if (resultType.type == value)
                    return resultType;
            return UNKNOWN;
        }
    }

    public enum BetResultType {
        STANDARD_PAYOUT(0x01),
        EVENT_REFUND(0x02),
        MONEYLINE_REFUND(0x03),
        UNKNOWN(-1);

        private int type;
        BetResultType(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static BetResultType fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (BetResultType resultType : BetResultType.values())
                if (resultType.type == value)
                    return resultType;
            return UNKNOWN;
        }
    }

    protected long blockheight;
    protected long timestamp;
    protected String txHash;
    protected String txISO;
    protected long version;
    protected BetTxType type;
    protected long eventID;
    protected BetResultType resultType;
    protected long homeScore;
    protected long awayScore;

    // constructor for DB
    public BetResultEntity(String txHash, BetTxType type, long version,
                           long eventID, BetResultType resultType, long homeScore, long awayScore,
                           long blockheight, long timestamp, String iso) {
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.txHash = txHash;
        this.txISO = iso;

        this.version = version;
        this.type = type;
        this.eventID = eventID;
        this.resultType = resultType;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }


    protected BetResultEntity() {
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

    public long getBlockheight() {
        return blockheight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTxHash() {
        return txHash;
    }

    public long getEventID() {
        return eventID;
    }

    public BetResultType getResultType() {
        return resultType;
    }

    public long getHomeScore() {
        return homeScore;
    }

    public long getAwayScore() {
        return awayScore;
    }
}

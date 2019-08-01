package com.wagerrwallet.presenter.entities;


import com.wagerrwallet.tools.crypto.WagerrOpCodeManager;

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

public class BetMappingEntity {
    public static final String TAG = BetMappingEntity.class.getName();

    public enum MappingNamespaceType {
        SPORT(0x01),
        ROUNDS(0x02),
        TEAM_NAME(0x03),
        TOURNAMENT(0x04),
        UNKNOWN(-1);

        private int type;
        MappingNamespaceType(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static MappingNamespaceType fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (MappingNamespaceType namespaceType : MappingNamespaceType.values())
                if (namespaceType.type == value)
                    return namespaceType;
            return UNKNOWN;
        }
    }

    private long blockheight;
    private long timestamp;
    private String txHash;
    private String txISO;

    private long version;
    private WagerrOpCodeManager.BetTransactionType type;
    private MappingNamespaceType namespaceID;
    private long mappingID;
    private String description;

    public BetMappingEntity(String txHash, long version, MappingNamespaceType namespaceID, long mappingID, String description,
                            long blockheight, long timestamp,  String iso) {
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.txHash = txHash;
        this.txISO = iso;

        this.version = version;
        this.type = WagerrOpCodeManager.BetTransactionType.MAPPING;
        this.namespaceID = namespaceID;
        this.mappingID = mappingID;
        this.description = description;
    }

    private BetMappingEntity() {
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

    public MappingNamespaceType getNamespaceID() {
        return namespaceID;
    }

    public long getMappingID() {
        return mappingID;
    }

    public String getDescription() {
        return description;
    }

    public String getTxISO() {
        return txISO;
    }

    public long getVersion() {
        return version;
    }

    public WagerrOpCodeManager.BetTransactionType getType() {
        return type;
    }
}

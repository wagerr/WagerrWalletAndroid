package com.wagerrwallet.tools.crypto;

import android.util.Log;

import com.wagerrwallet.core.BRCoreAddress;
import com.wagerrwallet.core.BRCoreTransaction;
import com.wagerrwallet.core.BRCoreTransactionOutput;
import com.wagerrwallet.presenter.entities.BetMappingEntity;
import com.wagerrwallet.tools.exceptions.WagerrTransactionException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/28/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 *
 *  (c) Wagerr Betting platform 2019
 *
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
public class WagerrOpCodeManager {
    public static final String TAG = WagerrOpCodeManager.class.getName();
    private static final int OP_RETURN=0x6a;
    private static final int SMOKE_TEST=0x42;

    private static final int OPCODE_POS=0;
    private static final int LENGHT_POS=1;
    private static final int SMOKE_TEST_POS=2;
    private static final int VERSION_POS=3;
    private static final int BTX_POS=4;
    private static final int NAMESPACE_POS=4;


    public static boolean DecodeBetTransaction(BRCoreTransaction tx) {
        boolean isBetTx = false;

        BRCoreTransactionOutput betOutput = null;
        for ( BRCoreTransactionOutput output : tx.getOutputs()) {
            BRCoreAddress address = new BRCoreAddress (output.getAddress());
            byte[] script = output.getScript();
            int opcode = script[OPCODE_POS] & 0xFF;
            int test = script[SMOKE_TEST_POS] & 0xFF;
            if (opcode==OP_RETURN && test==SMOKE_TEST)  {       // found wagerr bet tx!
                betOutput = output;
                break;
            }
        }

        if (betOutput != null) {
            try {
                byte[] script = betOutput.getScript();
                int opLength = script[LENGHT_POS] & 0xFF;
                if (opLength > 1) {
                    int opType = script[BTX_POS] & 0xFF;
                    BetTransactionType txType = BetTransactionType.fromValue(opType);
                    switch (txType) {
                        case MAPPING:
                            getMappingEntity(tx, script);
                            break;

                    }
                }
            }
            catch (WagerrTransactionException wEx) {
                Log.e(TAG, "Error processing bet tx " + wEx.getMessage());
            } catch (Exception ex) {
                Log.e(TAG, "Generic error processing the bet tx" + ex );
            }
        }
        return isBetTx;
    }

    protected static BetMappingEntity getMappingEntity( BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetMappingEntity mappingEntity = null;
        int opLength = script[LENGHT_POS] & 0xFF;
        int version = script[VERSION_POS] & 0xFF;
        int namespace = script[NAMESPACE_POS] & 0xFF;
        int mappingID = 0;
        BetMappingEntity.MappingNamespaceType namespaceType = BetMappingEntity.MappingNamespaceType.fromValue(namespace);

        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(script[NAMESPACE_POS+1]);
        bb.put(script[NAMESPACE_POS+2]);

        if ( namespaceType == BetMappingEntity.MappingNamespaceType.TEAM_NAME )
        {
            bb.put(script[NAMESPACE_POS+3]);
        }
        mappingID = bb.getInt();
        //byte[] stringBytes = Arrays.copyOfRange(script, from, to);
        //mappingEntity = new BetMappingEntity();
        return mappingEntity;
    }

    public enum BetTransactionType {
        MAPPING(0x01),
        EVENT_PEERLESS(0x02),
        BET_PEERLESS(0x03),
        RESULT_PEERLESS(0x04),
        UPDATE_PEERLESS((0x05)),
        EVENT_CHAIN_LOTTO(0x06),
        BET_CHAIN_LOTTO(0x07),
        RESULT_CHAIN_LOTTO(0x08),
        EVENT_PEERLESS_SPREAD(0x09),
        EVENT_PEERLESS_TOTAL(0x0a),
        UNKNOWN(-1);

        private int type;
        BetTransactionType(int type) {
            this.type = type;
        }

        public int getNumber()    {return type;}

        public static BetTransactionType fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (BetTransactionType txType : BetTransactionType.values())
                if (txType.type == value)
                    return txType;
            return UNKNOWN;
        }
    }

}

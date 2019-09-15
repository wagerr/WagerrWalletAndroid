package com.wagerrwallet.tools.crypto;

import android.content.Context;
import android.util.Log;

import com.wagerrwallet.BuildConfig;
import com.wagerrwallet.core.BRCoreAddress;
import com.wagerrwallet.core.BRCoreTransaction;
import com.wagerrwallet.core.BRCoreTransactionOutput;
import com.wagerrwallet.presenter.entities.BetEntity;
import com.wagerrwallet.presenter.entities.BetEventEntity;
import com.wagerrwallet.presenter.entities.BetMappingEntity;
import com.wagerrwallet.presenter.entities.BetResultEntity;
import com.wagerrwallet.presenter.entities.CryptoRequest;
import com.wagerrwallet.tools.exceptions.WagerrTransactionException;
import com.wagerrwallet.tools.sqlite.BetEventTxDataStore;
import com.wagerrwallet.tools.sqlite.BetMappingTxDataStore;
import com.wagerrwallet.tools.sqlite.BetResultTxDataStore;
import com.wagerrwallet.tools.sqlite.BetTxDataStore;
import com.wagerrwallet.tools.util.BytesUtil;
import com.wagerrwallet.tools.util.PositionPointer;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

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

    public static BetEntity getEventIdFromCoreTx( BRCoreTransaction tx ) {
        BetEntity ret = null;
        BetTransactionType txType = null;
        BetEntity betEntity = null;
        long betAmount = 0;

        BRCoreTransactionOutput betOutput = null;
        for ( BRCoreTransactionOutput output : tx.getOutputs()) {
            BRCoreAddress address = new BRCoreAddress (output.getAddress());
            byte[] script = output.getScript();
            if (script.length<=BTX_POS)   continue;    // prevent crash

            int opcode = script[OPCODE_POS] & 0xFF;
            int test = script[SMOKE_TEST_POS] & 0xFF;
            try {
                if (opcode == OP_RETURN && test == SMOKE_TEST) {       // found wagerr bet tx!
                    int opType = script[BTX_POS] & 0xFF;
                    txType = BetTransactionType.fromValue(opType);
                    switch (txType) {
                        case BET_PEERLESS:
                            ret = getPeerlessBet(tx, script, betAmount);
                            break;

                        case BET_CHAIN_LOTTO:
                            ret = getChainGamesBetEntity(tx, script, betAmount);
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
        return ret;
    }

    public static boolean DecodeBetTransaction(Context app,  BRCoreTransaction tx) {
        boolean isBetTx = false;
        long betAmount = 0;
        BetTransactionType txType = null;

        //if (true) return isBetTx;

        BRCoreTransactionOutput betOutput = null;
        for ( BRCoreTransactionOutput output : tx.getOutputs()) {
            BRCoreAddress address = new BRCoreAddress (output.getAddress());
            byte[] script = output.getScript();
            int opcode = script[OPCODE_POS] & 0xFF;
            int test = script[SMOKE_TEST_POS] & 0xFF;
            if (opcode==OP_RETURN && test==SMOKE_TEST)  {       // found wagerr bet tx!
                betOutput = output;
                isBetTx=true;
            }
            else {      // else accumulate bet amount
                betAmount += output.getAmount();
            }
        }

        if (betOutput != null) {
            try {
                BetMappingEntity betMappingEntity = null;
                BetEventEntity betEventEntity = null;
                BetEntity betEntity = null;
                BetResultEntity betResultEntity = null;
                boolean updateEntity = false;

                byte[] script = betOutput.getScript();
                int opLength = script[LENGHT_POS] & 0xFF;
                if (opLength > 1) {
                    int opType = script[BTX_POS] & 0xFF;
                    txType = BetTransactionType.fromValue(opType);
                    switch (txType) {
                        case MAPPING:
                            betMappingEntity = getMappingEntity(tx, script);
                            break;

                        case EVENT_PEERLESS:
                            betEventEntity = getPeerlessEventEntity(tx, script);
                            break;

                        case BET_PEERLESS:
                            betEntity = getPeerlessBet(tx, script, betAmount);
                            break;

                        case RESULT_PEERLESS:
                            betResultEntity = getPeerlessResult(tx,script);
                            break;

                        case UPDATE_PEERLESS:
                            betEventEntity = getPeerlessUpdateOddsEntity(tx, script);
                            updateEntity = true;
                            break;

                        case EVENT_CHAIN_LOTTO:
                            betEventEntity = getChainGamesLottoEventEntity(tx, script);
                            break;

                        case BET_CHAIN_LOTTO:
                            betEntity = getChainGamesBetEntity(tx, script, betAmount);
                            break;

                        case RESULT_CHAIN_LOTTO:
                            betResultEntity = getChainGamesLottoResult(tx, script);

                        case EVENT_PEERLESS_SPREAD:
                            betEventEntity = getPeerlessSpreadsMarket(tx, script);
                            break;

                        case EVENT_PEERLESS_TOTAL:
                            betEventEntity = getPeerlessTotalsMarket(tx, script);
                            break;
                    }
                }

                if (betMappingEntity != null)
                {
                    BetMappingTxDataStore bmds = BetMappingTxDataStore.getInstance(app);
                    Log.e(TAG, "storing bettx mapping :" + tx.getReverseHash() + ", " + tx.getBlockHeight() + "\n" + betMappingEntity );
                    BetMappingEntity betMappingEntity1 = bmds.putTransaction(app, WalletWagerrManager.ISO, betMappingEntity );
                    Log.e(TAG, "retrieving bettx mapping from DB: "  + tx.getReverseHash() + "\n" + betMappingEntity1 );
                }

                if (betEventEntity != null)
                {
                    BetEventTxDataStore beds = BetEventTxDataStore.getInstance(app);
                    switch (txType) {
                        case EVENT_PEERLESS:
                        case EVENT_CHAIN_LOTTO:
                            beds.putTransaction( app, WalletWagerrManager.ISO, betEventEntity);
                            break;

                        case UPDATE_PEERLESS:
                            beds.updateOdds( app, WalletWagerrManager.ISO, betEventEntity);
                            break;

                        case EVENT_PEERLESS_SPREAD:
                            beds.updateSpreadsMarket( app, WalletWagerrManager.ISO, betEventEntity);
                            break;

                        case EVENT_PEERLESS_TOTAL:
                            beds.updateTotalsMarket( app, WalletWagerrManager.ISO, betEventEntity);
                            break;
                    }
                    Log.e(TAG, "storing betEvent: "  + tx.getReverseHash() + ", " + tx.getBlockHeight() + ", " + txType.toString() +  "\n" + betEventEntity );
                }

                if (betEntity != null)
                {
                    BetTxDataStore btds = BetTxDataStore.getInstance(app);
                    btds.putTransaction( app, WalletWagerrManager.ISO, betEntity);
                }

                if (betResultEntity != null)
                {
                    BetResultTxDataStore beds = BetResultTxDataStore.getInstance(app);
                    Log.e(TAG, "storing bettx result :" + tx.getReverseHash() + ", " + tx.getBlockHeight() + "\n" + betResultEntity );
                    BetResultEntity betResultEntity1 = beds.putTransaction( app, WalletWagerrManager.ISO, betResultEntity);
                    Log.e(TAG, "retrieving bettx result from DB: "  + tx.getReverseHash() + "\n" + betResultEntity1 );
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

    protected static ByteOrder getByteOrder(long height)
    {
        ByteOrder ret = ByteOrder.LITTLE_ENDIAN;
        /*if (BuildConfig.BITCOIN_TESTNET) {
            if (height<100000)
                ret = ByteOrder.BIG_ENDIAN;
        }*/
        return ret;
    }

    protected static void passSmokeTestByte( String txHash, int testByte ) throws WagerrTransactionException
    {
        if (testByte!=SMOKE_TEST)
            throw new WagerrTransactionException("Bet transaction " + txHash + ": bad smoke test byte");
    }

    private static final int NAMESPACE_POS=5;

    protected static BetMappingEntity getMappingEntity( BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetMappingEntity mappingEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < NAMESPACE_POS+2 )
            throw new WagerrTransactionException("getMappingEntity " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far
        int namespace = script[NAMESPACE_POS] & 0xFF;
        BetMappingEntity.MappingNamespaceType namespaceType = BetMappingEntity.MappingNamespaceType.fromValue(namespace);
        if (namespaceType == BetMappingEntity.MappingNamespaceType.UNKNOWN)
            throw new WagerrTransactionException("getMappingEntity " + txHash + ": wrong namespace");

        int mappingID = 0;
        int to = opLength+2;
        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(NAMESPACE_POS+1);
        if ( namespaceType == BetMappingEntity.MappingNamespaceType.TEAM_NAME )
        {
            mappingID = getBufferInt( script, pos, byteOrder );
        }
        else {
            mappingID = getBufferShort( script, pos, byteOrder );
        }
        byte[] stringBytes = Arrays.copyOfRange(script, pos.getPos(), to);
        String description = new String(stringBytes);
        mappingEntity = new BetMappingEntity( txHash , version, namespaceType, mappingID, description,
                                            tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO);
        return mappingEntity;
    }

    private static final int EVENTID_POS=5;
    private static final int PEERLESS_EVENT_LENGTH=37;

    protected static BetEventEntity getPeerlessEventEntity(BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetEventEntity eventEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < PEERLESS_EVENT_LENGTH )
            throw new WagerrTransactionException("getPeerlessEventEntity " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far
        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);
        int eventTimestamp = getBufferInt( script, pos, byteOrder);
        short sportID = getBufferShort( script, pos, byteOrder);
        short tournamentID = getBufferShort( script, pos, byteOrder);
        short roundID = getBufferShort( script, pos, byteOrder);
        int homeTeamID = getBufferInt( script, pos, byteOrder);
        int awayTeamID = getBufferInt( script, pos, byteOrder);
        int homeOdds = getBufferInt( script, pos, byteOrder);
        int awayOdds = getBufferInt( script, pos, byteOrder);
        int drawOdds = getBufferInt( script, pos, byteOrder);

        eventEntity = new BetEventEntity( txHash , BetEventEntity.BetTxType.PEERLESS, version, eventID, eventTimestamp,
                sportID, tournamentID, roundID, homeTeamID, awayTeamID, homeOdds, awayOdds, drawOdds,
                0,0,0,0,0,0,0,
                tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO, tx.getTimestamp());
        return eventEntity;
    }

    private static final int PEERLESS_BET_LENGTH=8;

    protected static BetEntity getPeerlessBet(BRCoreTransaction tx, byte[] script, long betAmount ) throws WagerrTransactionException
    {
        BetEntity betEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < PEERLESS_BET_LENGTH )
            throw new WagerrTransactionException("getPeerlessBet " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far

        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);
        BetEntity.BetOutcome outcomeID = BetEntity.BetOutcome.fromValue(script[pos.getPos()] & 0xFF);

        betEntity = new BetEntity( txHash , BetEntity.BetTxType.PEERLESS, version, eventID, outcomeID, betAmount,
                tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO);
        return betEntity;
    }

    private static final int PEERLESS_RESULT_LENGTH=10;

    protected static BetResultEntity getPeerlessResult(BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetResultEntity betResultEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < PEERLESS_RESULT_LENGTH )
            throw new WagerrTransactionException("getPeerlessResult " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far

        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);
        BetResultEntity.BetResultType resultType = BetResultEntity.BetResultType.fromValue(script[pos.getPos()] & 0xFF);
        pos.Up(1);
        short homeTeamScore = getBufferShort( script, pos, byteOrder);
        short awayTeamScore = getBufferShort( script, pos, byteOrder);

        betResultEntity = new BetResultEntity( txHash , BetResultEntity.BetTxType.PEERLESS, version, eventID, resultType,
                homeTeamScore, awayTeamScore, tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO);
        return betResultEntity;
    }

    private static final int PEERLESS_UPDATEODDS_LENGTH=19;

    protected static BetEventEntity getPeerlessUpdateOddsEntity(BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetEventEntity eventEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < PEERLESS_UPDATEODDS_LENGTH )
            throw new WagerrTransactionException("getPeerlessUpdateOddsEntity " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far
        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);
        int homeOddsID = getBufferInt( script, pos, byteOrder);
        int awayOddsID = getBufferInt( script, pos, byteOrder);
        int drawOddsID = getBufferInt( script, pos, byteOrder);

        eventEntity = new BetEventEntity( txHash , BetEventEntity.BetTxType.UPDATEODDS, version, eventID, 0,
                0, 0, 0, 0, 0, homeOddsID, awayOddsID, drawOddsID,
                0,0,0,0,0,0,0,
                tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO, tx.getTimestamp());
        return eventEntity;
    }

    private static final int CHAIN_GAMES_LOTTO_EVENT_LENGTH=7;

    protected static BetEventEntity getChainGamesLottoEventEntity(BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetEventEntity eventEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < CHAIN_GAMES_LOTTO_EVENT_LENGTH )
            throw new WagerrTransactionException("getChainGamesLottoEventEntity " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far
        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        short eventID = getBufferShort( script, pos, byteOrder);
        short entryPrice = getBufferShort(script, pos, byteOrder);

        eventEntity = new BetEventEntity( txHash , BetEventEntity.BetTxType.CHAIN_LOTTO, version, eventID, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                entryPrice,0,0,0,0,0,0,
                tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO, tx.getTimestamp());
        return eventEntity;
    }

    private static final int CHAIN_GAMES_LOTTO_BET_LENGTH=5;

    protected static BetEntity getChainGamesBetEntity(BRCoreTransaction tx, byte[] script, long betAmount ) throws WagerrTransactionException
    {
        BetEntity betEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < CHAIN_GAMES_LOTTO_BET_LENGTH )
            throw new WagerrTransactionException("getChainGamesBetEntity " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far

        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);

        betEntity = new BetEntity( txHash , BetEntity.BetTxType.CHAIN_LOTTO, version, eventID, BetEntity.BetOutcome.UNKNOWN, betAmount,
                tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO);
        return betEntity;
    }

    private static final int CHAIN_GAMES_LOTTO_RESULT_LENGTH=5;

    protected static BetResultEntity getChainGamesLottoResult(BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetResultEntity betResultEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < CHAIN_GAMES_LOTTO_RESULT_LENGTH )
            throw new WagerrTransactionException("getChainGamesLottoResult " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far

        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);

        betResultEntity = new BetResultEntity( txHash , BetResultEntity.BetTxType.CHAIN_LOTTO, version, eventID, BetResultEntity.BetResultType.UNKNOWN,
                0, 0, tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO);
        return betResultEntity;
    }

    private static final int PEERLESS_SPREAD_MARKET_LENGTH=17;

    protected static BetEventEntity getPeerlessSpreadsMarket(BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetEventEntity eventEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < PEERLESS_SPREAD_MARKET_LENGTH )
            throw new WagerrTransactionException("getPeerlessSpreadsMarket " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far
        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);
        short spreadPoints = getBufferShort( script, pos, byteOrder);
        int homeOdds = getBufferInt( script, pos, byteOrder);
        int awayOdds = getBufferInt( script, pos, byteOrder);

        eventEntity = new BetEventEntity( txHash , BetEventEntity.BetTxType.PEERLESS_SPREAD, version, eventID, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, spreadPoints, homeOdds, awayOdds,0,0,0,
                tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO, tx.getTimestamp());
        return eventEntity;
    }

    private static final int PEERLESS_TOTALS_MARKET_LENGTH=17;

    protected static BetEventEntity getPeerlessTotalsMarket(BRCoreTransaction tx, byte[] script ) throws WagerrTransactionException
    {
        BetEventEntity eventEntity = null;
        String txHash = Utils.reverseHex(Utils.bytesToHex(tx.getHash()));
        int testByte = script[SMOKE_TEST_POS] & 0xFF;
        passSmokeTestByte( txHash, testByte);

        int opLength = script[LENGHT_POS] & 0xFF;
        if (opLength < PEERLESS_TOTALS_MARKET_LENGTH )
            throw new WagerrTransactionException("getPeerlessTotalsMarket " + txHash + ": wrong var_str length");

        int version = script[VERSION_POS] & 0xFF;   // ignore value so far
        ByteOrder byteOrder = getByteOrder(tx.getBlockHeight());
        PositionPointer pos = new PositionPointer(EVENTID_POS);
        int eventID = getBufferInt( script, pos, byteOrder);
        short totalPoints = getBufferShort( script, pos, byteOrder);
        int overOdds = getBufferInt( script, pos, byteOrder);
        int underOdds = getBufferInt( script, pos, byteOrder);

        eventEntity = new BetEventEntity( txHash , BetEventEntity.BetTxType.PEERLESS_TOTAL, version, eventID, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0,0, 0,0, totalPoints, overOdds, underOdds,
                tx.getBlockHeight(), tx.getTimestamp(), WalletWagerrManager.ISO, tx.getTimestamp());

        return eventEntity;
    }

    protected static short getBufferShort( byte[] script, PositionPointer startPointer, ByteOrder byteOrder  )
    {
        int start = startPointer.getPos();
        short ret = (byteOrder==ByteOrder.LITTLE_ENDIAN) ? BytesUtil.byteArrayToLeShort(script, start) : BytesUtil.byteArrayToGeShort(script, start);
        startPointer.Up(2);
        return ret;
    }

    protected static int getBufferInt( byte[] script, PositionPointer startPointer, ByteOrder byteOrder )
    {
        int start = startPointer.getPos();
        int ret = (byteOrder==ByteOrder.LITTLE_ENDIAN) ? BytesUtil.byteArrayToLeInt(script, start) : BytesUtil.byteArrayToGeInt(script, start);
        startPointer.Up(4);
        return ret;
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

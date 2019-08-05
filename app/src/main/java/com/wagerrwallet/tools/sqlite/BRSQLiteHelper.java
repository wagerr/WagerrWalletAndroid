package com.wagerrwallet.tools.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wagerrwallet.BuildConfig;
import com.wagerrwallet.presenter.entities.BRTransactionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/25/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
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

public class BRSQLiteHelper extends SQLiteOpenHelper {
    private static final String TAG = BRSQLiteHelper.class.getName();
    private static BRSQLiteHelper instance;

    private BRSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static BRSQLiteHelper getInstance(Context context) {
        if (instance == null) instance = new BRSQLiteHelper(context);
        return instance;
    }

    public static final String DATABASE_NAME = "breadwallet.db";
    private static final int DATABASE_VERSION = 15;

    /**
     * MerkleBlock table
     */
    public static final String MB_TABLE_NAME_OLD = "merkleBlockTable";
    public static final String MB_TABLE_NAME = "merkleBlockTable_v2";
    public static final String MB_COLUMN_ID = "_id";
    public static final String MB_BUFF = "merkleBlockBuff";
    public static final String MB_HEIGHT = "merkleBlockHeight";
    public static final String MB_ISO = "merkleBlockIso";

    private static final String MB_DATABASE_CREATE = "create table if not exists " + MB_TABLE_NAME + " (" +
            MB_COLUMN_ID + " integer primary key autoincrement, " +
            MB_BUFF + " blob, " +
            MB_ISO + " text DEFAULT 'BTC' , " +
            MB_HEIGHT + " integer);";

    /**
     * Transaction table
     */

    public static final String TX_TABLE_NAME_OLD = "transactionTable";
    public static final String TX_TABLE_NAME = "transactionTable_v2";
    public static final String TX_COLUMN_ID = "_id";
    public static final String TX_BUFF = "transactionBuff";
    public static final String TX_BLOCK_HEIGHT = "transactionBlockHeight";
    public static final String TX_TIME_STAMP = "transactionTimeStamp";
    public static final String TX_ISO = "transactionISO";

    private static final String TX_DATABASE_CREATE = "create table if not exists " + TX_TABLE_NAME + " (" +
            TX_COLUMN_ID + " text, " +
            TX_BUFF + " blob, " +
            TX_BLOCK_HEIGHT + " integer, " +
            TX_TIME_STAMP + " integer, " +
            TX_ISO + " text DEFAULT 'BTC' );";

    /**
     * Betting Transaction tables
     * betMappingTxTable
     * betEventTxTable
     * betResultTxTable
     * betTransactionTable
     * betUpdateOddsTxTable
     */

    // betMappingTxTable TYPE 01
    public static final String BMTX_TABLE_NAME = "betMappingTxTable";
    public static final String BMTX_COLUMN_ID = "_id";
    public static final String BMTX_TYPE = "type";
    public static final String BMTX_VERSION = "version";
    public static final String BMTX_BLOCK_HEIGHT = "transactionBlockHeight";
    public static final String BMTX_TIMESTAMP = "transactionTimeStamp";
    public static final String BMTX_ISO = "transactionISO";
    public static final String BMTX_NAMESPACEID = "namespaceId";
    public static final String BMTX_MAPPINGID = "mappingId";
    public static final String BMTX_STRING = "string";

    private static final String BMTX_DATABASE_CREATE = "create table if not exists " + BMTX_TABLE_NAME + " (" +
            BMTX_COLUMN_ID + " text, " +
            BMTX_TYPE + " integer, " +
            BMTX_VERSION + " integer, " +
            BMTX_NAMESPACEID + " integer, " +
            BMTX_MAPPINGID + " integer, " +
            BMTX_STRING + " text, " +
            BMTX_BLOCK_HEIGHT + " integer, " +
            BMTX_TIMESTAMP + " integer, " +
            BMTX_ISO + " text DEFAULT 'WGR' );";

    //  betEventTxTable 02 06 09 0a
    public static final String BETX_TABLE_NAME = "betEventTxTable";
    public static final String BETX_COLUMN_ID = "_id";
    public static final String BETX_TYPE = "type";
    public static final String BETX_VERSION = "version";
    public static final String BETX_BLOCK_HEIGHT = "transactionBlockHeight";
    public static final String BETX_TIMESTAMP = "transactionTimeStamp";
    public static final String BETX_ISO = "transactionISO";
    public static final String BETX_EVENTID = "eventId";
    public static final String BETX_EVENT_TIMESTAMP = "eventTimestamp";
    public static final String BETX_SPORT = "eventSport";
    public static final String BETX_TOURNAMENT = "eventTournament";
    public static final String BETX_ROUND = "eventRound";
    public static final String BETX_HOME_TEAM = "homeTeam";
    public static final String BETX_AWAY_TEAM = "awayTeam";
    public static final String BETX_HOME_ODDS = "homeOdds";
    public static final String BETX_AWAY_ODDS = "awayOdds";
    public static final String BETX_DRAW_ODDS = "drawOdds";
    public static final String BETX_ENTRY_PRICE = "entryPrice";
    public static final String BETX_SPREAD_POINTS = "spreadPoints";
    public static final String BETX_TOTAL_POINTS = "totalPoints";
    public static final String BETX_OVER_ODDS = "overOdds";
    public static final String BETX_UNDER_ODDS = "underOdds";

    private static final String BETX_DATABASE_CREATE = "create table if not exists " + BETX_TABLE_NAME + " (" +
            BETX_COLUMN_ID + " text, " +
            BETX_TYPE + " integer, " +
            BETX_VERSION + " integer, " +
            BETX_EVENTID + " integer, " +
            BETX_EVENT_TIMESTAMP + " integer, " +
            BETX_SPORT + " integer, " +
            BETX_TOURNAMENT + " integer, " +
            BETX_ROUND + " integer, " +
            BETX_HOME_TEAM + " integer, " +
            BETX_AWAY_TEAM + " integer, " +
            BETX_HOME_ODDS + " integer, " +
            BETX_AWAY_ODDS + " integer, " +
            BETX_DRAW_ODDS + " integer, " +
            BETX_ENTRY_PRICE + " integer, " +
            BETX_SPREAD_POINTS + " integer, " +
            BETX_TOTAL_POINTS + " integer, " +
            BETX_OVER_ODDS + " integer, " +
            BETX_UNDER_ODDS + " integer, " +
            BETX_BLOCK_HEIGHT + " integer, " +
            BETX_TIMESTAMP + " integer, " +
            BETX_ISO + " text DEFAULT 'WGR' );";

    //  betResultTxTable TYPE 04 08
    public static final String BRTX_TABLE_NAME = "betResultTxTable";
    public static final String BRTX_COLUMN_ID = "_id";
    public static final String BRTX_TYPE = "type";
    public static final String BRTX_VERSION = "version";
    public static final String BRTX_BLOCK_HEIGHT = "transactionBlockHeight";
    public static final String BRTX_TIMESTAMP = "transactionTimeStamp";
    public static final String BRTX_ISO = "transactionISO";
    public static final String BRTX_EVENTID = "eventId";
    public static final String BRTX_RESULTS_TYPE = "resultType";   // standard payout is 1    event refund is 02    moneyline refund is 03
    public static final String BRTX_HOME_TEAM_SCORE = "homeTeamScore";
    public static final String BRTX_AWAY_TEAM_SCORE = "awayTeamScore";

    private static final String BRTX_DATABASE_CREATE = "create table if not exists " + BRTX_TABLE_NAME + " (" +
            BRTX_COLUMN_ID + " text, " +
            BRTX_TYPE + " integer, " +
            BRTX_VERSION + " integer, " +
            BRTX_EVENTID + " integer, " +
            BRTX_RESULTS_TYPE + " integer, " +
            BRTX_HOME_TEAM_SCORE + " integer, " +
            BRTX_AWAY_TEAM_SCORE + " integer, " +
            BRTX_BLOCK_HEIGHT + " integer, " +
            BRTX_TIMESTAMP + " integer, " +
            BRTX_ISO + " text DEFAULT 'WGR' );";

    //  betTransactionTable  TYPE 03 07
    public static final String BTX_TABLE_NAME = "betTransactionTable";
    public static final String BTX_COLUMN_ID = "_id";
    public static final String BTX_TYPE = "type";
    public static final String BTX_VERSION = "version";
    public static final String BTX_BLOCK_HEIGHT = "transactionBlockHeight";
    public static final String BTX_TIME_STAMP = "transactionTimeStamp";
    public static final String BTX_ISO = "transactionISO";
    public static final String BTX_EVENTID = "eventId";
    public static final String BTX_OUTCOME   = "eventOutcome";
    public static final String BTX_AMOUNT   = "betAmount";

    private static final String BTX_DATABASE_CREATE = "create table if not exists " + BTX_TABLE_NAME + " (" +
            BTX_COLUMN_ID + " text, " +
            BTX_TYPE + " integer, " +
            BTX_VERSION + " integer, " +
            BTX_EVENTID + " integer, " +
            BTX_OUTCOME + " integer, " +
            BTX_AMOUNT + " integer, " +
            BTX_BLOCK_HEIGHT + " integer, " +
            BTX_TIME_STAMP + " integer, " +
            BTX_ISO + " text DEFAULT 'WGR' );";

    /**
     * Peer table
     */

    public static final String PEER_TABLE_NAME_OLD = "peerTable";
    public static final String PEER_TABLE_NAME = "peerTable_v2";
    public static final String PEER_COLUMN_ID = "_id";
    public static final String PEER_ADDRESS = "peerAddress";
    public static final String PEER_PORT = "peerPort";
    public static final String PEER_TIMESTAMP = "peerTimestamp";
    public static final String PEER_ISO = "peerIso";

    private static final String PEER_DATABASE_CREATE = "create table if not exists " + PEER_TABLE_NAME + " (" +
            PEER_COLUMN_ID + " integer primary key autoincrement, " +
            PEER_ADDRESS + " blob," +
            PEER_PORT + " blob," +
            PEER_TIMESTAMP + " blob," +
            PEER_ISO + "  text default 'BTC');";
    /**
     * Currency table
     */

    public static final String CURRENCY_TABLE_NAME_OLD = "currencyTable";
    public static final String CURRENCY_TABLE_NAME = "currencyTable_v2";
    public static final String CURRENCY_CODE = "code";
    public static final String CURRENCY_NAME = "name";
    public static final String CURRENCY_RATE = "rate";
    public static final String CURRENCY_ISO = "iso";//iso for the currency of exchange (BTC, BCH, ETH)

    private static final String CURRENCY_DATABASE_CREATE = "create table if not exists " + CURRENCY_TABLE_NAME + " (" +
            CURRENCY_CODE + " text," +
            CURRENCY_NAME + " text," +
            CURRENCY_RATE + " integer," +
            CURRENCY_ISO + " text DEFAULT 'BTC', " +
            "PRIMARY KEY (" + CURRENCY_CODE + ", " + CURRENCY_ISO + ")" +
            ");";


    @Override
    public void onCreate(SQLiteDatabase database) {
        //drop peers table due to multiple changes

        Log.e(TAG, "onCreate: " + MB_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + TX_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + PEER_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + CURRENCY_DATABASE_CREATE);
        database.execSQL(MB_DATABASE_CREATE);
        database.execSQL(TX_DATABASE_CREATE);
        database.execSQL(PEER_DATABASE_CREATE);
        database.execSQL(CURRENCY_DATABASE_CREATE);

        // betting DB
        Log.e(TAG, "onCreate: " + BTX_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + BETX_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + BMTX_DATABASE_CREATE);
        Log.e(TAG, "onCreate: " + BRTX_DATABASE_CREATE);
        database.execSQL(BTX_DATABASE_CREATE);
        database.execSQL(BETX_DATABASE_CREATE);
        database.execSQL(BMTX_DATABASE_CREATE);
        database.execSQL(BRTX_DATABASE_CREATE);

//        printTableStructures(database, MB_TABLE_NAME);
//        printTableStructures(database, TX_TABLE_NAME);
//        printTableStructures(database, PEER_TABLE_NAME);
//        printTableStructures(database, CURRENCY_TABLE_NAME);

//        database.execSQL("PRAGMA journal_mode=WAL;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 13 && (newVersion >= 13)) {
            boolean migrationNeeded = !tableExists(MB_TABLE_NAME, db);
            onCreate(db); //create new db tables

            if (migrationNeeded)
                migrateDatabases(db);
        } else {
            //drop everything maybe?
//            db.execSQL("DROP TABLE IF EXISTS " + MB_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + TX_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + PEER_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + CURRENCY_TABLE_NAME);
//            db.execSQL("PRAGMA journal_mode=WAL;");
        }
        //recreate if needed

    }

    private void migrateDatabases(SQLiteDatabase db) {
        db.beginTransaction();
        try {

            db.execSQL("INSERT INTO " + MB_TABLE_NAME + " (_id, merkleBlockBuff, merkleBlockHeight) SELECT _id, merkleBlockBuff, merkleBlockHeight FROM " + MB_TABLE_NAME_OLD);
            db.execSQL("INSERT INTO " + TX_TABLE_NAME + " (_id, transactionBuff, transactionBlockHeight, transactionTimeStamp) SELECT _id, transactionBuff, transactionBlockHeight, transactionTimeStamp FROM " + TX_TABLE_NAME_OLD);
            db.execSQL("INSERT INTO " + CURRENCY_TABLE_NAME + " (code, name, rate) SELECT code, name, rate FROM " + CURRENCY_TABLE_NAME_OLD);

            db.execSQL("DROP TABLE IF EXISTS " + PEER_TABLE_NAME_OLD);//drop this table (fully refactored schema)
            db.execSQL("DROP TABLE IF EXISTS " + MB_TABLE_NAME_OLD);
            db.execSQL("DROP TABLE IF EXISTS " + TX_TABLE_NAME_OLD);
            db.execSQL("DROP TABLE IF EXISTS " + CURRENCY_TABLE_NAME_OLD);

            copyTxsForBch(db);

            db.setTransactionSuccessful();
            Log.e(TAG, "migrateDatabases: SUCCESS");
        } finally {
            Log.e(TAG, "migrateDatabases: ENDED");
            db.endTransaction();
        }
    }

    public boolean tableExists(String tableName, SQLiteDatabase db) {

        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    private void copyTxsForBch(SQLiteDatabase db) {
        List<BRTransactionEntity> transactions = new ArrayList<>();
        Cursor cursorGet = null;
        int bCashForkBlockHeight = BuildConfig.BITCOIN_TESTNET ? 1155876 : 478559;
        int bCashForkTimeStamp = BuildConfig.BITCOIN_TESTNET ? 1501597117 : 1501568580;
        db.beginTransaction();
        try {
            cursorGet = db.query(BRSQLiteHelper.TX_TABLE_NAME,
                    BtcBchTransactionDataStore.allColumns, BRSQLiteHelper.TX_ISO + "=? AND " + BRSQLiteHelper.TX_BLOCK_HEIGHT + " <?", new String[]{"BTC", String.valueOf(bCashForkBlockHeight)}, null, null, null);

            cursorGet.moveToFirst();
            while (!cursorGet.isAfterLast()) {
                BRTransactionEntity transactionEntity = BtcBchTransactionDataStore.cursorToTransaction(null, "BTC", cursorGet);
                transactions.add(transactionEntity);
                cursorGet.moveToNext();
            }

            int count = 0;
            for (BRTransactionEntity tx : transactions) {
                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.TX_COLUMN_ID, tx.getTxHash());
                values.put(BRSQLiteHelper.TX_BUFF, tx.getBuff());
                values.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, tx.getBlockheight());
                values.put(BRSQLiteHelper.TX_ISO, "BCH");
                values.put(BRSQLiteHelper.TX_TIME_STAMP, tx.getTimestamp());

                db.insert(BRSQLiteHelper.TX_TABLE_NAME, null, values);
                count++;

            }
            Log.e(TAG, "copyTxsForBch: copied: " + count);
            db.setTransactionSuccessful();

        } finally {
            if (cursorGet != null)
                cursorGet.close();
            db.endTransaction();
        }
    }

    public void printTableStructures(SQLiteDatabase db, String tableName) {
        Log.e(TAG, "printTableStructures: " + tableName);
        String tableString = String.format("Table %s:\n", tableName);
        Cursor allRows = db.rawQuery("SELECT * FROM " + tableName, null);
        if (allRows.moveToFirst()) {
            String[] columnNames = allRows.getColumnNames();
            do {
                for (String name : columnNames) {
                    tableString += String.format("%s: %s\n", name,
                            allRows.getString(allRows.getColumnIndex(name)));
                }
                tableString += "\n";

            } while (allRows.moveToNext());
        }

        Log.e(TAG, "SQL:" + tableString);
    }

}

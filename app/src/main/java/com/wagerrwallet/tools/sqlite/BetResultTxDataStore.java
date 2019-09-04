package com.wagerrwallet.tools.sqlite;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/25/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 *
 * (c) Wagerr Betting platform 2019
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.wagerrwallet.presenter.entities.BetEventEntity;
import com.wagerrwallet.presenter.entities.BetResultEntity;
import com.wagerrwallet.tools.manager.BRReportsManager;
import com.wagerrwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

public class BetResultTxDataStore implements BRDataSourceInterface {
    private static final String TAG = BetResultTxDataStore.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    public static final String[] allColumns = {
            BRSQLiteHelper.BRTX_COLUMN_ID,
            BRSQLiteHelper.BRTX_TYPE,
            BRSQLiteHelper.BRTX_VERSION,
            BRSQLiteHelper.BRTX_EVENTID,
            BRSQLiteHelper.BRTX_RESULTS_TYPE,
            BRSQLiteHelper.BRTX_HOME_TEAM_SCORE,
            BRSQLiteHelper.BRTX_AWAY_TEAM_SCORE,
            BRSQLiteHelper.BRTX_BLOCK_HEIGHT,
            BRSQLiteHelper.BRTX_TIMESTAMP,
            BRSQLiteHelper.BRTX_ISO
    };

    private static BetResultTxDataStore instance;

    public static BetResultTxDataStore getInstance(Context context) {
        if (instance == null) {
            instance = new BetResultTxDataStore(context);
        }
        return instance;
    }

    private BetResultTxDataStore(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);

    }

    public BetResultEntity putTransaction(Context app, String iso, BetResultEntity transactionEntity) {

        Log.e(TAG, "putTransaction: " + transactionEntity.getTxISO() + ":" + transactionEntity.getTxHash() + ", b:" + transactionEntity.getBlockheight() + ", t:" + transactionEntity.getTimestamp());
        Cursor cursor = null;
        try {
            database = openDatabase();
            ContentValues values = new ContentValues();
            values.put(BRSQLiteHelper.BRTX_COLUMN_ID, transactionEntity.getTxHash());
            values.put(BRSQLiteHelper.BRTX_TYPE, transactionEntity.getType().getNumber());
            values.put(BRSQLiteHelper.BRTX_VERSION, transactionEntity.getVersion());
            values.put(BRSQLiteHelper.BRTX_EVENTID, transactionEntity.getEventID());
            values.put(BRSQLiteHelper.BRTX_RESULTS_TYPE, transactionEntity.getResultType().getNumber());
            values.put(BRSQLiteHelper.BRTX_HOME_TEAM_SCORE, transactionEntity.getHomeScore());
            values.put(BRSQLiteHelper.BRTX_AWAY_TEAM_SCORE, transactionEntity.getAwayScore());
            values.put(BRSQLiteHelper.BRTX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
            values.put(BRSQLiteHelper.BRTX_TIMESTAMP, transactionEntity.getTimestamp());
            values.put(BRSQLiteHelper.BRTX_ISO, iso.toUpperCase());

            database.beginTransaction();
            database.insert(BRSQLiteHelper.BRTX_TABLE_NAME, null, values);
            cursor = database.query(BRSQLiteHelper.BRTX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            cursor.moveToFirst();
            BetResultEntity transactionEntity1 = cursorToTransaction(app, iso.toUpperCase(), cursor);

            database.setTransactionSuccessful();
            return transactionEntity1;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting result tx into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
            Log.e(TAG, "###event result insert/update end: " + transactionEntity.getEventID());
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public void deleteAllTransactions(Context app, String iso) {
        try {
            database = openDatabase();

            database.delete(BRSQLiteHelper.BRTX_TABLE_NAME, BRSQLiteHelper.TX_ISO + "=?", new String[]{iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public List<BetResultEntity> getAllTransactions(Context app, String iso) {
        List<BetResultEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.BRTX_TABLE_NAME,
                    allColumns, BRSQLiteHelper.TX_ISO + "=?", new String[]{iso.toUpperCase()}, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BetResultEntity transactionEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
                transactions.add(transactionEntity);
                cursor.moveToNext();
            }

        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
            printTest(app, iso);
        }
        return transactions;
    }


    public static BetResultEntity cursorToTransaction(Context app, String iso, Cursor cursor) {

        return new BetResultEntity(cursor.getString(0), BetResultEntity.BetTxType.fromValue(cursor.getInt(1)), cursor.getLong(2),
                    cursor.getLong(3), BetResultEntity.BetResultType.fromValue(cursor.getInt(4)), cursor.getLong(5), cursor.getLong(6),
                    cursor.getLong(7), cursor.getLong(8), cursor.getString(9));
    }

    public void deleteTxByHash(Context app, String iso, String hash) {
        try {
            database = openDatabase();
            Log.e(TAG, "result transaction deleted with id: " + hash);
            database.delete(BRSQLiteHelper.BRTX_TABLE_NAME,
                    "_id=? AND " + BRSQLiteHelper.TX_ISO + "=?", new String[]{hash, iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public void deleteResultsOldEvents(Context app, String iso, long eventTimestamp) {
        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.BRTX_TABLE_NAME,
                    allColumns,  BRSQLiteHelper.BRTX_EVENTID+" not in (SELECT "+BRSQLiteHelper.BETX_EVENTID +" from "+BRSQLiteHelper.BETX_TABLE_NAME+" where "+ BRSQLiteHelper.TX_ISO + "=?)",
                    new String[]{ iso.toUpperCase()},null,null,null);
            String strNum = String.valueOf(cursor.getCount());
            cursor.close();
            Log.e(TAG, "delete "+strNum+" results with older event timestamp: " + eventTimestamp);
            database.delete(BRSQLiteHelper.BRTX_TABLE_NAME,
                    BRSQLiteHelper.BRTX_EVENTID+" not in (SELECT "+BRSQLiteHelper.BETX_EVENTID +" from "+BRSQLiteHelper.BETX_TABLE_NAME+" where "+ BRSQLiteHelper.TX_ISO + "=?)", new String[]{iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public BetResultEntity getById(Context app, String iso, int eventID)     {
        Cursor cursor = null;
        BetResultEntity resultEntity = null;
        try {
            int r=0;
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.BETX_TABLE_NAME,
                    allColumns,  BRSQLiteHelper.BETX_EVENTID + "=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                    new String[]{ String.valueOf(eventID), iso.toUpperCase()},null,null,null);
            if (cursor.getCount()==1)   {
                cursor.moveToFirst();
                resultEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
            }
            else if (cursor.getCount()>1) {     // should not happen
                resultEntity = new BetResultEntity("", BetResultEntity.BetTxType.UNKNOWN,0,0,BetResultEntity.BetResultType.UNKNOWN,0,0,0,0,"WGR");
            }
            cursor.close();
            return resultEntity;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error getById Event tx into SQLite", ex);
            //Error in between database transaction
        } finally {
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return resultEntity;
    }

    @Override
    public SQLiteDatabase openDatabase() {
        // Opening new database
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WAL);
        return database;
    }

    @Override
    public void closeDatabase() {

    }

    private void printTest(Context app, String iso) {
        Cursor cursor = null;
        try {
            database = openDatabase();
            StringBuilder builder = new StringBuilder();

            cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            builder.append("Total: " + cursor.getCount() + "\n");
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                BetResultEntity ent = cursorToTransaction(app, iso.toUpperCase(), cursor);
                builder.append("ISO:" + ent.getTxISO() + ", Hash:" + ent.getTxHash() + ", blockHeight:" + ent.getBlockheight() + ", timeStamp:" + ent.getTimestamp()
                        + ", event type:" + ent.getType().getNumber() + ", eventID:" + ent.getEventID() + ", resultType:" + ent.getResultType().getNumber()
                        + ", homeScore:" + ent.getHomeScore() + ", awayScore:" + ent.getAwayScore()
                        + "\n");
            }
            Log.e(TAG, "printTest: " + builder.toString());
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
    }
}
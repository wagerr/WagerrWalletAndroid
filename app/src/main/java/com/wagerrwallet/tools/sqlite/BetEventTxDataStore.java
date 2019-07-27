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
import com.wagerrwallet.tools.manager.BRReportsManager;
import com.wagerrwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

public class BetEventTxDataStore implements BRDataSourceInterface {
    private static final String TAG = BetEventTxDataStore.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    public static final String[] allColumns = {
            BRSQLiteHelper.BETX_COLUMN_ID ,
            BRSQLiteHelper.BETX_TYPE,
            BRSQLiteHelper.BETX_VERSION,
            BRSQLiteHelper.BETX_EVENTID,
            BRSQLiteHelper.BETX_EVENT_TIMESTAMP,
            BRSQLiteHelper.BETX_SPORT,
            BRSQLiteHelper.BETX_TOURNAMENT,
            BRSQLiteHelper.BETX_ROUND,
            BRSQLiteHelper.BETX_HOME_TEAM,
            BRSQLiteHelper.BETX_AWAY_TEAM,
            BRSQLiteHelper.BETX_HOME_ODDS,
            BRSQLiteHelper.BETX_AWAY_ODDS,
            BRSQLiteHelper.BETX_DRAW_ODDS,
            BRSQLiteHelper.BETX_ENTRY_PRICE,
            BRSQLiteHelper.BETX_SPREAD_POINTS,
            BRSQLiteHelper.BETX_TOTAL_POINTS,
            BRSQLiteHelper.BETX_OVER_ODDS,
            BRSQLiteHelper.BETX_UNDER_ODDS,
            BRSQLiteHelper.BETX_BLOCK_HEIGHT,
            BRSQLiteHelper.BETX_TIMESTAMP,
            BRSQLiteHelper.BETX_ISO
    };

    private static BetEventTxDataStore instance;

    public static BetEventTxDataStore getInstance(Context context) {
        if (instance == null) {
            instance = new BetEventTxDataStore(context);
        }
        return instance;
    }

    private BetEventTxDataStore(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);

    }

    public BetEventEntity putTransaction(Context app, String iso, BetEventEntity transactionEntity) {

        Log.e(TAG, "putTransaction: " + transactionEntity.getTxISO() + ":" + transactionEntity.getTxHash() + ", b:" + transactionEntity.getBlockheight() + ", t:" + transactionEntity.getTimestamp());
        Cursor cursor = null;
        try {
            database = openDatabase();
            ContentValues values = new ContentValues();
            values.put(BRSQLiteHelper.BETX_COLUMN_ID , transactionEntity.getTxHash());
            values.put(BRSQLiteHelper.BETX_TYPE, transactionEntity.getType().getNumber());
            values.put(BRSQLiteHelper.BETX_VERSION, transactionEntity.getVersion());
            values.put(BRSQLiteHelper.BETX_EVENTID, transactionEntity.getEventID());
            values.put(BRSQLiteHelper.BETX_EVENT_TIMESTAMP, transactionEntity.getEventTimestamp());
            values.put(BRSQLiteHelper.BETX_SPORT, transactionEntity.getSportID());
            values.put(BRSQLiteHelper.BETX_TOURNAMENT, transactionEntity.getTournamentID());
            values.put(BRSQLiteHelper.BETX_ROUND, transactionEntity.getRoundID());
            values.put(BRSQLiteHelper.BETX_HOME_TEAM, transactionEntity.getHomeTeamID());
            values.put(BRSQLiteHelper.BETX_AWAY_TEAM, transactionEntity.getAwayTeamID());
            values.put(BRSQLiteHelper.BETX_HOME_ODDS, transactionEntity.getHomeOdds());
            values.put(BRSQLiteHelper.BETX_AWAY_ODDS, transactionEntity.getAwayOdds());
            values.put(BRSQLiteHelper.BETX_DRAW_ODDS, transactionEntity.getDrawOdds());
            values.put(BRSQLiteHelper.BETX_ENTRY_PRICE, transactionEntity.getEntryPrice());
            values.put(BRSQLiteHelper.BETX_SPREAD_POINTS, transactionEntity.getSpreadPoints());
            values.put(BRSQLiteHelper.BETX_TOTAL_POINTS, transactionEntity.getTotalPoints());
            values.put(BRSQLiteHelper.BETX_OVER_ODDS, transactionEntity.getOverOdds());
            values.put(BRSQLiteHelper.BETX_UNDER_ODDS, transactionEntity.getUnderOdds());
            values.put(BRSQLiteHelper.BETX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
            values.put(BRSQLiteHelper.BETX_TIMESTAMP, transactionEntity.getTimestamp());
            values.put(BRSQLiteHelper.BETX_ISO, iso.toUpperCase());

            database.beginTransaction();
            database.insert(BRSQLiteHelper.BETX_TABLE_NAME, null, values);
            cursor = database.query(BRSQLiteHelper.BETX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            cursor.moveToFirst();
            BetEventEntity transactionEntity1 = cursorToTransaction(app, iso.toUpperCase(), cursor);

            database.setTransactionSuccessful();
            return transactionEntity1;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting Event tx into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return null;


    }

    public void deleteAllTransactions(Context app, String iso) {
        try {
            database = openDatabase();

            database.delete(BRSQLiteHelper.BETX_TABLE_NAME, BRSQLiteHelper.TX_ISO + "=?", new String[]{iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public List<BetEventEntity> getAllTransactions(Context app, String iso) {
        List<BetEventEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.BETX_TABLE_NAME,
                    allColumns, BRSQLiteHelper.TX_ISO + "=?", new String[]{iso.toUpperCase()}, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BetEventEntity transactionEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
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


    public static BetEventEntity cursorToTransaction(Context app, String iso, Cursor cursor) {

        return new BetEventEntity(cursor.getString(0), BetEventEntity.BetEventType.fromValue(cursor.getInt(1)), cursor.getLong(2),
                    cursor.getLong(3), cursor.getLong(4), cursor.getLong(5),
                    cursor.getLong(6), cursor.getLong(7), cursor.getLong(8),
                    cursor.getLong(9), cursor.getLong(10), cursor.getLong(11),
                    cursor.getLong(12), cursor.getLong(13), cursor.getLong(14),
                    cursor.getLong(15), cursor.getLong(16), cursor.getLong(17),
                    cursor.getLong(18), cursor.getLong(19), cursor.getString(20));
    }

    public void deleteTxByHash(Context app, String iso, String hash) {
        try {
            database = openDatabase();
            Log.e(TAG, "mapping transaction deleted with id: " + hash);
            database.delete(BRSQLiteHelper.BETX_TABLE_NAME,
                    "_id=? AND " + BRSQLiteHelper.TX_ISO + "=?", new String[]{hash, iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
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
                BetEventEntity ent = cursorToTransaction(app, iso.toUpperCase(), cursor);
                builder.append("ISO:" + ent.getTxISO() + ", Hash:" + ent.getTxHash() + ", blockHeight:" + ent.getBlockheight() + ", timeStamp:" + ent.getTimestamp()
                        + ", event type:" + ent.getType().getNumber() + ", eventID:" + ent.getEventID() + ", eventTimestamp:" + ent.getEventTimestamp()
                        + ", sport:" + ent.getSportID() + ", tournament:" + ent.getTournamentID() + ", round:" + ent.getRoundID()
                        + ", homeTeam:" + ent.getHomeTeamID() + ", awayTeam:" + ent.getAwayTeamID()
                        + ", homeOdds:" + ent.getHomeOdds() + ", awayOdds:" + ent.getAwayOdds() + ", drawOdds:" + ent.getDrawOdds()
                        + ", entryPrice:" + ent.getEntryPrice() + ", spreadPoints:" + ent.getSpreadPoints() + ", totalPoints:" + ent.getTotalPoints()
                        + ", overOdds:" + ent.getOverOdds() + ", underOdds:" + ent.getUnderOdds()
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
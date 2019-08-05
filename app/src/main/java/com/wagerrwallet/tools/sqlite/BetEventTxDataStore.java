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
import com.wagerrwallet.presenter.entities.BetMappingEntity;
import com.wagerrwallet.presenter.entities.BetResultEntity;
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

        return insertOrUpdate(app,iso,transactionEntity.getEventID(), values);
    }

    protected BetEventEntity insertOrUpdate(Context app, String iso, long eventID, ContentValues values) {
        Cursor cursor = null;
        try {
            int r=0;
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.BETX_TABLE_NAME,
                    allColumns,  BRSQLiteHelper.BETX_EVENTID + "=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                    new String[]{ String.valueOf(eventID), iso.toUpperCase()},null,null,null);
            database.beginTransaction();
            if (cursor.getCount()==0)   {
                database.insert(BRSQLiteHelper.BETX_TABLE_NAME, null, values);
                r = 1;
            }
            else {
                r = database.update(BRSQLiteHelper.BETX_TABLE_NAME, values, BRSQLiteHelper.BETX_EVENTID + "=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                        new String[]{String.valueOf(eventID), iso.toUpperCase()});
            }
            cursor.close();
            cursor = database.query(BRSQLiteHelper.BETX_TABLE_NAME,
                    allColumns,  BRSQLiteHelper.BETX_EVENTID + "=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                    new String[]{ String.valueOf(eventID), iso.toUpperCase()},null,null,null);
            BetEventEntity transactionEntity1 = null;
            if (cursor.getCount()==0 || r==0 )   {
                Log.e(TAG, "event insert/update failed: " + eventID);
            }
            else {
                cursor.moveToFirst();
                transactionEntity1 = cursorToTransaction(app, iso.toUpperCase(), cursor);
                database.setTransactionSuccessful();
            }
            return transactionEntity1;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting/updating Event tx into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public boolean updateOdds(Context app, String iso, BetEventEntity transactionEntity) {
        Log.e(TAG, "updateOdds: " + transactionEntity.getTxISO() + ":" + transactionEntity.getTxHash() + ", eventID:" + transactionEntity.getEventID() + ", b:" + transactionEntity.getBlockheight() + ", t:" + transactionEntity.getTimestamp());

        ContentValues args = new ContentValues();
        args.put(BRSQLiteHelper.BETX_HOME_ODDS, transactionEntity.getHomeOdds());
        args.put(BRSQLiteHelper.BETX_AWAY_ODDS, transactionEntity.getAwayOdds());
        args.put(BRSQLiteHelper.BETX_DRAW_ODDS, transactionEntity.getDrawOdds());

        BetEventEntity transactionEntity1 = insertOrUpdate(app,iso,transactionEntity.getEventID(), args);

        return (transactionEntity1!=null);
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
            String QUERY = "SELECT a." + BRSQLiteHelper.BETX_COLUMN_ID
                                + ", a." + BRSQLiteHelper.BETX_TYPE
                                + ", a." + BRSQLiteHelper.BETX_VERSION
                                + ", a." + BRSQLiteHelper.BETX_EVENTID
                                + ", a." + BRSQLiteHelper.BETX_EVENT_TIMESTAMP
                                + ", a." + BRSQLiteHelper.BETX_SPORT
                                + ", a." + BRSQLiteHelper.BETX_TOURNAMENT
                                + ", a." + BRSQLiteHelper.BETX_ROUND
                                + ", a." + BRSQLiteHelper.BETX_HOME_TEAM
                                + ", a." + BRSQLiteHelper.BETX_AWAY_TEAM
                                + ", a." + BRSQLiteHelper.BETX_HOME_ODDS
                                + ", a." + BRSQLiteHelper.BETX_AWAY_ODDS
                                + ", a." + BRSQLiteHelper.BETX_DRAW_ODDS
                                + ", a." + BRSQLiteHelper.BETX_ENTRY_PRICE
                                + ", a." + BRSQLiteHelper.BETX_SPREAD_POINTS
                                + ", a." + BRSQLiteHelper.BETX_TOTAL_POINTS
                                + ", a." + BRSQLiteHelper.BETX_OVER_ODDS
                                + ", a." + BRSQLiteHelper.BETX_UNDER_ODDS
                                + ", a." + BRSQLiteHelper.BETX_BLOCK_HEIGHT
                                + ", a." + BRSQLiteHelper.BETX_TIMESTAMP
                                + ", a." + BRSQLiteHelper.TX_ISO
                                // event mappings
                                + ", s." + BRSQLiteHelper.BMTX_STRING
                                + ", t." + BRSQLiteHelper.BMTX_STRING
                                + ", r." + BRSQLiteHelper.BMTX_STRING
                                + ", b." + BRSQLiteHelper.BMTX_STRING
                                + ", c." + BRSQLiteHelper.BMTX_STRING
                                // event results
                                + ", o." + BRSQLiteHelper.BRTX_RESULTS_TYPE
                                + ", o." + BRSQLiteHelper.BRTX_HOME_TEAM_SCORE
                                + ", o." + BRSQLiteHelper.BRTX_AWAY_TEAM_SCORE

                    + " FROM "+BRSQLiteHelper.BETX_TABLE_NAME+" a "
                    // sport (s), tournament (t), round (r)
                    + " LEFT OUTER JOIN " + BRSQLiteHelper.BMTX_TABLE_NAME+" s ON a."+BRSQLiteHelper.BETX_SPORT + "=s."+BRSQLiteHelper.BMTX_MAPPINGID
                    +" AND s."+BRSQLiteHelper.BMTX_NAMESPACEID+"="+ BetMappingEntity.MappingNamespaceType.SPORT.getNumber()
                    + " LEFT OUTER JOIN " + BRSQLiteHelper.BMTX_TABLE_NAME+" t ON a."+BRSQLiteHelper.BETX_TOURNAMENT+ "=t."+BRSQLiteHelper.BMTX_MAPPINGID
                    +" AND t."+BRSQLiteHelper.BMTX_NAMESPACEID+"="+ BetMappingEntity.MappingNamespaceType.TOURNAMENT.getNumber()
                    + " LEFT OUTER JOIN " + BRSQLiteHelper.BMTX_TABLE_NAME+" r ON a."+BRSQLiteHelper.BETX_ROUND + "=r."+BRSQLiteHelper.BMTX_MAPPINGID
                    +" AND r."+BRSQLiteHelper.BMTX_NAMESPACEID+"="+ BetMappingEntity.MappingNamespaceType.ROUNDS.getNumber()
                    // home team (b), away team (c)
                    + " LEFT OUTER JOIN " + BRSQLiteHelper.BMTX_TABLE_NAME+" b ON a."+BRSQLiteHelper.BETX_HOME_TEAM + "=b."+BRSQLiteHelper.BMTX_MAPPINGID
                            +" AND b."+BRSQLiteHelper.BMTX_NAMESPACEID+"="+ BetMappingEntity.MappingNamespaceType.TEAM_NAME.getNumber()
                    + " LEFT OUTER JOIN " + BRSQLiteHelper.BMTX_TABLE_NAME+" c ON a."+BRSQLiteHelper.BETX_AWAY_TEAM + "=c."+BRSQLiteHelper.BMTX_MAPPINGID
                            +" AND b."+BRSQLiteHelper.BMTX_NAMESPACEID+"="+ BetMappingEntity.MappingNamespaceType.TEAM_NAME.getNumber()
                    // result table (o)
                    + " LEFT OUTER JOIN " + BRSQLiteHelper.BRTX_TABLE_NAME+" o ON a."+BRSQLiteHelper.BETX_EVENTID + "=o."+BRSQLiteHelper.BRTX_EVENTID
                    + " WHERE a."+BRSQLiteHelper.TX_ISO+"=?";

            cursor = database.rawQuery(QUERY,  new String[]{iso.toUpperCase()});
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BetEventEntity transactionEntity = cursorToFullEvent(app, iso.toUpperCase(), cursor);
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

        return new BetEventEntity(cursor.getString(0), BetEventEntity.BetTxType.fromValue(cursor.getInt(1)), cursor.getLong(2),
                    cursor.getLong(3), cursor.getLong(4), cursor.getLong(5),
                    cursor.getLong(6), cursor.getLong(7), cursor.getLong(8),
                    cursor.getLong(9), cursor.getLong(10), cursor.getLong(11),
                    cursor.getLong(12), cursor.getLong(13), cursor.getLong(14),
                    cursor.getLong(15), cursor.getLong(16), cursor.getLong(17),
                    cursor.getLong(18), cursor.getLong(19), cursor.getString(20));
    }

    public static BetEventEntity cursorToFullEvent(Context app, String iso, Cursor cursor) {

        return new BetEventEntity(cursor.getString(0), BetEventEntity.BetTxType.fromValue(cursor.getInt(1)), cursor.getLong(2),
                cursor.getLong(3), cursor.getLong(4), cursor.getLong(5),
                cursor.getLong(6), cursor.getLong(7), cursor.getLong(8),
                cursor.getLong(9), cursor.getLong(10), cursor.getLong(11),
                cursor.getLong(12), cursor.getLong(13), cursor.getLong(14),
                cursor.getLong(15), cursor.getLong(16), cursor.getLong(17),
                cursor.getLong(18), cursor.getLong(19), cursor.getString(20),
                cursor.getString(21), cursor.getString(22), cursor.getString(23),
                cursor.getString(24), cursor.getString(25),
                BetResultEntity.BetResultType.fromValue(cursor.getInt(26)), cursor.getLong(27), cursor.getLong(28));
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

            cursor = database.query(BRSQLiteHelper.BETX_TABLE_NAME,
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
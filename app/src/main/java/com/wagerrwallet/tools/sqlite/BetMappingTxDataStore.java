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

import com.wagerrwallet.presenter.entities.BRTransactionEntity;
import com.wagerrwallet.presenter.entities.BetEventEntity;
import com.wagerrwallet.presenter.entities.BetMappingEntity;
import com.wagerrwallet.tools.manager.BRReportsManager;
import com.wagerrwallet.tools.util.BRConstants;
import com.wagerrwallet.wallet.wallets.util.CryptoUriParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BetMappingTxDataStore implements BRDataSourceInterface {
    private static final String TAG = BetMappingTxDataStore.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    public static final String[] allColumns = {
            BRSQLiteHelper.BMTX_COLUMN_ID,
            BRSQLiteHelper.BMTX_TYPE,
            BRSQLiteHelper.BMTX_VERSION,
            BRSQLiteHelper.BMTX_NAMESPACEID,
            BRSQLiteHelper.BMTX_MAPPINGID,
            BRSQLiteHelper.BMTX_STRING,
            BRSQLiteHelper.BMTX_BLOCK_HEIGHT,
            BRSQLiteHelper.BMTX_TIMESTAMP,
            BRSQLiteHelper.BMTX_ISO
    };

    private static BetMappingTxDataStore instance;

    public static BetMappingTxDataStore getInstance(Context context) {
        if (instance == null) {
            instance = new BetMappingTxDataStore(context);
        }
        return instance;
    }

    private BetMappingTxDataStore(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);

    }

    private ContentValues getValuesForMappingInsert(BetMappingEntity transactionEntity, String iso)   {
        ContentValues values = new ContentValues();
        values.put(BRSQLiteHelper.BMTX_COLUMN_ID, transactionEntity.getTxHash());
        values.put(BRSQLiteHelper.BMTX_TYPE, transactionEntity.getType().getNumber());
        values.put(BRSQLiteHelper.BMTX_VERSION, transactionEntity.getVersion());
        values.put(BRSQLiteHelper.BMTX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
        values.put(BRSQLiteHelper.BMTX_ISO, iso.toUpperCase());
        values.put(BRSQLiteHelper.BMTX_NAMESPACEID, transactionEntity.getNamespaceID().getNumber());
        values.put(BRSQLiteHelper.BMTX_MAPPINGID, transactionEntity.getMappingID());
        values.put(BRSQLiteHelper.BMTX_STRING, transactionEntity.getDescription());
        values.put(BRSQLiteHelper.BMTX_TIMESTAMP, transactionEntity.getTimestamp());

        return values;
    }

    private ContentValues getValuesForMappingUpdate(BetMappingEntity transactionEntity)   {
        ContentValues values = new ContentValues();
        values.put(BRSQLiteHelper.BMTX_COLUMN_ID, transactionEntity.getTxHash());
        values.put(BRSQLiteHelper.BMTX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
        values.put(BRSQLiteHelper.BMTX_STRING, transactionEntity.getDescription());
        values.put(BRSQLiteHelper.BMTX_TIMESTAMP, transactionEntity.getTimestamp());

        return values;
    }

    // update description only
    private ContentValues getValuesForMappingDescriptionUpdate(BetMappingEntity transactionEntity)   {
        ContentValues values = new ContentValues();
        values.put(BRSQLiteHelper.BMTX_STRING, transactionEntity.getDescription());
        return values;
    }

    public BetMappingEntity putTransaction(Context app, String iso, BetMappingEntity transactionEntity) {
        return putTransaction(app, iso, transactionEntity,
                getValuesForMappingInsert(transactionEntity, iso),
                getValuesForMappingUpdate(transactionEntity));
    }

    public void putAPITransaction(Context app, String iso, JSONArray jsonMappings, BetMappingEntity.MappingNamespaceType type ) {

        Cursor cursor = null;
        try {
            database = openDatabase();
            database.beginTransaction();
            for (int i = 0; i < jsonMappings.length(); i++) {
                try {
                    JSONObject o = (JSONObject) jsonMappings.get(i);
                    int id = o.getInt("wgrID");
                    String name = o.getString("name");

                    cursor = database.query(BRSQLiteHelper.BMTX_TABLE_NAME, allColumns,
                            BRSQLiteHelper.BMTX_NAMESPACEID + "=? AND " + BRSQLiteHelper.BMTX_MAPPINGID + "=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                            new String[]{String.valueOf(type.getNumber()), String.valueOf(id), iso.toUpperCase()}, null, null, null);

                    if (cursor.getCount() == 0) {   // does not exist, insert
                        BetMappingEntity mappingEntity = new BetMappingEntity(String.valueOf(id),0,type,id,name,0,0,iso);
                        database.insert(BRSQLiteHelper.BMTX_TABLE_NAME, null, getValuesForMappingInsert(mappingEntity, iso));
                    }
                    if (cursor != null) cursor.close();
                } catch (JSONException ignored) {
                }
            }
            database.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e(TAG, "Error inserting Mapping tx into SQLite", ex);
        } finally {
            database.endTransaction();
            closeDatabase();
        }

        return;
    }

    public BetMappingEntity putTransaction(Context app, String iso, BetMappingEntity transactionEntity, ContentValues insertValues, ContentValues updateValues) {

        Log.e(TAG, "putTransaction: " + transactionEntity.getTxISO() + ":" + transactionEntity.getTxHash() + ", b:" + transactionEntity.getBlockheight() + ", t:" + transactionEntity.getTimestamp());
        Cursor cursor = null;
        try {
            database = openDatabase();
            BetMappingEntity transactionEntity1 = getTxByHash(app,iso, transactionEntity.getTxHash());

            if (transactionEntity1==null) {
                transactionEntity1 = getMappingById(app, iso, transactionEntity.getNamespaceID(), transactionEntity.getMappingID());
                if (transactionEntity1==null) {     // not dupe, insert
                    database.beginTransaction();
                    database.insert(BRSQLiteHelper.BMTX_TABLE_NAME, null, insertValues);
                    database.setTransactionSuccessful();
                }
                else {   // duplicate, update only
                    if (transactionEntity.getBlockheight() > transactionEntity1.getBlockheight()) {
                        database.beginTransaction();
                        database.update(BRSQLiteHelper.BMTX_TABLE_NAME, updateValues, BRSQLiteHelper.BMTX_NAMESPACEID + "=? AND " + BRSQLiteHelper.BMTX_MAPPINGID + "=?",
                                new String[]{String.valueOf(transactionEntity.getNamespaceID().getNumber()), String.valueOf(transactionEntity.getMappingID())});
                        database.setTransactionSuccessful();
                    }
                }
            }
            return transactionEntity1;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting Mapping tx into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public BetMappingEntity getTxByHash(Context app, String iso, String hash) {
        Cursor cursor = null;
        BetMappingEntity mappingEntity = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.BMTX_TABLE_NAME, allColumns,
                    "_id=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                    new String[]{ hash, iso.toUpperCase()},null,null,null);
            if (cursor.getCount()==1)   {
                cursor.moveToFirst();
                mappingEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
            }
            else if (cursor.getCount()>1) {     // should not happen, delete all and insert new
                deleteTxByHash(app, iso, hash);
            }
            cursor.close();
        } finally {
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return mappingEntity;
    }

    public BetMappingEntity getMappingById(Context app, String iso, BetMappingEntity.MappingNamespaceType namespace, long mappingId) {
        Cursor cursor = null;
        BetMappingEntity mappingEntity = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.BMTX_TABLE_NAME, allColumns,
                    BRSQLiteHelper.BMTX_NAMESPACEID + "=? AND " + BRSQLiteHelper.BMTX_MAPPINGID + "=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                    new String[]{ String.valueOf( namespace.getNumber() ), String.valueOf( mappingId ), iso.toUpperCase()},null,null,null);
            if (cursor.getCount()==1)   {
                cursor.moveToFirst();
                mappingEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
            }
            cursor.close();
        } finally {
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return mappingEntity;
    }

    public void deleteAllTransactions(Context app, String iso) {
        try {
            database = openDatabase();

            database.delete(BRSQLiteHelper.BMTX_TABLE_NAME, BRSQLiteHelper.TX_ISO + "=?", new String[]{iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public List<BetMappingEntity> getAllTransactions(Context app, String iso, BetMappingEntity.MappingNamespaceType type ) {
        List<BetMappingEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.BMTX_TABLE_NAME,
                    allColumns, BRSQLiteHelper.TX_ISO + "=? AND "+ BRSQLiteHelper.BMTX_NAMESPACEID + "=?"
                    , new String[]{iso.toUpperCase(), String.valueOf(type.getNumber())}, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BetMappingEntity transactionEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
                transactions.add(transactionEntity);
                cursor.moveToNext();
            }

        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
            //printTest(app, iso);
        }
        return transactions;
    }

    public List<BetMappingEntity> getAllSports(Context app, String iso, long eventTimestamp ) {
        List<BetMappingEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();
            String QUERY = "SELECT * FROM " + BRSQLiteHelper.BMTX_TABLE_NAME
                    + " WHERE " + BRSQLiteHelper.BMTX_NAMESPACEID + "="+ BetMappingEntity.MappingNamespaceType.SPORT.getNumber()
                    + " AND " + BRSQLiteHelper.BMTX_MAPPINGID + " IN "
                    + "( SELECT DISTINCT "+ BRSQLiteHelper.BETX_SPORT + " FROM " + BRSQLiteHelper.BETX_TABLE_NAME;


            QUERY += " WHERE " + BRSQLiteHelper.BETX_EVENT_TIMESTAMP+"> " + String.valueOf(eventTimestamp)
                    + "  ) ";
            QUERY += " ORDER BY " + BRSQLiteHelper.BMTX_STRING;

            cursor = database.rawQuery(QUERY, null);
            cursor.moveToFirst();
            HashMap<String, String> map = new HashMap<String, String>();
            while (!cursor.isAfterLast()) {
                String hash = cursor.getString(0);
                String mappingId = String.valueOf(cursor.getLong(4));
                if ( !map.containsKey(mappingId) ) {     // avoid duplicate mapping (same id) with different hash
                    BetMappingEntity transactionEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
                    transactions.add(transactionEntity);
                    map.put(mappingId, hash);
                }
                cursor.moveToNext();
            }
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error reading Mapping tx into SQLite", ex);
        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
            //printTest(app, iso);
        }
        return transactions;
    }


    public List<BetMappingEntity> getAllTournaments(Context app, String iso, long sportID, long eventTimestamp ) {
        List<BetMappingEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();
            String QUERY = "SELECT * FROM " + BRSQLiteHelper.BMTX_TABLE_NAME
                    + " WHERE " + BRSQLiteHelper.BMTX_NAMESPACEID + "="+ BetMappingEntity.MappingNamespaceType.TOURNAMENT.getNumber()
                    + " AND " + BRSQLiteHelper.BMTX_MAPPINGID + " IN "
                    + "( SELECT DISTINCT "+ BRSQLiteHelper.BETX_TOURNAMENT + " FROM " + BRSQLiteHelper.BETX_TABLE_NAME;

            if (sportID>-1) {
                QUERY += " WHERE " + BRSQLiteHelper.BETX_SPORT + "=" + sportID
                        + " AND " +BRSQLiteHelper.BETX_EVENT_TIMESTAMP+"> " + String.valueOf(eventTimestamp)
                        + "  ) ";
            }
            else {
                QUERY += " ) ";
            }
            QUERY += " ORDER BY " + BRSQLiteHelper.BMTX_STRING;

            cursor = database.rawQuery(QUERY, null);
            cursor.moveToFirst();
            HashMap<String, String> map = new HashMap<String, String>();
            while (!cursor.isAfterLast()) {
                String hash = cursor.getString(0);
                String mappingId = String.valueOf(cursor.getLong(4));
                if ( !map.containsKey(mappingId) ) {     // avoid duplicate mapping (same id) with different hash
                    BetMappingEntity transactionEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
                    transactions.add(transactionEntity);
                    map.put(mappingId, hash);
                }
                cursor.moveToNext();
            }
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error reading Mapping tx into SQLite", ex);
        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
            //printTest(app, iso);
        }
        return transactions;
    }

    public static BetMappingEntity cursorToTransaction(Context app, String iso, Cursor cursor) {

        return new BetMappingEntity(cursor.getString(0), cursor.getLong(2),
                    BetMappingEntity.MappingNamespaceType.fromValue(cursor.getInt(3)),
                    cursor.getLong(4), cursor.getString(5),
                    cursor.getLong(6), cursor.getLong(7), cursor.getString(8));
    }

    public void deleteTxByHash(Context app, String iso, String hash) {
        try {
            database = openDatabase();
            Log.e(TAG, "mapping transaction deleted with id: " + hash);
            database.delete(BRSQLiteHelper.BMTX_TABLE_NAME,
                    "_id=? AND " + BRSQLiteHelper.TX_ISO + "=?", new String[]{hash, iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public void deleteDuplicateMappings(Context app, int mappingID) {
        Cursor cursor = null;
        try {
            database = openDatabase();
            Log.e(TAG, "mapping transaction deletion " );
            String QUERY = "delete from "+BRSQLiteHelper.BMTX_TABLE_NAME+" where " + BRSQLiteHelper.BMTX_COLUMN_ID + " in (select " + BRSQLiteHelper.BMTX_COLUMN_ID + " "
                    + "from "+BRSQLiteHelper.BMTX_TABLE_NAME+" where "+ BRSQLiteHelper.BMTX_MAPPINGID +" in "
                    + "(select "+ BRSQLiteHelper.BMTX_MAPPINGID +" from "+BRSQLiteHelper.BMTX_TABLE_NAME+" where " + BRSQLiteHelper.BMTX_NAMESPACEID +"="+mappingID+" "
                    + "group by " + BRSQLiteHelper.BMTX_NAMESPACEID + ", " + BRSQLiteHelper.BMTX_MAPPINGID + " "
                    + "having count(*)>1) "
                    + "and " + BRSQLiteHelper.BMTX_COLUMN_ID + " not in (select max(" + BRSQLiteHelper.BMTX_COLUMN_ID + ") from "+BRSQLiteHelper.BMTX_TABLE_NAME+" where "+ BRSQLiteHelper.BMTX_NAMESPACEID +"= "+mappingID+" "
                    + "group by " + BRSQLiteHelper.BMTX_NAMESPACEID + ", " + BRSQLiteHelper.BMTX_MAPPINGID + " "
                    + "having count(*)>1) )";

            database.rawQuery(QUERY, null).moveToFirst();
        } finally {
            closeDatabase();
        }
    }

    public class BetMappingDupItem {
        public long NamespaceID;
        public long MappingID;
        public int nCount;

        public BetMappingDupItem( long namespaceID, long mappingID, int ncount)
        {
            NamespaceID=namespaceID;
            MappingID=mappingID;
            nCount = ncount;
        }
    }

    public List<BetMappingDupItem> SearchDuplicates(Context app, String iso, BetMappingEntity.MappingNamespaceType ns) {
        Cursor cursor = null;
        BetMappingDupItem mappingEntity = null;
        List<BetMappingDupItem> list = new ArrayList<>();
        try {
            database = openDatabase();
            String QUERY = "SELECT " + BRSQLiteHelper.BMTX_NAMESPACEID + ", " + BRSQLiteHelper.BMTX_MAPPINGID + ", count(*) "
                    + " FROM " + BRSQLiteHelper.BMTX_TABLE_NAME
                    + " GROUP BY " + BRSQLiteHelper.BMTX_NAMESPACEID + ", " + BRSQLiteHelper.BMTX_MAPPINGID
                    + " HAVING count(*)>1 ";

            cursor = database.rawQuery(QUERY, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                mappingEntity = new BetMappingDupItem(cursor.getLong(0), cursor.getLong(1) , cursor.getInt(2));
                list.add(mappingEntity);
                cursor.moveToNext();
            }
            //mappingEntity = new BetMappingEntity("", 0, BetMappingEntity.MappingNamespaceType.UNKNOWN,0,"",0,0,"WGR");

        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
        //return mappingEntity;
        return list;
    }

    private List<BetMappingEntity> getById(Context app, String iso, BetMappingEntity.MappingNamespaceType ns, int mappingID) {
        Cursor cursor = null;
        BetMappingEntity mappingEntity = null;
        List<BetMappingEntity> list = new ArrayList<>();
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.BMTX_TABLE_NAME,
                    allColumns, BRSQLiteHelper.BMTX_NAMESPACEID + "=? AND " + BRSQLiteHelper.BMTX_MAPPINGID+ "=? AND " + BRSQLiteHelper.TX_ISO + "=?",
                    new String[]{ String.valueOf(ns.getNumber()), String.valueOf(mappingID), iso.toUpperCase()}, null, null, null);
            if (cursor.getCount()==1)   {
                cursor.moveToFirst();
                mappingEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
                list.add(mappingEntity);
            }
            else if (cursor.getCount()>1) {     // should not happen
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    mappingEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
                    list.add(mappingEntity);
                    cursor.moveToNext();
                }
                //mappingEntity = new BetMappingEntity("", 0, BetMappingEntity.MappingNamespaceType.UNKNOWN,0,"",0,0,"WGR");
            }
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
        //return mappingEntity;
        return list;
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

            cursor = database.query(BRSQLiteHelper.BMTX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            builder.append("Total: " + cursor.getCount() + "\n");
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                BetMappingEntity ent = cursorToTransaction(app, iso.toUpperCase(), cursor);
                builder.append("ISO:" + ent.getTxISO() + ", Hash:" + ent.getTxHash() + ", blockHeight:" + ent.getBlockheight() + ", timeStamp:" + ent.getTimestamp() + ", namespaceID:" + ent.getNamespaceID() + ", mappingID:" + ent.getMappingID() + ", description:" + ent.getDescription()  + "\n");
            }
            Log.e(TAG, "printTest: " + builder.toString());
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
    }
}
package com.breadwallet.tools.sqlite;

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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.ArrayList;
import java.util.List;

public class BtcBchTransactionDataStore implements BRDataSourceInterface {
    private static final String TAG = BtcBchTransactionDataStore.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.TX_COLUMN_ID,
            BRSQLiteHelper.TX_BUFF,
            BRSQLiteHelper.TX_BLOCK_HEIGHT,
            BRSQLiteHelper.TX_TIME_STAMP,
            BRSQLiteHelper.TX_ISO
    };

    private static BtcBchTransactionDataStore instance;

    public static BtcBchTransactionDataStore getInstance(Context context) {
        if (instance == null) {
            instance = new BtcBchTransactionDataStore(context);
        }
        return instance;
    }

    private BtcBchTransactionDataStore(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);

    }

    public BRTransactionEntity putTransaction(Context app, BaseWalletManager wallet, BRTransactionEntity transactionEntity) {

        Cursor cursor = null;
        try {
            database = openDatabase();
            ContentValues values = new ContentValues();
            values.put(BRSQLiteHelper.TX_COLUMN_ID, transactionEntity.getTxHash());
            values.put(BRSQLiteHelper.TX_BUFF, transactionEntity.getBuff());
            values.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
            values.put(BRSQLiteHelper.TX_ISO, wallet.getIso(app));
            values.put(BRSQLiteHelper.TX_TIME_STAMP, transactionEntity.getTimestamp());

            database.beginTransaction();
            database.insert(BRSQLiteHelper.TX_TABLE_NAME, null, values);
            cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            cursor.moveToFirst();
            BRTransactionEntity transactionEntity1 = cursorToTransaction(app, wallet, cursor);

            database.setTransactionSuccessful();
//            for (OnTxAdded listener : listeners) {
//                if (listener != null) listener.onTxAdded("BTC");
//            }
            return transactionEntity1;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return null;


    }

    public void deleteAllTransactions(Context app, BaseWalletManager wallet) {
        try {
            database = openDatabase();

            database.delete(BRSQLiteHelper.TX_TABLE_NAME, BRSQLiteHelper.TX_ISO + "=?", new String[]{wallet.getIso(app)});
        } finally {
            closeDatabase();
        }
    }

    public List<BRTransactionEntity> getAllTransactions(Context app, BaseWalletManager wallet) {
        List<BRTransactionEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.TX_TABLE_NAME,
                    allColumns, BRSQLiteHelper.TX_ISO + "=?", new String[]{wallet.getIso(app)}, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BRTransactionEntity transactionEntity = cursorToTransaction(app, wallet, cursor);
                transactions.add(transactionEntity);
                cursor.moveToNext();
            }

        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
        }
        return transactions;
    }

    private BRTransactionEntity cursorToTransaction(Context app, BaseWalletManager wallet, Cursor cursor) {
        return new BRTransactionEntity(cursor.getBlob(1), cursor.getInt(2), cursor.getLong(3), cursor.getString(0), wallet.getIso(app));
    }

    public boolean updateTransaction(Context app, BaseWalletManager wallet, BRTransactionEntity tx) {
        try {
            database = openDatabase();
            ContentValues args = new ContentValues();
            args.put(BRSQLiteHelper.TX_BLOCK_HEIGHT, tx.getBlockheight());
            args.put(BRSQLiteHelper.TX_TIME_STAMP, tx.getTimestamp());

//            Log.e(TAG, "updateTransaction: size before updating: " + getAllTransactions().size());
            database.update(BRSQLiteHelper.TX_TABLE_NAME, args, "_id=? AND " + BRSQLiteHelper.TX_ISO + "=?", new String[]{tx.getTxHash(), wallet.getIso(app)});
//            Log.e(TAG, "updateTransaction: size after updating: " + getAllTransactions().size());
            Log.e(TAG, "transaction updated with id: " + tx.getTxHash());
            return true;
        } finally {
            closeDatabase();
        }

    }

    public void deleteTxByHash(Context app, BaseWalletManager wallet, String hash) {
        try {
            database = openDatabase();
            Log.e(TAG, "transaction deleted with id: " + hash);
            database.delete(BRSQLiteHelper.TX_TABLE_NAME,
                    "_id=? AND " + BRSQLiteHelper.TX_ISO + "=?", new String[]{hash, wallet.getIso(app)});
        } finally {
            closeDatabase();
        }
    }

    @Override
    public SQLiteDatabase openDatabase() {
//        if (mOpenCounter.incrementAndGet() == 1) {
        // Opening new database
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WAL);
//        }
//        Log.d("Database open counter: ",  String.valueOf(mOpenCounter.get()));
        return database;
    }

    @Override
    public void closeDatabase() {

//        if (mOpenCounter.decrementAndGet() == 0) {
//            // Closing database
//            database.close();

//        }
//        Log.d("Database open counter: " , String.valueOf(mOpenCounter.get()));
    }
}
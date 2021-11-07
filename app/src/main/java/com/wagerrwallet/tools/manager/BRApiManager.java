package com.wagerrwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import com.platform.entities.TxExplorerInfo;
import com.platform.entities.TxExplorerPayoutInfo;
import com.wagerrwallet.WagerrApp;
import com.wagerrwallet.presenter.activities.util.ActivityUTILS;
import com.wagerrwallet.presenter.entities.BetMappingEntity;
import com.wagerrwallet.presenter.entities.CurrencyEntity;
import com.wagerrwallet.presenter.entities.SwapResponse;
import com.wagerrwallet.presenter.entities.SwapUiHolder;
import com.wagerrwallet.tools.sqlite.BetMappingTxDataStore;
import com.wagerrwallet.tools.sqlite.CurrencyDataSource;
import com.wagerrwallet.tools.threads.executor.BRExecutor;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.wagerrwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.APIClient;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;
import okhttp3.Response;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
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

public class BRApiManager {
    private static final String TAG = BRApiManager.class.getName();
    private static final String instaswapURL = "https://instaswap.wagerr.com/instaswap_service.php?s=";
    private static BRApiManager instance;
    private Timer timer;

    private TimerTask timerTask;

    private Handler handler;

    private BRApiManager() {
        handler = new Handler();
    }

    public static BRApiManager getInstance() {

        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context, BaseWalletManager walletManager) {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = fetchRates(context, walletManager);
            updateFeePerKb(context);
            if (arr != null) {
                int length = arr.length();
                for (int i = 1; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = (float) tmpObj.getDouble("rate");
                        String selectedISO = BRSharedPrefs.getPreferredFiatIso(context);
                        if (tmp.code.equalsIgnoreCase(selectedISO)) {
                            BRSharedPrefs.putPreferredFiatIso(context, tmp.code);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }

                // add Coin
                CurrencyEntity coin = new CurrencyEntity();
                coin.name="WAGERR";
                coin.code="WGR";
                coin.rate= fetchRatesCoin( context, walletManager );
                if (walletManager instanceof WalletWagerrManager)
                    ((WalletWagerrManager) walletManager).setCoinRate(coin.rate);
                set.add(coin);

            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        List tempList = new ArrayList<>(set);
//        Collections.reverse(tempList);
        Log.e(TAG, "getCurrencies: " + set.size());
        return new LinkedHashSet<>(set);
    }


    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (WagerrApp.isAppInBackground(context)) {
                                    Log.e(TAG, "doInBackground: Stopping timer, no activity on.");
                                    stopTimerTask();
                                }
                                for (BaseWalletManager w : WalletsMaster.getInstance(context).getAllWallets()) {
                                    String iso = w.getIso(context);
                                    Set<CurrencyEntity> tmp = getCurrencies((Activity) context, w);
                                    CurrencyDataSource.getInstance(context).putCurrencies(context, iso, tmp);
                                }
                                // sync mappings from API every 24h
                                long currentTime = System.currentTimeMillis();
                                long lastAPICall = BRSharedPrefs.getLastAPISyncTime(context, BetMappingEntity.MappingNamespaceType.TEAM_NAME);
                                if (currentTime-lastAPICall > 24*3600*1000) {
                                    SyncAPITeamNames((Activity) context);
                                    BRSharedPrefs.putLastAPISyncTime(context, BetMappingEntity.MappingNamespaceType.TEAM_NAME, System.currentTimeMillis());
                                }

                            }
                        });
                    }
                });
            }
        };
    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();
        Log.e(TAG, "startTimer: started...");
        //initialize the TimerTask's job
        initializeTimerTask(context);

        timer.schedule(timerTask, 1000, 60000);
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static JSONArray fetchRates(Activity app, BaseWalletManager walletManager) {
        String url = "https://" + WagerrApp.HOST + "/rates?currency=" + walletManager.getIso(app);
        String jsonString = urlGET(app, url);
        JSONArray jsonArray = null;
        if (jsonString == null) {
            Log.e(TAG, "fetchRates: failed, response is null");
            return null;
        }
        try {
            JSONObject obj = new JSONObject(jsonString);
            jsonArray = obj.getJSONArray("body");

        } catch (JSONException ignored) {
        }
        return jsonArray == null ? backupFetchRates(app, walletManager) : jsonArray;
    }

    public static JSONArray backupFetchRates(Activity app, BaseWalletManager walletManager) {
        if (!walletManager.getIso(app).equalsIgnoreCase(WalletBitcoinManager.getInstance(app).getIso(app))) {
            //todo add backup for BCH
            return null;
        }
        String jsonString = urlGET(app, "https://bitpay.com/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    /**
     *
     * @param app
     * @param walletManager
     * @return average rate for Coin referenced to 1 BTC
     *          taken from last price
     */
    public static float fetchRatesCoin(Activity app, BaseWalletManager walletManager) {
        String url1 = "https://api.crex24.com/v2/public/tickers?instrument=WGR-BTC";
        String jsonString1 = urlGET(app, url1);

        float price1=100000;

        JSONArray jsonArray1 = null;
        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1);        // retry
            if (jsonString1 == null) {
                Log.e(TAG, "fetchRates: crex24 failed, response is null");
                return price1;
            }
        }

        try {
            JSONArray arr = new JSONArray(jsonString1);
            JSONObject objectTicker = arr.getJSONObject(0);
            price1 = (1 / (float)(objectTicker.getDouble("last")) );
        } catch (JSONException ignored) {
        }

        return price1;
    }

    // Explorer API
    public static List<TxExplorerInfo> fetchExplorerTxInfo(Activity app, String txHash) {
        String url1 = WagerrApp.HOST_EXPLORER +  "/api/tx/" +  txHash;
        String jsonString1 = urlGET(app, url1);
        List<TxExplorerInfo> ret = new ArrayList<>();

        JSONArray jsonArray1 = null;
        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1);        // retry
            if (jsonString1 == null) {
                Log.e(TAG, "fetchExplorerTxInfo: explorer failed, response is null");
                return null;
            }
        }

        try {
            JSONObject object = new JSONObject(jsonString1);
            JSONArray arrVOut = object.getJSONArray("vout");
            for (int i = 0; i < arrVOut.length(); i++) {
                JSONObject vout = arrVOut.getJSONObject(i);
                String address = vout.getString("address");
                if ( address.startsWith("OP_RETURN"))   {
                    TxExplorerInfo txInfo = new TxExplorerInfo();
                    txInfo.PopulateFromJsonObject( vout );
                    ret.add(txInfo);
                }
            }
        } catch (JSONException ignored) {
        }

        return ret;
    }

    public static TxExplorerPayoutInfo fetchExplorerPayoutTxInfo(Activity app, String txHash, int vOut) {
        String url1 = WagerrApp.HOST_EXPLORER +  "/api/bet/infobypayout?payoutTx=" + txHash + "&nOut=" + vOut;
        String jsonString1 = urlGET(app, url1);
        TxExplorerPayoutInfo ret = new TxExplorerPayoutInfo();

        JSONArray jsonArray1 = null;
        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1);        // retry
            if (jsonString1 == null) {
                Log.e(TAG, "fetchExplorerPayoutTxInfo: explorer failed, response is null");
                return null;
            }
        }

        try {
            JSONObject object = new JSONObject(jsonString1);
            ret.PopulateFromJsonObject( object );
        } catch (JSONException ignored) {
        }

        return ret;
    }


    // END Explorer API

    // Instaswap API
    public static JSONObject InstaSwapTickers(Activity app, String getCoin, String giveCoin, String sendAmount) {
        String url1 = instaswapURL + "InstaswapTickers&getCoin="+getCoin+"&giveCoin="+giveCoin+"&sendAmount="+sendAmount;
        String jsonString1 = urlGET(app, url1);
        JSONObject objectTicker = null;

        JSONArray jsonArray1 = null;
        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1);        // retry
            if (jsonString1 == null) {
                Log.e(TAG, "InstaSwapTickers: instaswap URL failed, response is null");
                return null;
            }
        }

        try {
            JSONObject object = new JSONObject(jsonString1);
            String strInfo = object.getString("apiInfo");
            if (strInfo.equals("OK")) {
                objectTicker = object.getJSONObject("response");
            }
        } catch (JSONException ignored) {
        }

        return objectTicker;
    }

    public static List<String> InstaSwapAllowedPairs(Activity app) {
        String url1 = instaswapURL + "InstaswapReportAllowedPairs";
        String jsonString1 = urlGET(app, url1);
        List<String> ret = new ArrayList<>();

        JSONArray arrayResponse = null;
        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1);        // retry
            if (jsonString1 == null) {
                Log.e(TAG, "InstaSwapAllowedPairs: instaswap URL failed, response is null");
                return null;
            }
        }

        try {
            JSONObject object = new JSONObject(jsonString1);
            String strInfo = object.getString("apiInfo");
            if (strInfo.equals("OK")) {
                arrayResponse = object.getJSONArray("response");
                if (arrayResponse != null) {
                    int length = arrayResponse.length();
                    for (int i = 0; i < length; i++) {
                        JSONObject objectResponse = (JSONObject) arrayResponse.get(i);
                        String deposit = objectResponse.getString("depositCoin");
                        String receive = objectResponse.getString("receiveCoin");

                        if ( receive.equals(WalletWagerrManager.ISO))   {
                            ret.add(deposit);
                        }
                    }
                }
            }
        } catch (JSONException ignored) {
        }

        return ret;
    }

    public static List<SwapUiHolder> InstaSwapReport(Activity app, String wallet) {
        String url1 = instaswapURL + "InstaswapReportWalletHistory&wallet="+wallet;
        String jsonString1 = urlGET(app, url1);

        List<SwapUiHolder> swapList = new ArrayList<>();

        JSONArray jsonArray1 = null;
        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1);        // retry
            if (jsonString1 == null) {
                Log.e(TAG, "InstaSwapReport: instaswap API failed, response is null");
                return null;
            }
        }

        try {
            JSONObject object = new JSONObject(jsonString1);
            String strInfo = object.getString("apiInfo");
            if (strInfo.equals("OK")) {
                JSONArray arrayResponse = object.getJSONArray("response");
                if (arrayResponse != null) {
                    int length = arrayResponse.length();
                    for (int i = 0; i < length; i++) {
                        JSONObject objectResponse = (JSONObject) arrayResponse.get(i);
                        SwapUiHolder swapResponse = new SwapUiHolder(objectResponse.getString("transactionId"),
                                objectResponse.getString("depositCoin"),
                                objectResponse.getString("receiveCoin"),
                                objectResponse.getString("depositAmount"),
                                objectResponse.getString("receivingAmount"),
                                objectResponse.getString("refundWallet"),
                                objectResponse.getString("receiveWallet"),
                                objectResponse.getString("depositWallet"),
                                SwapUiHolder.TransactionState.fromValue(objectResponse.getString("transactionState")),
                                objectResponse.getString("timestamp"));

                        swapList.add(swapResponse);
                    }
                }
            }
        } catch (JSONException ignored) {
        }

        return swapList;
    }

    public static SwapResponse InstaSwapDoSwap(Activity app, String getCoin, String giveCoin, String sendAmount
            , String receiveWallet, String refundWallet) {
        String url1 = instaswapURL + "InstaswapSwap&getCoin="+getCoin+"&giveCoin="+giveCoin+"&sendAmount="+sendAmount
                +"&receiveWallet="+receiveWallet+"&refundWallet="+refundWallet;
        String jsonString1 = urlGET(app, url1);

        SwapResponse swapResponse = null;

        JSONArray jsonArray1 = null;
        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1);        // retry
            if (jsonString1 == null) {
                Log.e(TAG, "InstaSwapSwap: instaswap API failed, response is null");
                return null;
            }
        }

        try {
            JSONObject object = new JSONObject(jsonString1);
            String strInfo = object.getString("apiInfo");
            if (strInfo.equals("OK")) {
                JSONObject objectResponse = object.getJSONObject("response");
                swapResponse = new SwapResponse( objectResponse.getString("TransactionId"),
                        objectResponse.getString("depositWallet"),
                        objectResponse.getString("receivingAmount"));
            }
        } catch (JSONException ignored) {
        }

        return swapResponse;
    }
    // END Instaswap API

    // Wagerr Data API
    public static void SyncAPITeamNames(Activity app) {
        String url1 = "https://sync-api.wagerr.com/mappings/teamnames";
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put( "AccessAgent", "WgrMobile");
        String jsonString1 = urlGET(app, url1, extraHeaders);

        if (jsonString1 == null) {
            jsonString1 = urlGET(app, url1, extraHeaders);
            if (jsonString1 == null) {
                Log.e(TAG, "SyncAPI: API failed, response is null");
                return;
            }
        }

        try {
            JSONArray jsonArray1 = new JSONArray(jsonString1);
            BetMappingTxDataStore bmtds = BetMappingTxDataStore.getInstance(app);
            bmtds.putAPITransaction(app, "wgr", jsonArray1, BetMappingEntity.MappingNamespaceType.TEAM_NAME);
        } catch (JSONException ignored) {
        }

        return;
    }

    public static void updateFeePerKb(Context app) {
        WalletsMaster wm = WalletsMaster.getInstance(app);
        for (BaseWalletManager wallet : wm.getAllWallets()) {
            wallet.updateFee(app);
        }
    }

    public static String urlGET(Context app, String myURL)  {
        Map<String, String> extraHeaders = new HashMap<>();
        return urlGET(app, myURL, extraHeaders);
    }

    public static String urlGET(Context app, String myURL, Map<String, String> extraHeaders) {
//        System.out.println("Requested URL_EA:" + myURL);
        if (ActivityUTILS.isMainThread()) {
            Log.e(TAG, "urlGET: network on main thread");
            throw new RuntimeException("network on main thread");
        }
        Map<String, String> headers = WagerrApp.getBreadHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Iterator it2 = extraHeaders.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry pair = (Map.Entry) it2.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Request request = builder.build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request, false, 0);

        try {
            if (resp == null) {
                Log.e(TAG, "urlGET: " + myURL + ", resp is null");
                return null;
            }
            response = resp.body().string();
            String strDate = resp.header("date");
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return response;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            if (resp != null) resp.close();
        }
        return response;
    }

}

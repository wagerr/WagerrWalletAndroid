package com.wagerrwallet;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.firebase.FirebaseApp;
import com.wagerrwallet.presenter.activities.util.BRActivity;
import com.wagerrwallet.tools.listeners.SyncReceiver;
import com.wagerrwallet.tools.manager.InternetManager;
import com.wagerrwallet.tools.security.BRKeyStore;
import com.wagerrwallet.tools.util.Utils;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static com.platform.APIClient.BREAD_POINT;

import org.acra.*;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
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

public class WagerrApp extends Application {
    private static final String TAG = WagerrApp.class.getName();
    public static int DISPLAY_HEIGHT_PX;
    FingerprintManager mFingerprintManager;
    // host is the server(s) on which the API is hosted
    public static String HOST = "api.breadwallet.com";
    public static String HOST_UTXO = "https://chainz.cryptoid.info/";
    public static String HOST_EXPLORER = (BuildConfig.BITCOIN_TESTNET) ? "https://explorer2.wagerr.com" : "https://explorer.wagerr.com";
    public static String HOST_UTXO_KEY = "552651714eae";
    private static List<OnAppBackgrounded> listeners;
    private static Timer isBackgroundChecker;
    public static AtomicInteger activityCounter = new AtomicInteger();
    public static long backgroundedTime;
    public static boolean appInBackground;
    private static Context mContext;

    public static final boolean IS_ALPHA = false;

    public static final Map<String, String> mHeaders = new HashMap<>();

    private static Activity currentActivity;

/*    @AcraCore(buildConfigClass = BuildConfig.class,
            reportFormat = StringFormat.JSON)
    @AcraEmailSender(uri = "https://yourdomain.com/acra/report",
            httpMethod = HttpSender.Method.POST,
            basicAuthLogin = "*****",
            basicAuthPassword = "*****")
 */   public class MyApplication extends Application {
        @Override
        protected void attachBaseContext(Context base) {
            super.attachBaseContext(base);

            ACRAInit();
        }
    }

    public void ACRAInit()  {
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this)
                .setBuildConfigClass(BuildConfig.class)
                .setReportFormat(StringFormat.JSON);
        /*builder.getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class)
                .setMailTo("mip.putalocura@gmail.com")
                .setReportAsFile(true)
                .setSubject("Wagerr Crash Report")
                .setEnabled(true);*/
        builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
                .setHttpMethod(HttpSender.Method.POST)
                .setUri("http://167.86.74.98/acra.php")
                .setEnabled(true);
        builder.getPluginConfigurationBuilder(ToastConfigurationBuilder.class).setText("Sending crash report");
        // The following line triggers the initialization of ACRA
        ACRA.init(this,builder);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Utils.isEmulatorOrDebug(this)) {
            FirebaseApp.initializeApp(this);
//            BRKeyStore.putFailCount(0, this);
            HOST = "stage2.breadwallet.com";
            FirebaseCrash.setCrashCollectionEnabled(false);
//            FirebaseCrash.report(new RuntimeException("test with new json file"));
        }
        ACRAInit();
        mContext = this;

        if (!Utils.isEmulatorOrDebug(this) && IS_ALPHA)
            throw new RuntimeException("can't be alpha for release");

        boolean isTestVersion = BREAD_POINT.contains("staging") || BREAD_POINT.contains("stage");
        boolean isTestNet = BuildConfig.BITCOIN_TESTNET;
        String lang = getCurrentLocale(this);

        mHeaders.put("X-Is-Internal", IS_ALPHA ? "true" : "false");
        mHeaders.put("X-Testflight", isTestVersion ? "true" : "false");
        mHeaders.put("X-Bitcoin-Testnet", isTestNet ? "true" : "false");
        mHeaders.put("Accept-Language", lang);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int DISPLAY_WIDTH_PX = size.x;
        DISPLAY_HEIGHT_PX = size.y;
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);

        registerReceiver(InternetManager.getInstance(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

//        addOnBackgroundedListener(new OnAppBackgrounded() {
//            @Override
//            public void onBackgrounded() {
//
//            }
//        });

    }

    /**
     * Returns true if the device state is valid. The device state is considered valid, if the device password
     * is enabled and if the Android key store state is valid.  The Android key store can be invalided if the
     * device password was removed or if fingerprints are added/removed.
     *
     * @return "", if the device state is valid; !="", otherwise.
     */
    public String isDeviceStateValid() {
        boolean isDeviceStateValid;
        String dialogType = "";

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        if (!keyguardManager.isKeyguardSecure()) {
            isDeviceStateValid = false;
            dialogType = this.getString(R.string.Prompts_NoScreenLock_body_android);
        } else {
            switch (BRKeyStore.getValidityStatus()) {
                case VALID:
                    isDeviceStateValid = true;
                    break;
                case INVALID_WIPE:
                    isDeviceStateValid = false;
                    dialogType = this.getString(R.string.Alert_keystore_invalidated_wipe_android);
                    break;
                case INVALID_UNINSTALL:
                    isDeviceStateValid = false;
                    dialogType = this.getString(R.string.Alert_keystore_invalidated_uninstall_android);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid key store validity status.");
            }
        }

        return dialogType;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public String getCurrentLocale(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return ctx.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } else {
            //noinspection deprecation
            return ctx.getResources().getConfiguration().locale.getLanguage();
        }
    }

    public static Map<String, String> getBreadHeaders() {
        return mHeaders;
    }

    public static Context getBreadContext() {
        Context app = currentActivity;
        if (app == null) app = SyncReceiver.app;
        if (app == null) app = mContext;
        return app;
    }

    public static void setBreadContext(Activity app) {
        currentActivity = app;
    }

    public static synchronized void fireListeners() {
        if (listeners == null) return;
        List<OnAppBackgrounded> copy = listeners;

            for (OnAppBackgrounded lis : copy) {
                try {
                    if (lis != null) lis.onBackgrounded();
                }
                catch (ConcurrentModificationException e)   {
                }
            }
    }

    public static void addOnBackgroundedListener(OnAppBackgrounded listener) {
        if (listeners == null) listeners = new ArrayList<>();
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public static boolean isAppInBackground(final Context context) {
        return context == null || activityCounter.get() <= 0;
    }

    //call onStop on evert activity so
    public static void onStop(final BRActivity app) {
        if (isBackgroundChecker != null) isBackgroundChecker.cancel();
        isBackgroundChecker = new Timer();
        TimerTask backgroundCheck = new TimerTask() {
            @Override
            public void run() {
                if (isAppInBackground(app)) {
                    backgroundedTime = System.currentTimeMillis();
                    Log.e(TAG, "App went in background!");
                    // APP in background, do something
                    isBackgroundChecker.cancel();
                    fireListeners();
                }
                // APP in foreground, do something else
            }
        };

        isBackgroundChecker.schedule(backgroundCheck, 500, 500);
    }

    public interface OnAppBackgrounded {
        void onBackgrounded();
    }
}

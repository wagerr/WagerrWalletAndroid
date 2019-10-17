package com.wagerrwallet.wallet.wallets.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.wagerrwallet.R;
import com.wagerrwallet.core.BRCoreAddress;
import com.wagerrwallet.core.BRCoreKey;
import com.wagerrwallet.core.BRCoreTransaction;
import com.wagerrwallet.presenter.customviews.BRDialogView;
import com.wagerrwallet.presenter.entities.CryptoRequest;
import com.wagerrwallet.tools.animation.BRAnimator;
import com.wagerrwallet.tools.animation.BRDialog;
import com.wagerrwallet.tools.manager.BRClipboardManager;
import com.wagerrwallet.tools.manager.BREventManager;
import com.wagerrwallet.tools.manager.BRReportsManager;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.manager.SendManager;
import com.wagerrwallet.tools.security.PostAuth;
import com.wagerrwallet.tools.threads.ImportPrivKeyTask;
import com.wagerrwallet.tools.threads.PaymentProtocolTask;
import com.wagerrwallet.tools.util.BRConstants;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 10/19/15.
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

public class CryptoUriParser {
    private static final String TAG = CryptoUriParser.class.getName();
    private static final Object lockObject = new Object();

    public static final String AMOUNT = "amount";
    public static final String VALUE = "value";
    public static final String LABEL = "label";
    public static final String MESSAGE = "message";
    public static final String REQ = "req"; // "req" parameter, whose value is a required variable which are prefixed with a req-.
    public static final String R_URL = "r"; // "r" parameter, whose value is a URL from which a PaymentRequest message should be fetched
    public static final String SCAN_QR = "scanqr";
    public static final String ADDRESS_LIST = "addressList";
    public static final String ADDRESS = "address";

    public static synchronized boolean processRequest(Context app, String url, BaseWalletManager walletManager) {
        if (url == null) {
            Log.e(TAG, "processRequest: url is null");
            return false;
        }

        if (ImportPrivKeyTask.trySweepWallet(app, url, walletManager)) return true;

        if (tryBreadUrl(app, url)) return true; //see if it's a bread url

        CryptoRequest requestObject = parseRequest(app, url);

        if (requestObject == null) {
            if (app != null) {
                BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                        app.getString(R.string.Send_invalidAddressTitle), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, null, 0);
            }
            return false;
        }
        if (requestObject.isPaymentProtocol()) {
            return tryPaymentRequest(requestObject);
        } else if (requestObject.address != null) {
            return tryCryptoUrl(requestObject, app);
        } else {
            if (app != null) {
                BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                        app.getString(R.string.Send_remoteRequestError), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, null, 0);
            }
            return false;
        }
    }

    private static void pushUrlEvent(Uri u) {
        Map<String, String> attr = new HashMap<>();
        attr.put("scheme", u == null ? "null" : u.getScheme());
        attr.put("host", u == null ? "null" : u.getHost());
        attr.put("path", u == null ? "null" : u.getPath());
        BREventManager.getInstance().pushEvent("send.handleURL", attr);
    }

    public static boolean isCryptoUrl(Context app, String url) {
        if (Utils.isNullOrEmpty(url)) return false;
        if (BRCoreKey.isValidBitcoinBIP38Key(url) || BRCoreKey.isValidBitcoinPrivateKey(url))
            return true;

        CryptoRequest requestObject = parseRequest(app, url);
        // return true if the request is valid url and has param: r or param: address
        // return true if it is a valid bitcoinPrivKey
        return (requestObject != null && (requestObject.isPaymentProtocol() || requestObject.hasAddress()));
    }


    public static CryptoRequest parseRequest(Context app, String str) {
        if (str == null || str.isEmpty()) return null;
        CryptoRequest obj = new CryptoRequest();

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();

        if (scheme == null || "".equals(scheme) ) {
            scheme = WalletWagerrManager.getInstance(app).getScheme(app);
            obj.iso = WalletWagerrManager.getInstance(app).getIso(app);

        } else {
            for (BaseWalletManager walletManager : WalletsMaster.getInstance(app).getAllWallets()) {
                if (scheme.equalsIgnoreCase(walletManager.getScheme(app))) {
                    obj.iso = walletManager.getIso(app);
                    break;
                }
            }
        }

        obj.scheme = scheme;

        String schemeSpecific = u.getSchemeSpecificPart();
        if (schemeSpecific.startsWith("//")) {
            // Fix invalid bitcoin uri
            schemeSpecific = schemeSpecific.substring(2);
        }

        u = Uri.parse(scheme + "://" + schemeSpecific);

        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);

        String host = u.getHost();
        if (host != null) {
            String trimmedHost = host.trim();
            if (obj.iso.equalsIgnoreCase("bch"))
                trimmedHost = scheme + ":" + trimmedHost; //bitcoin cash has the scheme attached to the address
            String addrs = wm.undecorateAddress(app, trimmedHost);
            if (!Utils.isNullOrEmpty(addrs) && new BRCoreAddress(addrs).isValid()) {
                obj.address = addrs;
            }
        }
        String query = u.getQuery();
        pushUrlEvent(u);
        if (query == null) return obj;
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2)
                continue;
            if (keyValue[0].trim().equals("amount")) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                    obj.amount = bigDecimal.multiply(new BigDecimal("100000000"));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (keyValue[0].trim().equals("label")) {
                obj.label = keyValue[1].trim();
            } else if (keyValue[0].trim().equals("message")) {
                obj.message = keyValue[1].trim();
            } else if (keyValue[0].trim().startsWith("req")) {
                obj.req = keyValue[1].trim();
            } else if (keyValue[0].trim().startsWith("r")) {
                obj.r = keyValue[1].trim();
            }
        }
        return obj;
    }

    private static boolean tryBreadUrl(Context app, String url) {
        if (Utils.isNullOrEmpty(url)) return false;
        String tmp = url.trim().replaceAll("\n", "").replaceAll(" ", "%20");

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase("bread")) {
            String schemeSpecific = u.getSchemeSpecificPart();
            if (schemeSpecific != null && schemeSpecific.startsWith("//")) {
                // Fix invalid bitcoin uri
                schemeSpecific = schemeSpecific.substring(2);
            }

            u = Uri.parse(scheme + "://" + schemeSpecific);

            BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);

            String host = u.getHost();
            if (Utils.isNullOrEmpty(host)) return false;

            switch (host) {
                case "scanqr":
                    BRAnimator.openScanner((Activity) app, BRConstants.SCANNER_REQUEST);
                    break;
                case "addressList":
                    //todo implement
                    break;
                case "address":
                    BRClipboardManager.putClipboard(app, wm.decorateAddress(app, BRSharedPrefs.getReceiveAddress(app, wm.getIso(app))));

                    break;
            }

            return true;
        }
        return false;

    }

    private static boolean tryPaymentRequest(CryptoRequest requestObject) {
        String theURL = null;
        String url = requestObject.r;
        synchronized (lockObject) {
            try {
                theURL = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
            new PaymentProtocolTask().execute(theURL, requestObject.label);
        }
        return true;
    }

    private static boolean tryCryptoUrl(final CryptoRequest requestObject, final Context ctx) {
        final Activity app;
        if (ctx instanceof Activity) {
            app = (Activity) ctx;
        } else {
            Log.e(TAG, "tryCryptoUrl: " + "app isn't activity: " + ctx.getClass().getSimpleName());
            BRReportsManager.reportBug(new NullPointerException("app isn't activity: " + ctx.getClass().getSimpleName()));
            return false;
        }
        if (requestObject == null || requestObject.address == null || requestObject.address.isEmpty())
            return false;
        BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
        if (requestObject.iso != null && !requestObject.iso.equalsIgnoreCase(wallet.getIso(ctx))) {

            BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), "Not a valid " + wallet.getName(ctx) + " address", app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismiss();
                }
            }, null, null, 0);
            return true; //true since it's a crypto url but different iso than the currently chosen one
        }
//        String amount = requestObject.amount;

        if (requestObject.amount == null || requestObject.amount.doubleValue() == 0) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BRAnimator.showSendFragment(app, requestObject);
                }
            });
        } else {
            BRAnimator.killAllFragments(app);
            if (Utils.isNullOrEmpty(requestObject.address) || !new BRCoreAddress(requestObject.address).isValid()) {
                BRDialog.showSimpleDialog(app, app.getString(R.string.Send_invalidAddressTitle), "");
                return true;
            }
            BRCoreTransaction tx = wallet.getWallet().createTransaction(requestObject.amount.longValue(), new BRCoreAddress(requestObject.address));
            if (tx == null) {
                BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), "Insufficient amount for transaction", app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
                return true;
            }
            requestObject.tx = tx;
            SendManager.sendTransaction(app, requestObject, wallet);
        }

        return true;

    }

    public static Uri createCryptoUrl(Context app, BaseWalletManager wm, String addr, long satoshiAmount, String label, String message, String rURL) {
        Uri.Builder builder = new Uri.Builder();
        String walletScheme = wm.getScheme(app);
        String cleanAddress = addr;
        if (addr.contains(":")) {
            cleanAddress = addr.split(":")[1];
        }
        builder = builder.scheme(walletScheme);
        if (!Utils.isNullOrEmpty(cleanAddress))
            builder = builder.appendPath(cleanAddress);
        if (satoshiAmount != 0)
            builder = builder.appendQueryParameter("amount", new BigDecimal(satoshiAmount).divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE).toPlainString());
        if (label != null && !label.isEmpty())
            builder = builder.appendQueryParameter("label", label);
        if (message != null && !message.isEmpty())
            builder = builder.appendQueryParameter("message", message);
        if (rURL != null && !rURL.isEmpty())
            builder = builder.appendQueryParameter("r", rURL);

        return Uri.parse(builder.build().toString().replace("/", ""));

    }

    /**
     * Generate an Uri for a given CryptoRequest including the wallet schema.
     * @param context       Context were was called.
     * @param walletManager Wallet manager.
     * @param request       Request information to be included in the Uri.
     * @return  An Uri with the request information.
     */
    public static Uri createCryptoUrl(Context context,
                                      BaseWalletManager walletManager,
                                      CryptoRequest request) {
        String currencyCode = WalletWagerrManager.ISO;
        Uri.Builder builder = new Uri.Builder();
        String walletScheme = walletManager.getScheme(context);
        String cleanAddress = request.getAddress(false);
        builder = builder.scheme(walletScheme);
        if (!Utils.isNullOrEmpty(cleanAddress)) {
            builder = builder.appendPath(cleanAddress);
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            if (currencyCode.equalsIgnoreCase(WalletWagerrManager.ISO)) {
                BigDecimal amount = WalletWagerrManager.getInstance(context).getCryptoForSmallestCrypto(context, request.getAmount());
                builder = builder.appendQueryParameter(AMOUNT, amount.toPlainString());
            } else {
                throw new RuntimeException("URI not supported for: " + currencyCode);
            }
        }
        if (!Utils.isNullOrEmpty(request.getLabel())) {
            builder = builder.appendQueryParameter(LABEL, request.getLabel());
        }
        if (!Utils.isNullOrEmpty(request.getMessage())) {
            builder = builder.appendQueryParameter(MESSAGE, request.getMessage());
        }
        if (!Utils.isNullOrEmpty(request.getRUrl())) {
            builder = builder.appendQueryParameter(R_URL, request.getRUrl());
        }

        return Uri.parse(builder.build().toString().replace("/", ""));

    }
}

package com.wagerrwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.wagerrwallet.BuildConfig;
import com.wagerrwallet.R;
import com.wagerrwallet.core.BRCoreAddress;
import com.wagerrwallet.core.BRCoreTransaction;
import com.wagerrwallet.presenter.activities.SwapActivity;
import com.wagerrwallet.presenter.customviews.BRButton;
import com.wagerrwallet.presenter.customviews.BRDialogView;
import com.wagerrwallet.presenter.customviews.BRKeyboard;
import com.wagerrwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.wagerrwallet.presenter.customviews.BRText;
import com.wagerrwallet.presenter.entities.CryptoRequest;
import com.wagerrwallet.presenter.entities.SwapResponse;
import com.wagerrwallet.presenter.interfaces.BROnSignalCompletion;
import com.wagerrwallet.tools.animation.BRAnimator;
import com.wagerrwallet.tools.animation.BRDialog;
import com.wagerrwallet.tools.animation.SlideDetector;
import com.wagerrwallet.tools.animation.SpringAnimator;
import com.wagerrwallet.tools.manager.BRApiManager;
import com.wagerrwallet.tools.manager.BRClipboardManager;
import com.wagerrwallet.tools.manager.BRReportsManager;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.manager.SendManager;
import com.wagerrwallet.tools.manager.SwapManager;
import com.wagerrwallet.tools.manager.TxManager;
import com.wagerrwallet.tools.threads.executor.BRExecutor;
import com.wagerrwallet.tools.util.BRConstants;
import com.wagerrwallet.tools.util.CurrencyUtils;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.abstracts.BaseWalletManager;
import com.wagerrwallet.wallet.wallets.util.CryptoUriParser;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.platform.HTTPServer.URL_SUPPORT;
import static com.wagerrwallet.wallet.wallets.util.CryptoUriParser.parseRequest;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
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

public class FragmentSendSwap extends Fragment {
    private static final String TAG = FragmentSendSwap.class.getName();
    public ScrollView backgroundLayout;
    public LinearLayout signalLayout;
    private BRKeyboard keyboard;
    private EditText addressEdit;
    private Button paste;
    private Button send;
    private TextView walletReceive;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private TextView amountReceive;
    private EditText amountEdit;
    private long curBalance;
    private String selectedIso;
    private Button isoButton;
    private int keyboardIndex;
    private LinearLayout keyboardLayout;
    private ImageButton close;
    private ConstraintLayout amountLayout;
    private boolean feeButtonsShown = false;
    private boolean amountLabelOn = true;
    private TextView mTOSLabel;
    private TextView mTOSLink;
    private Switch mTOSAccept;

    private static String savedMemo;
    private static String savedIso;
    private static String savedAmount;

    private boolean ignoreCleanup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_sendswap, container, false);
        backgroundLayout = (ScrollView) rootView.findViewById(R.id.background_layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        keyboard = (BRKeyboard) rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        keyboard.setBRKeyboardColor(R.color.white);
        isoText = (TextView) rootView.findViewById(R.id.iso_text);
        addressEdit = (EditText) rootView.findViewById(R.id.address_edit);
        paste = (Button) rootView.findViewById(R.id.paste_button);
        send = (Button) rootView.findViewById(R.id.send_button);
        amountReceive = (TextView) rootView.findViewById(R.id.amount_receive);
        walletReceive = (TextView) rootView.findViewById(R.id.wallet_receive);
        amountEdit = (EditText) rootView.findViewById(R.id.amount_edit);
        isoButton = (Button) rootView.findViewById(R.id.iso_button);
        keyboardLayout = (LinearLayout) rootView.findViewById(R.id.keyboard_layout);
        amountLayout = (ConstraintLayout) rootView.findViewById(R.id.amount_layout);
        mTOSLabel =  (TextView) rootView.findViewById(R.id.content_text);
        mTOSLink =  (TextView) rootView.findViewById(R.id.link_text);
        mTOSAccept =  (Switch) rootView.findViewById(R.id.chk_acceptTOS);

        close = (ImageButton) rootView.findViewById(R.id.close_button);
        BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        selectedIso = "BTC";    // fixed by now    // = BRSharedPrefs.isCryptoPreferred(getActivity()) ? wm.getIso(getActivity()) : BRSharedPrefs.getPreferredFiatIso(getContext());

        amountBuilder = new StringBuilder(0);
        setListeners();
        isoText.setText(getString(R.string.Send_amountLabel));
        isoText.setTextSize(18);
        isoText.setTextColor(getContext().getColor(R.color.light_gray));
        isoText.requestLayout();
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        BRCoreAddress address = wm.getWallet().getAllAddresses()[0];
        if (Utils.isNullOrEmpty(address.stringify())) {
            Log.e(TAG, "getSwapUiHolders: ERROR, retrieved address:" + address);
        }
        walletReceive.setText( "Receive at: " + address.stringify() );

        signalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        keyboardIndex = signalLayout.indexOfChild(keyboardLayout);

        ImageButton faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
                    return;
                }
                BRAnimator.showSupportFragment(app, BRConstants.send);
            }
        });

        mTOSLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wagerr.zendesk.com/hc/en-us/articles/360040437891"));
                startActivity(browserIntent);
                getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });

        mTOSAccept.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ToggleSendButton( isChecked );
            }
        });

        showKeyboard(false);
        ToggleSendButton( false );

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        return rootView;
    }

    private void ToggleSendButton(Boolean bAccepted) {
        send.setAlpha( (bAccepted) ? 1.0f : .5f );
        send.setClickable(bAccepted);
    }

    private String getBitcoinRegexp()   {
        return "^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$";
    }

    private void setListeners() {
        amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard(true);
                if (amountLabelOn) { //only first time
                    amountLabelOn = false;
                    amountEdit.setHint("0");
                    amountEdit.setTextSize(24);
                    isoText.setTextColor(getContext().getColor(R.color.almost_black));
                    isoText.setText(CurrencyUtils.getSymbolByIso(getActivity(), selectedIso));
                    isoText.setTextSize(28);
                    final float scaleX = amountEdit.getScaleX();
                    amountEdit.setScaleX(0);

                    AutoTransition tr = new AutoTransition();
                    tr.setInterpolator(new OvershootInterpolator());
                    tr.addListener(new android.support.transition.Transition.TransitionListener() {
                        @Override
                        public void onTransitionStart(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionEnd(@NonNull android.support.transition.Transition transition) {
                            amountEdit.requestLayout();
                            amountEdit.animate().setDuration(100).scaleX(scaleX);
                        }

                        @Override
                        public void onTransitionCancel(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionPause(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionResume(@NonNull android.support.transition.Transition transition) {

                        }
                    });

                    ConstraintSet set = new ConstraintSet();
                    set.clone(amountLayout);
                    TransitionManager.beginDelayedTransition(amountLayout, tr);

                    int px4 = Utils.getPixelsFromDps(getContext(), 4);
//                    int px8 = Utils.getPixelsFromDps(getContext(), 8);
                    set.connect(isoText.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4);
                    set.connect(isoText.getId(), ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1);
                    set.applyTo(amountLayout);

                }

            }
        });

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                showKeyboard(false);
                String theUrl = BRClipboardManager.getClipboard(getActivity());
                if (Utils.isNullOrEmpty(theUrl)) {
                    sayClipboardEmpty();
                    return;
                }

                final BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());


                if (Utils.isEmulatorOrDebug(getActivity()) && BuildConfig.BITCOIN_TESTNET) {
                    theUrl = wm.decorateAddress(getActivity(), theUrl);
                }

                if (!theUrl.matches(getBitcoinRegexp())) {
                    sayInvalidAddress();
                }
                else {
                    addressEdit.setText(theUrl);
                }
            }
        });
/*
        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getPreferredFiatIso(getContext()))) {
                    Activity app = getActivity();
                    selectedIso = WalletsMaster.getInstance(app).getCurrentWallet(app).getIso(app);
                } else {
                    selectedIso = BRSharedPrefs.getPreferredFiatIso(getContext());
                }
                updateText();

            }
        });
        */

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //not allowed now
                if (!BRAnimator.isClickAllowed()) return;
                WalletsMaster master = WalletsMaster.getInstance(getActivity());
                BaseWalletManager wallet = master.getCurrentWallet(getActivity());
                //get the current wallet used
                if (wallet == null) {
                    Log.e(TAG, "onClick: Wallet is null and it can't happen.");
                    BRReportsManager.reportBug(new NullPointerException("Wallet is null and it can't happen."), true);
                    return;
                }
                boolean allFilled = true;
                String rawAddress = addressEdit.getText().toString();
                String amountStr = amountBuilder.toString();

                //inserted amount
                BigDecimal rawAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) ? "0" : amountStr);
                //is the chosen ISO a crypto (could be a fiat currency)
                boolean isIsoCrypto = master.isIsoCrypto(getActivity(), selectedIso);

                BigDecimal cryptoAmount = isIsoCrypto ? wallet.getSmallestCryptoForCrypto(getActivity(), rawAmount) : wallet.getSmallestCryptoForFiat(getActivity(), rawAmount);
                CryptoRequest req = CryptoUriParser.parseRequest(getActivity(), rawAddress);
                if (rawAmount.compareTo(new BigDecimal("0")) <= 0 ) {
                    sayInvalidAmount();
                    return;
                }

                if (!rawAddress.matches(getBitcoinRegexp())) {
                    sayInvalidAddress();
                    return;
                }
                // instaswap send
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        SwapActivity app = (SwapActivity)getActivity();
                        BRCoreAddress address = wallet.getWallet().getAllAddresses()[0];
                        if (Utils.isNullOrEmpty(address.stringify())) {
                            Log.e(TAG, "getSwapUiHolders: ERROR, retrieved address:" + address);
                        }
                        SwapResponse response = BRApiManager.InstaSwapDoSwap(app, "WGR", "BTC", amountStr, address.stringify(), rawAddress);
                        String error = null;
                        if (response == null)   {
                            error = app.getString(R.string.Instaswap_Error);
                        }
                        else {
                            BRClipboardManager.putClipboard(getContext(), response.getDepositWallet());
                        }

                        Message msg = Message.obtain(); // Creates an new Message instance
                        msg.arg1 = 2;   // DoSwap callback
                        msg.obj = error;
                        handler.sendMessage(msg);
                    }
                });
            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                if (app != null)
                    app.getFragmentManager().popBackStack();
            }
        });


        addressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    Utils.hideKeyboard(getActivity());
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showKeyboard(true);
                        }
                    }, 500);

                }
                return false;
            }
        });

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });

    }

    private void showKeyboard(boolean b) {
        int curIndex = keyboardIndex;

        if (!b) {
            signalLayout.removeView(keyboardLayout);

        } else {
            Utils.hideKeyboard(getActivity());
            if (signalLayout.indexOfChild(keyboardLayout) == -1)
                signalLayout.addView(keyboardLayout, curIndex);
            else
                signalLayout.removeView(keyboardLayout);

        }
    }

    private void sayClipboardEmpty() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_emptyPasteboard), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismiss();
            }
        }, null, null, 0);
        BRClipboardManager.putClipboard(getActivity(), "");
    }

    private void saySomethingWentWrong() {
        BRDialog.showCustomDialog(getActivity(), "", "Something went wrong.", getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismiss();
            }
        }, null, null, 0);
        BRClipboardManager.putClipboard(getActivity(), "");
    }

    private void sayInvalidAddress() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Instaswap_InvalidAddress), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismiss();
            }
        }, null, null, 0);
        BRClipboardManager.putClipboard(getActivity(), "");
    }

    private void sayInvalidAmount() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Instaswap_InvalidAmount), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismiss();
            }
        }, null, null, 0);
        BRClipboardManager.putClipboard(getActivity(), "");
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive())
                    observer.removeOnGlobalLayoutListener(this);
                BRAnimator.animateBackgroundDim(backgroundLayout, false);
                BRAnimator.animateSignalSlide(signalLayout, false, new BRAnimator.OnSlideAnimationEnd() {
                    @Override
                    public void onAnimationEnd() {

                    }
                });
            }
        });

    }


    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
        BRAnimator.animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null) {
                    try {
                        getActivity().getFragmentManager().popBackStack();
                    } catch (Exception ignored) {

                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
        if (!ignoreCleanup) {
            savedIso = null;
            savedAmount = null;
            savedMemo = null;
        }
    }

    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else if (key.charAt(0) == '.') {
            handleSeparatorClick();
        }
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        WalletsMaster master = WalletsMaster.getInstance(getActivity());
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= master.getCurrentWallet(getActivity()).getMaxAmount(getActivity()).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > 8 )))
                return;
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") )
            return;
        amountBuilder.append(".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.length() > 0) {
            amountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }

    }

    // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 1:     // Ticker Callback
                    String getAmount = (String) msg.obj;
                    amountReceive.setText("Amount to receive: " + getAmount + " WGR");
                    break;
                case 2:     // DoSwap Callback
                    String error = (String) msg.obj;
                    SwapActivity app = (SwapActivity)getActivity();
                    if (app instanceof Activity)
                        BRAnimator.showBreadSignal((Activity) app, Utils.isNullOrEmpty(error) ? app.getString(R.string.Instaswap_sendSuccess) : app.getString(R.string.Alert_error),
                                Utils.isNullOrEmpty(error) ? app.getString(R.string.Instaswap_sendSuccessSubheader) : "Error: " + error, Utils.isNullOrEmpty(error) ? R.drawable.ic_check_mark_white : R.drawable.ic_error_outline_black_24dp, new BROnSignalCompletion() {
                                    @Override
                                    public void onComplete() {
                                        if (!((Activity) app).isDestroyed()) {
                                            ((Activity) app).getFragmentManager().popBackStack();
                                        }

                                        if (BRAnimator.showFragmentEvent!=null) {
                                            BRAnimator.showEventDetails((Activity) app, BRAnimator.showFragmentEvent, 0);
                                            BRAnimator.showFragmentEvent=null;
                                            SwapManager.getInstance().adapter.updateData();
                                        }
                                    }
                                });
                    break;
            }

        }
    };

    private void updateText() {
        Activity app = getActivity();
        if (app == null) return;

        String stringAmount = amountBuilder.toString();
        setAmount();
        BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
        String balanceString;
        if (selectedIso == null)
            selectedIso = wallet.getIso(app);
        //String iso = selectedIso;
        curBalance = wallet.getCachedBalance(app);
        if (!amountLabelOn)
            isoText.setText(CurrencyUtils.getSymbolByIso(app, selectedIso));
        isoButton.setText( "BTC" );

        //is the chosen ISO a crypto (could be also a fiat currency)
        boolean isIsoCrypto = WalletsMaster.getInstance(getActivity()).isIsoCrypto(getActivity(), selectedIso);

        BigDecimal inputAmount = new BigDecimal(Utils.isNullOrEmpty(stringAmount) || stringAmount.equalsIgnoreCase(".") ? "0" : stringAmount);

        //smallest crypto e.g. satoshis
        BigDecimal cryptoAmount = isIsoCrypto ? wallet.getSmallestCryptoForCrypto(app, inputAmount) : wallet.getSmallestCryptoForFiat(app, inputAmount);

        amountLayout.requestLayout();

        // update received amount
        if (inputAmount.compareTo(new BigDecimal(0)) == 1 ) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    String getAmount = BRApiManager.InstaSwapTickers(app, "WGR", "BTC", stringAmount);
                    Message msg = Message.obtain(); // Creates an new Message instance
                    msg.arg1 = 1;   // Ticker callback
                    msg.obj = getAmount;
                    handler.sendMessage(msg);
                }
            });
        }
    }

    private void setAmount() {
        String tmpAmount = amountBuilder.toString();
        int divider = tmpAmount.length();
        if (tmpAmount.contains(".")) {
            divider = tmpAmount.indexOf(".");
        }
        StringBuilder newAmount = new StringBuilder();
        for (int i = 0; i < tmpAmount.length(); i++) {
            newAmount.append(tmpAmount.charAt(i));
            if (divider > 3 && divider - 1 != i && divider > i && ((divider - i - 1) % 3 == 0)) {
                newAmount.append(",");
            }
        }
        amountEdit.setText(newAmount.toString());
    }

    // from the link above
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Log.e(TAG, "onConfigurationChanged: hidden");
            showKeyboard(true);
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Log.e(TAG, "onConfigurationChanged: shown");
            showKeyboard(false);
        }
    }

}
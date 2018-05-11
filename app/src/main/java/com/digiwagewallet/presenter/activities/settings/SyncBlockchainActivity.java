package com.digiwagewallet.presenter.activities.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.digiwagewallet.R;
import com.digiwagewallet.presenter.activities.util.BRActivity;
import com.digiwagewallet.presenter.customviews.BRDialogView;
import com.digiwagewallet.tools.animation.BRAnimator;
import com.digiwagewallet.tools.animation.BRDialog;
import com.digiwagewallet.tools.manager.BRSharedPrefs;
import com.digiwagewallet.tools.threads.executor.BRExecutor;
import com.digiwagewallet.tools.util.BRConstants;
import com.digiwagewallet.wallet.WalletsMaster;


public class SyncBlockchainActivity extends BRActivity {
    private static final String TAG = SyncBlockchainActivity.class.getName();
    private Button scanButton;
    public static boolean appVisible = false;
    private static SyncBlockchainActivity app;

    public static SyncBlockchainActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_blockchain);

        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.reScan);
            }
        });

        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRDialog.showCustomDialog(SyncBlockchainActivity.this, getString(R.string.ReScan_alertTitle),
                        getString(R.string.ReScan_footer), getString(R.string.ReScan_alertAction), getString(R.string.Button_cancel),
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRSharedPrefs.putStartHeight(SyncBlockchainActivity.this, BRSharedPrefs.getCurrentWalletIso(SyncBlockchainActivity.this), 0);
                                        BRSharedPrefs.putAllowSpend(SyncBlockchainActivity.this, BRSharedPrefs.getCurrentWalletIso(SyncBlockchainActivity.this), false);
                                        WalletsMaster.getInstance(SyncBlockchainActivity.this).getCurrentWallet(SyncBlockchainActivity.this).getPeerManager().rescan();
                                        BRAnimator.startBreadActivity(SyncBlockchainActivity.this, false);

                                    }
                                });
                            }
                        }, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, 0);



            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}

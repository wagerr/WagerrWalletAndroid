package com.wagerrwallet.presenter.activities.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wagerrwallet.R;
import com.wagerrwallet.core.BRCoreKey;
import com.wagerrwallet.core.BRCoreMerkleBlock;
import com.wagerrwallet.core.BRCorePeer;
import com.wagerrwallet.presenter.activities.util.BRActivity;
import com.wagerrwallet.tools.animation.BRAnimator;
import com.wagerrwallet.tools.manager.BRSharedPrefs;
import com.wagerrwallet.tools.sqlite.MerkleBlockDataSource;
import com.wagerrwallet.tools.threads.executor.BRExecutor;
import com.wagerrwallet.tools.util.TrustedNode;
import com.wagerrwallet.tools.util.Utils;
import com.wagerrwallet.wallet.WalletsMaster;
import com.wagerrwallet.wallet.wallets.wagerr.WalletWagerrManager;

public class BetSettings extends BRActivity {
    private static final String TAG = BetSettings.class.getName();
    public static String FEATURE_DISPLAY_ODDS = "wgr_displaymodifiedodds";

    private AppCompatCheckBox chkDisplayOdds;
    private static BetSettings app;


    public static BetSettings getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_betsettings);

        chkDisplayOdds = findViewById(R.id.chk_displayodds);

        chkDisplayOdds.setChecked(BRSharedPrefs.getFeatureEnabled(this, FEATURE_DISPLAY_ODDS, true));
        chkDisplayOdds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BRSharedPrefs.putFeatureEnabled(app, isChecked, FEATURE_DISPLAY_ODDS);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}

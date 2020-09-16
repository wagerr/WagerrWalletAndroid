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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
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
    public static String FEATURE_DISPLAY_AMERICAN = "wgr_displayamerican";

    private Switch chkDisplayOdds;
    private Switch chkDisplayAmerican;
    private static BetSettings app;
    private ImageButton mBackButton;
    private EditText mDefaultBet;

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
        app = this;

        chkDisplayOdds = findViewById(R.id.chk_displayodds);
        chkDisplayOdds.setChecked(BRSharedPrefs.getFeatureEnabled(this, FEATURE_DISPLAY_ODDS, false));
        chkDisplayOdds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BRSharedPrefs.putFeatureEnabled(app, isChecked, FEATURE_DISPLAY_ODDS);
            }
        });

        chkDisplayAmerican = findViewById(R.id.chk_displayamerican);
        chkDisplayAmerican.setChecked(BRSharedPrefs.getFeatureEnabled(this, FEATURE_DISPLAY_AMERICAN, false));
        chkDisplayAmerican.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BRSharedPrefs.putFeatureEnabled(app, isChecked, FEATURE_DISPLAY_AMERICAN);
            }
        });

        mDefaultBet = (EditText) findViewById(R.id.editDefBet);
        int min = this.getResources().getInteger(R.integer.min_bet_amount);
        int def = BRSharedPrefs.getDefaultBetAmount(app);
        if ( def==0 )   def = min;
        mDefaultBet.setText("" + def);
        mDefaultBet.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mDefaultBet.clearFocus();
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    int newVal = Integer.parseInt( mDefaultBet.getText().toString() );
                    if ( newVal < min ) {
                        newVal = min;
                        mDefaultBet.setText("" + newVal);
                    }
                    BRSharedPrefs.putDefaultBetAmount(app, newVal );
                    return true;
                }
                return false;
            }
        });

        mBackButton = findViewById(R.id.back_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
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

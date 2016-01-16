package cz.mroczis.ambientbattery.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.mroczis.ambientbattery.R;
import cz.mroczis.ambientbattery.service.BatteryService;
import cz.mroczis.ambientbattery.util.Preferences;

/**
 * Main acitivty which allows us to activate or deactivate the app
 * Created by Michal Mroček [mroczis@gmail.com] on 27.12.15.
 */
public class MainActivity extends Activity {

    @Bind(R.id.state_text)
    protected TextView mState;

    @Bind(R.id.state_switch)
    protected Switch mSwitch;

    @Bind(R.id.action_lockscreen_checkbox)
    protected CheckBox mLockscreenCheckbox;

    @Bind(R.id.action_calculate_checkbox)
    protected CheckBox mCalculateCheckbox;

    @Bind(R.id.action_strip_checkbox)
    protected CheckBox mStripCheckbox;

    @Bind(R.id.toolbar)
    protected Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setActionBar(mToolbar);
        initViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.rate:

                String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initViews() {
        if (Preferences.isEnabled()) {
            startService();
            mState.setText(getString(R.string.state_on));
            mSwitch.setChecked(true);
            mLockscreenCheckbox.setEnabled(true);
            mCalculateCheckbox.setEnabled(true);
            mStripCheckbox.setEnabled(true);
        } else {
            stopService(new Intent(this, BatteryService.class));
            mState.setText(getString(R.string.state_off));
            mSwitch.setChecked(false);
            mLockscreenCheckbox.setEnabled(false);
            mCalculateCheckbox.setEnabled(false);
            mStripCheckbox.setEnabled(false);
            Preferences.setPercentSpeed(Preferences.SPEED_INVALID);
        }

        mLockscreenCheckbox.setChecked(Preferences.isLockscreenEnabled());
        mCalculateCheckbox.setChecked(Preferences.isCalculatingEnabled());
        mStripCheckbox.setChecked(Preferences.isStripEnabled());
    }

    @OnClick(R.id.state_layout)
    protected void onStateLayoutClick() {
        mSwitch.setChecked(!mSwitch.isChecked());
        onStateSwitchClick();
    }

    @OnClick(R.id.state_switch)
    protected void onStateSwitchClick() {
        Preferences.setEnabled(mSwitch.isChecked());
        initViews();
    }

    @OnClick (R.id.action_lockscreen)
    protected void onLockScreenLayoutClick () {
        if (mLockscreenCheckbox.isEnabled()) {
            mLockscreenCheckbox.setChecked(!mLockscreenCheckbox.isChecked());
            onLockScreenCheckboxClick();
        }
    }

    @OnClick (R.id.action_lockscreen_checkbox)
    protected void onLockScreenCheckboxClick () {
        Preferences.setLockscreenEnabled(mLockscreenCheckbox.isChecked());
    }

    @OnClick(R.id.action_calculate)
    protected void onCalculateLayoutClick () {
        if (mCalculateCheckbox.isEnabled()) {
            mCalculateCheckbox.setChecked(!mCalculateCheckbox.isChecked());
        }

        onCalculateCheckboxClick();
    }

    @OnClick (R.id.action_calculate_checkbox)
    protected void onCalculateCheckboxClick () {
        Preferences.setCalculatingEnabled(mCalculateCheckbox.isChecked());
        startService();
    }

    @OnClick(R.id.action_strip)
    protected void onStripLayoutClick () {
        if (mStripCheckbox.isEnabled()) {
            mStripCheckbox.setChecked(!mStripCheckbox.isChecked());
        }

        onStripCheckboxClick();
    }

    @OnClick(R.id.action_strip_checkbox)
    protected void onStripCheckboxClick () {
        Preferences.setStripEnabled(mStripCheckbox.isChecked());
        startService();
    }

    private void startService() {
        startService(new Intent(this, BatteryService.class));
    }

}

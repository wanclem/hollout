package com.wan.hollout.ui.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.afollestad.appthemeengine.customizers.ATEActivityThemeCustomizer;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.wan.hollout.R;
import com.wan.hollout.animations.BounceInterpolator;
import com.wan.hollout.components.ApplicationLoader;
import com.wan.hollout.ui.widgets.CircleImageView;
import com.wan.hollout.utils.AppConstants;
import com.wan.hollout.utils.HolloutLogger;
import com.wan.hollout.utils.HolloutPreferences;
import com.wan.hollout.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Wan Clem
 */

public class GenderAndAgeConfigurationActivity extends BaseActivity implements ATEActivityThemeCustomizer {

    private boolean isDarkTheme;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.age_box)
    EditText ageBox;

    @BindView(R.id.pick_photo_view)
    CircleImageView userPhotoView;

    @BindView(R.id.gender_radio_group)
    RadioGroup genderRadioGroup;

    @BindView(R.id.rootLayout)
    View rootLayout;

    @BindView(R.id.button_continue)
    CardView buttonContinue;

    @BindView(R.id.accept_app_license)
    CheckBox acceptLicenseCheck;

    private Animation bounceAnimation;

    private ParseUser signedInUser;

    public String selectedGenderType = null;

    private boolean keyboardListenersAttached = false;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;

    private String TAG = "GenderAndAgeConfiguration";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        isDarkTheme = HolloutPreferences.getHolloutPreferences().getBoolean("dark_theme", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gender_and_age_layout);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        signedInUser = ParseUser.getCurrentUser();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("You are ");
        }

        initBounceAnimation();

        if (signedInUser != null) {
            offloadUserDetails();
        }

        genderRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == R.id.male) {
                    selectedGenderType = getString(R.string.male);
                } else {
                    selectedGenderType = getString(R.string.female);
                }
                signedInUser.put(AppConstants.APP_USER_GENDER, selectedGenderType);
            }

        });

        setupTermsAndConditionsView();

        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (acceptLicenseCheck.getVisibility() == View.GONE) {
                    acceptLicenseCheck.setVisibility(View.VISIBLE);
                    acceptLicenseCheck.startAnimation(bounceAnimation);
                } else {
                    if (!acceptLicenseCheck.isChecked()) {
                        Snackbar.make(acceptLicenseCheck, "You must accept the license", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    if (selectedGenderType == null) {
                        Snackbar.make(acceptLicenseCheck, "Your gender please", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    String age = ageBox.getText().toString().trim();
                    if (StringUtils.isNotEmpty(age)) {
                        if (Integer.parseInt(age) < 16) {
                            Snackbar.make(acceptLicenseCheck, "Sorry, hollout is for 16yrs and above", Snackbar.LENGTH_LONG).show();
                            return;
                        }
                    } else {
                        Snackbar.make(ageBox, "Your age please", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    signedInUser.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                navigateBackToCaller();
                            } else {
                                Snackbar.make(acceptLicenseCheck, "An error occurred while updating details. Please review your data and try again",
                                        Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void setupTermsAndConditionsView() {
        acceptLicenseCheck.setText(UiUtils.fromHtml("By continuing, you accept our <a href=https://www.google.com>Terms, Conditions and Privacy Policy</a>"));
        acceptLicenseCheck.setMovementMethod(LinkMovementMethod.getInstance());
        acceptLicenseCheck.setClickable(true);
    }

    private void navigateBackToCaller() {
        Intent mCallerIntent = new Intent();
        setResult(RESULT_OK, mCallerIntent);
        finish();
    }

    private void initBounceAnimation() {
        bounceAnimation = AnimationUtils.loadAnimation(ApplicationLoader.getInstance(), R.anim.bounce);
        BounceInterpolator bounceInterpolator = new BounceInterpolator(0.2, 20);
        bounceAnimation.setInterpolator(bounceInterpolator);
    }

    private void offloadUserDetails() {
        String userPhotoUrl = signedInUser.getString(AppConstants.APP_USER_PROFILE_PHOTO_URL);
        if (StringUtils.isNotEmpty(userPhotoUrl)) {
            UiUtils.loadImage(this, userPhotoUrl, userPhotoView);
        }
        String userAge = signedInUser.getString(AppConstants.APP_USER_AGE);
        if (StringUtils.isNotEmpty(userAge)) {
            ageBox.setText(userAge);
        }
        String userGender = signedInUser.getString(AppConstants.APP_USER_GENDER);
        if (StringUtils.isNotEmpty(userGender)) {
            if (userGender.equals(getString(R.string.male))) {
                genderRadioGroup.check(R.id.male);
            } else if (userGender.equals(getString(R.string.female))) {
                genderRadioGroup.check(R.id.female);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(false);
        MenuItem settingsActionItem = menu.findItem(R.id.action_settings);
        settingsActionItem.setVisible(false);
        supportInvalidateOptionsMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public int getActivityTheme() {
        return isDarkTheme ? R.style.AppThemeNormalDark : R.style.AppThemeNormalLight;
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForConfigChanges();
        keyboardListenersAttached = true;
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardListenersAttached) {
            rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        }
    }

    private void listenForConfigChanges() {
        keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootLayout.getRootView().getHeight();
                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;
                HolloutLogger.d(TAG, "keypadHeight = " + keypadHeight);
                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    onKeyboardShown();
                } else {
                    // keyboard is closed
                    onKeyboardHidden();
                }
            }
        };
    }

    private void onKeyboardHidden() {
        HolloutLogger.d(TAG, "Keyboard Hidden");
        UiUtils.showView(buttonContinue, true);
    }

    private void onKeyboardShown() {
        HolloutLogger.d(TAG, "Keyboard Shown");
        UiUtils.showView(buttonContinue, false);
    }

}

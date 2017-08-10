package com.wan.hollout.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;

import com.afollestad.appthemeengine.customizers.ATEActivityThemeCustomizer;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.wan.hollout.R;
import com.wan.hollout.ui.adapters.InterestsSuggestionAdapter;
import com.wan.hollout.ui.widgets.HolloutTextView;
import com.wan.hollout.utils.AppConstants;
import com.wan.hollout.utils.HolloutLogger;
import com.wan.hollout.utils.HolloutPreferences;
import com.wan.hollout.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import bolts.Capture;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Wan Clem
 */

@SuppressWarnings({"deprecation", "FieldCanBeLocal"})
public class AboutUserActivity extends BaseActivity implements ATEActivityThemeCustomizer {

    private boolean isDarkTheme;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.pick_photo_view)
    ImageView userPhotoView;

    @BindView(R.id.interests_suggestion_recycler_view)
    RecyclerView interestsSuggestionRecyclerView;

    @BindView(R.id.more_about_user_field)
    EditText moreAboutUserField;

    @BindView(R.id.reason_for_interests_view)
    HolloutTextView reasonForInterestsView;

    @BindView(R.id.button_continue)
    CardView buttonContinue;

    @BindView(R.id.rootLayout)
    View rootLayout;

    private ParseUser signedInUser;

    private String NO_QUALIFIER_TIP = "Do not use the <b>I,a,am,an</b> prefixes. Use only keywords.<br/><br/><b>Example: Fashion Designer.</b> and not <b>A fashion Designer</b>";

    private Vibrator vibrator;
    private Animation shakeAnimation;

    private boolean keyboardListenersAttached = false;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
    private String TAG = "AboutUserActivity";

    private InterestsSuggestionAdapter interestsSuggestionAdapter;
    private Capture<String> lastSelection = new Capture<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        isDarkTheme = HolloutPreferences.getHolloutPreferences().getBoolean("dark_theme", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_user_layout);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        signedInUser = ParseUser.getCurrentUser();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("About You");
        }
        loadSignedInUserPhoto();
        initVibrator();
        initShakeAnimation();
        attemptToOffloadPersistedInfoAboutUser();
        initComponents();
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

    private void attemptToOffloadPersistedInfoAboutUser() {
        if (signedInUser != null) {
            List<String> aboutUser = signedInUser.getList(AppConstants.ABOUT_USER);
            if (aboutUser != null) {
                if (!aboutUser.isEmpty()) {
                    String aboutUserString = TextUtils.join(",", aboutUser);
                    moreAboutUserField.setText(aboutUserString);
                    moreAboutUserField.setSelection(moreAboutUserField.length());
                    if (aboutUser.size() == 1) {
                        lastSelection.set(aboutUserString);
                    } else {
                        lastSelection.set(StringUtils.substringAfterLast(aboutUserString, ","));
                    }
                }
            }
        }
    }

    private void suggestOccupation(final String searchKey) {
        ParseQuery<ParseObject> interestsQuery = ParseQuery.getQuery(AppConstants.INTERESTS);
        interestsQuery.whereContains(AppConstants.NAME, searchKey.trim().toLowerCase());
        interestsQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (StringUtils.isNotEmpty(moreAboutUserField.getText().toString().trim())) {
                        if (objects != null) {
                            if (!objects.isEmpty()) {
                                initInterestsAdapter(searchKey, objects);
                                UiUtils.showView(interestsSuggestionRecyclerView, true);
                            } else {
                                UiUtils.showView(interestsSuggestionRecyclerView, false);
                            }
                        } else {
                            UiUtils.showView(interestsSuggestionRecyclerView, false);
                        }
                    } else {
                        UiUtils.showView(interestsSuggestionRecyclerView, false);
                    }
                } else {
                    UiUtils.showView(interestsSuggestionRecyclerView, false);
                }
            }
        });
    }

    private void initInterestsAdapter(String searchKey, List<ParseObject> occupations) {
        interestsSuggestionAdapter = new InterestsSuggestionAdapter(AboutUserActivity.this, occupations, searchKey,
                new InterestsSuggestionAdapter.OccupationSelectedListener() {
                    @Override
                    public void onSelectedOccupation(String occupation) {
                        lastSelection.set(occupation);
                        if (!moreAboutUserField.getText().toString().trim().contains(",")) {
                            moreAboutUserField.setText(occupation);
                            moreAboutUserField.setSelection(moreAboutUserField.getText().toString().trim().length());
                        } else {
                            int start = moreAboutUserField.getText().toString().lastIndexOf(",");
                            int end = Math.max(moreAboutUserField.getSelectionEnd(), 0);
                            moreAboutUserField.getText().replace(Math.min(start, end), Math.max(start, end),
                                    "," + occupation, 0, ("," + occupation).length());
                            moreAboutUserField.setSelection(moreAboutUserField.getText().toString().trim().length());
                        }
                        interestsSuggestionAdapter.notifyDataSetChanged();
                        UiUtils.showView(interestsSuggestionRecyclerView, false);
                        UiUtils.dismissKeyboard(moreAboutUserField);
                    }
                });
        LinearLayoutManager horizontalLinearLayoutManager = new LinearLayoutManager(AboutUserActivity.this, LinearLayoutManager.HORIZONTAL, false);
        interestsSuggestionRecyclerView.setLayoutManager(horizontalLinearLayoutManager);
        interestsSuggestionRecyclerView.setAdapter(interestsSuggestionAdapter);
    }

    private void initComponents() {
        reasonForInterestsView.setMovementMethod(ScrollingMovementMethod.getInstance());

        if (signedInUser != null) {
            interestsSuggestionRecyclerView.setNestedScrollingEnabled(true);
        }

        moreAboutUserField.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String textInOccupationField = moreAboutUserField.getText().toString().trim();
                if (StringUtils.isNotEmpty(textInOccupationField)) {
                    if (!textInOccupationField.contains(",")) {
                        if (!StringUtils.equalsIgnoreCase(textInOccupationField, lastSelection.get())) {
                            suggestOccupation(textInOccupationField);
                        }
                    } else {
                        String lastStringInOccupationField = StringUtils.substringAfterLast(textInOccupationField, ",");
                        if (!StringUtils.equalsIgnoreCase(lastStringInOccupationField, lastSelection.get())) {
                            suggestOccupation(lastStringInOccupationField);
                        }
                    }
                } else {
                    reasonForInterestsView.setText(getString(R.string.describe_yourself));
                    UiUtils.showView(reasonForInterestsView, true);
                    UiUtils.showView(reasonForInterestsView, false);
                }
                if (StringUtils.startsWithIgnoreCase(textInOccupationField, "Am a")
                        || StringUtils.startsWithIgnoreCase(textInOccupationField, "I am a")
                        || StringUtils.startsWithIgnoreCase(textInOccupationField, "I am ")
                        || StringUtils.startsWith(textInOccupationField, "A ")
                        || StringUtils.startsWithIgnoreCase(textInOccupationField, "An ")
                        || StringUtils.startsWithIgnoreCase(textInOccupationField, "The ")) {
                    vibrateDevice();
                } else {
                    reasonForInterestsView.setTextColor(ContextCompat.getColor(AboutUserActivity.this, R.color.light_grey));
                    UiUtils.showView(reasonForInterestsView, false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });

        buttonContinue.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (canMoveFurther()) {

                    List<String> existingInterests = signedInUser.getList(AppConstants.INTERESTS);
                    List<String> occupations = new ArrayList<>();
                    List<String> interests = existingInterests != null ? existingInterests : new ArrayList<String>();
                    String enteredInterests = moreAboutUserField.getText().toString().trim();

                    buildInterests(occupations, enteredInterests);

                    //Save occupations and move further
                    for (String occupation : occupations) {
                        if (!interests.contains(occupation)) {
                            interests.add(occupation);
                        }
                    }

                    signedInUser.put(AppConstants.INTERESTS, interests);
                    signedInUser.put(AppConstants.ABOUT_USER, interests);
                    UiUtils.showProgressDialog(AboutUserActivity.this, "Please wait...");
                    signedInUser.saveInBackground(new SaveCallback() {

                        @Override
                        public void done(ParseException e) {
                            UiUtils.dismissProgressDialog();
                            if (e == null) {
                                if (!HolloutPreferences.isUserWelcomed()) {
                                    Intent peopleILikeToMeetIntent = new Intent(AboutUserActivity.this, PeopleILikeToMeetActivity.class);
                                    startActivity(peopleILikeToMeetIntent);
                                    finish();
                                } else {
                                    Intent mainIntent = new Intent(AboutUserActivity.this, MainActivity.class);
                                    startActivity(mainIntent);
                                    finish();
                                }
                            } else {
                                UiUtils.showSafeToast("Error completing operation. Please try again.");
                            }
                        }
                    });
                } else {
                    Snackbar.make(buttonContinue, cantMoveFurtherErrorMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }

        });

    }

    private void buildInterests(List<String> interests, String interestTag) {
        String[] enteredOccupations = interestTag.split(",");
        for (String interest : enteredOccupations) {
            if (!interests.contains(interest.toLowerCase())) {
                interests.add(interest.toLowerCase());
            }
        }
    }


    public boolean canMoveFurther() {
        return StringUtils.isNotEmpty(moreAboutUserField.getText().toString().trim());
    }

    public String cantMoveFurtherErrorMessage() {
        return getString(R.string.about_you_please);
    }

    private void vibrateDevice() {
        vibrator.vibrate(100);
        moreAboutUserField.startAnimation(shakeAnimation);
        UiUtils.showView(reasonForInterestsView, true);
        reasonForInterestsView.setTextColor(ContextCompat.getColor(AboutUserActivity.this, R.color.dark_gray));
        reasonForInterestsView.setText(UiUtils.fromHtml(NO_QUALIFIER_TIP));
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

    private void initVibrator() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void initShakeAnimation() {
        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSignedInUserPhoto() {
        if (signedInUser != null) {
            String signedInUserPhotoUrl = signedInUser.getString(AppConstants.USER_PHOTO_URL);
            if (StringUtils.isNotEmpty(signedInUserPhotoUrl)) {
                UiUtils.loadImage(this, signedInUserPhotoUrl, userPhotoView);
            }
        }
    }

    @Override
    public int getActivityTheme() {
        return isDarkTheme ? R.style.AppThemeNormalDark : R.style.AppThemeNormalLight;
    }

}

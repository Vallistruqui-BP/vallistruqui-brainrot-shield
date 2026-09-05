package com.vallistruqui.brainrotshield;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final long STATUS_REFRESH_MS = 30_000L;
    private static final long PROTECTION_STATE_TRANSITION_MS = 180L;
    private static final long ADMIN_ACTIONS_ENTER_MS = 160L;
    private static final long ADMIN_ACTIONS_EXIT_MS = 120L;
    private static final PathInterpolator CALM_EASE_OUT =
            new PathInterpolator(0.23f, 1f, 0.32f, 1f);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            mainHandler.postDelayed(this, STATUS_REFRESH_MS);
        }
    };

    private ShieldPreferences preferences;
    private UsageStatsTracker usageStatsTracker;
    private SettingsAccessManager settingsAccessManager;
    private View pageRoot;
    private View protectionSummaryCard;
    private TextView protectionStatusChip;
    private TextView protectionStateTitle;
    private TextView protectionStateDetail;
    private TextView accessibilityStatus;
    private TextView usageAccessStatus;
    private TextView settingsAccessStatus;
    private TextView installedAppsStatus;
    private TextView dailyLimitValue;
    private TextView activeRulesSummary;
    private LinearLayout timeWindowsContainer;
    private LinearLayout adminProtectedActions;
    private Switch youtubeSwitch;
    private Switch instagramSwitch;
    private Switch tiktokSwitch;
    private Switch shortsSwitch;
    private Switch dailyLimitSwitch;
    private Switch scheduleSwitch;
    private Button addTimeWindowButton;
    private Button accessibilitySettingsButton;
    private Button usageAccessButton;
    private Button pauseShortsButton;
    private Button pauseLimitButton;
    private Button pauseScheduleButton;
    private Button configurePinButton;
    private Button lockNowButton;
    private Button removePinButton;
    private AlertDialog unlockDialog;
    private boolean settingsUnlocked;
    private boolean rendering;
    private boolean protectionTransitionRunning;
    private ProtectionPresentation.State renderedProtectionState;
    private Boolean renderedPinEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = new ShieldPreferences(this);
        usageStatsTracker = new UsageStatsTracker(this);
        settingsAccessManager = new SettingsAccessManager(this);
        pageRoot = findViewById(R.id.page_root);

        protectionSummaryCard = findViewById(R.id.protection_summary_card);
        protectionStatusChip = findViewById(R.id.protection_status_chip);
        protectionStateTitle = findViewById(R.id.protection_state_title);
        protectionStateDetail = findViewById(R.id.protection_state_detail);
        accessibilityStatus = findViewById(R.id.accessibility_status);
        usageAccessStatus = findViewById(R.id.usage_access_status);
        settingsAccessStatus = findViewById(R.id.settings_access_status);
        installedAppsStatus = findViewById(R.id.installed_apps_status);
        dailyLimitValue = findViewById(R.id.daily_limit_value);
        activeRulesSummary = findViewById(R.id.active_rules_summary);
        timeWindowsContainer = findViewById(R.id.time_windows_container);
        adminProtectedActions = findViewById(R.id.admin_protected_actions);
        youtubeSwitch = findViewById(R.id.youtube_switch);
        instagramSwitch = findViewById(R.id.instagram_switch);
        tiktokSwitch = findViewById(R.id.tiktok_switch);
        shortsSwitch = findViewById(R.id.shorts_switch);
        dailyLimitSwitch = findViewById(R.id.daily_limit_switch);
        scheduleSwitch = findViewById(R.id.schedule_switch);
        addTimeWindowButton = findViewById(R.id.add_time_window_button);
        accessibilitySettingsButton = findViewById(R.id.accessibility_settings_button);
        usageAccessButton = findViewById(R.id.usage_access_button);
        configurePinButton = findViewById(R.id.configure_pin_button);
        lockNowButton = findViewById(R.id.lock_now_button);
        removePinButton = findViewById(R.id.remove_pin_button);

        accessibilitySettingsButton.setOnClickListener(ignored -> showAccessibilityDisclosure());
        usageAccessButton.setOnClickListener(ignored -> showUsageAccessDisclosure());
        findViewById(R.id.open_test_app_button).setOnClickListener(
                ignored -> showAppLauncher());
        findViewById(R.id.youtube_controls_help_button).setOnClickListener(
                ignored -> showYouTubeControlsGuidance());
        findViewById(R.id.change_daily_limit_button).setOnClickListener(
                ignored -> showDailyLimitDialog());
        addTimeWindowButton.setOnClickListener(ignored -> addTimeWindow());
        pauseShortsButton = findViewById(R.id.pause_shorts_button);
        pauseLimitButton = findViewById(R.id.pause_limit_button);
        pauseScheduleButton = findViewById(R.id.pause_schedule_button);
        pauseShortsButton.setOnClickListener(
                ignored -> showPauseDialog(RestrictionType.SHORT_FORM));
        pauseLimitButton.setOnClickListener(
                ignored -> showPauseDialog(RestrictionType.DAILY_LIMIT));
        pauseScheduleButton.setOnClickListener(
                ignored -> showPauseDialog(RestrictionType.FOCUS_SCHEDULE));
        configurePinButton.setOnClickListener(ignored -> {
            if (settingsAccessManager.hasPin()) {
                showCurrentPinVerification(
                        R.string.verify_pin_to_change,
                        () -> showPinSetupDialog(true));
            } else {
                showPinSetupDialog(false);
            }
        });
        lockNowButton.setOnClickListener(ignored -> lockSettingsNow());
        removePinButton.setOnClickListener(ignored -> showCurrentPinVerification(
                R.string.verify_pin_to_remove,
                this::confirmRemovePin));

        bindSwitches();
        settingsUnlocked = !settingsAccessManager.hasPin();
        setSettingsContentVisible(settingsUnlocked);
        applySystemBarInsets(pageRoot);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mainHandler.removeCallbacks(refreshRunnable);
        mainHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        if (settingsAccessManager != null && settingsAccessManager.hasPin()) {
            settingsUnlocked = false;
            setSettingsContentVisible(false);
            dismissUnlockDialog();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        mainHandler.removeCallbacks(refreshRunnable);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (settingsAccessManager.hasPin() && !settingsUnlocked) {
            setSettingsContentVisible(false);
            showUnlockDialog();
        } else {
            setSettingsContentVisible(true);
        }
        refreshStatus();
        maybeShowYouTubeControlsGuidance();
    }

    private void bindSwitches() {
        youtubeSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                preferences.setAppEnabled(ProtectedApp.YOUTUBE, checked);
                refreshStatus();
            }
        });
        instagramSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                preferences.setAppEnabled(ProtectedApp.INSTAGRAM, checked);
                refreshStatus();
            }
        });
        tiktokSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                preferences.setAppEnabled(ProtectedApp.TIKTOK, checked);
                refreshStatus();
            }
        });
        shortsSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                preferences.setShortFormEnabled(checked);
                if (checked) {
                    preferences.clearPause(RestrictionType.SHORT_FORM);
                }
                refreshStatus();
            }
        });
        dailyLimitSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                preferences.setDailyLimitEnabled(checked);
                if (checked) {
                    preferences.clearPause(RestrictionType.DAILY_LIMIT);
                    if (!usageStatsTracker.hasAccess()) {
                        showUsageAccessDisclosure();
                    }
                }
                refreshStatus();
            }
        });
        scheduleSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!rendering) {
                preferences.setFocusScheduleEnabled(checked);
                if (checked) {
                    preferences.clearPause(RestrictionType.FOCUS_SCHEDULE);
                }
                refreshStatus();
            }
        });
    }

    private void refreshStatus() {
        if (preferences == null || usageStatsTracker == null) {
            return;
        }
        boolean serviceEnabled = isAccessibilityServiceEnabled();
        boolean usageAccess = usageStatsTracker.hasAccess();
        setStatus(accessibilityStatus, serviceEnabled,
                R.string.status_active, R.string.status_inactive);
        setStatus(usageAccessStatus, usageAccess,
                R.string.status_granted, R.string.status_required);
        boolean pinEnabled = settingsAccessManager.hasPin();
        setStatus(settingsAccessStatus, pinEnabled,
                R.string.status_pin_protected, R.string.status_pin_unprotected);
        configurePinButton.setText(pinEnabled
                ? R.string.change_admin_pin
                : R.string.create_admin_pin);
        renderAdminActions(pinEnabled);

        ProtectionPresentation presentation = ProtectionPresentation.evaluate(
                serviceEnabled,
                usageAccess,
                preferences.getEnabledApps().size(),
                preferences.isShortFormEnabled(),
                preferences.isDailyLimitEnabled(),
                preferences.isFocusScheduleEnabled());
        renderProtectionPresentation(presentation);
        renderSetupActionPriority(presentation.primaryAction());

        rendering = true;
        youtubeSwitch.setChecked(preferences.isAppEnabled(ProtectedApp.YOUTUBE));
        instagramSwitch.setChecked(preferences.isAppEnabled(ProtectedApp.INSTAGRAM));
        tiktokSwitch.setChecked(preferences.isAppEnabled(ProtectedApp.TIKTOK));
        shortsSwitch.setChecked(preferences.isShortFormEnabled());
        dailyLimitSwitch.setChecked(preferences.isDailyLimitEnabled());
        scheduleSwitch.setChecked(preferences.isFocusScheduleEnabled());
        rendering = false;

        setPauseButtonEnabled(pauseShortsButton, preferences.isShortFormEnabled());
        setPauseButtonEnabled(pauseLimitButton, preferences.isDailyLimitEnabled());
        setPauseButtonEnabled(pauseScheduleButton, preferences.isFocusScheduleEnabled());

        installedAppsStatus.setText(buildInstalledAppsText());
        dailyLimitValue.setText(getString(
                R.string.daily_limit_value,
                formatMinutes(preferences.getDailyLimitMinutes())));
        renderTimeWindows();
        activeRulesSummary.setText(buildActiveRulesSummary(System.currentTimeMillis(), usageAccess));
    }

    private void renderProtectionPresentation(ProtectionPresentation presentation) {
        int title;
        int detail;
        switch (presentation.state()) {
            case PROTECTED:
                title = R.string.protection_active_title;
                detail = R.string.protection_active_detail;
                break;
            case NEEDS_ACCESSIBILITY:
                title = R.string.protection_accessibility_title;
                detail = R.string.protection_accessibility_detail;
                break;
            case NEEDS_USAGE_ACCESS:
                title = R.string.protection_usage_title;
                detail = R.string.protection_usage_detail;
                break;
            case NO_APPS:
                title = R.string.protection_no_apps_title;
                detail = R.string.protection_no_apps_detail;
                break;
            case NO_RULES:
                title = R.string.protection_no_rules_title;
                detail = R.string.protection_no_rules_detail;
                break;
            default:
                throw new IllegalStateException(
                        "Unsupported protection state " + presentation.state());
        }

        String titleText = getString(title);
        String detailText = getString(detail);
        protectionStateTitle.setText(titleText);
        protectionStateDetail.setText(detailText);
        setStatus(
                protectionStatusChip,
                presentation.isProtected(),
                R.string.protection_status_active,
                R.string.protection_status_action);

        if (ProtectionPresentation.shouldAnnounce(
                renderedProtectionState, presentation.state())) {
            protectionSummaryCard.announceForAccessibility(getString(
                    R.string.protection_state_announcement,
                    titleText,
                    detailText));
            protectionSummaryCard.animate().setListener(null);
            protectionSummaryCard.animate().cancel();
            if (!protectionTransitionRunning) {
                protectionSummaryCard.setAlpha(0.68f);
            }
            protectionTransitionRunning = true;
            protectionSummaryCard.animate()
                    .alpha(1f)
                    .setDuration(PROTECTION_STATE_TRANSITION_MS)
                    .setInterpolator(CALM_EASE_OUT)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            protectionTransitionRunning = false;
                            protectionSummaryCard.setAlpha(1f);
                            protectionSummaryCard.animate().setListener(null);
                        }
                    })
                    .start();
        }
        renderedProtectionState = presentation.state();
    }

    private void renderSetupActionPriority(ProtectionPresentation.PrimaryAction primaryAction) {
        setSetupActionPrimary(
                accessibilitySettingsButton,
                primaryAction == ProtectionPresentation.PrimaryAction.ACCESSIBILITY);
        setSetupActionPrimary(
                usageAccessButton,
                primaryAction == ProtectionPresentation.PrimaryAction.USAGE_ACCESS);
    }

    private void setSetupActionPrimary(Button button, boolean primary) {
        button.setBackgroundResource(primary
                ? R.drawable.button_primary_background
                : R.drawable.button_tertiary_background);
        button.setTextColor(getColor(primary ? R.color.on_primary : R.color.ink));
    }

    private void renderAdminActions(boolean pinEnabled) {
        if (renderedPinEnabled == null) {
            adminProtectedActions.setAlpha(1f);
            adminProtectedActions.setVisibility(pinEnabled ? View.VISIBLE : View.GONE);
            renderedPinEnabled = pinEnabled;
            return;
        }
        if (renderedPinEnabled == pinEnabled) {
            return;
        }

        adminProtectedActions.animate().setListener(null);
        adminProtectedActions.animate().cancel();
        renderedPinEnabled = pinEnabled;

        if (pinEnabled) {
            boolean wasHidden = adminProtectedActions.getVisibility() != View.VISIBLE;
            if (wasHidden) {
                adminProtectedActions.setAlpha(0f);
                adminProtectedActions.setTranslationY(
                        getResources().getDimension(R.dimen.admin_actions_enter_offset));
            }
            adminProtectedActions.setVisibility(View.VISIBLE);
            adminProtectedActions.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(ADMIN_ACTIONS_ENTER_MS)
                    .setInterpolator(CALM_EASE_OUT)
                    .start();
            return;
        }

        adminProtectedActions.animate()
                .alpha(0f)
                .setDuration(ADMIN_ACTIONS_EXIT_MS)
                .setInterpolator(CALM_EASE_OUT)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (Boolean.FALSE.equals(renderedPinEnabled)) {
                            adminProtectedActions.setVisibility(View.GONE);
                            adminProtectedActions.setAlpha(1f);
                            adminProtectedActions.setTranslationY(0f);
                        }
                        adminProtectedActions.animate().setListener(null);
                    }
                })
                .start();
    }

    private void showUnlockDialog() {
        if (!settingsAccessManager.hasPin()) {
            settingsUnlocked = true;
            setSettingsContentVisible(true);
            return;
        }
        if (unlockDialog != null && unlockDialog.isShowing()) {
            return;
        }

        LinearLayout form = createPinForm(R.string.unlock_settings_body);
        EditText pin = addPinField(form, R.string.admin_pin_label);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.unlock_settings_title)
                .setView(form)
                .setNegativeButton(R.string.exit_app, (ignored, which) -> finishAndRemoveTask())
                .setPositiveButton(R.string.unlock, null)
                .setCancelable(false)
                .create();
        unlockDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (unlockDialog == dialog) {
                unlockDialog = null;
            }
        });
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                SettingsAccessManager.VerificationResult result = settingsAccessManager.verify(
                        pin.getText().toString(), System.currentTimeMillis());
                if (result.status() == SettingsAccessManager.VerificationStatus.SUCCESS) {
                    settingsUnlocked = true;
                    setSettingsContentVisible(true);
                    refreshStatus();
                    pageRoot.announceForAccessibility(getString(R.string.settings_unlocked));
                    dialog.dismiss();
                    maybeShowYouTubeControlsGuidance();
                    return;
                }
                showPinVerificationError(pin, result);
            });
            showExistingLockout(pin);
            pin.requestFocus();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private void showPinSetupDialog(boolean replacingExistingPin) {
        LinearLayout form = createPinForm(R.string.create_pin_body);
        EditText pin = addPinField(form, R.string.new_admin_pin_label);
        EditText confirmation = addPinField(form, R.string.confirm_admin_pin_label);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(replacingExistingPin
                        ? R.string.change_admin_pin
                        : R.string.create_admin_pin)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                String pinValue = pin.getText().toString();
                if (!SettingsAccessManager.isValidPin(pinValue)) {
                    pin.setError(getString(R.string.pin_requirements_error));
                    pin.requestFocus();
                    return;
                }
                if (!pinValue.contentEquals(confirmation.getText())) {
                    confirmation.setError(getString(R.string.pin_mismatch_error));
                    confirmation.requestFocus();
                    return;
                }
                if (!settingsAccessManager.setPin(pinValue)) {
                    pin.setError(getString(R.string.pin_save_error));
                    pin.requestFocus();
                    return;
                }
                settingsUnlocked = true;
                refreshStatus();
                Toast.makeText(this, R.string.pin_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            pin.requestFocus();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private void showCurrentPinVerification(int bodyText, Runnable onSuccess) {
        LinearLayout form = createPinForm(bodyText);
        EditText pin = addPinField(form, R.string.current_admin_pin_label);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.verify_admin_pin_title)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.continue_action, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                SettingsAccessManager.VerificationResult result = settingsAccessManager.verify(
                        pin.getText().toString(), System.currentTimeMillis());
                if (result.status() == SettingsAccessManager.VerificationStatus.SUCCESS) {
                    dialog.dismiss();
                    onSuccess.run();
                    return;
                }
                showPinVerificationError(pin, result);
            });
            showExistingLockout(pin);
            pin.requestFocus();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private LinearLayout createPinForm(int bodyText) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);

        TextView body = new TextView(this);
        body.setText(bodyText);
        body.setTextColor(getColor(R.color.muted));
        body.setTextSize(14f);
        body.setLineSpacing(0f, 1.2f);
        form.addView(body, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return form;
    }

    private EditText addPinField(LinearLayout form, int labelText) {
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(getColor(R.color.ink));
        label.setTextSize(14f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(16);
        form.addView(label, labelParams);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setSingleLine(true);
        input.setMinHeight(dp(52));
        input.setTextColor(getColor(R.color.ink));
        input.setHintTextColor(getColor(R.color.muted));
        input.setHint(R.string.pin_hint);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        input.setContentDescription(getString(labelText));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        form.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private void showExistingLockout(EditText pin) {
        long remaining = settingsAccessManager.getRemainingLockoutMillis(
                System.currentTimeMillis());
        if (remaining > 0L) {
            pin.setError(formatPinLockout(remaining));
        }
    }

    private void showPinVerificationError(EditText pin,
            SettingsAccessManager.VerificationResult result) {
        pin.setText("");
        switch (result.status()) {
            case INVALID:
                pin.setError(getString(R.string.pin_incorrect_error));
                break;
            case LOCKED:
                pin.setError(formatPinLockout(result.retryAfterMillis()));
                break;
            default:
                pin.setError(getString(R.string.pin_verification_error));
                break;
        }
        pin.requestFocus();
    }

    private void confirmRemovePin() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_admin_pin_title)
                .setMessage(R.string.remove_admin_pin_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    if (settingsAccessManager.clearPin()) {
                        settingsUnlocked = true;
                        setSettingsContentVisible(true);
                        refreshStatus();
                        Toast.makeText(this, R.string.pin_removed, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.pin_remove_error, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void lockSettingsNow() {
        if (!settingsAccessManager.hasPin()) {
            return;
        }
        settingsUnlocked = false;
        setSettingsContentVisible(false);
        showUnlockDialog();
    }

    private void setSettingsContentVisible(boolean visible) {
        if (pageRoot == null) {
            return;
        }
        pageRoot.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        pageRoot.setImportantForAccessibility(visible
                ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private void dismissUnlockDialog() {
        if (unlockDialog != null) {
            AlertDialog dialog = unlockDialog;
            unlockDialog = null;
            dialog.dismiss();
        }
    }

    private static long secondsRoundedUp(long millis) {
        return Math.max(1L, (millis + 999L) / 1_000L);
    }

    private String formatPinLockout(long millis) {
        int seconds = (int) secondsRoundedUp(millis);
        return getResources().getQuantityString(
                R.plurals.pin_locked_error, seconds, seconds);
    }

    private CharSequence buildInstalledAppsText() {
        List<String> installed = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (ProtectedApp app : ProtectedApp.ALL) {
            if (getPackageManager().getLaunchIntentForPackage(app.packageName()) != null) {
                installed.add(app.displayName());
            } else {
                missing.add(app.displayName());
            }
        }
        if (installed.isEmpty()) {
            return getString(R.string.no_supported_apps_found);
        }
        if (missing.isEmpty()) {
            return getString(R.string.all_supported_apps_found, joinNames(installed));
        }
        return getString(R.string.some_supported_apps_found,
                joinNames(installed), joinNames(missing));
    }

    private CharSequence buildActiveRulesSummary(long nowMillis, boolean usageAccess) {
        List<ProtectedApp> enabledApps = preferences.getEnabledApps();
        String appNames = enabledApps.isEmpty()
                ? getString(R.string.none)
                : joinAppNames(enabledApps);
        StringBuilder summary = new StringBuilder();
        summary.append(getString(R.string.summary_apps, appNames));

        summary.append('\n').append(getString(
                R.string.summary_shorts,
                describeRule(RestrictionType.SHORT_FORM,
                        preferences.isShortFormEnabled(), nowMillis)));

        if (!preferences.isDailyLimitEnabled()) {
            summary.append('\n').append(getString(
                    R.string.summary_daily_limit, getString(R.string.rule_disabled)));
        } else if (!usageAccess) {
            summary.append('\n').append(getString(
                    R.string.summary_daily_limit, getString(R.string.rule_needs_usage_access)));
        } else {
            long usedMillis = usageStatsTracker.getTodayUsageMillis(enabledApps, nowMillis);
            String state = describeRule(
                    RestrictionType.DAILY_LIMIT, true, nowMillis)
                    + " · " + getString(R.string.usage_today,
                    formatMinutes(usedMillis / 60_000L),
                    formatMinutes(preferences.getDailyLimitMinutes()));
            summary.append('\n').append(getString(R.string.summary_daily_limit, state));
        }

        if (!preferences.isFocusScheduleEnabled()) {
            summary.append('\n').append(getString(
                    R.string.summary_schedule, getString(R.string.rule_disabled)));
        } else if (preferences.isPaused(RestrictionType.FOCUS_SCHEDULE, nowMillis)) {
            summary.append('\n').append(getString(
                    R.string.summary_schedule,
                    describeRule(RestrictionType.FOCUS_SCHEDULE, true, nowMillis)));
        } else {
            TimeWindow active = preferences.getActiveTimeWindow(nowMillis);
            String state = active == null
                    ? getString(R.string.schedule_waiting)
                    : getString(R.string.schedule_blocking_now, active.format());
            summary.append('\n').append(getString(R.string.summary_schedule, state));
        }
        return summary;
    }

    private String describeRule(RestrictionType restriction, boolean enabled, long nowMillis) {
        if (!enabled) {
            return getString(R.string.rule_disabled);
        }
        if (preferences.isPaused(restriction, nowMillis)) {
            return getString(R.string.rule_paused_until,
                    formatTime(preferences.getPauseUntil(restriction)));
        }
        return getString(R.string.rule_active);
    }

    private void renderTimeWindows() {
        timeWindowsContainer.removeAllViews();
        List<TimeWindow> windows = preferences.getTimeWindows();
        if (windows.isEmpty()) {
            TextView empty = createWindowLabel();
            empty.setText(R.string.no_time_windows);
            empty.setTextColor(getColor(R.color.muted));
            timeWindowsContainer.addView(empty);
        } else {
            for (int index = 0; index < windows.size(); index++) {
                timeWindowsContainer.addView(createTimeWindowRow(windows.get(index), index));
            }
        }
        boolean canAdd = windows.size() < ShieldPreferences.MAX_TIME_WINDOWS;
        addTimeWindowButton.setEnabled(canAdd);
        addTimeWindowButton.setAlpha(canAdd ? 1f : 0.5f);
    }

    private View createTimeWindowRow(TimeWindow window, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView label = createWindowLabel();
        label.setText(getString(R.string.time_window_item, window.format()));
        label.setContentDescription(getString(R.string.edit_time_window_description, window.format()));
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setMinHeight(dp(48));
        label.setPadding(dp(12), 0, dp(12), 0);
        int selectableBackground = resolveSelectableBackground();
        if (selectableBackground != 0) {
            label.setBackgroundResource(selectableBackground);
        }
        label.setOnClickListener(ignored -> editTimeWindow(index, window));
        row.addView(label, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button remove = new Button(this, null, android.R.attr.buttonStyleSmall);
        remove.setText(R.string.remove);
        remove.setAllCaps(false);
        remove.setMinHeight(dp(48));
        remove.setContentDescription(getString(R.string.remove_time_window_description,
                window.format()));
        remove.setOnClickListener(ignored -> confirmRemoveTimeWindow(index, window));
        row.addView(remove, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private TextView createWindowLabel() {
        TextView label = new TextView(this);
        label.setTextColor(getColor(R.color.ink));
        label.setTextSize(15f);
        return label;
    }

    private void showAccessibilityDisclosure() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.accessibility_disclosure_title)
                .setMessage(R.string.accessibility_disclosure_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.accept_and_continue,
                        (dialog, which) -> startActivity(
                                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .show();
    }

    private void showUsageAccessDisclosure() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.usage_disclosure_title)
                .setMessage(R.string.usage_disclosure_body)
                .setNegativeButton(R.string.not_now, null)
                .setPositiveButton(R.string.accept_and_continue,
                        (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            try {
                                startActivity(intent);
                            } catch (RuntimeException exception) {
                                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                            }
                        })
                .show();
    }

    private void showAppLauncher() {
        List<ProtectedApp> installed = new ArrayList<>();
        for (ProtectedApp app : ProtectedApp.ALL) {
            if (getPackageManager().getLaunchIntentForPackage(app.packageName()) != null) {
                installed.add(app);
            }
        }
        if (installed.isEmpty()) {
            Toast.makeText(this, R.string.no_supported_apps_found, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] names = new CharSequence[installed.size()];
        for (int index = 0; index < installed.size(); index++) {
            names[index] = installed.get(index).displayName();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_app_to_open)
                .setItems(names, (dialog, index) -> openApp(installed.get(index)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openApp(ProtectedApp app) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.packageName());
        if (launchIntent == null) {
            Toast.makeText(this,
                    getString(R.string.app_not_installed, app.displayName()),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(launchIntent);
    }

    private void maybeShowYouTubeControlsGuidance() {
        boolean youtubeInstalled = getPackageManager().getLaunchIntentForPackage(
                ProtectedApp.YOUTUBE.packageName()) != null;
        if (!YouTubeCompatibility.shouldOfferControlsGuidance(
                isAccessibilityServiceEnabled(),
                settingsUnlocked,
                preferences.isAppEnabled(ProtectedApp.YOUTUBE),
                youtubeInstalled,
                preferences.hasShownYouTubeControlsGuidance())) {
            return;
        }

        preferences.markYouTubeControlsGuidanceShown();
        showYouTubeControlsGuidance();
    }

    private void showYouTubeControlsGuidance() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.youtube_controls_help_dialog_title)
                .setMessage(R.string.youtube_controls_help_dialog_body)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.open_youtube,
                        (dialog, which) -> openApp(ProtectedApp.YOUTUBE))
                .show();
    }

    private void showDailyLimitDialog() {
        int currentMinutes = preferences.getDailyLimitMinutes();
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(8), dp(24), dp(8));

        NumberPicker hours = new NumberPicker(this);
        hours.setMinValue(0);
        hours.setMaxValue(12);
        hours.setWrapSelectorWheel(false);
        hours.setValue(currentMinutes / 60);
        hours.setContentDescription(getString(R.string.hours));
        content.addView(hours);

        NumberPicker quarterHours = new NumberPicker(this);
        quarterHours.setMinValue(0);
        quarterHours.setMaxValue(3);
        quarterHours.setDisplayedValues(new String[]{"00 min", "15 min", "30 min", "45 min"});
        quarterHours.setWrapSelectorWheel(false);
        quarterHours.setValue((currentMinutes % 60) / 15);
        quarterHours.setContentDescription(getString(R.string.minutes));
        hours.setOnValueChangedListener((picker, oldValue, newValue) -> {
            if (newValue == 12) {
                quarterHours.setValue(0);
            }
        });
        quarterHours.setOnValueChangedListener((picker, oldValue, newValue) -> {
            if (newValue > 0 && hours.getValue() == 12) {
                hours.setValue(11);
            }
        });
        LinearLayout.LayoutParams minuteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        minuteParams.leftMargin = dp(16);
        content.addView(quarterHours, minuteParams);

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_daily_limit)
                .setMessage(R.string.daily_limit_dialog_body)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    int selected = hours.getValue() * 60 + quarterHours.getValue() * 15;
                    if (selected == 0) {
                        selected = ShieldPreferences.MIN_DAILY_LIMIT_MINUTES;
                    }
                    preferences.setDailyLimitMinutes(selected);
                    refreshStatus();
                })
                .show();
    }

    private void addTimeWindow() {
        List<TimeWindow> windows = preferences.getTimeWindows();
        if (windows.size() >= ShieldPreferences.MAX_TIME_WINDOWS) {
            Toast.makeText(this, R.string.time_window_limit_reached, Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar now = Calendar.getInstance();
        int start = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        showTimeWindowPickers(-1, new TimeWindow(start, (start + 60) % TimeWindow.MINUTES_PER_DAY));
    }

    private void editTimeWindow(int index, TimeWindow window) {
        showTimeWindowPickers(index, window);
    }

    private void showTimeWindowPickers(int index, TimeWindow initial) {
        TimePickerDialog startDialog = new TimePickerDialog(
                this,
                (view, startHour, startMinute) -> {
                    int selectedStart = startHour * 60 + startMinute;
                    TimePickerDialog endDialog = new TimePickerDialog(
                            this,
                            (endView, endHour, endMinute) -> saveTimeWindow(
                                    index,
                                    new TimeWindow(selectedStart, endHour * 60 + endMinute)),
                            initial.endMinute() / 60,
                            initial.endMinute() % 60,
                            true);
                    endDialog.setTitle(R.string.choose_end_time);
                    endDialog.show();
                },
                initial.startMinute() / 60,
                initial.startMinute() % 60,
                true);
        startDialog.setTitle(R.string.choose_start_time);
        startDialog.show();
    }

    private void saveTimeWindow(int index, TimeWindow window) {
        List<TimeWindow> windows = new ArrayList<>(preferences.getTimeWindows());
        if (index >= 0 && index < windows.size()) {
            windows.set(index, window);
        } else if (windows.size() < ShieldPreferences.MAX_TIME_WINDOWS) {
            windows.add(window);
        }
        preferences.setTimeWindows(windows);
        refreshStatus();
    }

    private void confirmRemoveTimeWindow(int index, TimeWindow window) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_time_window_title)
                .setMessage(getString(R.string.remove_time_window_body, window.format()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    List<TimeWindow> windows = new ArrayList<>(preferences.getTimeWindows());
                    if (index >= 0 && index < windows.size()) {
                        windows.remove(index);
                        preferences.setTimeWindows(windows);
                        refreshStatus();
                    }
                })
                .show();
    }

    private void showPauseDialog(RestrictionType restriction) {
        CharSequence[] choices = getResources().getTextArray(R.array.pause_durations);
        new AlertDialog.Builder(this)
                .setTitle(pauseDialogTitle(restriction))
                .setItems(choices, (dialog, index) -> {
                    long now = System.currentTimeMillis();
                    switch (index) {
                        case 0:
                            preferences.clearPause(restriction);
                            break;
                        case 1:
                            preferences.pause(restriction, now + 15L * 60L * 1_000L);
                            break;
                        case 2:
                            preferences.pause(restriction, now + 30L * 60L * 1_000L);
                            break;
                        case 3:
                            preferences.pause(restriction, now + 60L * 60L * 1_000L);
                            break;
                        case 4:
                            preferences.pause(restriction, now + 2L * 60L * 60L * 1_000L);
                            break;
                        case 5:
                            preferences.pause(restriction, startOfTomorrow());
                            break;
                        default:
                            return;
                    }
                    refreshStatus();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private int pauseDialogTitle(RestrictionType restriction) {
        switch (restriction) {
            case SHORT_FORM:
                return R.string.pause_shorts_title;
            case DAILY_LIMIT:
                return R.string.pause_limit_title;
            case FOCUS_SCHEDULE:
                return R.string.pause_schedule_title;
            default:
                throw new IllegalArgumentException("Unsupported restriction " + restriction);
        }
    }

    private long startOfTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void setStatus(TextView view, boolean positive, int positiveText, int negativeText) {
        view.setText(positive ? positiveText : negativeText);
        view.setTextColor(getColor(positive ? R.color.status_ok_text : R.color.status_warning_text));
        view.setBackgroundResource(
                positive ? R.drawable.status_ok_background : R.drawable.status_warning_background);
    }

    private static void setPauseButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expected = new ComponentName(this, ShortsBlockerAccessibilityService.class);
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName enabled = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(enabled)) {
                return true;
            }
        }
        return false;
    }

    private String joinAppNames(List<ProtectedApp> apps) {
        List<String> names = new ArrayList<>();
        for (ProtectedApp app : apps) {
            names.add(app.displayName());
        }
        return joinNames(names);
    }

    private static String joinNames(List<String> names) {
        return TextUtils.join(", ", names);
    }

    private static String formatMinutes(long totalMinutes) {
        long safeMinutes = Math.max(0L, totalMinutes);
        long hours = safeMinutes / 60L;
        long minutes = safeMinutes % 60L;
        if (hours == 0L) {
            return minutes + " min";
        }
        if (minutes == 0L) {
            return hours + " h";
        }
        return hours + " h " + minutes + " min";
    }

    private String formatTime(long timestampMillis) {
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                .format(new Date(timestampMillis));
    }

    private int resolveSelectableBackground() {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);
        return value.resourceId;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarInsets(View root) {
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            int left;
            int top;
            int right;
            int bottom;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = windowInsets.getSystemWindowInsetLeft();
                top = windowInsets.getSystemWindowInsetTop();
                right = windowInsets.getSystemWindowInsetRight();
                bottom = windowInsets.getSystemWindowInsetBottom();
            }

            view.setPadding(
                    initialLeft + left,
                    initialTop + top,
                    initialRight + right,
                    initialBottom + bottom);
            return windowInsets;
        });
        root.requestApplyInsets();
    }
}

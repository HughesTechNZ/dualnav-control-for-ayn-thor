package nz.co.thor.navcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(16, 23, 21);
    private static final int CARD = Color.rgb(28, 40, 37);
    private static final int TEXT = Color.rgb(238, 244, 242);
    private static final int MUTED = Color.rgb(169, 190, 184);
    private static final int ACCENT = Color.rgb(131, 214, 201);
    private static final int DANGER = Color.rgb(244, 177, 170);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView statusView;
    private TextView workingView;
    private CheckBox bootCheck;
    private CheckBox autoImmersiveCheck;

    private enum PostAction {
        NONE,
        APPLY_PROFILE,
        RESTORE_PROFILE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildDashboardUi());
        refreshStatus();
        if (!DeviceSupport.isSupportedThor(this)) {
            showUnsupportedDeviceWarning();
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root = column();
        root.setPadding(dp(24), dp(28), dp(24), dp(36));
        scroll.addView(root);

        TextView eyebrow = text(
                "DUALNAV · ROOT UTILITY · v1.2",
                12,
                ACCENT);
        eyebrow.setLetterSpacing(0.12f);
        root.addView(eyebrow);

        TextView title = text("Navigation Control", 30, TEXT);
        title.setPadding(0, dp(8), 0, dp(6));
        root.addView(title);

        TextView intro = text(
                "Hide persistent navigation UI while keeping hardware-button control. " +
                "The LSPosed framework hook prevents display 4 from revealing navigation.",
                15, MUTED);
        intro.setLineSpacing(0, 1.2f);
        root.addView(intro);

        workingView = text("", 13, ACCENT);
        workingView.setPadding(0, dp(14), 0, 0);
        root.addView(workingView);

        LinearLayout statusCard = card();
        statusCard.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.setMargins(0, dp(22), 0, dp(14));
        root.addView(statusCard, cardParams);

        TextView statusLabel = text("CURRENT STATUS", 12, MUTED);
        statusLabel.setLetterSpacing(0.1f);
        statusCard.addView(statusLabel);

        statusView = text("Checking root and navigation settings…", 14, TEXT);
        statusView.setPadding(0, dp(10), 0, 0);
        statusView.setTypeface(android.graphics.Typeface.MONOSPACE);
        statusCard.addView(statusView);

        Button apply = button("Apply Thor workaround", ACCENT, Color.rgb(13, 45, 40));
        apply.setOnClickListener(v -> confirmApply());
        root.addView(apply, spacedButton());

        Button restore = button("Restore standard gestures", TEXT, Color.rgb(48, 65, 60));
        restore.setOnClickListener(v -> confirmAction(
                "Restore standard gestures?",
                "This restores Android gesture navigation and removes all workaround settings.",
                NavCommands.RESTORE,
                "Standard gestures restored",
                PostAction.RESTORE_PROFILE));
        root.addView(restore, spacedButton());

        Button three = button("Restore three-button navigation", TEXT, Color.rgb(48, 65, 60));
        three.setOnClickListener(v -> confirmAction(
                "Restore three-button navigation?",
                "This removes the workaround and restores Android's Back, Home, and Recents buttons.",
                NavCommands.THREE_BUTTON,
                "Three-button navigation restored",
                PostAction.RESTORE_PROFILE));
        root.addView(three, spacedButton());

        Button restart = button("Restart System UI", DANGER, Color.rgb(69, 43, 40));
        restart.setOnClickListener(v -> confirmAction(
                "Restart System UI?",
                "The interface will flicker briefly. Running games should remain open.",
                NavCommands.RESTART_SYSTEM_UI,
                "System UI restarted",
                PostAction.NONE));
        root.addView(restart, spacedButton());

        bootCheck = new CheckBox(this);
        bootCheck.setText("Verify and reapply after boot");
        bootCheck.setTextColor(TEXT);
        bootCheck.setTextSize(15);
        bootCheck.setButtonTintList(new android.content.res.ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { ACCENT, MUTED }));
        bootCheck.setChecked(getSharedPreferences("nav_control", MODE_PRIVATE)
                .getBoolean("reapply_on_boot", false));
        bootCheck.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences("nav_control", MODE_PRIVATE)
                        .edit().putBoolean("reapply_on_boot", isChecked).apply());
        LinearLayout.LayoutParams bootParams = matchWrap();
        bootParams.setMargins(0, dp(18), 0, 0);
        root.addView(bootCheck, bootParams);

        TextView warning = text(
                "Root approval is required. If the interface becomes difficult to navigate, " +
                "open this app with a hardware button and choose a Restore option.",
                13, MUTED);
        warning.setPadding(0, dp(12), 0, 0);
        warning.setLineSpacing(0, 1.15f);
        root.addView(warning);

        return scroll;
    }

    private View buildDashboardUi() {
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(22), dp(14), dp(22), dp(16));

        TextView eyebrow = text(
                "DUALNAV · ROOT UTILITY · v1.2",
                11,
                ACCENT);
        eyebrow.setLetterSpacing(0.12f);
        root.addView(eyebrow);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(0, dp(2), 0, dp(10));
        root.addView(heading, matchWrap());

        TextView title = text("Navigation Control", 27, TEXT);
        heading.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.42f));

        TextView intro = text(
                "Hides navigation on both displays while preserving hardware controls " +
                "and full lower-screen touch input.",
                13, MUTED);
        intro.setGravity(Gravity.END);
        heading.addView(intro, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.58f));

        LinearLayout dashboard = new LinearLayout(this);
        dashboard.setOrientation(LinearLayout.HORIZONTAL);
        dashboard.setGravity(Gravity.TOP);
        root.addView(dashboard, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout left = column();
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 0.46f);
        leftParams.setMargins(0, 0, dp(8), 0);
        dashboard.addView(left, leftParams);

        LinearLayout statusCard = card();
        statusCard.setPadding(dp(18), dp(15), dp(18), dp(15));
        left.addView(statusCard, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView statusLabel = text("CURRENT STATUS", 12, MUTED);
        statusLabel.setLetterSpacing(0.1f);
        statusCard.addView(statusLabel);

        statusView = text("Checking root and navigation settings…", 13, TEXT);
        statusView.setPadding(0, dp(10), 0, 0);
        statusView.setTypeface(android.graphics.Typeface.MONOSPACE);
        statusCard.addView(statusView);

        workingView = text("", 12, ACCENT);
        workingView.setPadding(0, dp(6), 0, 0);
        statusCard.addView(workingView, matchWrap());

        LinearLayout actions = card();
        actions.setPadding(dp(16), dp(11), dp(16), dp(11));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 0.54f);
        actionParams.setMargins(dp(8), 0, 0, 0);
        dashboard.addView(actions, actionParams);

        TextView actionsLabel = text("CONTROLS", 12, MUTED);
        actionsLabel.setLetterSpacing(0.1f);
        actions.addView(actionsLabel);

        Button apply = button("Apply Thor workaround", ACCENT, Color.rgb(13, 45, 40));
        apply.setOnClickListener(v -> confirmApply());
        actions.addView(apply, compactButton());

        Button restore = button("Restore standard gestures", TEXT, Color.rgb(48, 65, 60));
        restore.setOnClickListener(v -> confirmAction(
                "Restore standard gestures?",
                "This restores Android gesture navigation and removes all workaround settings.",
                NavCommands.RESTORE,
                "Standard gestures restored",
                PostAction.RESTORE_PROFILE));
        actions.addView(restore, compactButton());

        Button three = button("Restore three-button navigation", TEXT, Color.rgb(48, 65, 60));
        three.setOnClickListener(v -> confirmAction(
                "Restore three-button navigation?",
                "This removes the workaround and restores Android's Back, Home, and Recents buttons.",
                NavCommands.THREE_BUTTON,
                "Three-button navigation restored",
                PostAction.RESTORE_PROFILE));
        actions.addView(three, compactButton());

        Button restart = button("Restart System UI", DANGER, Color.rgb(69, 43, 40));
        restart.setOnClickListener(v -> confirmAction(
                "Restart System UI?",
                "The interface will flicker briefly. Running games should remain open.",
                NavCommands.RESTART_SYSTEM_UI,
                "System UI restarted",
                PostAction.NONE));
        actions.addView(restart, compactButton());

        bootCheck = new CheckBox(this);
        bootCheck.setText("Verify and reapply after boot");
        bootCheck.setTextColor(TEXT);
        bootCheck.setTextSize(14);
        bootCheck.setButtonTintList(new android.content.res.ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { ACCENT, MUTED }));
        bootCheck.setChecked(getSharedPreferences("nav_control", MODE_PRIVATE)
                .getBoolean("reapply_on_boot", false));
        bootCheck.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences("nav_control", MODE_PRIVATE)
                        .edit().putBoolean("reapply_on_boot", isChecked).apply());
        LinearLayout.LayoutParams bootParams = matchWrap();
        bootParams.setMargins(0, dp(7), 0, 0);
        actions.addView(bootCheck, bootParams);

        autoImmersiveCheck = new CheckBox(this);
        autoImmersiveCheck.setText("Auto-force immersive apps");
        autoImmersiveCheck.setTextColor(TEXT);
        autoImmersiveCheck.setTextSize(14);
        autoImmersiveCheck.setButtonTintList(new android.content.res.ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { ACCENT, MUTED }));
        autoImmersiveCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            runAction(isChecked ? NavCommands.AUTO_IMMERSIVE_ON : NavCommands.AUTO_IMMERSIVE_OFF,
                    isChecked ? "Automatic immersive mode enabled" : "Automatic immersive mode disabled",
                    PostAction.NONE);
        });
        actions.addView(autoImmersiveCheck, matchWrap());

        TextView note = text("Root required · LSPosed scope: System Framework", 11, MUTED);
        note.setPadding(0, dp(4), 0, 0);
        actions.addView(note);

        return root;
    }

    private void confirmApply() {
        if (!DeviceSupport.isSupportedThor(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsupported device")
                    .setMessage("No changes were made. The Thor workaround is restricted to " +
                            "a detected AYN Thor with a secondary display.\n\nDetected: " +
                            DeviceSupport.description(this) + "\n\nResults on other devices " +
                            "may vary. Use is at your own risk.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        confirmAction(
                "Apply the Thor workaround?",
                "This selects gesture mode internally, hides both navigation windows, and " +
                "uses the LSPosed hook to block display 4's bottom navigation swipe. " +
                "Hardware buttons and the entire touchscreen remain available.\n\n" +
                "This is an unofficial root modification supplied without warranty. " +
                "You accept responsibility for using it on your device.",
                NavCommands.APPLY + "; " + NavCommands.RESTART_SYSTEM_UI,
                "Thor navigation workaround applied",
                PostAction.APPLY_PROFILE);
    }

    private void showUnsupportedDeviceWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Device not officially supported")
                .setMessage("DualNav Control was developed and tested for the AYN Thor. " +
                        "This device does not match the supported AYN Thor configuration.\n\n" +
                        "Detected: " + DeviceSupport.description(this) + "\n\n" +
                        "Results on other hardware or firmware may vary. Root-level changes " +
                        "can cause navigation or interface problems. This software is provided " +
                        "without warranty, and you use it at your own risk.\n\n" +
                        "The Thor workaround will remain blocked on this device. Restore " +
                        "controls are still available.")
                .setPositiveButton("I understand", null)
                .show();
    }

    private void confirmAction(
            String title,
            String message,
            String command,
            String success,
            PostAction postAction) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue",
                        (dialog, which) -> runAction(command, success, postAction))
                .show();
    }

    private void runAction(String command, String successMessage, PostAction postAction) {
        setWorking("Working… approve the Magisk prompt if shown.");
        executor.execute(() -> {
            RootShell.Result result = RootShell.run(command);
            runOnUiThread(() -> {
                setWorking("");
                if (result.ok) {
                    if (postAction == PostAction.APPLY_PROFILE) {
                        LowerEdgeBlockService.stop(this);
                        getSharedPreferences("nav_control", MODE_PRIVATE)
                                .edit().putBoolean("lower_edge_blocker_running", false).apply();
                        bootCheck.setChecked(true);
                    } else if (postAction == PostAction.RESTORE_PROFILE) {
                        LowerEdgeBlockService.stop(this);
                        bootCheck.setChecked(false);
                    }
                    Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                } else {
                    showError(result.output);
                }
                refreshStatus();
            });
        });
    }

    private void refreshStatus() {
        setWorking("Reading current configuration…");
        executor.execute(() -> {
            RootShell.Result result = RootShell.run(NavCommands.STATUS);
            runOnUiThread(() -> {
                setWorking("");
                if (!result.ok) {
                    statusView.setText("Root unavailable\n" + safe(result.output));
                    statusView.setTextColor(DANGER);
                } else {
                    statusView.setText(formatStatus(result.output));
                    statusView.setTextColor(TEXT);
                    if (autoImmersiveCheck != null) {
                        autoImmersiveCheck.setChecked(
                                result.output.contains("auto_immersive=1"));
                    }
                }
            });
        });
    }

    private String formatStatus(String raw) {
        boolean applied = raw.contains("root=0")
                && raw.contains("navigation_mode=2")
                && raw.contains("hide_nav_bar=1")
                && raw.contains("secondary_disabled=1")
                && raw.contains("policy=immersive.navigation=*")
                && raw.contains("hide_navbar_window=false");
        return (applied ? "● WORKAROUND APPLIED\n\n" : "○ STANDARD / PARTIAL CONFIGURATION\n\n")
                + "display_4_hook=managed by LSPosed"
                + "\n\n" + raw;
    }

    private void showError(String detail) {
        new AlertDialog.Builder(this)
                .setTitle("Command failed")
                .setMessage(TextUtils.isEmpty(detail)
                        ? "No output was returned. Check Magisk root approval."
                        : detail)
                .setPositiveButton("OK", null)
                .show();
    }

    private void setWorking(String value) {
        workingView.setText(value);
    }

    private static String safe(String value) {
        return TextUtils.isEmpty(value) ? "No command output" : value;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.TOP);
        return layout;
    }

    private LinearLayout card() {
        LinearLayout card = column();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.rgb(54, 74, 68));
        card.setBackground(bg);
        return card;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label, int textColor, int fillColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(54));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fillColor);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.argb(100, 131, 214, 201));
        button.setBackground(bg);
        return button;
    }

    private LinearLayout.LayoutParams spacedButton() {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(10), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams compactButton() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        params.setMargins(0, dp(6), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

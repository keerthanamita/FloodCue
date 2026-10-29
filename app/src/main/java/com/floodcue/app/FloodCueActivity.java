package com.floodcue.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Every FloodCue screen extends this, which gives the app two things:
 *
 *  1. The language the user picked — the app language for normal screens, the
 *     survival language for the safety-content screens (see
 *     {@link #usesSurvivalLanguage()}).
 *  2. A shared bottom navigation bar (Home · Contacts · SOS · Settings) that is
 *     attached to every screen after setup, so the four main destinations are
 *     always one tap away and the current one is highlighted.
 *
 * The bar is overlaid on the screen's content frame and the content gains
 * bottom padding equal to the bar's measured height, so nothing — buttons,
 * map controls, lists — is ever hidden behind it, even when the system
 * gesture bar makes the bar taller. Screens that must not show it (splash,
 * first-run language pick, login, sign-up, setup) override
 * {@link #showsBottomNavigation()}.
 *
 * Tapping a destination reuses an existing instance instead of stacking a
 * second one, and tapping the item for the screen you are already on does
 * nothing.
 */
public class FloodCueActivity extends AppCompatActivity {

    private static final String BOTTOM_NAV_TAG = "floodcue_bottom_nav";

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    private static final int NAV_HOME = 0;
    private static final int NAV_CONTACTS = 1;
    private static final int NAV_SOS = 2;
    private static final int NAV_SETTINGS = 3;

    private static final String[] NAV_GLYPHS = {"🏠", "👥", "🆘", "⚙️"};
    private static final int[] NAV_ICONS = {
            R.drawable.ic_nav_home,
            R.drawable.ic_nav_contacts,
            R.drawable.ic_nav_sos,
            R.drawable.ic_nav_settings
    };
    private static final int[] NAV_LABELS = {
            R.string.nav_home, R.string.nav_contacts, R.string.nav_sos, R.string.nav_settings
    };
    private static final Class<?>[] NAV_TARGETS = {
            HomeActivity.class, EmergencyContactsActivity.class,
            SosActivity.class, SettingsActivity.class
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(usesSurvivalLanguage()
                ? LocaleManager.withSurvivalLanguage(newBase)
                : LocaleManager.withAppLanguage(newBase));
    }

    @Override
    public void setContentView(int layoutRes) {
        super.setContentView(layoutRes);
        attachBottomNavigation();
    }

    /**
     * True for screens that show survival information (checklist, tips,
     * emergency guide). Those follow the survival language; everything else
     * follows the app language.
     */
    protected boolean usesSurvivalLanguage() {
        return false;
    }

    /** Splash, first-run language pick, login, sign-up and setup opt out. */
    protected boolean showsBottomNavigation() {
        return true;
    }

    // ---------------------------------------------------- bottom navigation

    private void attachBottomNavigation() {
        if (!showsBottomNavigation()) {
            return;
        }
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }
        if (content.findViewWithTag(BOTTOM_NAV_TAG) != null) {
            return; // already attached
        }

        final View root = content.getChildAt(0);
        final int originalBottomPadding = root.getPaddingBottom();

        LinearLayout bar = new LinearLayout(this);
        bar.setTag(BOTTOM_NAV_TAG);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundResource(R.drawable.bg_bottom_nav);
        bar.setElevation(dp(6));
        bar.setPadding(dp(8), dp(4), dp(8), dp(4));

        int selected = selectedNavItem();
        for (int i = 0; i < NAV_TARGETS.length; i++) {
            bar.addView(buildNavItem(i, i == selected),
                    new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }

        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70), Gravity.BOTTOM);
        content.addView(bar, barParams);

        // Keep the whole screen above the bar, and re-apply whenever the bar
        // grows because the system gesture/navigation bar overlaps it.
        bar.addOnLayoutChangeListener((v, left, top, right, bottom,
                                       oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = bottom - top;
            if (height > 0 && root.getPaddingBottom() != originalBottomPadding + height) {
                root.setPadding(root.getPaddingLeft(), root.getPaddingTop(),
                        root.getPaddingRight(), originalBottomPadding + height);
            }
        });

        final int barBaseBottom = dp(2);
        ViewCompat.setOnApplyWindowInsetsListener(bar, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int wanted = barBaseBottom + bottomInset;
            if (v.getPaddingBottom() != wanted) {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                        v.getPaddingRight(), wanted);
            }
            return insets;
        });
    }

    private View buildNavItem(int index, boolean selected) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);

        int color = selected
                ? (index == NAV_SOS ? getColor(R.color.error_color)
                : getColor(R.color.button_primary))
                : getColor(R.color.text_secondary);

        // Selection marker: a small rounded bar over the active destination.
        GradientDrawable marker = new GradientDrawable();
        marker.setShape(GradientDrawable.RECTANGLE);
        marker.setCornerRadius(dp(2));
        marker.setColor(color);
        View markerView = new View(this);
        markerView.setBackground(marker);
        markerView.setVisibility(selected ? View.VISIBLE : View.GONE);
        item.addView(markerView, new LinearLayout.LayoutParams(dp(26), dp(3)));

        ImageView glyph = new ImageView(this);
        glyph.setImageResource(NAV_ICONS[index]);
        glyph.setColorFilter(color);
        glyph.setContentDescription(getString(NAV_LABELS[index]));
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(
                dp(24), dp(24));
        glyphParams.topMargin = dp(3);
        item.addView(glyph, glyphParams);

        TextView label = new TextView(this);
        label.setText(NAV_LABELS[index]);
        label.setTextColor(color);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        label.setGravity(Gravity.CENTER);
        if (selected) {
            label.setTypeface(label.getTypeface(), Typeface.BOLD);
        }
        item.addView(label);

        item.setContentDescription(getString(NAV_LABELS[index]));
        item.setOnClickListener(v -> openNavItem(index));
        return item;
    }

    private void openNavItem(int index) {
        if (index == selectedNavItem()) {
            return; // already here
        }
        Intent intent = new Intent(this, NAV_TARGETS[index]);
        // Bring an existing destination forward without clearing the task stack.
        // This keeps Contacts and the other bottom-nav destinations from
        // appearing to jump back when they are selected repeatedly.
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try {
            startActivity(intent);
        } catch (RuntimeException unavailable) {
            // Defensive: a destination that cannot be opened leaves the user
            // exactly where they were.
        }
    }

    /**
     * Home and everything launched from it (weather, checklist, tips, guide,
     * alerts, offline maps, offline communication) highlight Home; profile and
     * the About screens belong to Settings.
     */
    private int selectedNavItem() {
        if (this instanceof EmergencyContactsActivity) {
            return NAV_CONTACTS;
        }
        if (this instanceof SosActivity || this instanceof SosEmergencyActivity) {
            return NAV_SOS;
        }
        if (this instanceof SettingsActivity || this instanceof ProfileDetailsActivity
                || this instanceof AboutBridgefyActivity
                || this instanceof AboutFloodCueActivity) {
            return NAV_SETTINGS;
        }
        return NAV_HOME;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

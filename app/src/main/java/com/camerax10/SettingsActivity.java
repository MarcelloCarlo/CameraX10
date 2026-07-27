package com.camerax10;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);

        PreferenceCategory generalCat = new PreferenceCategory(this);
        generalCat.setTitle("General");
        screen.addPreference(generalCat);

        ListPreference jpegQuality = new ListPreference(this);
        jpegQuality.setKey("jpeg_quality");
        jpegQuality.setTitle("JPEG Quality");
        jpegQuality.setEntries(new CharSequence[]{"Low (70)", "Medium (80)", "High (90)", "Maximum (100)"});
        jpegQuality.setEntryValues(new CharSequence[]{"70", "80", "90", "100"});
        jpegQuality.setDefaultValue("90");
        generalCat.addPreference(jpegQuality);

        ListPreference volumeKey = new ListPreference(this);
        volumeKey.setKey("volume_key_action");
        volumeKey.setTitle("Volume Key Action");
        volumeKey.setEntries(new CharSequence[]{"None (Volume)", "Capture", "Zoom"});
        volumeKey.setEntryValues(new CharSequence[]{"none", "capture", "zoom"});
        volumeKey.setDefaultValue("none");
        generalCat.addPreference(volumeKey);

        ListPreference gridLines = new ListPreference(this);
        gridLines.setKey("grid_lines");
        gridLines.setTitle("Grid Lines");
        gridLines.setEntries(new CharSequence[]{"None", "Rule of Thirds", "4x4 Grid", "Crosshair", "Golden Ratio"});
        gridLines.setEntryValues(new CharSequence[]{"none", "thirds", "grid4x4", "crosshair", "golden"});
        gridLines.setDefaultValue("none");
        generalCat.addPreference(gridLines);

        ListPreference cameraSound = new ListPreference(this);
        cameraSound.setKey("camera_sound");
        cameraSound.setTitle("Camera Sound");
        cameraSound.setEntries(new CharSequence[]{"App Sounds", "System Sounds"});
        cameraSound.setEntryValues(new CharSequence[]{"app", "system"});
        cameraSound.setDefaultValue("app");
        generalCat.addPreference(cameraSound);

        Preference helpGuide = new Preference(this);
        helpGuide.setTitle("Help Guide");
        helpGuide.setSummary("How to use CameraX10");
        helpGuide.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, HelpActivity.class));
                return true;
            }
        });
        generalCat.addPreference(helpGuide);

        PreferenceCategory cameraCat = new PreferenceCategory(this);
        cameraCat.setTitle("Camera");
        screen.addPreference(cameraCat);

        addDynamicPref(cameraCat, "white_balance", "White Balance", "white_balances");
        addDynamicPref(cameraCat, "color_effect", "Color Effect", "color_effects");

        setPreferenceScreen(screen);
        initSummaries(screen);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            pref.setSummary(lp.getEntry());
        }
    }

    private void addDynamicPref(PreferenceCategory category, String key,
                                String title, String extraKey) {
        String[] values = getIntent().getStringArrayExtra(extraKey);
        if (values == null || values.length == 0) return;

        ListPreference pref = new ListPreference(this);
        pref.setKey(key);
        pref.setTitle(title);

        CharSequence[] entries = new CharSequence[values.length];
        CharSequence[] entryValues = new CharSequence[values.length];
        for (int i = 0; i < values.length; i++) {
            entries[i] = formatLabel(values[i]);
            entryValues[i] = values[i];
        }

        pref.setEntries(entries);
        pref.setEntryValues(entryValues);
        category.addPreference(pref);
    }

    private void initSummaries(PreferenceScreen screen) {
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference pref = screen.getPreference(i);
            if (pref instanceof PreferenceCategory) {
                PreferenceCategory cat = (PreferenceCategory) pref;
                for (int j = 0; j < cat.getPreferenceCount(); j++) {
                    updateSummary(cat.getPreference(j));
                }
            } else {
                updateSummary(pref);
            }
        }
    }

    private void updateSummary(Preference pref) {
        if (pref instanceof ListPreference) {
            ListPreference lp = (ListPreference) pref;
            if (lp.getEntry() != null) {
                lp.setSummary(lp.getEntry());
            }
        }
    }

    private String formatLabel(String value) {
        if (value == null) return "";
        return value.replace("-", " ").replace("_", " ");
    }
}

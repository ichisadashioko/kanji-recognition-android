package io.github.ichisadashioko.android.kanji.views;


import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import io.github.ichisadashioko.android.kanji.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);
    }
}
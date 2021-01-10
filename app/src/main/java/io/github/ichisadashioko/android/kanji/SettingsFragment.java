package io.github.ichisadashioko.android.kanji;

import static io.github.ichisadashioko.android.kanji.SettingsActivity.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);
        FragmentActivity thisActivity = getActivity();

        EditTextPreference hintTextAlpha =
                findPreference(getString(R.string.pref_key_hint_text_type_alpha));

        if (hintTextAlpha != null) {
            hintTextAlpha.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        }
                    });

            hintTextAlpha.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (newValue instanceof String) {
                                int newHintTextAlphaValue = Integer.parseInt((String) newValue);
                                if (newHintTextAlphaValue < 0) {
                                    PreferencesUtils.SetHintTextAlphaPreference(thisActivity, 0);
                                    return false;
                                } else if (newHintTextAlphaValue > 255) {
                                    PreferencesUtils.SetHintTextAlphaPreference(thisActivity, 255);
                                    return false;
                                }

                                return true;
                            } else {
                                return false;
                            }
                        }
                    });
        }

        EditTextPreference writingStrokeWidthPref =
                findPreference(getString(R.string.pref_key_stroke_width));

        if (writingStrokeWidthPref != null) {
            writingStrokeWidthPref.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        }
                    });

            writingStrokeWidthPref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (newValue instanceof String) {
                                int newStrokeWidthValue = -1;
                                try {
                                    newStrokeWidthValue = Integer.parseInt((String) newValue);
                                } catch (Exception ex) {
                                    ex.printStackTrace(System.err);
                                    System.err.println(
                                            "Failed to parse stroke width value in preference setting!");
                                }

                                if (newStrokeWidthValue < 1) {
                                    PreferencesUtils.SetWritingStrokeWidthPreference(
                                            thisActivity, 1);
                                    return false;
                                } else {
                                    return true;
                                }
                            } else {
                                return false;
                            }
                        }
                    });
        }

        SwitchPreferenceCompat saveDataPref =
                findPreference(getString(R.string.pref_key_save_data));

        if (saveDataPref != null) {
            saveDataPref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        /**
                         * Listen to the changed value. If the value is set to `true`, check
                         * `WRITE_EXTERNAL_STORAGE` permission and request it if the permission is
                         * not granted.
                         *
                         * @param preference
                         * @param newValue
                         * @return `true` if the permission is successfully changed.
                         */
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            boolean saveDataPrefValue = (boolean) newValue;
                            if (saveDataPrefValue) {
                                // check and request `WRITE_EXTERNAL_STORAGE`
                                if (ContextCompat.checkSelfPermission(
                                                thisActivity,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    // We can only request permission while in foreground.
                                    // Users can change permission from Settings while our app is
                                    // paused.
                                    if (thisActivity
                                            .getLifecycle()
                                            .getCurrentState()
                                            .isAtLeast(Lifecycle.State.RESUMED)) {
                                        ActivityCompat.requestPermissions(
                                                thisActivity,
                                                new String[] {
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                },
                                                PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE);
                                    }

                                    return false;
                                } else {
                                    // Permission has already been granted
                                    return true;
                                }
                            } else {
                                // We only turn off the preference.
                                return true;
                            }
                        }
                    });
        }
    }
}

package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import static io.github.ichisadashioko.android.kanji.SettingsActivity.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE;

public class SettingsFragment extends PreferenceFragmentCompat
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.preference, rootKey);
        FragmentActivity thisActivity = getActivity();

        SwitchPreferenceCompat saveDataPref = findPreference(getString(R.string.pref_key_save_data));

        if (saveDataPref != null)
        {
            saveDataPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                /**
                 * Listen to the changed value. If the value is set to `true`,
                 * check `WRITE_EXTERNAL_STORAGE` permission and request it
                 * if the permission is not granted.
                 * @param preference
                 * @param newValue
                 * @return `true` if the permission is successfully changed.
                 */
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    boolean saveDataPrefValue = (boolean) newValue;
                    if (saveDataPrefValue)
                    {
                        // check and request `WRITE_EXTERNAL_STORAGE`

                        // We can only request permission while in foreground.
                        // Users can change permission from Settings while our app is paused.
                        if (thisActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED))
                        {
                            if (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                            {
                                // Permission is not granted
                                // Should we show an explanation?
                                if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                                {
                                    // TODO
                                    // Show an explanation to the user *asynchronously* -- don't block
                                    // this thread waiting for the user's response! After the user
                                    // sees the explanation, try again to request the permission.
                                    ActivityCompat.requestPermissions(thisActivity, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE);
                                }
                                else
                                {
                                    // No explanation needed; request the permission
                                    ActivityCompat.requestPermissions(thisActivity, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE);

                                    // PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE is an app-defined
                                    // int constant. The callback method gets the result of the request.
                                }

                                return false;
                            }
                            else
                            {
                                // Permission has already been granted
                                return true;
                            }
                        }
                        else
                        {
                            return true;
                        }
                    }
                    else
                    {
                        // We only turn off the preference.
                        return true;
                    }
                }
            });
        }
    }
}

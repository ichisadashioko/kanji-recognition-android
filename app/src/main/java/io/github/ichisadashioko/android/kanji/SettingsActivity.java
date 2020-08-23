package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.view.View;

public class SettingsActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        if (this.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED)
        {
            setSaveWritingDataPreference(false);
        }

        FragmentManager fragmentManager = getFragmentManager();

        if (fragmentManager.findFragmentById(R.id.settings_container) == null)
        {
            fragmentManager.beginTransaction().add(R.id.settings_container, new SettingsFragment()).commit();
        }
    }

    public void setSaveWritingDataPreference(boolean value)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor     = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.pref_key_save_data), value);
        editor.apply();
    }

    public void closeSettings(View v)
    {
        this.finish();
    }
}

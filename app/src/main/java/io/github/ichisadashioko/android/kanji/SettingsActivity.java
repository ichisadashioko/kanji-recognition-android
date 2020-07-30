package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback
{
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            setSaveWritingDataPreference(false);
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, new SettingsFragment()).commit();
    }

    public void setSaveWritingDataPreference(boolean value)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor     = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.pref_key_save_data), value);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, yay! Do the task you need to do.
                    setSaveWritingDataPreference(true);
                }
                else
                {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    setSaveWritingDataPreference(false);
                }

                reload();

                return;
            }

                // other 'case' lines to check for other  permissions this app might request.
        }
    }

    public void reload()
    {
        // https://stackoverflow.com/a/38720750/8364403
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }

    public void closeSettings(View v)
    {
        this.finish();
    }
}

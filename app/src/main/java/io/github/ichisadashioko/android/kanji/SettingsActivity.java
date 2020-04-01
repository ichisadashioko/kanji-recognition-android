package io.github.ichisadashioko.android.kanji;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import io.github.ichisadashioko.android.kanji.views.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }
}

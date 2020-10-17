package io.github.ichisadashioko.android.kanji;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class PreferencesUtils
{
    public static void SetSaveWritingDataPreference(Context context, boolean value)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor     = sharedPreferences.edit();
        editor.putBoolean(context.getString(R.string.pref_key_save_data), value);
        editor.apply();
    }

    public static void SetHintTextAlphaPreference(Context context, int value)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor     = sharedPreferences.edit();
        editor.putInt(context.getString(R.string.pref_key_hint_text_type_alpha), value);
        editor.apply();
    }
}

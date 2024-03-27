package com.fortanix.qrcode_scanner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SyncActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static String TAG = "SyncActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.settings_activity);
        setContentView(R.layout.sync_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.sync, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("endpoint") || key.equals("apikey")) {
            Log.i(TAG, "Preference value " + key + " was updated to: " + sharedPreferences.getString(key, ""));
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sync_preferences, rootKey);
        }
    }
}
package com.example.pawel.quizflags;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /* Preferenced Key*/
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    /* Application run on phone ?*/
    private boolean phoneDevice = true;

    /*  Change preference*/
    private boolean preferencesChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* Assign default value to the object SharedPreferences */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        /* Registering the object thaht listens for changes to the object SharedPreferences*/
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        /* Get screen size  */
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK;

        /* If screen size is typical for tablet... */
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
        {
            phoneDevice = false;
        }

        /* If application run on phone */
        if (phoneDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if (preferencesChanged)
        {
            MainActivityFragment fragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            fragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            fragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            fragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        /* Get information about orientation */
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else {
            return false;
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent preferencesIntent = new Intent(this, SettingActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }


    /* The object that listens for changes to the object SharedPreferences */
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            /* User change application preferences */
            preferencesChanged = true;

            /* Initialization MainActivityFragment */
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);

            /*  instruction for the type of changed settings*/
            if (key.equals("CHOICES"))
            {
                /* Update displayed rows with answer buttons */
                quizFragment.updateGuessRows(sharedPreferences);

                /* Quiz reset */
                quizFragment.resetQuiz();

            } else  if (key.equals("REGIONS"))
            {
                /* Get list of selected regions */
                Set<String> regions = sharedPreferences.getStringSet("REGIONS",null);

                /* If selected more than one regions */
                if (regions != null && regions.size() >0)
                {
                    quizFragment.updateRegions(sharedPreferences);
                    quizFragment.resetQuiz();
                }
                /* If not selected any regions */
                else {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    regions.add(getString(R.string.default_region));
                    editor.putStringSet(REGIONS, regions);
                    editor.apply();

                    Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                }

                /* Information about quiz restarting*/
                Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
            }
        };
    };
}

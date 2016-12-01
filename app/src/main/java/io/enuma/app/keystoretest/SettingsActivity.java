package io.enuma.app.keystoretest;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.widget.EditText;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    public static void showPreferences(Activity currentActivity) {
        Intent intent = new Intent(currentActivity, SettingsActivity.class);
        currentActivity.startActivityForResult(intent, 0);
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private final static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else if (preference instanceof EditTextPreference) {
                // For EditTextPreferences, apply the registered transformation (if any)
                EditText edit = ((EditTextPreference) preference).getEditText();
                String pref = edit.getTransformationMethod().getTransformation(stringValue, edit).toString();
                preference.setSummary(pref);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference, Preference.OnPreferenceChangeListener listener) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(listener);

        // Trigger the summary listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new GeneralPreferenceFragment()).commit();

        Intent intent = new Intent(this, ImapService.class);
        stopService(intent);
        Intent intent2 = new Intent(this, SmtpService.class);
        stopService(intent2);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        //loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        protected void putStringIfEmpty(String prefName, String value) {
            EditTextPreference editTextPreference = (EditTextPreference)findPreference(prefName);
            if (!value.equals("") && editTextPreference.getText().equals("")) {
                editTextPreference.setText(value);
                sBindPreferenceSummaryToValueListener.onPreferenceChange(editTextPreference, value);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("email_address"), new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            String stringValue = newValue.toString();
                            String[] x = stringValue.split("@");

                            putStringIfEmpty("smtp_username", stringValue);
                            putStringIfEmpty("imap_username", stringValue);
                            if (x.length == 2) {
                                putStringIfEmpty("display_name", x[0]);
                                if (x[1].equalsIgnoreCase("gmail.com")) {
                                    putStringIfEmpty("smtp_server", "smtp.gmail.com:587");
                                    putStringIfEmpty("imap_server", "imap.gmail.com:993");
                                }
                                else if (x[1].equalsIgnoreCase("outlook.com")) {
                                    putStringIfEmpty("smtp_server", "smtp-mail.outlook.com:587");
                                    putStringIfEmpty("imap_server", "imap-mail.outlook.com:993");
                                }
                                else if (x[1].equalsIgnoreCase("live.com")) {
                                    putStringIfEmpty("smtp_server", "smtp.live.com:587");
                                    putStringIfEmpty("imap_server", "imap.live.com:993");
                                }
                                else {
                                    putStringIfEmpty("smtp_server", x[1]);
                                    putStringIfEmpty("imap_server", x[1]);
                                }
                            }
                            return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue);
                        }
                    });
            bindPreferenceSummaryToValue(findPreference("display_name"), sBindPreferenceSummaryToValueListener);
            bindPreferenceSummaryToValue(findPreference("smtp_server"), sBindPreferenceSummaryToValueListener);
            bindPreferenceSummaryToValue(findPreference("smtp_username"), sBindPreferenceSummaryToValueListener);
            bindPreferenceSummaryToValue(findPreference("smtp_password"), new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringValue = newValue.toString();
                    putStringIfEmpty("imap_password", stringValue);
                    return sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue);
                }
            });
            bindPreferenceSummaryToValue(findPreference("imap_server"), sBindPreferenceSummaryToValueListener);
            bindPreferenceSummaryToValue(findPreference("imap_username"), sBindPreferenceSummaryToValueListener);
            bindPreferenceSummaryToValue(findPreference("imap_password"), sBindPreferenceSummaryToValueListener);
            bindPreferenceSummaryToValue(findPreference("pref_security"), sBindPreferenceSummaryToValueListener);

            ((MyEditTextPreference)findPreference("email_address")).show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}

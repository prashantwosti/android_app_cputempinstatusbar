package info.mzimmermann.xposed.cputempstatusbar.activities;

import info.mzimmermann.xposed.cputempstatusbar.R;
import info.mzimmermann.xposed.cputempstatusbar.Utils;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

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
public class SettingsActivity extends PreferenceActivity {
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	public static final String ACTION_SETTINGS_UPDATE = "cputemp-statusbar-settings-update";
	private static Context mContext = null;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mContext = getApplicationContext();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		addPreferencesFromResource(R.xml.pref_general);

		
		bindPreferenceSummaryToValue(findPreference("update_interval"));
		bindPreferenceSummaryToValue(findPreference("position"));
		
		ListPreference temperature_file = (ListPreference)findPreference("temperature_file");
		bindPreferenceSummaryToValue(findPreference("temperature_divider"));
		String[] files = Utils.getTemperatureFiles();
		temperature_file.setEntries(files);
		temperature_file.setEntryValues(files);
		bindPreferenceSummaryToValue(findPreference("temperature_file"));
		bindPreferenceSummaryToValue(findPreference("measurement"));
		findPreference("manual_color").setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
		findPreference("configured_color").setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference.getKey().equals("position")) {
				int position = Integer.parseInt(stringValue);
				if (mContext != null) {
					Intent i = new Intent(ACTION_SETTINGS_UPDATE);
					i.putExtra("position", position);
					mContext.sendBroadcast(i);
				}
			}
			if (preference.getKey().equals("update_interval")) {
				int updateInterval = Integer.parseInt(stringValue);
				if (mContext != null) {
					Intent i = new Intent(ACTION_SETTINGS_UPDATE);
					i.putExtra("update_interval", updateInterval);
					mContext.sendBroadcast(i);
				}
			}
			if (preference.getKey().equals("temperature_file")) {
				String temperatureFile = stringValue;
				if (mContext != null) {
					Intent i = new Intent(ACTION_SETTINGS_UPDATE);
					i.putExtra("temperature_file", temperatureFile);
					mContext.sendBroadcast(i);
				}
			}
			if (preference.getKey().equals("temperature_divider")) {
				int updateInterval = Integer.parseInt(stringValue);
				if (mContext != null) {
					Intent i = new Intent(ACTION_SETTINGS_UPDATE);
					i.putExtra("temperature_divider", updateInterval);
					mContext.sendBroadcast(i);
				}
			}
			if (preference.getKey().equals("measurement")) {
				String measurement = stringValue;
				if (mContext != null) {
					Intent i = new Intent(ACTION_SETTINGS_UPDATE);
					i.putExtra("measurement", measurement);
					mContext.sendBroadcast(i);
				}
			}
			
			if (preference.getKey().equals("manual_color")) {
				boolean manual_color = Boolean.parseBoolean(stringValue);
				if (mContext != null) {
					Intent i = new Intent(ACTION_SETTINGS_UPDATE);
					i.putExtra("manual_color", manual_color);
					mContext.sendBroadcast(i);
				}
				return true;
			}
			
			if (preference.getKey().equals("configured_color")) {
				int configured_color = Integer.parseInt(stringValue);
				if (mContext != null) {
					Intent i = new Intent(ACTION_SETTINGS_UPDATE);
					i.putExtra("configured_color", configured_color);
					mContext.sendBroadcast(i);
				}
				return true;
			}

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);

			} else if (preference instanceof RingtonePreference) {
				// For ringtone preferences, look up the correct display value
				// using RingtoneManager.
				if (TextUtils.isEmpty(stringValue)) {
					// Empty values correspond to 'silent' (no ringtone).
					// preference.setSummary(R.string.pref_ringtone_silent);

				} else {
					Ringtone ringtone = RingtoneManager.getRingtone(
							preference.getContext(), Uri.parse(stringValue));

					if (ringtone == null) {
						// Clear the summary if there was a lookup error.
						preference.setSummary(null);
					} else {
						// Set the summary to reflect the new ringtone display
						// name.
						String name = ringtone
								.getTitle(preference.getContext());
						preference.setSummary(name);
					}
				}

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference
				.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("example_text"));
			bindPreferenceSummaryToValue(findPreference("example_list"));
		}
	}
}

package info.mzimmermann.xposed.cputempstatusbar.widget;

import info.mzimmermann.xposed.cputempstatusbar.Utils;
import info.mzimmermann.xposed.cputempstatusbar.XposedInit;
import info.mzimmermann.xposed.cputempstatusbar.activities.SettingsActivity;

import java.io.File;
import java.io.FileInputStream;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class CpuTemp extends TextView implements OnSharedPreferenceChangeListener {
	final public static String INTENT_ACTION_UPDATE = "cputemp_update_timer";
	final private Context mContext;
	final public static String PREF_KEY = "cputemp_preferences";
	private PendingIntent pi = null;
	private File freqFile = null;
	public LinearLayout containerLayoutLeft = null;
	public LinearLayout containerLayoutRight = null;

	public CpuTemp(Context context) {
		this(context, null);
	}

	public CpuTemp(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CpuTemp(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		
		// init
		initFreqFile();
		try {
			Utils.log("freqFile="+freqFile==null?"null":freqFile.getPath());
		} catch (Exception e) {
			Utils.log(Log.getStackTraceString(e));
		}

		// style
		setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		setTextColor(context.getResources().getColor(
				android.R.color.holo_blue_light));
		setSingleLine(true);
		setPadding(6, 0, 0, 0);
		setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	}
	
	private void initFreqFile() {
		String temperature_file = mContext.getSharedPreferences(PREF_KEY, 0).getString("temperature_file", null);
		freqFile = Utils.getFreqFile(mContext, temperature_file);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		IntentFilter filter = new IntentFilter();
		filter.addAction(INTENT_ACTION_UPDATE);
		filter.addAction(SettingsActivity.ACTION_SETTINGS_UPDATE);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		mContext.registerReceiver(mBroadcastReceiver, filter);
		mContext.getSharedPreferences(PREF_KEY, 0).registerOnSharedPreferenceChangeListener(this);

		// start update interval
		int updateInterval = mContext.getSharedPreferences(PREF_KEY, 0).getInt("update_interval", 1000);
		setAlarm(updateInterval);
		
		// set text color
		TextView mClock = XposedInit.getClock();
		if(mClock!=null) {
			setTextColor(mClock.getCurrentTextColor());
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		mContext.unregisterReceiver(mBroadcastReceiver);
		mContext.getSharedPreferences(PREF_KEY, 0).unregisterOnSharedPreferenceChangeListener(this);
		cancelAlarm();
		super.onDetachedFromWindow();
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		private boolean isScreenOn = true;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				isScreenOn = true;
			}
			else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				isScreenOn = false;
			}
			else if(intent.getAction().equals(INTENT_ACTION_UPDATE) && isScreenOn) {
				updateFrequency();
			}
			else if(intent.getAction().equals(SettingsActivity.ACTION_SETTINGS_UPDATE)) {
				if(mContext!=null) {
					SharedPreferences sp = mContext.getSharedPreferences(PREF_KEY, 0);
					Editor editor = sp.edit();
					if(intent.hasExtra("update_interval")) {
						editor.putInt("update_interval", intent.getIntExtra("update_interval", 1000));
					}
					if(intent.hasExtra("position")) {
						editor.putInt("position", intent.getIntExtra("position", 0));
					}
					if(intent.hasExtra("temperature_file")) {
						editor.putString("temperature_file", intent.getStringExtra("temperature_file"));
					}
					if(intent.hasExtra("temperature_divider")) {
						editor.putInt("temperature_divider", intent.getIntExtra("temperature_divider", 1));
					}
					editor.commit();
				}
			}
		}
	};

	public void setAlarm(int interval) {
		AlarmManager am = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(INTENT_ACTION_UPDATE);
		pi = PendingIntent.getBroadcast(mContext, 0, intent, 0);
		am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), interval, pi);
	}
	
	public void cancelAlarm() {
		AlarmManager am = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		if(pi!=null)  {
			am.cancel(pi);
			pi.cancel();
		}
	}

	private void updateFrequency() {
		try {
			FileInputStream fis = new FileInputStream(freqFile);
			StringBuffer sbFreq = new StringBuffer("");

			byte[] buffer = new byte[1024];
			while (fis.read(buffer) != -1) {
				sbFreq.append(new String(buffer));
			}
			fis.close();

			String sFreq = sbFreq.toString().replaceAll("[^0-9]+", "");
			float freq = Float.valueOf(sFreq);
			int divider = mContext.getSharedPreferences(PREF_KEY, 0).getInt("temperature_divider", 1);
			if(divider!=0)
				freq = freq/divider;

			setText((int)freq + "°C");

		} catch (Exception e) {
			e.printStackTrace();
			Utils.log(Log.getStackTraceString(e));
			setText("-");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if(key.equals("position")) {
			int position = pref.getInt("position", 0);
			if(position==0) {
				containerLayoutRight.removeView(this);
				containerLayoutLeft.removeView(this);

				containerLayoutRight.addView(this, 0);
				containerLayoutLeft.setVisibility(View.GONE);
			}
			else if(position==1) {
				containerLayoutRight.removeView(this);
				containerLayoutLeft.removeView(this);

				containerLayoutRight.addView(this);
				containerLayoutLeft.setVisibility(View.GONE);
			}
			else if(position==2) {
				containerLayoutRight.removeView(this);
				containerLayoutLeft.removeView(this);

				containerLayoutLeft.addView(this);
				containerLayoutLeft.setVisibility(View.VISIBLE);
			}
		}
		
		else if(key.equals("update_interval")) {
			int updateInterval = pref.getInt("update_interval", 1000);
			cancelAlarm();
			setAlarm(updateInterval);
		}
		
		else if(key.equals("temperature_file")) {
			String temperature_file = pref.getString("temperature_file", null);
			freqFile = Utils.getFreqFile(mContext, temperature_file);
		}
		
		else if(key.equals("temperature_divider")) {
			int updateInterval = pref.getInt("temperature_divider", 1);
			cancelAlarm();
			setAlarm(updateInterval);
		}
	}
}
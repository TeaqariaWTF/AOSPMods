package sh.siava.pixelxpert.xposed.utils;

import static sh.siava.pixelxpert.utils.ExtendedSharedPreferences.IS_PREFS_INITIATED_KEY;

import android.content.Context;

import com.crossbowffs.remotepreferences.RemotePreferences;

import java.util.ArrayList;
import java.util.List;

import sh.siava.rangesliderpreference.RangeSliderPreference;

public class ExtendedRemotePreferences extends RemotePreferences {
	public List<OnSharedPreferenceChangeListener> mOnSharedPreferenceChangeListeners = new ArrayList<>();
	boolean mIsPrefsInitiated;

	//must be declared as field or it will be disposed by GC
	OnSharedPreferenceChangeListener l = (sharedPreferences, key) -> {
		if(IS_PREFS_INITIATED_KEY.equals(key))
		{
			mIsPrefsInitiated = getBoolean(IS_PREFS_INITIATED_KEY, false);
		}

		if(mIsPrefsInitiated && !mOnSharedPreferenceChangeListeners.isEmpty())
		{
			mOnSharedPreferenceChangeListeners.forEach(listener -> listener.onSharedPreferenceChanged(sharedPreferences, key));
		}
	};

	/** @noinspection unused*/
	public ExtendedRemotePreferences(Context context, String authority, String prefFileName) {
		super(context, authority, prefFileName);

		initListener();
	}

	public ExtendedRemotePreferences(Context context, String authority, String prefFileName, boolean strictMode) {
		super(context, authority, prefFileName, strictMode);

		initListener();
	}

	private void initListener() {
		mIsPrefsInitiated = super.getBoolean(IS_PREFS_INITIATED_KEY, false);


		super.registerOnSharedPreferenceChangeListener(l);
	}

	public int getSliderInt(String key, int defaultVal)
	{
		return RangeSliderPreference.getSingleIntValue(this, key, defaultVal);
	}

	public float getSliderFloat(String key, float defaultVal)
	{
		return RangeSliderPreference.getSingleFloatValue(this, key, defaultVal);
	}

	public List<Float> getSliderValues(String key, float defaultValue)
	{
		return RangeSliderPreference.getValues(this, key, defaultValue);
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		mOnSharedPreferenceChangeListeners.add(listener);
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		mOnSharedPreferenceChangeListeners.remove(listener);
	}

}

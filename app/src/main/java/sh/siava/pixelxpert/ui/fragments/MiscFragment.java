package sh.siava.pixelxpert.ui.fragments;

import android.os.Bundle;
import android.widget.Toast;

import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.ui.preferences.MaterialPrimarySwitchPreference;
import sh.siava.pixelxpert.utils.ControlledPreferenceFragmentCompat;
import sh.siava.pixelxpert.utils.NTPTimeSyncer;
import sh.siava.pixelxpert.utils.TimeSyncScheduler;

public class MiscFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.misc_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.misc_prefs;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		super.onCreatePreferences(savedInstanceState, rootKey);

		findPreference("SyncNTPTimeNow")
				.setOnPreferenceClickListener(preference -> {
					syncNTP();

					return true;
				});
	}

	@Override
	public void updateScreen(String key) {
		super.updateScreen(key);

		if (key == null) return;

		switch (key) {
			case "SyncNTPTime":
			case "TimeSyncInterval":
				TimeSyncScheduler.scheduleTimeSync(getContext());
				break;
		}
	}

	private void syncNTP() {
		boolean successful = new NTPTimeSyncer(getContext()).syncTimeNow();

		int toastResource = successful
				? R.string.sync_ntp_successful
				: R.string.sync_ntp_failed;

		Toast.makeText(getContext(), toastResource, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onResume() {
		super.onResume();
		MaterialPrimarySwitchPreference sleepOnFlat = findPreference("SleepOnFlatScreen");
		sleepOnFlat.setChecked(mPreferences.getBoolean("SleepOnFlatScreen", false));
	}

}

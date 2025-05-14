package sh.siava.pixelxpert.ui.fragments;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.ui.activities.FakeSplashActivity;
import sh.siava.pixelxpert.utils.ControlledPreferenceFragmentCompat;
import sh.siava.pixelxpert.utils.UpdateScheduler;

public class OwnPrefsFragment extends ControlledPreferenceFragmentCompat {
	@Override
	public String getTitle() {
		return getString(R.string.own_prefs_header);
	}

	@Override
	public int getLayoutResource() {
		return R.xml.own_prefs_header;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		super.onCreatePreferences(savedInstanceState, rootKey);

		findPreference("GitHubRepo")
				.setOnPreferenceClickListener(preference -> {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("https://pixelxpert.siava.sh"));
						startActivity(intent);
					} catch (Exception ignored) {
						Toast.makeText(getContext(), getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
					}
					return true;
				});

		findPreference("TelegramGroup")
				.setOnPreferenceClickListener(preference -> {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("https://t.me/PixelXpert_Discussion"));
						startActivity(intent);
					} catch (Exception ignored) {
						Toast.makeText(getContext(), getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
					}
					return true;
				});

		findPreference("CrowdinProject").setSummary(getString(R.string.crowdin_summary, getString(R.string.app_name)));
		findPreference("CrowdinProject")
				.setOnPreferenceClickListener(preference -> {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("https://pixelxpert.siava.sh/translate"));
						startActivity(intent);
					} catch (Exception ignored) {
						Toast.makeText(getContext(), getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
					}
					return true;
				});

		findPreference("UsageWiki")
				.setOnPreferenceClickListener(preference -> {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("https://pixelxpert.siava.sh/wiki"));
						startActivity(intent);
					} catch (Exception ignored) {
						Toast.makeText(getContext(), getString(R.string.browser_not_found), Toast.LENGTH_SHORT).show();
					}
					return true;
				});
	}

	@Override
	public void updateScreen(String key) {
		super.updateScreen(key);

		if (key == null) return;

		switch (key) {
			case "appLanguage":
				try {
					if (getActivity() != null) {
						getActivity().recreate();
					}
				} catch (Exception ignored) {
				}
				break;

			case "AlternativeThemedAppIcon":
				try {
					boolean AlternativeThemedAppIconEnabled = mPreferences.getBoolean("AlternativeThemedAppIcon", false);

					new MaterialAlertDialogBuilder(getContext(), R.style.MaterialComponents_MaterialAlertDialog)
							.setTitle(R.string.app_kill_alert_title)
							.setMessage(R.string.app_kill_alert_body)
							.setPositiveButton(R.string.app_kill_ok_btn, (dialog, which) -> setAlternativeAppIcon(AlternativeThemedAppIconEnabled))
							.setCancelable(false)
							.show();
				} catch (Exception ignored) {
				}
				break;

			case "AutoUpdate":
				UpdateScheduler.scheduleUpdates(getContext());
				break;
		}
	}

	private void setAlternativeAppIcon(boolean alternativeThemedAppIconEnabled) {
		PackageManager packageManager = getActivity().getPackageManager();

		packageManager.setComponentEnabledSetting(
				new ComponentName(BuildConfig.APPLICATION_ID, FakeSplashActivity.class.getName()),
				alternativeThemedAppIconEnabled ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP
		);

		// Enable themed app icon component
		packageManager.setComponentEnabledSetting(
				new ComponentName(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + ".FakeSplashActivityAlternateIcon"),
				alternativeThemedAppIconEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP
		);

		getActivity().finish();
	}
}

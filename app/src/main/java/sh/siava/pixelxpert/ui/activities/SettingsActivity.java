package sh.siava.pixelxpert.ui.activities;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;
import static sh.siava.pixelxpert.R.string.update_channel_name;
import static sh.siava.pixelxpert.ui.Constants.UPDATES_CHANNEL_ID;
import static sh.siava.pixelxpert.utils.AppUtils.isLikelyPixelBuild;
import static sh.siava.pixelxpert.utils.NavigationExtensionKt.navigateTo;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import java.util.Locale;
import java.util.Objects;

import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.SettingsActivityBinding;
import sh.siava.pixelxpert.ui.fragments.HeaderFragment;
import sh.siava.pixelxpert.ui.fragments.UpdateFragment;
import sh.siava.pixelxpert.ui.preferences.preferencesearch.SearchPreferenceResult;
import sh.siava.pixelxpert.ui.preferences.preferencesearch.SearchPreferenceResultListener;
import sh.siava.pixelxpert.utils.AppUtils;
import sh.siava.pixelxpert.utils.ExtendedSharedPreferences;
import sh.siava.pixelxpert.utils.PrefManager;
import sh.siava.pixelxpert.utils.PreferenceHelper;

public class SettingsActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SearchPreferenceResultListener {

	private static final int REQUEST_IMPORT = 7;
	private static final int REQUEST_EXPORT = 9;
	private SettingsActivityBinding binding;
	private HeaderFragment headerFragment;
	private NavController navController;

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = SettingsActivityBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		tryMigratePrefs();
		createNotificationChannel();
		setupBottomNavigationView();

		PreferenceHelper.init(ExtendedSharedPreferences.from(getDefaultSharedPreferences(createDeviceProtectedStorageContext())));

		if (getIntent() != null) {
			if (getIntent().getBooleanExtra("updateTapped", false)) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putBoolean("updateTapped", intent.getBooleanExtra("updateTapped", false));
				bundle.putString("filePath", intent.getStringExtra("filePath"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navController, R.id.updateFragment, bundle);
			} else if ("true".equals(getIntent().getStringExtra("migratePrefs"))) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putString("migratePrefs", intent.getStringExtra("migratePrefs"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navController, R.id.updateFragment, bundle);
			} else if (getIntent().getBooleanExtra("newUpdate", false)) {
				navigateTo(navController, R.id.updateFragment);
			}
		}

		if (!isLikelyPixelBuild() && !BuildConfig.DEBUG) {
			new MaterialAlertDialogBuilder(this, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(R.string.incompatible_alert_title)
					.setMessage(R.string.incompatible_alert_body)
					.setPositiveButton(R.string.incompatible_alert_ok_btn, (dialog, which) -> dialog.dismiss())
					.show();
		}
	}

	@SuppressLint({"RestrictedApi", "NonConstantResourceId"})
	private void setupBottomNavigationView() {
		NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
		navController = Objects.requireNonNull(navHostFragment).getNavController();
		NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);
	}

	private void tryMigratePrefs() {
		String migrateFileName = "PX_migrate.tmp";
		@SuppressLint("SdCardPath")
		String migrateFilePath = "/sdcard/" + migrateFileName;
		if (!Shell.cmd(String.format("stat %s", migrateFilePath)).exec().getOut().isEmpty()) {
			String PXPrefsPath = "/data/user_de/0/sh.siava.pixelxpert/shared_prefs/sh.siava.pixelxpert_preferences.xml";
			Shell.cmd(String.format("mv %s %s", migrateFilePath, PXPrefsPath)).exec();
			Shell.cmd(String.format("chmod 777 %s", PXPrefsPath)).exec(); //system will correct the permissions upon next launch. let's just give it access to do so

			new MaterialAlertDialogBuilder(this, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(R.string.app_kill_alert_title)
					.setMessage(R.string.reboot_alert_body)
					.setPositiveButton(R.string.reboot_word, (dialog, which) -> AppUtils.Restart("system"))
					.setCancelable(false)
					.show();
		}
	}

	@Override
	public void onSearchResultClicked(@NonNull final SearchPreferenceResult result, NavController navController) {
		headerFragment = new HeaderFragment();
		new Handler(getMainLooper()).post(() -> headerFragment.onSearchResultClicked(result, navController));
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		SharedPreferences prefs = getDefaultSharedPreferences(newBase.createDeviceProtectedStorageContext());

		String localeCode = prefs.getString("appLanguage", "");
		Locale locale = !localeCode.isEmpty() ? Locale.forLanguageTag(localeCode) : Locale.getDefault();

		Resources res = newBase.getResources();
		Configuration configuration = res.getConfiguration();

		configuration.setLocale(locale);

		LocaleList localeList = new LocaleList(locale);
		LocaleList.setDefault(localeList);
		configuration.setLocales(localeList);

		super.attachBaseContext(newBase.createConfigurationContext(configuration));
	}

	private void createNotificationChannel() {
		NotificationManager notificationManager = getSystemService(NotificationManager.class);

		notificationManager.createNotificationChannel(new NotificationChannel(UPDATES_CHANNEL_ID, getString(update_channel_name), IMPORTANCE_DEFAULT));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences prefs = getDefaultSharedPreferences(createDeviceProtectedStorageContext());

		int itemID = item.getItemId();

		if (itemID == android.R.id.home) {
			navController.navigateUp();
		} else if (itemID == R.id.menu_clearPrefs) {
			PrefManager.clearPrefs(prefs);
			AppUtils.Restart("systemui");
		} else if (itemID == R.id.menu_exportPrefs) {
			importExportSettings(true);
		} else if (itemID == R.id.menu_importPrefs) {
			importExportSettings(false);
		} else if (itemID == R.id.menu_restart) {
			AppUtils.Restart("system");
		} else if (itemID == R.id.menu_restartSysUI) {
			AppUtils.Restart("systemui");
		} else if (itemID == R.id.menu_soft_restart) {
			AppUtils.Restart("zygote");
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private void importExportSettings(boolean export) {
		Intent fileIntent = new Intent();
		fileIntent.setAction(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_GET_CONTENT);
		fileIntent.setType("*/*");
		fileIntent.putExtra(Intent.EXTRA_TITLE, "PixelXpert_Config" + ".bin");
		startActivityForResult(fileIntent, export ? REQUEST_EXPORT : REQUEST_IMPORT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data == null) return; //user hit cancel. Nothing to do

		SharedPreferences prefs = getDefaultSharedPreferences(createDeviceProtectedStorageContext());
		switch (requestCode) {
			case REQUEST_IMPORT:
				try {
					PrefManager.importPath(prefs, getContentResolver().openInputStream(data.getData()));
					AppUtils.Restart("systemui");
				} catch (Exception ignored) {
				}
				break;
			case REQUEST_EXPORT:
				try {
					PrefManager.exportPrefs(prefs, getContentResolver().openOutputStream(data.getData()));
				} catch (Exception ignored) {
				}
				break;
		}
	}

	@Override
	public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
		String key = pref.getKey();
		if (key == null) return false;

		switch (key) {
			case "quicksettings_header":
				return navigateTo(navController, R.id.action_headerFragment_to_quickSettingsFragment);

			case "lockscreen_header":
				return navigateTo(navController, R.id.action_headerFragment_to_lockScreenFragment);

			case "theming_header":
				return navigateTo(navController, R.id.action_headerFragment_to_themingFragment);

			case "statusbar_header":
				return navigateTo(navController, R.id.action_headerFragment_to_statusbarFragment);

			case "nav_header":
				return navigateTo(navController, R.id.action_headerFragment_to_navFragment);

			case "dialer_header":
				return navigateTo(navController, R.id.action_headerFragment_to_dialerFragment);

			case "hotspot_header":
				return navigateTo(navController, R.id.action_headerFragment_to_hotSpotFragment);

			case "pm_header":
				return navigateTo(navController, R.id.action_headerFragment_to_packageManagerFragment);

			case "misc_header":
				return navigateTo(navController, R.id.action_headerFragment_to_miscFragment);

			case "CheckForUpdate":
				navController.popBackStack(R.id.headerFragment, false);
				return navigateTo(navController, R.id.action_headerFragment_to_updateFragment);

			case "qs_tile_qty":
				return navigateTo(navController, R.id.action_quickSettingsFragment_to_QSTileQtyFragment);

			case "network_settings_header_qs":
				return navigateTo(navController, R.id.action_quickSettingsFragment_to_networkFragment);

			case "sbc_header":
				return navigateTo(navController, R.id.action_statusbarFragment_to_SBCFragment);

			case "sbbb_header":
				return navigateTo(navController, R.id.action_statusbarFragment_to_SBBBFragment);

			case "sbbIcon_header":
				return navigateTo(navController, R.id.action_statusbarFragment_to_SBBIconFragment);

			case "network_settings_header":
				return navigateTo(navController, R.id.action_statusbarFragment_to_networkFragment);

			case "threebutton_header":
				return navigateTo(navController, R.id.action_navFragment_to_threeButtonNavFragment);

			case "gesturenav_header":
				return navigateTo(navController, R.id.action_navFragment_to_gestureNavFragment);

			case "remap_physical_buttons":
				return navigateTo(navController, R.id.action_miscFragment_to_physicalButtonRemapFragment);

			case "netstat_header":
				return navigateTo(navController, R.id.action_miscFragment_to_networkStatFragment);
		}

		return false;
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
}
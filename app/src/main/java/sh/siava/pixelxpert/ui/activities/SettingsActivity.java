package sh.siava.pixelxpert.ui.activities;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static sh.siava.pixelxpert.R.string.update_channel_name;
import static sh.siava.pixelxpert.ui.Constants.UPDATES_CHANNEL_ID;
import static sh.siava.pixelxpert.ui.utils.ViewUtils.fadeIn;
import static sh.siava.pixelxpert.ui.utils.ViewUtils.fadeOut;
import static sh.siava.pixelxpert.utils.AppUtils.isLikelyPixelBuild;
import static sh.siava.pixelxpert.utils.MiscUtils.REQUEST_EXPORT;
import static sh.siava.pixelxpert.utils.MiscUtils.REQUEST_IMPORT;
import static sh.siava.pixelxpert.utils.MiscUtils.weakVibrate;
import static sh.siava.pixelxpert.utils.NavigationExtensionKt.navigateTo;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.PixelXpert;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.SettingsActivityBinding;
import sh.siava.pixelxpert.service.tileServices.SleepOnSurfaceTileService;
import sh.siava.pixelxpert.ui.fragments.HeaderFragment;
import sh.siava.pixelxpert.ui.fragments.UpdateFragment;
import sh.siava.pixelxpert.ui.misc.StateManager;
import sh.siava.pixelxpert.ui.preferences.preferencesearch.SearchPreferenceResult;
import sh.siava.pixelxpert.ui.preferences.preferencesearch.SearchPreferenceResultListener;
import sh.siava.pixelxpert.utils.AppUtils;
import sh.siava.pixelxpert.utils.DisplayUtils;
import sh.siava.pixelxpert.utils.PXPreferences;
import sh.siava.pixelxpert.utils.PrefManager;
import sh.siava.pixelxpert.utils.PreferenceHelper;
import sh.siava.pixelxpert.xposed.modpacks.android.TargetOptimizer;

@AndroidEntryPoint
public class SettingsActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SearchPreferenceResultListener {

	private SettingsActivityBinding binding;
	private HeaderFragment headerFragment;
	private NavController navControllerMain;
	private NavController navControllerDetails;
	private final boolean isTabletDevice = DisplayUtils.isTablet();

	@Inject
	StateManager stateManager;

	private final BroadcastReceiver updateCheckReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int latestVersionCode = intent.getIntExtra("latestVersionCode", BuildConfig.VERSION_CODE);
			handleUpdateBadge(latestVersionCode);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = SettingsActivityBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		createNotificationChannel();
		setupNavigation(savedInstanceState);

		PreferenceHelper.init();

		if (getIntent() != null) {
			if (getIntent().getBooleanExtra("updateTapped", false)) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putBoolean("updateTapped", intent.getBooleanExtra("updateTapped", false));
				bundle.putString("filePath", intent.getStringExtra("filePath"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navControllerMain, R.id.updateFragment, bundle);
			} else if ("true".equals(getIntent().getStringExtra("migratePrefs"))) {
				Intent intent = getIntent();
				Bundle bundle = new Bundle();
				bundle.putString("migratePrefs", intent.getStringExtra("migratePrefs"));
				UpdateFragment updateFragment = new UpdateFragment();
				updateFragment.setArguments(bundle);
				navigateTo(navControllerMain, R.id.updateFragment, bundle);
			} else if (getIntent().getBooleanExtra("newUpdate", false)) {
				navigateTo(navControllerMain, R.id.updateFragment);
			} else if (getIntent().hasExtra(Intent.EXTRA_COMPONENT_NAME)) {
				ComponentName callerComponentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
				if(callerComponentName != null) {
					String callerClassName = callerComponentName.getClassName();
					if (SleepOnSurfaceTileService.class.getName().equals(callerClassName)) {
						NavController navController = isTabletDevice ? navControllerDetails : navControllerMain;
						navigateTo(navController, R.id.sleepOnFlatFragment);
					}
				}
			}
		}

		if (PXPreferences.getBoolean(TargetOptimizer.SYSTEM_RESTART_PENDING_KEY, false)) {
			new MaterialAlertDialogBuilder(this, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(R.string.optimization_restart_needed_title)
					.setMessage(R.string.optimization_restart_needed_message)
					.setPositiveButton(R.string.restart_now, (dialog, which) -> AppUtils.restart("system"))
					.setNegativeButton(R.string.restart_postpone, (dialog, which) -> dialog.dismiss())
					.show();
		}

		//noinspection ConstantValue
		if (!isLikelyPixelBuild() && !BuildConfig.VERSION_NAME.contains("canary")) {
			new MaterialAlertDialogBuilder(this, R.style.MaterialComponents_MaterialAlertDialog)
					.setTitle(R.string.incompatible_alert_title)
					.setMessage(R.string.incompatible_alert_body)
					.setPositiveButton(R.string.incompatible_alert_ok_btn, (dialog, which) -> dialog.dismiss())
					.show();
		}

		observeRestartFlag();
		setupFloatingActionButtons();
	}

	private void observeRestartFlag() {
		stateManager.getRequiresSystemUIRestart().observe(this, isRequired -> showOrHidePendingActionButton(binding, isRequired));
	}

	private void setupFloatingActionButtons() {
		// Initially hide all FABs
		binding.hideAll.hide();
		binding.restartSystemui.hide();
		binding.pendingActions.shrink();

		// Show or hide the main pending actions FAB based on Dynamic flags
		showOrHidePendingActionButton(
				binding,
				Boolean.TRUE.equals(stateManager.getRequiresSystemUIRestart().getValue())
		);

		// Pending Action FAB clicked
		binding.pendingActions.setOnClickListener(v -> {
			weakVibrate(binding.pendingActions);
			showOrHideFabButtons();
		});

		// Hide All FAB clicked
		binding.hideAll.setOnClickListener(v -> {
			weakVibrate(binding.hideAll);
			stateManager.setRequiresSystemUIRestart(false);
		});

		// Restart System UI FAB clicked
		binding.restartSystemui.setOnClickListener(v -> {
			weakVibrate(binding.restartSystemui);
			stateManager.setRequiresSystemUIRestart(false);

			new Handler(Looper.getMainLooper()).postDelayed(() -> AppUtils.restart("systemui"), 500);
		});
	}

	private void showOrHideFabButtons() {
		try {
			boolean requiresSystemUIRestart = Boolean.TRUE.equals(stateManager.getRequiresSystemUIRestart().getValue());
			boolean pendingActionsShown = binding.pendingActions.isShown();
			boolean isAnyButtonShown;

			// Hide All FAB logic
			if (!binding.hideAll.isShown() && pendingActionsShown) {
				binding.hideAll.show();
				fadeIn(binding.hideAllText);
				isAnyButtonShown = true;
			} else {
				binding.hideAll.hide();
				fadeOut(binding.hideAllText);
				isAnyButtonShown = false;
			}

			// Restart System UI FAB logic
			if (!binding.restartSystemui.isShown() && requiresSystemUIRestart && pendingActionsShown) {
				binding.restartSystemui.show();
				fadeIn(binding.restartSystemuiText);
				isAnyButtonShown = true;
			} else {
				binding.restartSystemui.hide();
				fadeOut(binding.restartSystemuiText);
			}

			// Extend or shrink the main FAB based on visibility
			if (isAnyButtonShown) {
				binding.pendingActions.extend();
			} else {
				binding.pendingActions.shrink();
			}
		} catch (Exception ignored) {
		}
	}

	public static void showOrHidePendingActionButton(SettingsActivityBinding binding, boolean requiresSystemUiRestart) {
		try {
			if (!requiresSystemUiRestart) {
				binding.hideAll.hide();
				fadeOut(binding.hideAllText);
				binding.restartSystemui.hide();
				fadeOut(binding.restartSystemuiText);
				binding.pendingActions.hide();
				binding.pendingActions.shrink();
			} else {
				// Restart System UI button visibility logic
				if (binding.hideAll.isShown() && !binding.restartSystemui.isShown()) {
					binding.restartSystemui.show();
					fadeIn(binding.restartSystemuiText);
				}

				// Shrink or extend main FAB
				if (!binding.hideAll.isShown()) {
					binding.pendingActions.shrink();
				} else {
					binding.pendingActions.extend();
				}

				binding.pendingActions.show();
			}
		} catch (Exception ignored) {
		}
	}

	@SuppressLint({"RestrictedApi", "NonConstantResourceId"})
	private void setupNavigation(Bundle savedInstanceState) {
		NavHostFragment navHostFragmentMain = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainerView);
		navControllerMain = Objects.requireNonNull(navHostFragmentMain).getNavController();

		NavHostFragment navHostFragmentDetails = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.detailFragmentContainerView);
		navControllerDetails = Objects.requireNonNull(navHostFragmentDetails).getNavController();

		NavGraph navGraphMain = navControllerMain.getNavInflater().inflate(isTabletDevice ? R.navigation.nav_graph_tablet_main : R.navigation.nav_graph_phone);
		navControllerMain.setGraph(navGraphMain, savedInstanceState);

		binding.detailFragmentContainerView.setVisibility(isTabletDevice ? View.VISIBLE : View.GONE);

		if (isTabletDevice) {
			binding.bottomNavigationView.setVisibility(View.GONE);
			binding.navigationRailView.setVisibility(View.VISIBLE);
			binding.navigationRailView.setOnItemSelectedListener(this::setupOnItemSelectedListener);
			binding.navigationRailView.setOnItemReselectedListener(this::setupOnItemReselectedListener);
			NavigationUI.setupWithNavController(binding.navigationRailView, navControllerMain);
		} else {
			binding.navigationRailView.setVisibility(View.GONE);
			binding.bottomNavigationView.setVisibility(View.VISIBLE);
			binding.bottomNavigationView.setOnItemSelectedListener(this::setupOnItemSelectedListener);
			binding.bottomNavigationView.setOnItemReselectedListener(this::setupOnItemReselectedListener);
			NavigationUI.setupWithNavController(binding.bottomNavigationView, navControllerMain);
		}

		ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
			boolean isRtl = view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

			if (insets.left > 0 || insets.right > 0) {
				int startInset = isRtl ? insets.right : insets.left;

				((ViewGroup) binding.navigationRailView.getParent()).setPaddingRelative(
						startInset + binding.navigationRailView.getPaddingStart(),
						0, 0, 0
				);
			}

			return windowInsets;
		});
	}

	private boolean setupOnItemSelectedListener(MenuItem item) {
		if (item.getItemId() == R.id.headerFragment) {
			return navControllerMain.popBackStack(R.id.headerFragment, false);
		} else if (item.getItemId() == R.id.updateFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
			return navigateTo(navControllerMain, R.id.updateFragment);
		} else if (item.getItemId() == R.id.hooksFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
			return navigateTo(navControllerMain, R.id.hooksFragment);
		} else if (item.getItemId() == R.id.ownPrefsFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
			return navigateTo(navControllerMain, R.id.ownPrefsFragment);
		}
		return false;
	}

	private void setupOnItemReselectedListener(MenuItem item) {
		if (item.getItemId() == R.id.headerFragment) {
			navControllerMain.popBackStack(R.id.headerFragment, false);
		} else if (item.getItemId() == R.id.updateFragment) {
			navControllerMain.popBackStack(R.id.updateFragment, false);
		} else if (item.getItemId() == R.id.hooksFragment) {
			navControllerMain.popBackStack(R.id.hooksFragment, false);
		} else if (item.getItemId() == R.id.ownPrefsFragment) {
			navControllerMain.popBackStack(R.id.ownPrefsFragment, false);
		}
	}

	@Override
	public void onSearchResultClicked(@NonNull final SearchPreferenceResult result, NavController navController) {
		headerFragment = new HeaderFragment();
		NavController myNavController = isTabletDevice ? navControllerDetails : navController;
		new Handler(getMainLooper()).post(() -> headerFragment.onSearchResultClicked(result, myNavController, this));
	}

	private void createNotificationChannel() {
		NotificationManager notificationManager = getSystemService(NotificationManager.class);

		notificationManager.createNotificationChannel(new NotificationChannel(UPDATES_CHANNEL_ID, getString(update_channel_name), IMPORTANCE_DEFAULT));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data == null) return; //user hit cancel. Nothing to do

		SharedPreferences prefs = PixelXpert.get().getDefaultPreferences();
		switch (requestCode) {
			case REQUEST_IMPORT:
				try {
					PixelXpert.get().setPrefsValidity(false);
					PrefManager.importPath(prefs, getContentResolver().openInputStream(data.getData()));
					PixelXpert.get().initiatePreferences(false);

					AppUtils.restart("systemui");
					recreate();
				} catch (Exception ignored) {
				}
				break;
			case REQUEST_EXPORT:
				try {
					//noinspection DataFlowIssue
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

		NavController navController = isTabletDevice ? navControllerDetails : navControllerMain;

		return switch (key) {
			case "quicksettings_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_quickSettingsFragment);
			}
			case "lockscreen_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_lockScreenFragment);
			}
			case "theming_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_themingFragment);
			}
			case "statusbar_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_statusbarFragment);
			}
			case "nav_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_navFragment);
			}
			case "dialer_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_dialerFragment);
			}
			case "hotspot_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_hotSpotFragment);
			}
			case "pm_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_packageManagerFragment);
			}
			case "misc_header" -> {
				if (isTabletDevice) navController.popBackStack(R.id.headerFragment, false);
				yield navigateTo(navController, R.id.action_headerFragment_to_miscFragment);
			}
			case "CheckForUpdate" -> {
				if (isTabletDevice) {
					binding.navigationRailView.setSelectedItemId(R.id.updateFragment);
				} else {
					binding.bottomNavigationView.setSelectedItemId(R.id.updateFragment);
				}
				yield true;
			}
			case "qs_tile_qty" ->
					navigateTo(navController, R.id.action_quickSettingsFragment_to_QSTileQtyFragment);
			case "network_settings_header_qs" ->
					navigateTo(navController, R.id.action_quickSettingsFragment_to_networkFragment);
			case "sbc_header" ->
					navigateTo(navController, R.id.action_statusbarFragment_to_SBCFragment);
			case "BBarEnabled" ->
					navigateTo(navController, R.id.action_statusbarFragment_to_SBBBFragment);
			case "network_settings_header" ->
					navigateTo(navController, R.id.action_statusbarFragment_to_networkFragment);
			case "threebutton_header" ->
					navigateTo(navController, R.id.action_navFragment_to_threeButtonNavFragment);
			case "taskbar_header" ->
					navigateTo(navController, R.id.action_navFragment_to_taskbarNavFragment);
			case "gesturenav_header" ->
					navigateTo(navController, R.id.action_navFragment_to_gestureNavFragment);
			case "remap_physical_buttons" ->
					navigateTo(navController, R.id.action_miscFragment_to_physicalButtonRemapFragment);
			case "netstat_header" ->
					navigateTo(navController, R.id.action_miscFragment_to_networkStatFragment);
			case "SleepOnFlatScreen" ->
					navigateTo(navController, R.id.action_miscFragment_to_sleepOnFlatFragment);
			case "icon_packs" ->
					navigateTo(navController, R.id.action_themingFragment_to_iconPackFragment);
			default -> false;
		};
	}

	private void handleUpdateBadge(int latestVersionCode) {
		if (latestVersionCode > BuildConfig.VERSION_CODE) {
			BadgeDrawable badge;
			if (isTabletDevice) {
				badge = binding.navigationRailView.getOrCreateBadge(R.id.updateFragment);
			} else {
				badge = binding.bottomNavigationView.getOrCreateBadge(R.id.updateFragment);
			}
			badge.setVisible(true);
		} else {
			if (isTabletDevice) {
				binding.navigationRailView.removeBadge(R.id.updateFragment);
			} else {
				binding.bottomNavigationView.removeBadge(R.id.updateFragment);
			}
		}
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				updateCheckReceiver,
				new IntentFilter(BuildConfig.APPLICATION_ID + ".UPDATE_CHECK")
		);
		handleUpdateBadge(PXPreferences.getInt("latestVersionCode", -1));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(updateCheckReceiver);
	}
}

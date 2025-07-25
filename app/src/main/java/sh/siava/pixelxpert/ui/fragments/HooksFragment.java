package sh.siava.pixelxpert.ui.fragments;

import static android.content.Context.RECEIVER_EXPORTED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sh.siava.pixelxpert.xposed.Constants.KSU_NEXT_PACKAGE;
import static sh.siava.pixelxpert.xposed.Constants.KSU_PACKAGE;
import static sh.siava.pixelxpert.xposed.Constants.SYSTEM_FRAMEWORK_PACKAGE;
import static sh.siava.pixelxpert.xposed.Constants.SYSTEM_UI_PACKAGE;
import static sh.siava.pixelxpert.xposed.utils.BootLoopProtector.PACKAGE_STRIKE_KEY_KEY;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import sh.siava.pixelxpert.IRootProviderService;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.databinding.FragmentHooksBinding;
import sh.siava.pixelxpert.xposed.Constants;
import sh.siava.pixelxpert.xposed.XPrefs;
import sh.siava.pixelxpert.service.RootProvider;
import sh.siava.pixelxpert.utils.AppUtils;

public class HooksFragment extends BaseFragment {

	private FragmentHooksBinding binding;
	/**
	 * @noinspection unused
	 */
	private final String TAG = getClass().getSimpleName();
	IntentFilter intentFilterHookedPackages = new IntentFilter();
	private final List<String> hookedPackageList = new ArrayList<>();
	private List<String> monitorPackageList;
	private int dotCount = 0;
	/**
	 * @noinspection FieldCanBeLocal
	 */
	private ServiceConnection mCoreRootServiceConnection;
	private IRootProviderService mRootServiceIPC = null;
	private boolean rebootPending = false;
	private final String reboot_key = "reboot_pending";

	@Override
	public String getTitle() {
		return getString(R.string.hooked_packages_title);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		XPrefs.init(requireContext());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentHooksBinding.inflate(inflater, container, false);

		if (savedInstanceState != null) {
			rebootPending = savedInstanceState.getBoolean(reboot_key);
		}

		binding.rebootButton.setOnClickListener(view -> AppUtils.restart("system"));

		if (!rebootPending) {
			binding.rebootButton.hide();
		}

		startRootService();

		return binding.getRoot();
	}

	private void startRootService() {
		// Start RootService connection
		Intent intent = new Intent(requireContext(), RootProvider.class);
		mCoreRootServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				binding.loadingIndicator.setVisibility(GONE);
				mRootServiceIPC = IRootProviderService.Stub.asInterface(service);
				onRootServiceStarted();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mRootServiceIPC = null;
			}
		};
		RootService.bind(intent, mCoreRootServiceConnection);
	}

	private void onRootServiceStarted() {
		if (getContext() == null) {
			return;
		}

		intentFilterHookedPackages.addAction(Constants.ACTION_XPOSED_CONFIRMED);
		requireContext().registerReceiver(receiverHookedPackages, intentFilterHookedPackages, RECEIVER_EXPORTED);

		monitorPackageList = Arrays.asList(getResources().getStringArray(R.array.module_scope));
		checkHookedPackages();

		binding.swipeRefreshLayout.setOnRefreshListener(() -> {
			checkHookedPackages();
			binding.swipeRefreshLayout.setRefreshing(false);
		});
	}

	private final BroadcastReceiver receiverHookedPackages = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), Constants.ACTION_XPOSED_CONFIRMED)) {
				String broadcastPackageName = intent.getStringExtra("packageName");

				for (int i = 0; i < binding.content.getChildCount(); i++) {
					View list = binding.content.getChildAt(i);
					TextView desc = list.findViewById(R.id.desc);
					String pkgName = ((TextView) list.findViewById(R.id.title)).getText().toString();

					if (pkgName.equals(broadcastPackageName)) {
						binding.content.post(() -> {
							desc.setText(getText(R.string.package_hooked_successful));
							desc.setTextColor(requireContext().getColor(R.color.success));
						});
					}
				}

				if (!hookedPackageList.contains(broadcastPackageName)) {
					hookedPackageList.add(broadcastPackageName);
				}
			}
		}
	};

	private final CountDownTimer countDownTimer = new CountDownTimer(5000, 500) {
		@Override
		public void onTick(long millisUntilFinished) {
			dotCount = (dotCount + 1) % 4;
			String dots = new String(new char[dotCount]).replace('\0', '.');

			for (int i = 0; i < binding.content.getChildCount(); i++) {
				View list = binding.content.getChildAt(i);
				TextView desc = list.findViewById(R.id.desc);

				if (((String) desc.getText()).contains(getString(R.string.package_checking, ""))) {
					desc.setText(getString(R.string.package_checking, dots));
				}
			}
		}

		@Override
		public void onFinish() {
			dotCount = 0;
			refreshListItem();
		}
	};

	private void checkHookedPackages() {
		hookedPackageList.clear();

		initListItem(monitorPackageList);
		new Thread(() -> requireContext().sendBroadcast(new Intent().setAction(Constants.ACTION_CHECK_XPOSED_ENABLED))).start();
		waitAndRefresh();
	}

	private void waitAndRefresh() {
		countDownTimer.start();
	}

	private void initListItem(List<String> pack) {
		dotCount = 0;
		countDownTimer.cancel();

		if (binding.content.getChildCount() > 0) {
			binding.content.removeAllViews();
		}

		List<String> filteredPack = new ArrayList<>();
		for (String packageName : pack) {
			if (isAppInstalled(packageName) || (!packageName.equals(KSU_PACKAGE) && !packageName.equals(KSU_NEXT_PACKAGE))) {
				filteredPack.add(packageName);
			}
		}

		for (int i = 0; i < filteredPack.size(); i++) {
			View list = LayoutInflater.from(requireContext()).inflate(R.layout.view_hooked_package_list, binding.content, false);
			String packageName = filteredPack.get(i);
			boolean isAppInstalled = isAppInstalled(packageName);

			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) list.getLayoutParams();
			if (i == 0) {
				list.setBackgroundResource(R.drawable.container_top);
				params.topMargin = dp2px(requireContext(), 16);
			} else if (i == filteredPack.size() - 1) {
				list.setBackgroundResource(R.drawable.container_bottom);
				params.bottomMargin = dp2px(requireContext(), 16);
			} else {
				list.setBackgroundResource(R.drawable.container_mid);
			}

			ImageView preview = list.findViewById(R.id.icon);
			preview.setImageDrawable(getAppIcon(packageName));

			TextView title = list.findViewById(R.id.title);
			title.setText(packageName);

			TextView desc = list.findViewById(R.id.desc);
			if (isAppInstalled) {
				desc.setText(getString(R.string.package_checking, ""));
			} else {
				desc.setText(getText(R.string.package_not_found));
				desc.setTextColor(requireContext().getColor(R.color.error));
				preview.setAlpha(0.4f);
				title.setAlpha(0.4f);
				desc.setAlpha(0.4f);
			}

			int finalI = i;

			Button activateInLSPosed = list.findViewById(R.id.activate_in_lsposed);
			activateInLSPosed.setOnClickListener(view -> {
				activateInLSPosed.setEnabled(false);
				try {
					if (mRootServiceIPC.activateInLSPosed(filteredPack.get(finalI))) {
						activateInLSPosed.animate().setDuration(300).withEndAction(() -> activateInLSPosed.setVisibility(GONE)).start();
						Toast.makeText(requireContext(), getText(R.string.package_activated), Toast.LENGTH_SHORT).show();
						binding.rebootButton.show();
						rebootPending = true;
					} else {
						Toast.makeText(requireContext(), getText(R.string.package_activation_failed), Toast.LENGTH_SHORT).show();
						activateInLSPosed.setEnabled(true);
					}
				} catch (RemoteException e) {
					Toast.makeText(requireContext(), getText(R.string.package_activation_failed), Toast.LENGTH_SHORT).show();
					activateInLSPosed.setEnabled(true);
					Log.e(TAG, e.toString());
				}
			});

			list.setOnClickListener(view -> {
				// show ripple effect and do nothing
			});

			PopupMenu popupMenu = new PopupMenu(requireContext(), list, Gravity.END);
			MenuInflater inflater = popupMenu.getMenuInflater();
			inflater.inflate(R.menu.hooks_menu, popupMenu.getMenu());

			popupMenu.setOnMenuItemClickListener(item -> {
				int itemId = item.getItemId();

				if (itemId == R.id.launch_app) {
					Intent intent = requireContext()
							.getPackageManager()
							.getLaunchIntentForPackage(filteredPack.get(finalI));
					if (intent != null) {
						startActivity(intent);
					} else {
						Toast.makeText(
								requireContext(),
								requireContext().getString(R.string.package_not_launchable),
								Toast.LENGTH_SHORT
						).show();
					}
				} else if (itemId == R.id.restart_app) {
					handleApplicationRestart(filteredPack.get(finalI));
				}

				return true;
			});

			list.setOnLongClickListener(v -> {
				if (isAppInstalled) {
					popupMenu.show();
				}
				return isAppInstalled;
			});

			binding.content.addView(list);
		}
	}

	private void handleApplicationRestart(String packageName) {
		if (SYSTEM_FRAMEWORK_PACKAGE.equals(packageName)) {
			AppUtils.restart("android");
		} else if (SYSTEM_UI_PACKAGE.equals(packageName)) {
			AppUtils.restart("systemui");
		} else {
			Shell.cmd(
					"killall " + packageName,
					"am force-stop " + packageName
			).exec();
			Intent intent = requireContext()
					.getPackageManager()
					.getLaunchIntentForPackage(packageName);
			if (intent != null) {
				startActivity(intent);
			}
		}
	}

	private void refreshListItem() {
		for (int i = 0; i < binding.content.getChildCount(); i++) {
			View list = binding.content.getChildAt(i);
			TextView desc = list.findViewById(R.id.desc);
			String pkgName = ((TextView) list.findViewById(R.id.title)).getText().toString();
			String reason;

			if (hookedPackageList.contains(pkgName)) {
				reason = "";
				desc.setText(getText(R.string.package_hooked_successful));
				desc.setTextColor(requireContext().getColor(R.color.success));
			} else {
				desc.setTextColor(requireContext().getColor(R.color.error));
				String description;

				if (!isAppInstalled(pkgName)) {
					description = getText(R.string.package_not_found).toString();
					reason = "";
				} else if (!checkLSPosedDB(pkgName)) {
					description = getText(R.string.package_not_hook_enabled).toString();
					reason = getString(R.string.package_not_hook_enabled_info, getString(R.string.activate_in_lsposed));
				} else if (hasBootLooped(pkgName)) {
					description = getText(R.string.package_hook_bootlooped).toString();
					reason = getString(R.string.package_hook_bootlooped_info);
				} else {
					description = getText(R.string.package_hook_no_response).toString();
					reason = getString(R.string.package_hook_no_response_info);
				}

				desc.setText(description);
			}

			if (desc.getText() == getText(R.string.package_not_hook_enabled)) {
				Button activateInLSPosed = list.findViewById(R.id.activate_in_lsposed);
				activateInLSPosed.setVisibility(VISIBLE);
				activateInLSPosed.setEnabled(true);
			}

			if (!reason.isBlank()) {
				TextView info = list.findViewById(R.id.reason);
				info.setVisibility(VISIBLE);
				info.setOnClickListener(view -> {
					new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialComponents_MaterialAlertDialog)
							.setTitle(R.string.whats_wrong)
							.setMessage(reason)
							.setPositiveButton(R.string.okay, (dialog, which) -> dialog.dismiss())
							.show();
				});
			}
		}
	}

	private boolean isAppInstalled(String packageName) {
		try {
			return mRootServiceIPC.isPackageInstalled(packageName);
		} catch (RemoteException e) {
			return false;
		}
	}

	private Drawable getAppIcon(String packageName) {
		try {
			return requireContext().getPackageManager().getApplicationIcon(packageName);
		} catch (PackageManager.NameNotFoundException ignored) {
			return ContextCompat.getDrawable(requireContext(), R.drawable.ic_android);
		}
	}

	private boolean checkLSPosedDB(String pkgName) {
		try {
			return mRootServiceIPC.checkLSPosedDB(pkgName);
		} catch (RemoteException e) {
			return false;
		}
	}

	private boolean hasBootLooped(String pkgName) {
		return XPrefs.Xprefs.getInt(String.format("%s%s", PACKAGE_STRIKE_KEY_KEY, pkgName), 0) >= 3;
	}

	@SuppressWarnings("SameParameterValue")
	private int dp2px(Context context, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}

	@SuppressWarnings("unused")
	public static class StringBooleanMap {
		private final HashMap<String, Boolean> map = new HashMap<>();

		public void put(String key, boolean value) {
			map.put(key, value);
		}

		public boolean get(String key) {
			Boolean value = map.get(key);
			return value != null ? value : false;
		}

		public boolean containsKey(String key) {
			return map.containsKey(key);
		}

		public void remove(String key) {
			map.remove(key);
		}

		public void clear() {
			map.clear();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(reboot_key, rebootPending);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null) {
			rebootPending = savedInstanceState.getBoolean(reboot_key);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		countDownTimer.cancel();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			requireContext().unregisterReceiver(receiverHookedPackages);
		} catch (Exception ignored) {
		}
		countDownTimer.cancel();
	}
}
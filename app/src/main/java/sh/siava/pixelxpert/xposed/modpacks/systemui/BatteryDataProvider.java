package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.xposed.XPrefs;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;

/**
 * @noinspection RedundantThrows
 */
@SystemUIModPack
public class BatteryDataProvider extends XposedModPack {
	public static final int BATTERY_STATUS_DISCHARGING = 3;
	public static final int MILLION = 1000000;
	public static final int USB_5_WATT = 5;
	public static final int CHARGING_SLOW = 1;
	public static final int CHARGING_FAST = 2;

	@SuppressLint("StaticFieldLeak")
	private static BatteryDataProvider instance = null;

	List<BatteryStatusCallback> mStatusCallbacks = new ArrayList<>();
	private boolean mCharging;
	private int mCurrentLevel = 0;


	private final ArrayList<BatteryInfoCallback> mInfoCallbacks = new ArrayList<>();
	private boolean mPowerSave = false;
	private boolean mIsFastCharging = false;
	private boolean mIsBatteryDefender = false;
	private static int FastChargingWattage = USB_5_WATT;


	public BatteryDataProvider(Context context) {
		super(context);
		instance = this;
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		FastChargingWattage = XPrefs.Xprefs.getSliderInt("FastChargingWattage", USB_5_WATT);
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass BatteryStatusClass = ReflectedClass.of("com.android.settingslib.fuelgauge.BatteryStatus");
		ReflectedClass BatteryControllerImplClass = ReflectedClass.of("com.android.systemui.statusbar.policy.BatteryControllerImpl");

		//once an intent is received, it's either battery level change, powersave change, or demo mode. we don't expect demo
		// intents normally. So it's safe to assume we'll need to update battery anyway
		BatteryControllerImplClass
				.after("onReceive")
				.run(param -> {
					mCurrentLevel = getIntField(param.thisObject, "mLevel");
					mCharging = getBooleanField(param.thisObject, "mPluggedIn")
							|| getBooleanField(param.thisObject, "mCharging")
							|| getBooleanField(param.thisObject, "mWirelessCharging");
					mPowerSave = getBooleanField(param.thisObject, "mPowerSave");

					try {
						mIsBatteryDefender = getBooleanField(param.thisObject, "mIsBatteryDefender");
					}
					catch (Throwable ignored) { //older versions of Android don't have defender
						mIsBatteryDefender = false;
					}

					fireBatteryInfoChanged();
				});

		BatteryStatusClass
				.before("getChargingSpeed")
				.run(param -> {
					if(FastChargingWattage > USB_5_WATT)
					{
						int maxChargingWattage = (int) getObjectField(param.thisObject, "maxChargingWattage") / MILLION;
						mIsFastCharging = maxChargingWattage >= FastChargingWattage;
						param.setResult(mIsFastCharging ? CHARGING_FAST : CHARGING_SLOW);
					}
				});

		BatteryStatusClass
				.afterConstruction()
				.run(param -> {
					mIsFastCharging = callMethod(param.thisObject, "getChargingSpeed", mContext).equals(CHARGING_FAST);

					if (param.args.length > 0 && (param.args[0] instanceof Intent)) {
						onBatteryStatusChanged((int) getObjectField(param.thisObject, "status"), (Intent) param.args[0]);
					}
				});
	}

	private void onBatteryStatusChanged(int status, Intent intent) {
		for (BatteryStatusCallback callback : mStatusCallbacks) {
			try {
				callback.onBatteryStatusChanged(status, intent);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void registerStatusCallback(BatteryStatusCallback callback) {
		try {
			instance.mStatusCallbacks.add(callback);
		} catch (Throwable ignored) {
		}
	}

	/**
	 * @noinspection unused
	 */
	public static void unRegisterStatusCallback(BatteryStatusCallback callback) {
		try {
			instance.mStatusCallbacks.remove(callback);
		} catch (Throwable ignored) {
		}
	}

	public static void registerInfoCallback(BatteryInfoCallback callback) {
		try {
			instance.mInfoCallbacks.add(callback);
		} catch (Throwable ignored) {
		}
	}

	/**
	 * @noinspection unused
	 */
	public static void unRegisterInfoCallback(BatteryInfoCallback callback) {
		try {
			instance.mInfoCallbacks.remove(callback);
		} catch (Throwable ignored) {
		}
	}

	public static boolean isCharging() {
		try {
			return instance.mCharging && !instance.mIsBatteryDefender;
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean isBatteryDefender()
	{
		try {
			return instance.mIsBatteryDefender;
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static int getCurrentLevel() {
		try {
			return instance.mCurrentLevel;
		} catch (Throwable ignored) {
			return 0;
		}
	}

	public static boolean isPowerSaving() {
		try {
			return instance.mPowerSave;
		} catch (Throwable ignored) {
			return false;
		}
	}

	public static boolean isFastCharging() {
		try {
			return instance.mCharging && instance.mIsFastCharging;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private void fireBatteryInfoChanged() {
		for (BatteryInfoCallback callback : mInfoCallbacks) {
			try {
				callback.onBatteryInfoChanged();
			} catch (Throwable ignored) {
			}
		}
	}

	public interface BatteryInfoCallback {
		void onBatteryInfoChanged();
	}


	public interface BatteryStatusCallback {
		void onBatteryStatusChanged(int batteryStatus, Intent batteryStatusIntent);
	}
}


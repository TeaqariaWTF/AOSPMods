package sh.siava.pixelxpert.modpacks.android;

import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

public class RingerVolSeparator extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static final String VOLUME_SEPARATE_NOTIFICATION = "volume_separate_notification";

	private static boolean SeparateRingNotifVol = false;
	private ReflectedClass DeviceConfigClass;

	public RingerVolSeparator(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		SeparateRingNotifVol = Xprefs.getBoolean("SeparateRingNotifVol", false);

		try {
			if (Key.length > 0 && Key[0].equals("SeparateRingNotifVol")) {
				setSeparateRingerNotif(SeparateRingNotifVol);
			}
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			DeviceConfigClass = ReflectedClass.of("android.provider.DeviceConfig");

			DeviceConfigClass
					.before("setProperty")
					.run(param -> {
						if ("systemui_PixelXpert".equals(param.args[0])) {
							param.args[0] = "systemui";
							return;
						}
						if (SeparateRingNotifVol && VOLUME_SEPARATE_NOTIFICATION.equals(param.args[1])) {
							param.setResult(true);
						}
					});

			if (Constants.SYSTEM_FRAMEWORK_PACKAGE.equals(lpParam.packageName))
				setSeparateRingerNotif(SeparateRingNotifVol);
		} catch (Throwable ignored) {
		}
	}

	private void setSeparateRingerNotif(boolean enabled) {
		try {
			DeviceConfigClass
					.callStaticMethod("setProperty", "systemui_PixelXpert", VOLUME_SEPARATE_NOTIFICATION, String.valueOf(enabled), true);
		} catch (Throwable ignored) {}
	}
}

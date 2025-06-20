package sh.siava.pixelxpert.modpacks.android;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.os.Binder;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class SystemScreenRecord extends XposedModPack {
	public static final String TARGET_PACKAGE = Constants.SYSTEM_FRAMEWORK_PACKAGE;

	private static boolean InsecureScreenRecord = false;

	public SystemScreenRecord(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		InsecureScreenRecord = Xprefs.getBoolean("InsecureScreenRecord", false);
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			ReflectedClass DisplayManagerServiceClass = ReflectedClass.of("com.android.server.display.DisplayManagerService");

			DisplayManagerServiceClass
					.before("canProjectSecureVideo")
					.run(param -> {
						try {
							if (InsecureScreenRecord && (boolean) callMethod(param.thisObject, "validatePackageName",
									Binder.getCallingUid(),
									Constants.SYSTEM_UI_PACKAGE))
								param.setResult(true);
						} catch (Throwable ignored) {
						}
					});
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean isTargeting(String packageName) {
		return TARGET_PACKAGE.equals(packageName);
	}
}
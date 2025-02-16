package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.GoogleMonochromeIconFactory;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class FeatureFlags extends XposedModPack {
	private static final String listenPackage = Constants.LAUNCHER_PACKAGE;
	private static boolean ForceThemedLauncherIcons = false;
	private int mIconBitmapSize;
	private Object mModel;
	private Object LAS;

	public FeatureFlags(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		ForceThemedLauncherIcons = Xprefs.getBoolean("ForceThemedLauncherIcons", false);

		if (Key.length > 0) {
            if (Key[0].equals("ForceThemedLauncherIcons")) {
                reloadIcons();
            }
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			ReflectedClass BaseIconFactoryClass = ReflectedClass.of("com.android.launcher3.icons.BaseIconFactory");
			ReflectedClass LauncherAppStateClass = ReflectedClass.of("com.android.launcher3.LauncherAppState");

			LauncherAppStateClass
					.afterConstruction()
					.run(param -> LAS = param.thisObject);

			BaseIconFactoryClass
					.afterConstruction()
					.run(param -> mIconBitmapSize = getIntField(param.thisObject, "mIconBitmapSize"));

			ReflectedClass.of(AdaptiveIconDrawable.class)
					.after("getMonochrome")
					.run(param -> {
						try {
							if(param.getResult() == null && ForceThemedLauncherIcons)
							{
								if(new Throwable().getStackTrace()[4].getMethodName().toLowerCase().contains("override")) //It's from com.android.launcher3.icons.IconProvider.getIconWithOverrides. Monochrome is included
								{
									return;
								}

								GoogleMonochromeIconFactory mono = (GoogleMonochromeIconFactory) getAdditionalInstanceField(param.thisObject, "mMonoFactoryPX");
								if(mono == null)
								{
									mono = new GoogleMonochromeIconFactory((AdaptiveIconDrawable) param.thisObject, mIconBitmapSize);
									setAdditionalInstanceField(param.thisObject, "mMonoFactoryPX", mono);
								}
								param.setResult(mono);
							}
						}
						catch (Throwable ignored){}
					});
		}
		catch (Throwable ignored){} //Android 13
	}

	private void reloadIcons() {
		Object iconCache = getObjectField(LAS, "mIconCache");

		boolean isA16 = (findFieldIfExists(iconCache.getClass(), "cache") != null);

		Object cache = isA16
				? getObjectField(iconCache, "cache")
				: getObjectField(iconCache, "mCache");

		Object iconDb = isA16
				? getObjectField(iconCache, "iconDb")
				: getObjectField(iconCache, "mIconDb");

		mModel = getObjectField(LAS, "mModel");

		new Handler(Looper.getMainLooper()).post(() -> {
			callMethod(cache, "clear");
			callMethod(iconDb, "clear");
			callMethod(mModel, "forceReload");
		});
	}
}
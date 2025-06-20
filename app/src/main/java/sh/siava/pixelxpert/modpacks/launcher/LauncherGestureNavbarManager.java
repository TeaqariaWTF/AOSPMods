package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import java.util.Arrays;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

/**
 * @noinspection RedundantThrows
 */
public class LauncherGestureNavbarManager extends XposedModPack {
	private static final String TARGET_PACKAGE = Constants.LAUNCHER_PACKAGE;

	private static boolean navPillColorAccent = false;
	private static float widthFactor = 1f;
	private static int GesPillHeightFactor = 100;

	private boolean mColorReplaced = false;
	private int mStashedHandleLightColor;
	private int mStashedHandleDarkColor;
	private boolean mIsHooked = false;

	public LauncherGestureNavbarManager(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		navPillColorAccent = Xprefs.getBoolean("navPillColorAccent", false);

		widthFactor = Xprefs.getSliderInt("GesPillWidthModPos", 50) * .02f;
		GesPillHeightFactor = Xprefs.getSliderInt("GesPillHeightFactor", 100);

		if (Xprefs.getBoolean("HideNavbar", false)) {
			widthFactor = 0f;
		}

		if (mIsHooked && Key.length > 0 && Arrays.asList(
				"GesPillWidthModPos",
				"GesPillHeightFactor",
				"HideNavbar").contains(Key[0])) {
			SystemUtils.doubleToggleDarkMode();
		}
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass StashedHandleViewClass = ReflectedClass.ofIfPossible("com.android.launcher3.taskbar.StashedHandleView");

		if (StashedHandleViewClass.getClazz() == null) return; //It's an older version

		mIsHooked = true;

		ReflectedClass StashedHandleViewControllerClass = ReflectedClass.of("com.android.launcher3.taskbar.StashedHandleViewController");

		StashedHandleViewControllerClass
				.afterConstruction()
				.run(param -> setAdditionalInstanceField(param.thisObject, "OriginalStashedHandleHeight", getObjectField(param.thisObject, "mStashedHandleHeight")));

		StashedHandleViewControllerClass
				.before("init")
				.run(param ->
						setObjectField(param.thisObject,
								"mStashedHandleHeight",
								Math.round(
										(int) getAdditionalInstanceField(
												param.thisObject,
												"OriginalStashedHandleHeight")
												* GesPillHeightFactor / 100f)));

		StashedHandleViewControllerClass
				.after("init")
				.run(param ->
						setObjectField(param.thisObject,
								"mStashedHandleWidth",
								Math.round(widthFactor * getIntField(param.thisObject, "mStashedHandleWidth"))));


		StashedHandleViewClass
				.afterConstruction()
				.run(param -> {
					mStashedHandleLightColor = (int) getObjectField(param.thisObject, "mStashedHandleLightColor");
					mStashedHandleDarkColor = (int) getObjectField(param.thisObject, "mStashedHandleDarkColor");
				});


		StashedHandleViewClass
				.before("updateHandleColor")
				.run(param -> {
					if (navPillColorAccent || mColorReplaced) {
						setObjectField(param.thisObject, "mStashedHandleLightColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_200, mContext.getTheme()) : mStashedHandleLightColor);
						setObjectField(param.thisObject, "mStashedHandleDarkColor", (navPillColorAccent) ? mContext.getResources().getColor(android.R.color.system_accent1_600, mContext.getTheme()) : mStashedHandleDarkColor);
						mColorReplaced = true;
					}
				});
	}

	@Override
	public boolean isTargeting(String packageName) {
		return TARGET_PACKAGE.equals(packageName);
	}
}
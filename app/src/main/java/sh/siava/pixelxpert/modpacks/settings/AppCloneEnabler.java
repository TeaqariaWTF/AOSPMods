package sh.siava.pixelxpert.modpacks.settings;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XposedModPack;

@SuppressWarnings({"RedundantThrows"})
public class AppCloneEnabler extends XposedModPack {
	private static final String listenPackage = Constants.SETTINGS_PACKAGE;
	private static final int AVAILABLE = 0;

	public AppCloneEnabler(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU;
	}

	@SuppressLint("ResourceType")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals(listenPackage)) return;

		Class<?> ClonedAppsPreferenceControllerClass = findClass("com.android.settings.applications.ClonedAppsPreferenceController", lpparam.classLoader);
		Class<?> AppStateClonedAppsBridgeClass = findClass("com.android.settings.applications.AppStateClonedAppsBridge", lpparam.classLoader);

		hookAllConstructors(AppStateClonedAppsBridgeClass, new XC_MethodHook() {
			@SuppressLint("QueryPermissionsNeeded")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				ArrayList<String> packageList = new ArrayList<>();
				PackageManager pm = mContext.getPackageManager();

				for(@SuppressWarnings("SingleStatementInBlock") PackageInfo installedPackage : pm.getInstalledPackages(PackageManager.GET_ACTIVITIES))
				{
					if(installedPackage.packageName != null && installedPackage.packageName.length() > 0)
					{
						packageList.add(installedPackage.packageName);
					}
				}

				setObjectField(param.thisObject, "mAllowedApps", packageList);
			}
		});

		//the way to manually clone the app
/*		Class<?> CloneBackendClass = findClass("com.android.settings.applications.manageapplications.CloneBackend", lpparam.classLoader);

		Object cb = callStaticMethod(CloneBackendClass, "getInstance", mContext);
		callMethod(cb, "installCloneApp", "com.whatsapp");*/

		//Adding the menu to settings app
		hookAllMethods(ClonedAppsPreferenceControllerClass, "getAvailabilityStatus", new XC_MethodHook() {
			@SuppressLint("ResourceType")
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				param.setResult(AVAILABLE);
			}
		});

		hookAllMethods(ClonedAppsPreferenceControllerClass, "updateSummary", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callMethod(
						getObjectField(param.thisObject, "mPreference"),
						"setSummary",
						ResourceManager.modRes.getText(R.string.settings_cloned_apps_active));

				param.setResult(null);
			}
		});
	}
}
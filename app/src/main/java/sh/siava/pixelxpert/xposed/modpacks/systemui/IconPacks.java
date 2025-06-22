package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.utils.ThemePackMapping.DRAWABLE_MAPPING_KEY;
import static sh.siava.pixelxpert.utils.ThemePackMapping.TYPE_DRAWABLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.xposed.annotations.SettingsModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIMainProcessModPack;
import sh.siava.pixelxpert.xposed.XPLauncher;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedMethod;
import sh.siava.pixelxpert.utils.ThemePackMapping.IDMapping;
import sh.siava.pixelxpert.utils.ThemePackMapping.Mapping;
import sh.siava.pixelxpert.utils.ThemePackMapping.OverlayID;
import sh.siava.pixelxpert.utils.ThemePackMapping.OverlayIDName;

@SystemUIMainProcessModPack
@SettingsModPack
public class IconPacks extends XposedModPack {
	static IDMapping drawableMapping = new IDMapping();
	private final PackageManager mPackageManager;
	private final List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
	private boolean mPackageLoaded = false;

	public IconPacks(Context context) {
		super(context);
		mPackageManager = mContext.getPackageManager();
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		updateMapping(Key.length > 0 && Key[0].equals(DRAWABLE_MAPPING_KEY));
	}

	private void hookAll() {
		if(hooks.isEmpty() && mPackageLoaded && !drawableMapping.isEmpty())
		{
			hooks.addAll(ReflectedMethod.ofExactData(ReflectedClass.of(Resources.class), "getDrawable", int.class, Resources.Theme.class)
					.beforeThat(param -> {
						Drawable drawable = getDrawable((int)param.args[0], (Resources.Theme) param.args[1]);
						if(drawable != null)
						{
							param.setResult(drawable);
						}
					}));

			hooks.addAll(ReflectedMethod.ofExactData(ReflectedClass.of(Resources.class), "getDrawable", int.class)
					.beforeThat(param -> {
						Drawable drawable = getDrawable((int)param.args[0], mContext.getTheme());
						if(drawable != null)
						{
							param.setResult(drawable);
						}
					}));

			hooks.addAll(ReflectedMethod.ofExactData(ReflectedClass.of(Resources.class), "getDrawableForDensity", int.class, int.class, Resources.Theme.class)
					.beforeThat(param -> {
						Drawable drawable = getDrawable((int)param.args[0], (Resources.Theme) param.args[2]);
						if(drawable != null)
						{
							param.setResult(drawable);
						}
					}));

			hooks.addAll(ReflectedMethod.ofExactData(ReflectedClass.of(Resources.class), "getDrawableForDensity", int.class, int.class)
					.beforeThat(param -> {
						Drawable drawable = getDrawable((int)param.args[0],  mContext.getTheme());
						if(drawable != null)
						{
							param.setResult(drawable);
						}
					}));
		}
	}

	private void unhookAll() {
		for (XC_MethodHook.Unhook hook : hooks) {
			hook.unhook();
		}
		hooks.clear();
	}

	private void updateMapping(boolean shallRefresh) {
		new Thread(() -> {
			drawableMapping = getIDMapping(TYPE_DRAWABLE, "drawable");
			if(drawableMapping.isEmpty())
			{
				unhookAll();
			}
			else
			{
				hookAll();
			}
			if(shallRefresh)
				refreshUI();
		}).start();
	}

	private void refreshUI() {
		XPLauncher.enqueueProxyCommand(proxy -> proxy.runCommand("cmd overlay disable com.google.android.systemui.gxoverlay; cmd overlay enable com.google.android.systemui.gxoverlay"));
	}

	/** @noinspection SameParameterValue*/
	private IDMapping getIDMapping(int prefType, String type) {
		Mapping prefMapping = Mapping.loadMapping(Xprefs, prefType);

		IDMapping idMapping = new IDMapping();
		for (String key : prefMapping.keySet()) {
			try {
				OverlayIDName overlayIDName = prefMapping.get(key);

				int mappingID = getMappingID(key, type);

				//noinspection DataFlowIssue
				int replacementID = getReplacementID(overlayIDName, type);

				if(replacementID == 0 || mappingID == 0) //some id cannot be found. ignore
				{
					continue;
				}

				idMapping.put(mappingID, new OverlayID(overlayIDName.packageName, replacementID));
			} catch (Throwable ignored) {}
		}
		return idMapping;
	}

	@SuppressLint("DiscouragedApi")
	private int getReplacementID(OverlayIDName overlayIDName, String type) throws PackageManager.NameNotFoundException {
		return mPackageManager.getResourcesForApplication(overlayIDName.packageName).getIdentifier(overlayIDName.resName, type, overlayIDName.packageName);
	}

	@SuppressLint("DiscouragedApi")
	private int getMappingID(String key, String type)
	{
		String[] keyParts = key.split(":");
		String resName = keyParts[keyParts.length - 1];

		String sourcePackage = keyParts.length > 1 ? keyParts[0] : mContext.getPackageName();
		if(sourcePackage.equals("module"))
		{
			sourcePackage = BuildConfig.APPLICATION_ID;
		}

		return mContext.getResources().getIdentifier(resName, type, sourcePackage);
	}
	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		mPackageLoaded = true;
		hookAll();
	}

	private Drawable getDrawable(int id, Resources.Theme theme) throws Throwable {
		if(drawableMapping.containsKey(id))
		{
			OverlayID overlayID = drawableMapping.get(id);

			//noinspection DataFlowIssue
			Resources res = mPackageManager.getResourcesForApplication(overlayID.packageName);

			return Drawable.createFromXml(res, res.getXml(overlayID.resID), theme);
		}
		return null;
	}
}

package sh.siava.pixelxpert.xposed;

import android.content.Context;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

public abstract class XposedModPack extends Logger {
	protected Context mContext;

	public XposedModPack(Context context) {
		mContext = context;
	}

	public abstract void onPreferenceUpdated(String... Key);

	public abstract void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable;
}
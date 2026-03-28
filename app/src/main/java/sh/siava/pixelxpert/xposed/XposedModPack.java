package sh.siava.pixelxpert.xposed;

import android.content.Context;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

public abstract class XposedModPack extends Logger {
	protected Context mContext;

	public XposedModPack(Context context) {
		mContext = context;
	}

	public abstract void onPreferenceUpdated(String... Key);

	public final void onPackageLoadedInternal(XposedModuleInterface.PackageReadyParam PRParam, XposedInterface xposedInterface) throws Throwable {
		onPackageLoaded(PRParam);
	}

	public abstract void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable;
}
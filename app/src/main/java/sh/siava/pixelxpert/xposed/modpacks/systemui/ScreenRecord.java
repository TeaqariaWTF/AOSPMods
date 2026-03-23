package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class ScreenRecord extends XposedModPack {
	private static boolean InsecureScreenRecord = false;

	public ScreenRecord(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		InsecureScreenRecord = Xprefs.getBoolean("InsecureScreenRecord", false);
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		ReflectedClass.of(MediaProjection.class)
				.before("createVirtualDisplay")
				.run(param -> {
					if(InsecureScreenRecord
							&& ((Method) param.method).getParameterCount() == 8)
					{
						int flags = (int) param.args[4];
						flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;
						param.args[4] = flags;
					}
				});
	}
}

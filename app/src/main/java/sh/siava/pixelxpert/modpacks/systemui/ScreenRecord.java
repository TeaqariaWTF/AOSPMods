package sh.siava.pixelxpert.modpacks.systemui;

import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;

import java.lang.reflect.Method;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.annotations.SystemUIMainProcessModPack;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIMainProcessModPack
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
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
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

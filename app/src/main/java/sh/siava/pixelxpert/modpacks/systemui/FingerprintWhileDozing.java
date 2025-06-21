package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.annotations.SystemUIMainProcessModPack;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIMainProcessModPack
public class FingerprintWhileDozing extends XposedModPack {
	private static boolean fingerprintWhileDozing = true;

	public FingerprintWhileDozing(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		fingerprintWhileDozing = Xprefs.getBoolean("fingerprintWhileDozing", true);
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass KeyguardUpdateMonitorClass = ReflectedClass.of("com.android.keyguard.KeyguardUpdateMonitor");

		KeyguardUpdateMonitorClass
				.before("shouldListenForFingerprint")
				.run(param -> {
					if (fingerprintWhileDozing) return;

					if(!getBooleanField(param.thisObject, "mDeviceInteractive"))
					{
						param.setResult(false);
					}
				});
	}
}
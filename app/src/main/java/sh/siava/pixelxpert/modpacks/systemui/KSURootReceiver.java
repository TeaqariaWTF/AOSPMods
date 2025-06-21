package sh.siava.pixelxpert.modpacks.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.annotations.SystemUIMainProcessModPack;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.utils.AppUtils;

@SystemUIMainProcessModPack
public class KSURootReceiver extends XposedModPack {
	public KSURootReceiver(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				AppUtils.runKSURootActivity(mContext, false);
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_KSU_ACQUIRE_ROOT);

		mContext.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED);
	}
}
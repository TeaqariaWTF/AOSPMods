package sh.siava.pixelxpert.xposed.modpacks.allApps;

import static sh.siava.pixelxpert.xposed.Constants.ACTION_CHECK_XPOSED_ENABLED;
import static sh.siava.pixelxpert.xposed.Constants.ACTION_XPOSED_CONFIRMED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.CommonModPack;

@CommonModPack
public class HookTester extends XposedModPack {
	public HookTester(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				new Thread(() -> {
					Intent broadcast = new Intent(ACTION_XPOSED_CONFIRMED);

					broadcast.putExtra("packageName", PRParam.getPackageName());

					broadcast.setPackage(BuildConfig.APPLICATION_ID);

					mContext.sendBroadcast(broadcast);
				}).start();
			}
		};

		mContext.registerReceiver(broadcastReceiver,
				new IntentFilter(ACTION_CHECK_XPOSED_ENABLED),
				Context.RECEIVER_EXPORTED);
	}
}
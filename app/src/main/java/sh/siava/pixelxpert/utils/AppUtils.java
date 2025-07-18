package sh.siava.pixelxpert.utils;

import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;

import android.content.Context;
import android.content.Intent;
import android.os.FileUtils;
import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import sh.siava.pixelxpert.xposed.Constants;

public class AppUtils {
	public static void restart(String what) {
		switch (what.toLowerCase())
		{
			case "systemui":
				Shell.cmd("killall com.android.systemui").exec();
				break;
			case "system":
				Shell.cmd("am start -a android.intent.action.REBOOT").exec();
				break;
			case "zygote":
			case "android":
				Shell.cmd("kill $(pidof zygote)").submit();
				Shell.cmd("kill $(pidof zygote64)").submit();
				break;
			default:
				Shell.cmd(String.format("killall %s", what)).exec();
		}
	}

	public static void runKSURootActivity(Context context, boolean launchApp)
	{
		try {
			//we first send a broadcast. if app is running it will get it
			Intent broadcastIntent = new Intent(Constants.PX_ROOT_EXTRA);
			if (launchApp) {
				broadcastIntent.putExtra("launchApp", 1);
			}

			broadcastIntent.setPackage(Constants.KSU_PACKAGE);
			context.sendBroadcast(broadcastIntent);

			//if app isn't running, it won't see the broadcast. but it will see the intent instead
			Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(Constants.KSU_PACKAGE);
			//noinspection DataFlowIssue
			launchIntent.putExtra(Constants.PX_ROOT_EXTRA, 1);
			launchIntent.setFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			if (launchApp) {
				launchIntent.putExtra("launchApp", 1);
			}

			context.startActivity(launchIntent);
		}
		catch (Throwable ignored){}
	}

	public static boolean isLikelyPixelBuild()
	{
		try
		{
			Process process = Runtime.getRuntime().exec("getprop ro.build.id");
			process.waitFor();
			byte[] buffer = new byte[process.getInputStream().available()];
			//noinspection ResultOfMethodCallIgnored
			process.getInputStream().read(buffer);
			String result = new String(buffer, StandardCharsets.US_ASCII).replace("\n", "");
			return Pattern.matches("^[TUAB][A-Z]([A-Z0-9]){2}\\.[0-9]{6}\\.[0-9]{3}(\\.[A-Z0-9]{2})?$", result); //Pixel standard build number of A13/14 + new weird build numbers of 'A,B,...' prefix
		}
		catch (Throwable ignored)
		{
			return false;
		}
	}

	public static boolean installDoubleZip(String DoubleZipped) //installs the zip magisk module. even if it's zipped inside another zip
	{
		try {
			//copy it to somewhere under our control
			File tempFile = File.createTempFile("doubleZ", ".zip");
			Shell.cmd(String.format("cp %s %s", DoubleZipped, tempFile.getAbsolutePath())).exec();

			//unzip once, IF double zipped
			ZipFile unzipper = new ZipFile(tempFile);

			File unzippedFile;
			if (unzipper.stream().count() == 1) {
				unzippedFile = File.createTempFile("singleZ", "zip");
				FileOutputStream unzipOutputStream = new FileOutputStream(unzippedFile);
				FileUtils.copy(unzipper.getInputStream(unzipper.entries().nextElement()), unzipOutputStream);
				unzipOutputStream.close();
			} else {
				unzippedFile = tempFile;
			}

			//install
			Shell.cmd(String.format("magisk --install-module %s", unzippedFile.getAbsolutePath())).exec(); //magisk
			Shell.cmd(String.format("ksud module install %s", unzippedFile.getAbsolutePath())).exec(); //ksu

			//cleanup
			//noinspection ResultOfMethodCallIgnored
			tempFile.delete();
			//noinspection ResultOfMethodCallIgnored
			unzippedFile.delete();
			return true;
		} catch (Exception e) {
			Log.e("PixelXpert Installer", "PixelXpert zip install error: ", e);
			return false;
		}
	}
}
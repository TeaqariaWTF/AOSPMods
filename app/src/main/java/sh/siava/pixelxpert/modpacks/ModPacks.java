package sh.siava.pixelxpert.modpacks;

import java.util.ArrayList;

import sh.siava.pixelxpert.modpacks.allApps.HookTester;
import sh.siava.pixelxpert.modpacks.android.BrightnessRange;
import sh.siava.pixelxpert.modpacks.android.FaceUpScreenSleep;
import sh.siava.pixelxpert.modpacks.android.HotSpotController;
import sh.siava.pixelxpert.modpacks.android.PackageManager;
import sh.siava.pixelxpert.modpacks.android.PhoneWindowManager;
import sh.siava.pixelxpert.modpacks.android.ScreenOffKeys;
import sh.siava.pixelxpert.modpacks.android.ScreenRotation;
import sh.siava.pixelxpert.modpacks.android.StatusbarSize;
import sh.siava.pixelxpert.modpacks.android.SystemScreenRecord;
import sh.siava.pixelxpert.modpacks.dialer.RecordingMessage;
import sh.siava.pixelxpert.modpacks.ksu.KSUInjector;
import sh.siava.pixelxpert.modpacks.launcher.ClearAllButtonMod;
import sh.siava.pixelxpert.modpacks.launcher.CustomNavGestures;
import sh.siava.pixelxpert.modpacks.launcher.FeatureFlags;
import sh.siava.pixelxpert.modpacks.launcher.LauncherGestureNavbarManager;
import sh.siava.pixelxpert.modpacks.launcher.PixelXpertIconUpdater;
import sh.siava.pixelxpert.modpacks.launcher.TaskbarActivator;
import sh.siava.pixelxpert.modpacks.settings.AppCloneEnabler;
import sh.siava.pixelxpert.modpacks.settings.PXSettingsLauncher;
import sh.siava.pixelxpert.modpacks.systemui.BatteryDataProvider;
import sh.siava.pixelxpert.modpacks.systemui.DepthWallpaper;
import sh.siava.pixelxpert.modpacks.systemui.EasyUnlock;
import sh.siava.pixelxpert.modpacks.systemui.FeatureFlagsMods;
import sh.siava.pixelxpert.modpacks.systemui.FingerprintWhileDozing;
import sh.siava.pixelxpert.modpacks.systemui.FlashlightTile;
import sh.siava.pixelxpert.modpacks.systemui.GestureNavbarManager;
import sh.siava.pixelxpert.modpacks.systemui.IconPacks;
import sh.siava.pixelxpert.modpacks.systemui.KSURootReceiver;
import sh.siava.pixelxpert.modpacks.systemui.KeyGuardPinScrambler;
import sh.siava.pixelxpert.modpacks.systemui.KeyguardMods;
import sh.siava.pixelxpert.modpacks.systemui.MultiStatusbarRows;
import sh.siava.pixelxpert.modpacks.systemui.NotificationExpander;
import sh.siava.pixelxpert.modpacks.systemui.NotificationManager;
import sh.siava.pixelxpert.modpacks.systemui.PowerMenu;
import sh.siava.pixelxpert.modpacks.systemui.QSTileGrid;
import sh.siava.pixelxpert.modpacks.systemui.ScreenGestures;
import sh.siava.pixelxpert.modpacks.systemui.ScreenRecord;
import sh.siava.pixelxpert.modpacks.systemui.ScreenshotManager;
import sh.siava.pixelxpert.modpacks.systemui.StatusIconTuner;
import sh.siava.pixelxpert.modpacks.systemui.StatusbarGestures;
import sh.siava.pixelxpert.modpacks.systemui.StatusbarMods;
import sh.siava.pixelxpert.modpacks.systemui.ThermalProvider;
import sh.siava.pixelxpert.modpacks.systemui.UDFPSManager;
import sh.siava.pixelxpert.modpacks.systemui.VolumeTile;
import sh.siava.pixelxpert.modpacks.telecom.CallVibrator;


public class ModPacks {

	public static ArrayList<Class<? extends XposedModPack>> getMods(String packageName)
	{
		ArrayList<Class<? extends XposedModPack>> modPacks = new ArrayList<>();

		//all packages
		modPacks.add(HookTester.class);

		switch (packageName)
		{
			case Constants.SYSTEM_FRAMEWORK_PACKAGE:
				modPacks.add(StatusbarSize.class);
				modPacks.add(PackageManager.class);
				modPacks.add(BrightnessRange.class);
				modPacks.add(PhoneWindowManager.class);
				modPacks.add(ScreenRotation.class);
				modPacks.add(ScreenOffKeys.class);
				modPacks.add(HotSpotController.class);
				modPacks.add(SystemScreenRecord.class);
				modPacks.add(FaceUpScreenSleep.class);
				break;

			case Constants.SYSTEM_UI_PACKAGE:
				if(XPLauncher.isChildProcess && XPLauncher.processName.contains("screenshot"))
				{
					modPacks.add(ScreenshotManager.class);
				}
				else
				{
					//load before others
					modPacks.add(ThermalProvider.class);
					modPacks.add(BatteryDataProvider.class);

					modPacks.add(IconPacks.class);
					modPacks.add(BrightnessRange.class);
					modPacks.add(NotificationExpander.class);
					modPacks.add(QSTileGrid.class);
					modPacks.add(FeatureFlagsMods.class);
					modPacks.add(ScreenGestures.class);
					modPacks.add(MiscSettings.class);
					modPacks.add(StatusbarGestures.class);
					modPacks.add(KeyguardMods.class); //good
					modPacks.add(UDFPSManager.class); //good
					modPacks.add(EasyUnlock.class); //good
					modPacks.add(MultiStatusbarRows.class); //good already
					modPacks.add(StatusbarMods.class);
					modPacks.add(GestureNavbarManager.class); //good already
					modPacks.add(KeyGuardPinScrambler.class); //good already
					modPacks.add(FingerprintWhileDozing.class); //good already
					modPacks.add(StatusbarSize.class); //good already
					modPacks.add(NotificationManager.class); //good
					modPacks.add(ScreenRecord.class); //good already
					modPacks.add(DepthWallpaper.class); //good
					modPacks.add(KSURootReceiver.class);
					modPacks.add(PowerMenu.class); //good already
					modPacks.add(StatusIconTuner.class); //good already
					modPacks.add(FlashlightTile.class);
					modPacks.add(VolumeTile.class);
				}
				break;

			case Constants.LAUNCHER_PACKAGE:
				modPacks.add(LauncherGestureNavbarManager.class);
				modPacks.add(TaskbarActivator.class);
				modPacks.add(CustomNavGestures.class);
				modPacks.add(ClearAllButtonMod.class);
				modPacks.add(PixelXpertIconUpdater.class);
				modPacks.add(FeatureFlags.class);
				break;

			case Constants.TELECOM_SERVER_PACKAGE:
				modPacks.add(CallVibrator.class);
				break;

			case Constants.SETTINGS_PACKAGE:
				modPacks.add(PXSettingsLauncher.class);
				modPacks.add(IconPacks.class);
				modPacks.add(AppCloneEnabler.class);

			case Constants.DIALER_PACKAGE:
				modPacks.add(RecordingMessage.class);
				break;

			case Constants.KSU_PACKAGE:
			case Constants.KSU_NEXT_PACKAGE:
				modPacks.add(KSUInjector.class);
				break;
		}

		//All Apps

		return modPacks;
	}
}
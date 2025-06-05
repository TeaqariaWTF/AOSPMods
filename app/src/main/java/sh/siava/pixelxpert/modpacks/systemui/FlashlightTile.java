package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.getFlashlightLevel;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.getMaxFlashLevel;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.isFlashOn;

import android.content.Context;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.AlertSlider;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

public class FlashlightTile extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private boolean leveledFlashTile = false;
	private boolean AnimateFlashlight = false;

	public FlashlightTile(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		leveledFlashTile = Xprefs.getBoolean("leveledFlashTile", false);
		AnimateFlashlight = Xprefs.getBoolean("AnimateFlashlight", false);
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass FlashlightTileClass = ReflectedClass.of("com.android.systemui.qs.tiles.FlashlightTile");

		FlashlightTileClass
				.before("handleLongClick")
				.run(param -> {
					if(leveledFlashTile && handleFlashLongClick())
						param.setResult(null);
				});

		FlashlightTileClass
				.after("newTileState")
				.run(param ->
						setObjectField(param.getResult(), "handlesLongClick", true));
	}

	private boolean handleFlashLongClick() throws Throwable {
		AlertSlider.SliderEventCallback flashlightSliderCallback = new AlertSlider.SliderEventCallback() {
			@Override
			public void onStartTrackingTouch(Object slider) {}

			@Override
			public void onStopTrackingTouch(Object slider) {
				float value = (float) callMethod(slider, "getValue");

				Xprefs.edit()
						.putInt("flashPCT",
								Math.round((value * 100f) / getMaxFlashLevel())
						).apply();

				if(!isFlashOn())
				{
					SystemUtils.setFlash(true, AnimateFlashlight);
				}
			}

			@Override
			public void onValueChange(Object slider, float value, boolean fromUser) {
				if(isFlashOn()) {
					SystemUtils.setFlash(true, Math.round(value), false);
				}
			}
		};

		new AlertSlider().show(mContext,
				getFlashlightLevel(
						Xprefs.getInt("flashPCT", 50)
								/ 100f),
				1,
				getMaxFlashLevel(),
				1,
				flashlightSliderCallback);

		return true;
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
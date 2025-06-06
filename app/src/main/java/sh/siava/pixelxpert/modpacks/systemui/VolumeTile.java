package sh.siava.pixelxpert.modpacks.systemui;

import static android.media.AudioManager.STREAM_MUSIC;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.AudioManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.Tile;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.AlertSlider;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.service.tileServices.VolumeTileService;

/** @noinspection DataFlowIssue*/
public class VolumeTile extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	private AlertSlider mAlertSlider;
	private boolean mReceiverRegistered = false;
	private Object mVolumeTile;

	public VolumeTile(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass CustomTileClass = ReflectedClass.of("com.android.systemui.qs.external.CustomTile");
		CustomTileClass
				.afterConstruction()
				.run(param -> {
					ComponentName componentName = (ComponentName) getObjectField(param.thisObject, "mComponent");

					if(componentName.getClassName().equals(VolumeTileService.class.getName()))
					{
						mVolumeTile = param.thisObject;
					}
				});

		CustomTileClass
				.after("handleClick")
				.run(param -> {
					if(param.thisObject == mVolumeTile)
					{
						SystemUtils.toggleMute();

						Tile mTile = (Tile) getObjectField(param.thisObject, "mTile");
						mTile.setState(SystemUtils.AudioManager().isStreamMute(STREAM_MUSIC) ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);

						callMethod(param.thisObject, "applyTileState", mTile, true);
						callMethod(param.thisObject, "refreshState", new Object[]{null});
					}
				});
		
		CustomTileClass
				.before("getLongClickIntent")
				.run(param -> {
					if(param.thisObject == mVolumeTile)
					{
						if(!mReceiverRegistered)
						{
							registerUpdateReceiver();
						}
						if(handleVolumeLongClick())
							param.setResult(new Intent());
					}
				});
	}

	private void registerUpdateReceiver() {
		SystemUtils
				.registerVolumeChangeListener(newVal -> {
					if(mAlertSlider != null)
					{
						mAlertSlider.setSliderCurrentValue(newVal);
					}
				});

		mReceiverRegistered = true;
	}

	/** @noinspection DataFlowIssue*/
	private boolean handleVolumeLongClick() throws Throwable {
		AlertSlider.SliderEventCallback volumeSliderCallback = new AlertSlider.SliderEventCallback() {
			@Override
			public void onStartTrackingTouch(Object slider) {}

			@Override
			public void onStopTrackingTouch(Object slider) {}

			@Override
			public void onValueChange(Object slider, float value, boolean fromUser) {
				changeVolume(Math.round(value));
			}
		};

		mAlertSlider = new AlertSlider();

		mAlertSlider.show(mContext,
				AudioManager().getStreamVolume(STREAM_MUSIC),
				AudioManager().getStreamMinVolume(STREAM_MUSIC),
				AudioManager().getStreamMaxVolume(STREAM_MUSIC),
				1,
				volumeSliderCallback);

		return true;
	}

	private void changeVolume(int currentValue) {
		AudioManager().setStreamVolume(
				STREAM_MUSIC,
				currentValue,
				0 /* don't show UI */);
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
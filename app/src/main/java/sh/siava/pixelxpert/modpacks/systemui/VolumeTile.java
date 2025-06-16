package sh.siava.pixelxpert.modpacks.systemui;

import static android.media.AudioManager.STREAM_MUSIC;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.AudioManager;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.registerVolumeChangeListener;

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
	private Object mTile;
	private boolean mNextTileIsVolume;

	public VolumeTile(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass CustomTileClass = ReflectedClass.of("com.android.systemui.qs.external.CustomTile");
		ReflectedClass QSFactoryImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSFactoryImpl");
		ReflectedClass QSTileImplClass = ReflectedClass.of("com.android.systemui.qs.tileimpl.QSTileImpl");

		QSFactoryImplClass
				.before("createTile")
				.run(param -> {
					String arg = (String) param.args[0];
					if(arg.contains(VolumeTileService.class.getSimpleName()))
					{
						mNextTileIsVolume = true;
					}
				});

		CustomTileClass
				.beforeConstruction()
				.run(param -> {
					if(mNextTileIsVolume)
					{
						mTile = param.thisObject;
						mNextTileIsVolume = false;
					}
				});

		CustomTileClass
				.afterConstruction()
				.run(param -> {
					if(param.thisObject == mTile)
					{
						registerVolumeChangeListener(newVal -> updateTile());
						updateTile();
					}
				});

		CustomTileClass
				.after("newTileState")
				.run(param -> {
					if(param.thisObject == mTile)
					{
						Object state = param.getResult();
						setObjectField(state, "handlesSecondaryClick", true);
					}
				});

		CustomTileClass
				.after("handleClick")
				.run(param -> {
					if(param.thisObject == mTile)
					{
						handleVolumeLongClick();
					}
				});

		QSTileImplClass
				.before("handleSecondaryClick")
				.run(param -> {
					if(param.thisObject == mTile)
					{
						SystemUtils.toggleMute();

						updateTile();

						param.setResult(null); //otherwise it will also call click
					}
				});

		CustomTileClass
				.before("getLongClickIntent")
				.run(param -> {
					if(param.thisObject == mTile)
					{
						if(handleVolumeLongClick())
							param.setResult(new Intent());
					}
				});
	}

	private void updateTile() {
		Tile mTile = (Tile) getObjectField(this.mTile, "mTile");
		mTile.setState(SystemUtils.AudioManager().isStreamMute(STREAM_MUSIC) ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);

		callMethod(this.mTile, "refreshState", new Object[]{null});
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

	private boolean handleVolumeLongClick() throws Throwable {
		if(!mReceiverRegistered)
		{
			registerUpdateReceiver();
		}


		if(mAlertSlider == null) {
			createAlertSlider();
		}

		mAlertSlider.show();

		return true;
	}

	private void createAlertSlider() throws Throwable {
		AlertSlider.SliderEventCallback volumeSliderCallback = new AlertSlider.SliderEventCallback() {
			@Override
			public void onStartTrackingTouch(Object slider) {}

			@Override
			public void onStopTrackingTouch(Object slider) {}

			@Override
			public void onValueChange(Object slider, float value, boolean fromUser) {
				if(fromUser)
					changeVolume(Math.round(value));
			}
		};

		mAlertSlider = new AlertSlider(mContext,
				AudioManager().getStreamVolume(STREAM_MUSIC),
				AudioManager().getStreamMinVolume(STREAM_MUSIC),
				AudioManager().getStreamMaxVolume(STREAM_MUSIC),
				1,
				volumeSliderCallback);
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
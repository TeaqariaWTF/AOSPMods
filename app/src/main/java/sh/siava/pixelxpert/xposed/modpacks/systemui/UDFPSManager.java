package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.toolkit.ColorUtils.getColorAttrDefaultColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@SystemUIModPack
public class UDFPSManager extends XposedModPack {
	private static final int TRANSPARENT = 0;
	private static final int OPAQUE = 255;
	private static boolean transparentBG = false;
	private static boolean transparentFG = false;
	private View mDeviceEntryIconView;
	private ReflectedClass StateFlowImplClass;
	private ReflectedClass ReadonlyStateFlowClass;

	public UDFPSManager(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Xprefs == null) return;
		transparentBG = Xprefs.getBoolean("fingerprint_circle_hide", false);
		transparentFG = Xprefs.getBoolean("fingerprint_icon_hide", false);

		if(Key.length == 0) return;

		switch (Key[0])
		{
			case "fingerprint_circle_hide":
			case "fingerprint_icon_hide":
				setUDFPSGraphics(true);
		}
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) {
		ReflectedClass UdfpsKeyguardViewClass = ReflectedClass.ofIfPossible("com.android.systemui.biometrics.UdfpsKeyguardViewLegacy"); //A4B3
		if (UdfpsKeyguardViewClass.getClazz() == null) { //A13
			UdfpsKeyguardViewClass = ReflectedClass.ofIfPossible("com.android.systemui.biometrics.UdfpsKeyguardView");
		}

		if(UdfpsKeyguardViewClass.getClazz() == null) //A15 Beta 2 - Compose
		{
			ReflectedClass DeviceEntryIconViewClass = ReflectedClass.of("com.android.systemui.keyguard.ui.view.DeviceEntryIconView");
			ReflectedClass DeviceEntryIconViewModelClass = ReflectedClass.of("com.android.systemui.keyguard.ui.viewmodel.DeviceEntryIconViewModel");

			StateFlowImplClass = ReflectedClass.of("kotlinx.coroutines.flow.StateFlowImpl");
			ReadonlyStateFlowClass = ReflectedClass.of("kotlinx.coroutines.flow.ReadonlyStateFlow");

			DeviceEntryIconViewModelClass
					.afterConstruction()
					.run(param -> {
						if((transparentBG && !transparentFG)) {
							try {
								Object FalseFlow = StateFlowImplClass.getClazz().getConstructor(Object.class).newInstance(false);
								setObjectField(param.thisObject, "useBackgroundProtection", ReadonlyStateFlowClass.getClazz().getConstructors()[0].newInstance(FalseFlow));
							} catch (Throwable ignored) {}
						}
					});

			DeviceEntryIconViewClass
					.afterConstruction()
					.run(param -> {
						mDeviceEntryIconView = (View) param.thisObject;

						setUDFPSGraphics(false);


						//making sure it remains on top on wallpaper subject
						mDeviceEntryIconView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
							@Override
							public void onViewAttachedToWindow(@NonNull View v) {
								v.setZ(100);
							}

							@Override
							public void onViewDetachedFromWindow(@NonNull View v) {

							}
						});

						mDeviceEntryIconView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
								v.setZ(100));
					});
		}
		else
		{
			ReflectedClass LockIconViewControllerClass = ReflectedClass.of("com.android.keyguard.LockIconViewController");

			LockIconViewControllerClass
					.after("updateIsUdfpsEnrolled")
					.run(param -> {
						if(transparentBG) {
							setObjectField(
									getObjectField(param.thisObject, "mView"),
									"mUseBackground",
									false);

							callMethod(getObjectField(param.thisObject, "mView"), "updateColorAndBackgroundVisibility");
						}
					});

			UdfpsKeyguardViewClass
					.afterConstruction()
					.run(param -> {
						try {
							ReflectedClass.of(getObjectField(param.thisObject, "mLayoutInflaterFinishListener").getClass())
									.after("onInflateFinished")
									.run(param1 -> removeUDFPSGraphicsLegacy(param.thisObject));
						} catch (Throwable ignored) {
						}//A13
					});

			UdfpsKeyguardViewClass
					.after("onFinishInflate")
					.run(param -> removeUDFPSGraphicsLegacy(param.thisObject));

			UdfpsKeyguardViewClass
					.before("updateColor")
					.run(param -> {
						if (!transparentBG ||
								!getBooleanField(param.thisObject, "mFullyInflated"))
							return;

						Object mLockScreenFp = getObjectField(param.thisObject, "mLockScreenFp");

						@SuppressLint("DiscouragedApi")
						int mTextColorPrimary = getColorAttrDefaultColor(
								mContext,
								mContext.getResources().getIdentifier("wallpaperTextColorAccent", "attr", mContext.getPackageName()));

						setObjectField(param.thisObject, "mTextColorPrimary", mTextColorPrimary);

						callMethod(mLockScreenFp, "invalidate");
						param.setResult(null);
					});
		}
	}

	/** @noinspection ConstantValue*/
	private void setUDFPSGraphics(boolean force) {
		if(mDeviceEntryIconView == null) return;

		if(transparentFG || force)
		{
			((ImageView) getObjectField(mDeviceEntryIconView, "iconView"))
					.setImageAlpha(transparentFG
							? TRANSPARENT
							: OPAQUE);
		}
		if(transparentFG || transparentBG || force) {
			((ImageView) getObjectField(mDeviceEntryIconView, "bgView"))
					.setImageAlpha(transparentFG || transparentBG
							? TRANSPARENT
							: OPAQUE);
		}
	}

	private void removeUDFPSGraphicsLegacy(Object object) {
		try
		{
			if (transparentBG) {
				ImageView mBgProtection = (ImageView) getObjectField(object, "mBgProtection");
				mBgProtection.setImageDrawable(new ShapeDrawable());
			}

			if (transparentFG) {
				ImageView mLockScreenFp = (ImageView) getObjectField(object, "mLockScreenFp");
				mLockScreenFp.setImageDrawable(new ShapeDrawable());
				
				ImageView mAodFp = (ImageView) getObjectField(object, "mAodFp");
				mAodFp.setImageDrawable(new ShapeDrawable());
			}
		}
		catch (Throwable ignored){}
	}
}

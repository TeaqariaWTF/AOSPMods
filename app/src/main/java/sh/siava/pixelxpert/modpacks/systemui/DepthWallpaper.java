package sh.siava.pixelxpert.modpacks.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static sh.siava.pixelxpert.modpacks.Constants.AI_METHOD_MLKIT;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.SystemUtils.idOf;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.reAddView;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools;

/** @noinspection RedundantThrows, SameParameterValue */
public class DepthWallpaper extends XposedModPack {
	private static final String TARGET_PACKAGE = Constants.SYSTEM_UI_PACKAGE;
	private static boolean lockScreenSubjectCacheValid = false;
	private Object mScrimController;
	private static boolean DWallpaperEnabled = false;
	private static int DWOpacity = 192;

	private static boolean DWonAOD = false;
	private FrameLayout mLockScreenSubject;
	private Drawable mSubjectDimmingOverlay;
	private FrameLayout mWallpaperBackground;
	private FrameLayout mWallpaperBitmapContainer;
	private FrameLayout mWallpaperDimmingOverlay;
	private boolean mLayersCreated = false;

	private static int SegmentorAI = AI_METHOD_MLKIT;
	public DepthWallpaper(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		DWallpaperEnabled = Xprefs.getBoolean("DWallpaperEnabled", false);
		DWOpacity = Xprefs.getSliderInt("DWOpacity", 192);
		DWonAOD = Xprefs.getBoolean("DWonAOD", false);
		SegmentorAI = Integer.parseInt(Xprefs.getString("SegmentorAI", String.valueOf(AI_METHOD_MLKIT)));
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass CanvasEngineClass = ReflectedClass.of("com.android.systemui.wallpapers.ImageWallpaper$CanvasEngine");
		ReflectedClass ScrimControllerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.ScrimController");
		ReflectedClass ScrimViewClass = ReflectedClass.of("com.android.systemui.scrim.ScrimView");
		ReflectedClass AodBurnInLayerClass = ReflectedClass.ofIfPossible("com.android.systemui.keyguard.ui.view.layout.sections.AodBurnInLayer");

		AodBurnInLayerClass
				.afterConstruction()
				.run(param -> {
					View entryV = (View) param.thisObject;

					if(!DWallpaperEnabled) return;

					entryV.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
						@SuppressLint("DiscouragedApi")
						@Override
						public void onViewAttachedToWindow(@NonNull View v) {
							ReflectionTools.runDelayedOnMainThread(entryV, 1000, () -> {
								ViewGroup rootView = (ViewGroup) entryV.getParent();

								rootView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
									@Override
									public void onChildViewAdded(View parent, View child) {
										hookClocks(rootView);
									}

									@Override
									public void onChildViewRemoved(View parent, View child) {
									}
								});
								hookClocks(rootView.getRootView());

								if(!mLayersCreated) {
									createLayers();
								}

								reAddView((ViewGroup) rootView.getRootView(), mWallpaperBackground, 0);
								reAddView(rootView, mLockScreenSubject);
							});
						}

						@Override
						public void onViewDetachedFromWindow(@NonNull View v) {
						}
					});
				});

		ScrimViewClass
				.before("setViewAlpha")
				.run(param -> {
					if(!mLayersCreated) return;

					setDepthWallpaper();
					//noinspection ConstantValue
					if(DWonAOD
							&& !getObjectField(mScrimController, "mState").toString().equals("KEYGUARD")) {
						mLockScreenSubject.post(() -> mLockScreenSubject.setAlpha(DWOpacity));
					}
					else if(getObjectField(mScrimController, "mNotificationsScrim").equals(param.thisObject)) //instead of using the mScrimName since older ones don't have that field
					{
						float mScrimBehindAlphaKeyguard = getFloatField(mScrimController, "mScrimBehindAlphaKeyguard");

						float notificationAlpha = (float)param.args[0];

						if(notificationAlpha < mScrimBehindAlphaKeyguard)
							notificationAlpha = 0;

						float subjectAlpha = (notificationAlpha > mScrimBehindAlphaKeyguard)
								? (1f - notificationAlpha) / (1f - mScrimBehindAlphaKeyguard)
								: 1f;

						mLockScreenSubject.post(() -> mLockScreenSubject.setAlpha(subjectAlpha));
					}
				});

		CanvasEngineClass
				.after("onSurfaceDestroyed")
				.run(param -> {
					if(DWallpaperEnabled && isLockScreenWallpaper(param.thisObject))
					{
						invalidateLSWSC();
					}
				});

		CanvasEngineClass
				.after("onCreate")
				.run(param -> {
					if(
							callMethod(
									getObjectField(param.thisObject, "mWallpaperManager"),
									"getWallpaperInfo", WallpaperManager.FLAG_LOCK)
									!= null) //it's live wallpaper. we can't use that
					{
						invalidateLSWSC();
					}
				});

		final Thread[] wallpaperProcessorThread = {null};

		CanvasEngineClass
				.after("drawFrameOnCanvas")
				.run(param -> {
					if(wallpaperProcessorThread[0] != null)
					{
						wallpaperProcessorThread[0].interrupt();
					}

					if(DWallpaperEnabled && isLockScreenWallpaper(param.thisObject))
					{
						wallpaperProcessorThread[0] =new Thread(() -> {
							Bitmap wallpaperBitmap = Bitmap.createBitmap((Bitmap) param.args[0]);

							boolean cacheIsValid = assertCache(wallpaperBitmap);

							Rect displayBounds =  ((Context) callMethod(param.thisObject, "getDisplayContext")).getSystemService(WindowManager.class)
									.getCurrentWindowMetrics()
									.getBounds();

							float ratioW = 1f * displayBounds.width() / wallpaperBitmap.getWidth();
							float ratioH = 1f * displayBounds.height() / wallpaperBitmap.getHeight();

							int desiredHeight = Math.round(Math.max(ratioH, ratioW) * wallpaperBitmap.getHeight());
							int desiredWidth = Math.round(Math.max(ratioH, ratioW) * wallpaperBitmap.getWidth());

							int xPixelShift = (desiredWidth - displayBounds.width()) / 2;
							int yPixelShift = (desiredHeight - displayBounds.height()) / 2;

							Bitmap scaledWallpaperBitmap = Bitmap.createScaledBitmap(wallpaperBitmap, desiredWidth, desiredHeight, true);

							//crop to display bounds
							scaledWallpaperBitmap = Bitmap.createBitmap(scaledWallpaperBitmap, xPixelShift, yPixelShift, displayBounds.width(), displayBounds.height());
							Bitmap finalScaledWallpaperBitmap = Bitmap.createBitmap(scaledWallpaperBitmap);

							if(!mLayersCreated) {
								createLayers();
							}

							mWallpaperBackground.post(() -> mWallpaperBitmapContainer.setBackground(new BitmapDrawable(mContext.getResources(), finalScaledWallpaperBitmap)));

							if(!cacheIsValid) {
								try {
									String cachePath = Constants.getLockScreenSubjectCachePath(mContext);

									Bitmap subjectBitmap = XPLauncher.getRootProviderProxy().extractSubject(finalScaledWallpaperBitmap, SegmentorAI);

									if(subjectBitmap != null) {
										FileOutputStream subjectOutputStream = new FileOutputStream(cachePath);
										subjectBitmap.compress(Bitmap.CompressFormat.PNG, 100, subjectOutputStream);
										subjectOutputStream.close();

										Thread.sleep(500); //letting the filesystem settle down

										setDepthWallpaper();
									}
								} catch (Throwable ignored) {}
							}

							wallpaperProcessorThread[0] = null;
						});
						wallpaperProcessorThread[0].start();
					}
				});

		ScrimControllerClass
				.afterConstruction()
				.run(param -> mScrimController = param.thisObject);
	}

	private void hookClocks(View rootView) {
		ViewGroup keyguardRoot = rootView.findViewById(idOf("keyguard_root_view"));
		for(int i = 0; i < keyguardRoot.getChildCount(); i++)
		{
			View child = keyguardRoot.getChildAt(i);
			if(child.getClass().getName().startsWith("com.android.systemui.clocks") || child.getClass().getName().startsWith("com.android.systemui.shared.clocks.view"))
			{
				setSendBackListeners(child);
				child.setZ(-1);
			}
		}
	}

	private void setSendBackListeners(View thisView) {
		if(getAdditionalInstanceField(thisView, "DepthHooked") != null)
			return;

		setAdditionalInstanceField(thisView, "DepthHooked", true);
		thisView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(@NonNull View v) {
				v.setZ(-1);
			}

			@Override
			public void onViewDetachedFromWindow(@NonNull View v) {

			}
		});

		thisView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
				v.setZ(-1));
	}

	private boolean assertCache(Bitmap wallpaperBitmap) {

		boolean cacheIsValid = false;
		try
		{
			File wallpaperCacheFile = new File(Constants.getLockScreenBitmapCachePath(mContext));

			ByteArrayOutputStream compressedBitmap = new ByteArrayOutputStream();
			wallpaperBitmap.compress(Bitmap.CompressFormat.JPEG, 100, compressedBitmap);
			if(wallpaperCacheFile.exists())
			{
				FileInputStream cacheStream = new FileInputStream(wallpaperCacheFile);

				if(Arrays.equals(cacheStream.readAllBytes(), compressedBitmap.toByteArray()))
				{
					cacheIsValid = true;
				}
				cacheStream.close();
			}

			if(!cacheIsValid)
			{
				FileOutputStream newCacheStream = new FileOutputStream(wallpaperCacheFile);
				compressedBitmap.writeTo(newCacheStream);
				newCacheStream.close();
			}
			compressedBitmap.close();
		}
		catch (Throwable ignored)
		{}

		if(!cacheIsValid)
		{
			invalidateLSWSC();
		}

		return cacheIsValid;
	}

	private void createLayers() {
		mWallpaperBackground = new FrameLayout(mContext);
		mWallpaperBackground.setId(View.generateViewId());
		mWallpaperDimmingOverlay = new FrameLayout(mContext);
		mWallpaperBitmapContainer = new FrameLayout(mContext);
		FrameLayout.LayoutParams lpw = new FrameLayout.LayoutParams(-1, -1);

		mWallpaperDimmingOverlay.setBackgroundColor(Color.BLACK);
		mWallpaperDimmingOverlay.setLayoutParams(lpw);
		mWallpaperBitmapContainer.setLayoutParams(lpw);

		mWallpaperBackground.addView(mWallpaperBitmapContainer);
		mWallpaperBackground.addView(mWallpaperDimmingOverlay);
		mWallpaperBackground.setLayoutParams(lpw);

		mLockScreenSubject = new FrameLayout(mContext);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
		mLockScreenSubject.setLayoutParams(lp);

		mLockScreenSubject.setId(View.generateViewId()); //a fake ID so that it can be added to constrained layout

		mLayersCreated = true;
	}

	private boolean isLockScreenWallpaper(Object canvasEngine)
	{
		return (getWallpaperFlag(canvasEngine)
				& WallpaperManager.FLAG_LOCK)
				== WallpaperManager.FLAG_LOCK;
	}
	private void setDepthWallpaper()
	{
		String state = getObjectField(mScrimController, "mState").toString();
		boolean showSubject = DWallpaperEnabled
				&&
				(
						state.equals("KEYGUARD")
						||
								(DWonAOD
										&&
										(state.equals("AOD") || state.equals("PULSING"))
								)
				);

		if(showSubject) {
			if(!lockScreenSubjectCacheValid && isSubjectCacheAvailable())
			{
				try (FileInputStream inputStream = new FileInputStream(Constants.getLockScreenSubjectCachePath(mContext)))
				{
					Drawable bitmapDrawable = BitmapDrawable.createFromStream(inputStream, "");
					bitmapDrawable.setAlpha(255);

					mSubjectDimmingOverlay = bitmapDrawable.getConstantState().newDrawable().mutate();
					mSubjectDimmingOverlay.setTint(Color.BLACK);

					mLockScreenSubject.setBackground(new LayerDrawable(new Drawable[]{bitmapDrawable, mSubjectDimmingOverlay}));
					lockScreenSubjectCacheValid = true;
				}
				catch (Throwable ignored) {}
			}

			if(lockScreenSubjectCacheValid) {
				mLockScreenSubject.getBackground().setAlpha(DWOpacity);

				if(!state.equals("KEYGUARD")) { //AOD
					mSubjectDimmingOverlay.setAlpha(192 /*Math.round(192 * (DWOpacity / 255f))*/);
				}
				else {
					//this is the dimmed wallpaper coverage
					mSubjectDimmingOverlay.setAlpha(Math.round(getFloatField(mScrimController, "mScrimBehindAlphaKeyguard") * 240)); //A tad bit lower than max. show it a bit lighter than other stuff
					mWallpaperDimmingOverlay.setAlpha(getFloatField(mScrimController, "mScrimBehindAlphaKeyguard"));
				}

				mWallpaperBackground.setVisibility(VISIBLE);
				mLockScreenSubject.setVisibility(VISIBLE);
			}
		}
		else if(mLayersCreated)
		{
			mLockScreenSubject.setVisibility(GONE);

			if (state.equals("UNLOCKED")) {
				mWallpaperBackground.setVisibility(GONE);
			}
		}
		mLockScreenSubject.setZ(-.5f);
	}

	private boolean isSubjectCacheAvailable() {
		try {
			return new File(Constants.getLockScreenSubjectCachePath(mContext)).length() > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private int getWallpaperFlag(Object canvasEngine) {
		return (int) callMethod(canvasEngine, "getWallpaperFlags");
	}

	private void invalidateLSWSC() //invalidate lock screen wallpaper subject cache
	{
		lockScreenSubjectCacheValid = false;
		if(mLayersCreated) {
			mLockScreenSubject.post(() -> {
				mLockScreenSubject.setVisibility(GONE);
				mLockScreenSubject.setBackground(null);
				mWallpaperBackground.setVisibility(GONE);
				mWallpaperBitmapContainer.setBackground(null);
			});
		}
		try {
			//noinspection ResultOfMethodCallIgnored
			new File(Constants.getLockScreenSubjectCachePath(mContext)).delete();
		}
		catch (Throwable ignored){}
	}

	@Override
	public boolean isTargeting(String packageName) {
		return TARGET_PACKAGE.equals(packageName) && !XPLauncher.isChildProcess;
	}
}

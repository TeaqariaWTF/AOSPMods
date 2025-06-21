package sh.siava.pixelxpert.modpacks.launcher;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Process;
import android.view.MotionEvent;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.annotations.LauncherModPack;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

/** @noinspection ConstantValue*/
@SuppressWarnings("RedundantThrows")
@LauncherModPack
public class CustomNavGestures extends XposedModPack {
	private static final int NO_ACTION = -1;
	private static final int ACTION_SCREENSHOT = 1;
	private static final int ACTION_BACK = 2;
	private static final int ACTION_KILL_APP = 3;
	private static final int ACTION_NOTIFICATION = 4;
	private static final int ACTION_ONE_HANDED = 5;
	private static final int ACTION_SLEEP = 7;
	private static final int ACTION_SWITCH_APP_PROFILE = 8;
	private static final int TAKE_SCREENSHOT_FULLSCREEN = 1;

	private static final int SWIPE_NONE = 0;
	private static final int SWIPE_LEFT = 1;
	private static final int SWIPE_RIGHT = 2;
	private static final int SWIPE_TWO_FINGER = 3;

	private boolean FCHandled = false;
	private float leftSwipeUpPercentage = 0.25f, rightSwipeUpPercentage = 0.25f;
	private int displayW = -1, displayH = -1;
	private static float swipeUpPercentage = 0.2f;
	private int swipeType = SWIPE_NONE;

	@SuppressWarnings("FieldCanBeLocal")
	private boolean isLandscape = false;
	private float mSwipeUpThreshold = 0;
	private float mLongThreshold = 0;
	private static boolean FCLongSwipeEnabled = false;
	private Object mSystemUIProxy = null;
	private static int leftSwipeUpAction = NO_ACTION, rightSwipeUpAction = NO_ACTION, twoFingerSwipeUpAction = NO_ACTION;
	private Object mSysUiProxy;
	private Object currentFocusedTask = null;
	private boolean mTasksIsList = false;

	public CustomNavGestures(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		FCLongSwipeEnabled = Xprefs.getBoolean("FCLongSwipeEnabled", false);
		leftSwipeUpAction = readAction(Xprefs, "leftSwipeUpAction");
		rightSwipeUpAction = readAction(Xprefs, "rightSwipeUpAction");
		twoFingerSwipeUpAction = readAction(Xprefs, "twoFingerSwipeUpAction");
		leftSwipeUpPercentage = Xprefs.getSliderFloat( "leftSwipeUpPercentage", 25f) / 100f;
		rightSwipeUpPercentage = Xprefs.getSliderFloat( "rightSwipeUpPercentage", 25f) / 100f;
		swipeUpPercentage = Xprefs.getSliderFloat( "swipeUpPercentage", 25f) / 100f;
	}

	private static int readAction(SharedPreferences xprefs, String prefName) {
		try {
			return Integer.parseInt(xprefs.getString(prefName, "").trim());
		} catch (Exception ignored) {
			return NO_ACTION;
		}
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass OtherActivityInputConsumerClass = ReflectedClass.of("com.android.quickstep.inputconsumers.OtherActivityInputConsumer"); //When apps are open
		ReflectedClass OverviewInputConsumerClass = ReflectedClass.of("com.android.quickstep.inputconsumers.OverviewInputConsumer"); //When on Home screen and Recents
		ReflectedClass SystemUiProxyClass = ReflectedClass.of("com.android.quickstep.SystemUiProxy");
		ReflectedClass RecentTasksListClass = ReflectedClass.of("com.android.quickstep.RecentTasksList");



		//noinspection DataFlowIssue
		Rect displayBounds = SystemUtils.WindowManager().getMaximumWindowMetrics().getBounds();
		displayW = Math.min(displayBounds.width(), displayBounds.height());
		displayH = Math.max(displayBounds.width(), displayBounds.height());

		RecentTasksListClass
				.afterConstruction()
				.run(param -> mSysUiProxy = getObjectField(param.thisObject, "mSysUiProxy"));

		SystemUiProxyClass
				.afterConstruction()
				.run(param -> mSystemUIProxy = param.thisObject);

		OtherActivityInputConsumerClass
				.before("onMotionEvent")
				.run(param -> onMotionEvent(param, false));

		OverviewInputConsumerClass
				.before("onMotionEvent")
				.run(param -> onMotionEvent(param, true));
	}

	private void onMotionEvent(XC_MethodHook.MethodHookParam param, boolean isOverViewListener) {
		MotionEvent e = (MotionEvent) param.args[0];

		boolean mPassedWindowMoveSlop = isOverViewListener //if it's overview page (read: home page) we don't need this. true is good
				|| getBooleanField(param.thisObject, "mPassedWindowMoveSlop"); //checking if they've swiped long enough to cancel touch for app

		int action = e.getActionMasked();
		int pointers = e.getPointerCount();

		if (action == MotionEvent.ACTION_DOWN) //Let's get ready
		{
			saveFocusedTask();

			FCHandled = false;
			swipeType = SWIPE_NONE;

			if (isOverViewListener
					&& getBooleanField(param.thisObject, "mStartingInActivityBounds")) {
				return;
			}

			mSwipeUpThreshold = e.getY() * (1f - swipeUpPercentage);
			mLongThreshold = e.getY() / 10f;

			isLandscape = e.getY() < displayW; //launcher rotation can be always 0. So....
			int currentW = isLandscape ? displayH : displayW;

			if (pointers == 1) {
				if (leftSwipeUpAction != NO_ACTION
						&& e.getX() < currentW * leftSwipeUpPercentage) {
					swipeType = SWIPE_LEFT;
				} else if (rightSwipeUpAction != NO_ACTION
						&& e.getX() > currentW * (1f - rightSwipeUpPercentage)) {
					swipeType = SWIPE_RIGHT;
				}
			}
		}

		if (twoFingerSwipeUpAction != NO_ACTION
				&& pointers == 2
				&& swipeType != SWIPE_TWO_FINGER) { //must be outside down. usually down is one finger
			swipeType = SWIPE_TWO_FINGER;
		}

		if (mPassedWindowMoveSlop
				&& swipeType != SWIPE_NONE) { //shouldn't reach the main code anymore
			param.setResult(null);
		}

		if (pointers == 1) {
			boolean FCAllowed = swipeType == SWIPE_NONE;

			if (FCAllowed
					&& FCLongSwipeEnabled
					&& e.getY() < mLongThreshold
					&& !FCHandled
					&& !isOverViewListener) { //swiped up too much
				setObjectField(param.thisObject, "mActivePointerId", MotionEvent.INVALID_POINTER_ID);
				setObjectField(param.thisObject, "mPassedWindowMoveSlop", false);
				FCHandled = true;
				runAction(ACTION_KILL_APP);
			}
		}

		if (action == MotionEvent.ACTION_UP
				&& swipeType != SWIPE_NONE) {
			if (!isOverViewListener) {
				callMethod(param.thisObject, "forceCancelGesture", e);
			}

			if (e.getY() < mSwipeUpThreshold) {
				switch (swipeType) {
					case SWIPE_LEFT:
						runAction(leftSwipeUpAction);
						break;
					case SWIPE_RIGHT:
						runAction(rightSwipeUpAction);
						break;
					case SWIPE_TWO_FINGER:
						runAction(twoFingerSwipeUpAction);
						break;
				}
				swipeType = SWIPE_NONE;
			}

			currentFocusedTask = null;
		}
	}

	String mTasksFieldName = null; // in case the code was obfuscated
	private void saveFocusedTask() {
		try
		{
			ArrayList<?> recentTaskList = (ArrayList<?>) callMethod(
					mSysUiProxy,
					"getRecentTasks",
					1,
					callMethod(Process.myUserHandle(), "getIdentifier"));

			if(recentTaskList.isEmpty()) return;

			if (mTasksFieldName == null) {
				for (Field f : recentTaskList.get(0).getClass().getDeclaredFields()) {
					if (f.getType().getName().contains("RecentTaskInfo")) {
						mTasksFieldName = f.getName();
					}
				}
			}
			if (mTasksFieldName == null) {
				for (Field f : recentTaskList.get(0).getClass().getDeclaredFields()) {
					if (f.getType().getName().contains("List"))
					{
						if(!f.isAccessible()) {
							f.setAccessible(true);
						}
						//noinspection unchecked
						List<Object> list = (List<Object>) f.get(recentTaskList.get(0));
						if(list != null && findFieldIfExists(list.get(0).getClass(), "isFocused") != null) {
							mTasksFieldName = f.getName();
							mTasksIsList = true;
						}
					}
				}
			}

			Optional<?> focusedTask = recentTaskList.stream().filter(recentTask ->
					(boolean) getObjectField(
							mTasksIsList
									? ((List<?>) getObjectField(recentTask, mTasksFieldName)).get(0)
									: ((Object[]) getObjectField(recentTask, mTasksFieldName))[0],
							"isFocused"
					)).findFirst();

			currentFocusedTask = focusedTask.map(o -> mTasksIsList
					? ((List<?>) getObjectField(o, mTasksFieldName)).get(0)
					: ((Object[]) getObjectField(o, mTasksFieldName))[0])
					.orElse(null);
		}
		catch (Throwable ignored){}
	}

	private void runAction(int action) {
		switch (action) {
			case ACTION_BACK:
				goBack();
				break;
			case ACTION_SCREENSHOT:
				takeScreenshot();
				break;
			case ACTION_NOTIFICATION:
				toggleNotification();
				break;
			case ACTION_KILL_APP:
				killForeground();
				break;
			case ACTION_ONE_HANDED:
				startOneHandedMode();
				break;
			case ACTION_SLEEP:
				goToSleep();
				break;
			case ACTION_SWITCH_APP_PROFILE:
				switchAppProfile();
				break;
		}
	}

	private void switchAppProfile() {
		new Thread(() -> {
			try
			{
				SystemUtils.threadSleep(200); //waiting for recents window to vanish

				mContext.sendBroadcast(Constants.getAppProfileSwitchIntent());
			}
			catch (Throwable ignored)
			{}
		}).start();
	}

	private void goToSleep() {
		Intent broadcast = new Intent();
		broadcast.setAction(Constants.ACTION_SLEEP);
		mContext.sendBroadcast(broadcast);
	}

	private void killForeground() {
		if(currentFocusedTask == null) return;

		try
		{
			Toast.makeText(mContext, "App Killed", Toast.LENGTH_SHORT).show();

			callMethod(mContext.getSystemService(Context.ACTIVITY_SERVICE),
					"forceStopPackageAsUser",
					((ComponentName) getObjectField(currentFocusedTask, "realActivity")).getPackageName(),
					getObjectField(currentFocusedTask, "userId"));

			goHome();
		}
		catch (Throwable ignored) {}
	}

	private void goBack() {
		callMethod(mSystemUIProxy, "onBackPressed");
	}

	private void startOneHandedMode() {
		callMethod(mSystemUIProxy, "startOneHandedMode");
	}

	private void toggleNotification() {
		callMethod(mSystemUIProxy, "toggleNotificationPanel");
	}

	private void takeScreenshot() {
		try {
			ReflectedClass ScreenshotRequestBuilderClass = ReflectedClass.of("com.android.internal.util.ScreenshotRequest$Builder");

			Object screenshotRequestBuilder = ScreenshotRequestBuilderClass
					.getClazz()
					.getConstructor(int.class, int.class)
					.newInstance(TAKE_SCREENSHOT_FULLSCREEN,1);

			callMethod(mSystemUIProxy,
					"takeScreenshot",
					callMethod(
							screenshotRequestBuilder,
							"build")
			);
		}
		catch (Throwable ignored) {}
	}

	private void goHome() {
		Intent broadcast = new Intent();
		broadcast.setAction(Constants.ACTION_HOME);
		mContext.sendBroadcast(broadcast);
	}
}
package sh.siava.pixelxpert.xposed.modpacks.android;

import static android.os.VibrationAttributes.USAGE_ACCESSIBILITY;
import static android.os.VibrationEffect.EFFECT_TICK;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_CAMERA;
import static android.view.KeyEvent.KEYCODE_POWER;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.AudioManager;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.CameraManager;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.PowerManager;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.getFlashStrengthPCT;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.isFlashOn;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.sleep;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.threadSleep;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.toggleFlash;
import static sh.siava.pixelxpert.xposed.utils.SystemUtils.vibrate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import org.apache.commons.lang3.SystemProperties;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.xposed.annotations.FrameworkModPack;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedMethod;

@SuppressWarnings("RedundantThrows")
@FrameworkModPack
public class ScreenOffKeys extends XposedModPack {
	public static final int PHYSICAL_ACTION_NONE = -1;

	public static final int PHYSICAL_ACTION_DEFAULT = 0;
	public static final int PHYSICAL_ACTION_TORCH = 1;
	public static final int PHYSICAL_ACTION_CAMERA = 2;
	public static final int PHYSICAL_ACTION_ASSISTANT = 3;
	/**
	 * @noinspection unused
	 */
	public static final int PHYSICAL_ACTION_DND = 4;
	public static final int PHYSICAL_ACTION_PLAY_PAUSE = 5;
	public static final int PHYSICAL_ACTION_MEDIA_NEXT = 6;
	public static final int PHYSICAL_ACTION_MEDIA_PREV = 7;

	public static final int WAKE_REASON_POWER_BUTTON = 1;
	public static final int CAMERA_LAUNCH_SOURCE_POWER_DOUBLE_TAP = 1;
	private static final int INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS = 6;
	public static final int ACTION_COMPLETE = 1; // SingleKeyGestureEvent


	private static int longPressPowerButtonScreenOff = 0;
	private static int longPressPowerButtonScreenOn = 0;
	private static int doublePressPowerButtonScreenOff = 0;
	private static int doublePressPowerButtonScreenOn = 0;
	private static int longPressVolumeUpButtonScreenOff = 0;
	private static int longPressVolumeDownButtonScreenOff = 0;
	private static boolean AnimateFlashlight = false;

	private static boolean controlFlashWithVolKeys = false;

	ReflectedMethod launchAssistActionMethod;
	private Object windowMan;
	private long mWakeTime = 0;

	VolumeLongPressRunnable mVolumeLongPress = new VolumeLongPressRunnable(PHYSICAL_ACTION_DEFAULT);
	FlashAdjustLongRunnable mFlashAdjustRunnable = new FlashAdjustLongRunnable(KEYCODE_VOLUME_DOWN);
	final Object mLock = new Object();
	boolean mKeyIsDown = false;
	boolean mLoopRan = false;

	public ScreenOffKeys(Context context) {
		super(context);
	}

	@SuppressLint("CheckResult")
	@Override
	public void onPreferenceUpdated(String... Key) {
		try {
			longPressPowerButtonScreenOff = Integer.parseInt(Xprefs.getString("longPressPowerButtonScreenOff", "0"));
			longPressPowerButtonScreenOn = Integer.parseInt(Xprefs.getString("longPressPowerButtonScreenOn", "0"));

			doublePressPowerButtonScreenOff = Integer.parseInt(Xprefs.getString("doublePressPowerButtonScreenOff", "0"));
			doublePressPowerButtonScreenOn = Integer.parseInt(Xprefs.getString("doublePressPowerButtonScreenOn", "0"));

			longPressVolumeUpButtonScreenOff = Integer.parseInt(Xprefs.getString("longPressVolumeUpButtonScreenOff", "0"));
			longPressVolumeDownButtonScreenOff = Integer.parseInt(Xprefs.getString("longPressVolumeDownButtonScreenOff", "0"));

			controlFlashWithVolKeys = Xprefs.getBoolean("controlFlashWithVolKeys", false);

			AnimateFlashlight = Xprefs.getBoolean("AnimateFlashlight", false);
			//noinspection ResultOfMethodCallIgnored
			CameraManager(); //init CameraManager to listen to flash status
		} catch (Throwable ignored) {
		}
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			ReflectedClass PhoneWindowManagerClass = ReflectedClass.of("com.android.server.policy.PhoneWindowManager");
			ReflectedClass PowerKeyRuleClass = ReflectedClass.of("com.android.server.policy.PhoneWindowManager$PowerKeyRule");
			ReflectedClass GestureLauncherServiceClass = ReflectedClass.of("com.android.server.GestureLauncherService");

			launchAssistActionMethod = ReflectedMethod.ofName(PhoneWindowManagerClass, "launchAssistAction");

			GestureLauncherServiceClass.before("handleCameraGesture").run(param -> {
				boolean screenIsOn = screenIsOn();

				boolean handled = launchAction(resolveAction(KEYCODE_CAMERA, screenIsOn),
						screenIsOn,
						true);

				if (handled)
					param.setResult(true);
			});

			PhoneWindowManagerClass
					.after("enableScreen")
					.run(param -> {
						windowMan = param.thisObject;

						setObjectField(getObjectField(param.thisObject, "mGestureLauncherService"),
								"mCameraDoubleTapPowerEnabled",
								true);
					});

			PowerKeyRuleClass
					.before("onLongPress")
					.run(param -> {
						try { //TODO: no need to try/catch once QPR1 stable is released
							if ((int) callMethod(
									param.args[0],
									"getAction")
									!= ACTION_COMPLETE)
								return;
						} catch (Throwable ignored){}

						boolean screenIsOn = screenIsOn();

						if (launchAction(resolveAction(KEYCODE_POWER, screenIsOn),
								screenIsOn,
								false))
							param.setResult(null);
					});

					PhoneWindowManagerClass
							.before("startedWakingUp")
							.run(param -> {
								if ((int) param.args[param.args.length - 1] == WAKE_REASON_POWER_BUTTON) {
									mWakeTime = SystemClock.uptimeMillis();
								}
							});

			PhoneWindowManagerClass
					.before("interceptKeyBeforeQueueing")
					.run(param -> {
						try {
							KeyEvent event = (KeyEvent) param.args[0];
							int keyCode = event.getKeyCode();

							if ((keyCode == KEYCODE_VOLUME_UP || keyCode == KEYCODE_VOLUME_DOWN)
									&& controlFlashWithVolKeys
									&& isFlashOn()) {
								Handler handler = (Handler) getObjectField(param.thisObject, "mHandler");
								handleFlashKeys(event, handler);
								param.setResult(0);
								return;
							}

							if (!deviceIsInteractive() &&
									((keyCode == KEYCODE_VOLUME_UP && longPressVolumeUpButtonScreenOff != PHYSICAL_ACTION_DEFAULT) ||
											(keyCode == KEYCODE_VOLUME_DOWN && longPressVolumeDownButtonScreenOff != PHYSICAL_ACTION_DEFAULT))) {
								Handler handler = (Handler) getObjectField(param.thisObject, "mHandler");

								switch (event.getAction()) {
									case KeyEvent.ACTION_UP:
										if (handler.hasCallbacks(mVolumeLongPress)) {
											AudioManager().adjustStreamVolume(AudioManager.STREAM_MUSIC, keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, 0);
											handler.removeCallbacks(mVolumeLongPress);
											param.setResult(0);
										}
										return;
									case KeyEvent.ACTION_DOWN:
										int action = resolveAction(keyCode, false);

										mVolumeLongPress = new VolumeLongPressRunnable(action);
										if (isActionLaunchable(action)) {
											handler.postDelayed(mVolumeLongPress, ViewConfiguration.getLongPressTimeout());
											param.setResult(0);
										}
										break;
								}
							}
						} catch (Throwable ignored) {
						}
					});
		} catch (Throwable ignored) {
		}
	}

	private void handleFlashKeys(KeyEvent event, Handler handler) {
		int action = event.getAction();

		switch (action) {
			case ACTION_DOWN:
				mFlashAdjustRunnable = new FlashAdjustLongRunnable(event.getKeyCode());
				synchronized (mLock) {
					mKeyIsDown = true;
					mLoopRan = false;
				}
				handler.postDelayed(mFlashAdjustRunnable, ViewConfiguration.getLongPressTimeout());
				break;
			case ACTION_UP:
				synchronized (mLock) {
					mKeyIsDown = false;
				}
				if (handler.hasCallbacks(mFlashAdjustRunnable)) {
					handler.removeCallbacks(mFlashAdjustRunnable);
				}
				synchronized (mLock) {
					if (!mLoopRan) {
						adjustFlashStep(event.getKeyCode());
					}
				}
				break;
		}
	}

	private void adjustFlashStep(int keyCode) {
		float step = (keyCode == KEYCODE_VOLUME_UP ? 1f : -1f)
				/ SystemProperties.getInt("ro.config.media_vol_steps", () -> 10);

		SystemUtils.setFlash(true, SystemUtils.getFlashlightLevel(getFlashStrengthPCT() + step), false);
	}

	private int resolveAction(int keyCode, boolean screenIsOn) {
		boolean flashIsOn = isFlashOn();

		return switch (keyCode) {
			case KEYCODE_CAMERA -> {
				if (doublePressPowerButtonScreenOff == PHYSICAL_ACTION_TORCH && flashIsOn) {
					yield PHYSICAL_ACTION_TORCH;
				}
				yield screenIsOn
						? doublePressPowerButtonScreenOn
						: doublePressPowerButtonScreenOff;
			}
			case KEYCODE_VOLUME_DOWN -> longPressVolumeDownButtonScreenOff;
			case KEYCODE_VOLUME_UP -> longPressVolumeUpButtonScreenOff;
			case KEYCODE_POWER -> {
				if (longPressPowerButtonScreenOff == PHYSICAL_ACTION_TORCH && flashIsOn) {
					yield PHYSICAL_ACTION_TORCH;
				}
				yield screenIsOn
						? longPressPowerButtonScreenOn
						: longPressPowerButtonScreenOff;
			}
			default -> PHYSICAL_ACTION_DEFAULT;
		};
	}

	private boolean isActionLaunchable(int action) {
		return switch (action) {
			case PHYSICAL_ACTION_MEDIA_NEXT, PHYSICAL_ACTION_MEDIA_PREV ->
				//noinspection DataFlowIssue
					AudioManager().isMusicActive();
			default -> true;
		};
	}

	private boolean screenIsOn() { //for power button, display state isn't reliable enough because pressing power will trigger it
		return SystemClock.uptimeMillis() - mWakeTime > 1000;
	}

	private boolean deviceIsInteractive() {
		//noinspection DataFlowIssue
		return PowerManager().isInteractive();
	}

	/**
	 * @noinspection DataFlowIssue
	 */
	private boolean launchAction(int action, boolean screenIsOn, boolean delaySleep) {
		try {
			boolean handled = false;
			boolean shouldSleep = true;

			switch (action) {
				case PHYSICAL_ACTION_NONE:
					handled = true;
					break;
				case PHYSICAL_ACTION_TORCH:
					toggleFlash(AnimateFlashlight);
					handled = true;
					break;
				case PHYSICAL_ACTION_CAMERA:
					try {
						Object gestureLauncherService = getObjectField(windowMan, "mGestureLauncherService");
						handled = (boolean) callMethod(gestureLauncherService, "handleCameraGesture", false, CAMERA_LAUNCH_SOURCE_POWER_DOUBLE_TAP);
						shouldSleep = false;
					} catch (Throwable ignored) {
					}
					break;
				case PHYSICAL_ACTION_ASSISTANT:
					try {
						launchAssistActionMethod.invoke(windowMan, null, -2, SystemClock.uptimeMillis(), INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS);
						handled = true;
						shouldSleep = false;
					} catch (Throwable ignored) {
					}
					break;
				case PHYSICAL_ACTION_PLAY_PAUSE:
					dispatchAudioKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
					handled = true;
					break;
				case PHYSICAL_ACTION_MEDIA_NEXT:
					if (AudioManager().isMusicActive()) {
						dispatchAudioKey(KeyEvent.KEYCODE_MEDIA_NEXT);
					}
					handled = true;
					break;
				case PHYSICAL_ACTION_MEDIA_PREV:
					if (AudioManager().isMusicActive()) {
						dispatchAudioKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
					}
					handled = true;
					break;
			}

			if (handled) {
				if (action != PHYSICAL_ACTION_NONE) {
					vibrate(EFFECT_TICK, USAGE_ACCESSIBILITY);
				}
				if (!screenIsOn && shouldSleep) {
					new Thread(() -> {
						if (delaySleep) {
							threadSleep(500);
						}
						sleep();
					}).start();
				}
			}

			return handled;
		} catch (Throwable ignored) {
		}
		return false;
	}

	private void dispatchAudioKey(int keyCode) {
		//noinspection DataFlowIssue
		AudioManager().dispatchMediaKeyEvent(new KeyEvent(ACTION_DOWN, keyCode));

		AudioManager().dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
	}

	class VolumeLongPressRunnable implements Runnable {
		int mAction;

		public VolumeLongPressRunnable(int action) {
			mAction = action;
		}

		@Override
		public void run() {
			launchAction(mAction, false, false);
		}
	}

	class FlashAdjustLongRunnable implements Runnable {
		int mKeyCode;

		public FlashAdjustLongRunnable(int keyCode) {
			mKeyCode = keyCode;
		}

		@Override
		public void run() {
			synchronized (mLock) {
				mLoopRan = true;
			}
			new Thread(() -> {
				try {
					while (true) {
						adjustFlashStep(mKeyCode);

						//noinspection BusyWait
						Thread.sleep(100);

						synchronized (mLock) {
							if (!mKeyIsDown) break;
						}
					}
				} catch (Throwable ignored) {
				}
			}).start();
		}
	}

}
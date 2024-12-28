package sh.siava.pixelxpert.modpacks.systemui;

import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.LinearLayout.VERTICAL;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.ResourceManager.modRes;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ColorUtils.getColorAttrDefaultColor;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import org.objenesis.ObjenesisHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import javax.security.auth.callback.Callback;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.NetworkTraffic;
import sh.siava.pixelxpert.modpacks.utils.NotificationIconContainerOverride;
import sh.siava.pixelxpert.modpacks.utils.ShyLinearLayout;
import sh.siava.pixelxpert.modpacks.utils.StringFormatter;
import sh.siava.pixelxpert.modpacks.utils.StringFormatter.FormattedStringCallback;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.batteryStyles.BatteryBarView;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

/**
 * @noinspection RedundantThrows
 */
public class StatusbarMods extends XposedModPack {
	private static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;
	//region Clock
	public static final int POSITION_LEFT = 0;
	public static final int POSITION_CENTER = 1;
	public static final int POSITION_RIGHT = 2;
	public static final int POSITION_LEFT_EXTRA_LEVEL = 3;

	private static final int AM_PM_STYLE_SMALL = 1;
	private static final int AM_PM_STYLE_GONE = 2;
	private int leftClockPadding = 0, rightClockPadding = 0;
	private static int clockPosition = POSITION_LEFT;
	private static int mAmPmStyle = AM_PM_STYLE_GONE;
	private static boolean mShowSeconds = false;
	private static String mStringFormatBefore = "", mStringFormatAfter = "";
	private static boolean mBeforeSmall = true, mAfterSmall = true;
	private Integer mBeforeClockColor = null, mAfterClockColor = null, clockColor = null;
	//endregion

	//region vibration icon
	private static boolean showVibrationIcon = false;
	//endregion

	//region network traffic
	private FrameLayout NTQSHolder = null;
	private static boolean networkOnSBEnabled = false;
	private static boolean networkOnQSEnabled = false;
	private static int networkTrafficPosition = POSITION_LEFT;
	private NetworkTraffic networkTrafficSB = null;
	private NetworkTraffic networkTrafficQS = null;
	//endregion

	//region battery bar
	private static boolean BBarEnabled;
	private static boolean BBarColorful;
	private static boolean BBOnlyWhileCharging;
	private static boolean BBOnBottom;
	private static boolean BBSetCentered;
	private static int BBOpacity = 100;
	private static int BBarHeight = 10;
	private static List<Float> batteryLevels = Arrays.asList(20f, 40f);
	private static int[] batteryColors = new int[]{Color.RED, Color.YELLOW};
	private static int chargingColor = Color.WHITE;
	private static int fastChargingColor = Color.WHITE;
	private static int powerSaveColor = Color.parseColor("#FFBF00");
	private static boolean indicateCharging = false;
	private static boolean indicateFastCharging = false;
	private static boolean indicatePowerSave = false;
	private static boolean BBarTransitColors = false;
	private static boolean BBAnimateCharging = false;
	//endregion

	//region privacy chip
	private Object mPrivacyItemController;
	private static boolean HidePrivacyChip = false;
	//endregion

	//region general use
	private static final float PADDING_DEFAULT = -0.5f;
	private static final ArrayList<ClockVisibilityCallback> clockVisibilityCallbacks = new ArrayList<>();
	private Object mActivityStarter;
	private Object QSBH = null;
	private ViewGroup mStatusBar;
	private static boolean notificationAreaMultiRow = false;
	private static int NotificationAODIconLimit = 3;
	private static int NotificationIconLimit = 4;
	private Object AODNIC;
	private Object SBNIC;
	private Object mCollapsedStatusBarFragment = null;
	private ViewGroup mStatusbarStartSide = null;
	private View mCenteredIconArea = null;
	private LinearLayout mSystemIconArea = null;
	private static int currentClockColor = 0;
	private static final ArrayList<StatusbarTextColorCallback> mTextColorCallbacks = new ArrayList<>();
	private FrameLayout fullStatusbar;
	//    private Object STB = null;

	private TextView mClockView;
	private ViewGroup mNotificationIconContainer = null;
	LinearLayout mNotificationContainerContainer;
	private LinearLayout mLeftVerticalSplitContainer;
	private LinearLayout mLeftExtraRowContainer;
	private static float SBPaddingStart = 0, SBPaddingEnd = 0;
	private Object PSBV;

	//endregion

	//region vo_data
	private static final String VO_LTE_SLOT = "volte";
	private static final String VO_WIFI_SLOT = "vowifi";

	private static boolean VolteIconEnabled = false;
	private final Executor voDataExec = Runnable::run;

	private Object mStatusBarIconController;
	private int mRemoveAllIconsForSlotParams = 1;

	private ReflectedClass StatusBarIconClass;
	private ReflectedClass StatusBarIconHolderClass;
	private ReflectedClass SystemUIDialogClass;
	private Object volteStatusbarIconHolder;
	private boolean telephonyCallbackRegistered = false;
	private boolean lastVolteAvailable = false;
	private final serverStateCallback voDataCallback = new serverStateCallback();
	//endregion

	private static boolean VowifiIconEnabled = false;
	private Object vowifiStatusbarIconHolder;
	private boolean lastVowifiAvailable = false;
	//endregion

	//region combined signal icons
	private boolean mWifiVisible = false;
	private static boolean CombineSignalIcons = false;
	private static boolean HideRoamingState = false;
	private Object mTunerService;
	public static final String ICON_HIDE_LIST = "icon_blacklist";
	//endregion
	//region app profile switch
	private static final String APP_SWITCH_SLOT = "app_switch";
	private Object mAppSwitchStatusbarIconHolder = null;

	private static boolean StatusbarAppSwitchIconEnabled = false;

	private final BroadcastReceiver mAppProfileSwitchReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Constants.ACTION_PROFILE_SWITCH_AVAILABLE.equals(intent.getAction())) {
				boolean isAvailable = intent.getBooleanExtra("available", false);
				if (isAvailable
						&& StatusbarAppSwitchIconEnabled
						&& mStatusBarIconController != null) {
					callMethod(mStatusBarIconController, "setIcon", APP_SWITCH_SLOT, mAppSwitchStatusbarIconHolder);
				} else {
					removeSBIconSlot(APP_SWITCH_SLOT);
				}
			}
		}
	};
	//endregion

	@SuppressLint("DiscouragedApi")
	public StatusbarMods(Context context) {
		super(context);
		if (!listensTo(context.getPackageName())) return;

		rightClockPadding = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("status_bar_clock_starting_padding", "dimen", mContext.getPackageName()));
		leftClockPadding = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("status_bar_left_clock_end_padding", "dimen", mContext.getPackageName()));
	}

	private void initSwitchIcon() {
		try {
			Icon appSwitchIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_app_switch);

			Object appSwitchStatusbarIcon = getStatusbarIconFor(appSwitchIcon, APP_SWITCH_SLOT);

			mAppSwitchStatusbarIconHolder = getStatusbarIconHolderFor(appSwitchStatusbarIcon);
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		StatusbarAppSwitchIconEnabled = Xprefs.getBoolean("StatusbarAppSwitchIconEnabled", false);

		HidePrivacyChip = Xprefs.getBoolean("HidePrivacyChip", false);

		HideRoamingState = Xprefs.getBoolean("HideRoamingState", false);

		CombineSignalIcons = Xprefs.getBoolean("combinedSignalEnabled", false);
		wifiVisibleChanged();

		if (Key.length > 0 && Key[0].equals("notificationAreaMultiRow")) { //WHY we check the old value? because if prefs is empty it will fill it up and count an unwanted change
			boolean newnotificationAreaMultiRow = Xprefs.getBoolean("notificationAreaMultiRow", false);
			if (newnotificationAreaMultiRow != notificationAreaMultiRow) {
				SystemUtils.killSelf();
			}
		}
		notificationAreaMultiRow = Xprefs.getBoolean("notificationAreaMultiRow", false);

		try {
			NotificationIconLimit = Integer.parseInt(Xprefs.getString("NotificationIconLimit", "").trim());
		} catch (Throwable ignored) {
			NotificationIconLimit = getIntegerResource("max_notif_static_icons", 4);
		}


		try {
			NotificationAODIconLimit = Integer.parseInt(Xprefs.getString("NotificationAODIconLimit", "").trim());
		} catch (Throwable ignored) {
			NotificationAODIconLimit = getIntegerResource("max_notif_icons_on_aod", 3);
		}

		if (AODNIC != null) {
			setObjectField(AODNIC, "maxIcons", NotificationAODIconLimit);
			setObjectField(SBNIC, "maxIcons", NotificationIconLimit);
		}

		List<Float> paddings = Xprefs.getSliderValues("statusbarPaddings", 0);

		if (paddings.size() > 1) {
			SBPaddingStart = paddings.get(0);
			SBPaddingEnd = 100f - paddings.get(1);
		}

		//region BatteryBar Settings
		BBarEnabled = Xprefs.getBoolean("BBarEnabled", false);
		BBarColorful = Xprefs.getBoolean("BBarColorful", false);
		BBOnlyWhileCharging = Xprefs.getBoolean("BBOnlyWhileCharging", false);
		BBOnBottom = Xprefs.getBoolean("BBOnBottom", false);
		BBSetCentered = Xprefs.getBoolean("BBSetCentered", false);
		BBOpacity = Xprefs.getSliderInt("BBOpacity", 100);
		BBarHeight = Xprefs.getSliderInt("BBarHeight", 50);
		BBarTransitColors = Xprefs.getBoolean("BBarTransitColors", false);
		BBAnimateCharging = Xprefs.getBoolean("BBAnimateCharging", false);

		batteryLevels = Xprefs.getSliderValues("batteryWarningRange", 0);

		batteryColors = new int[]{
				Xprefs.getInt("batteryCriticalColor", Color.RED),
				Xprefs.getInt("batteryWarningColor", Color.YELLOW)};


		indicateFastCharging = Xprefs.getBoolean("indicateFastCharging", false);
		indicatePowerSave = Xprefs.getBoolean("indicatePowerSave", false);
		indicateCharging = Xprefs.getBoolean("indicateCharging", true);

		chargingColor = Xprefs.getInt("batteryChargingColor", Color.GREEN);
		fastChargingColor = Xprefs.getInt("batteryFastChargingColor", Color.BLUE);
		powerSaveColor = Xprefs.getInt("batteryPowerSaveColor", Color.parseColor("#FFBF00"));

		if (BBarEnabled) {
			placeBatteryBar();
		}

		if (BatteryBarView.hasInstance()) {
			refreshBatteryBar(BatteryBarView.getInstance());
		}
		//endregion BatteryBar Settings


		//region network Traffic settings
		networkOnSBEnabled = Xprefs.getBoolean("networkOnSBEnabled", false);
		networkOnQSEnabled = Xprefs.getBoolean("networkOnQSEnabled", false);
		String networkTrafficModeStr = Xprefs.getString("networkTrafficMode", "0");
		int networkTrafficMode = Integer.parseInt(networkTrafficModeStr);

		boolean networkTrafficRXTop = Xprefs.getBoolean("networkTrafficRXTop", true);
		int networkTrafficDLColor = Xprefs.getInt("networkTrafficDLColor", Color.GREEN);
		int networkTrafficULColor = Xprefs.getInt("networkTrafficULColor", Color.RED);
		int networkTrafficOpacity = Xprefs.getSliderInt("networkTrafficOpacity", 100);
		int networkTrafficInterval = Xprefs.getSliderInt("networkTrafficInterval", 1);
		boolean networkTrafficColorful = Xprefs.getBoolean("networkTrafficColorful", false);
		boolean networkTrafficShowIcons = Xprefs.getBoolean("networkTrafficShowIcons", true);
		boolean networkTrafficShowInBits = Xprefs.getBoolean("networkTrafficShowInBits", false);

		if (networkOnSBEnabled || networkOnQSEnabled) {
			networkTrafficPosition = Integer.parseInt(Xprefs.getString("networkTrafficPosition", String.valueOf(POSITION_RIGHT)));
			if (networkTrafficPosition == POSITION_LEFT_EXTRA_LEVEL) {
				Xprefs.edit().putString("networkTrafficPosition", String.valueOf(POSITION_LEFT)).apply();
				networkTrafficPosition = POSITION_LEFT;
			}

			String thresholdText = Xprefs.getString("networkTrafficThreshold", "10");

			int networkTrafficThreshold;
			try {
				networkTrafficThreshold = Math.round(Float.parseFloat(thresholdText));
			} catch (Exception ignored) {
				networkTrafficThreshold = 10;
			}
			NetworkTraffic.setConstants(networkTrafficInterval, networkTrafficThreshold, networkTrafficMode, networkTrafficRXTop, networkTrafficColorful, networkTrafficDLColor, networkTrafficULColor, networkTrafficOpacity, networkTrafficShowIcons, networkTrafficShowInBits);

		}
		if (networkOnSBEnabled) {
			networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
			networkTrafficSB.update();
		}
		if (networkOnQSEnabled) {
			networkTrafficQS = NetworkTraffic.getInstance(mContext, false);
			networkTrafficQS.update();
		}
		placeNTSB();
		placeNTQS();

		//endregion network settings

		//region vibration settings
		boolean newshowVibrationIcon = Xprefs.getBoolean("SBshowVibrationIcon", false);
		if (newshowVibrationIcon != showVibrationIcon) {
			showVibrationIcon = newshowVibrationIcon;
			setShowVibrationIcon();
		}
		//endregion


		//region clock settings
		clockPosition = Integer.parseInt(Xprefs.getString("SBClockLoc", String.valueOf(POSITION_LEFT)));
		if (clockPosition == POSITION_LEFT_EXTRA_LEVEL) {
			Xprefs.edit().putString("SBClockLoc", String.valueOf(POSITION_LEFT)).apply();
			clockPosition = POSITION_LEFT;
		}

		mShowSeconds = Xprefs.getBoolean("SBCShowSeconds", false);
		mAmPmStyle = Integer.parseInt(Xprefs.getString("SBCAmPmStyle", String.valueOf(AM_PM_STYLE_GONE)));

		mStringFormatBefore = Xprefs.getString("DateFormatBeforeSBC", "");
		mStringFormatAfter = Xprefs.getString("DateFormatAfterSBC", "");
		mBeforeSmall = Xprefs.getBoolean("BeforeSBCSmall", true);
		mAfterSmall = Xprefs.getBoolean("AfterSBCSmall", true);

		if (Xprefs.getBoolean("SBCClockColorful", false)) {
			clockColor = Xprefs.getInt("SBCClockColor", Color.WHITE);
			mBeforeClockColor = Xprefs.getInt("SBCBeforeClockColor", Color.WHITE);
			mAfterClockColor = Xprefs.getInt("SBCAfterClockColor", Color.WHITE);
		} else {
			clockColor
					= mBeforeClockColor
					= mAfterClockColor
					= null;
		}


		if ((mStringFormatBefore + mStringFormatAfter).trim().isEmpty()) {
			int SBCDayOfWeekMode = Integer.parseInt(Xprefs.getString("SBCDayOfWeekMode", "0"));

			switch (SBCDayOfWeekMode) {
				case 0:
					mStringFormatAfter = mStringFormatBefore = "";
					break;
				case 1:
					mStringFormatBefore = "$GEEE ";
					mStringFormatAfter = "";
					mBeforeSmall = false;
					break;
				case 2:
					mStringFormatBefore = "$GEEE ";
					mStringFormatAfter = "";
					mBeforeSmall = true;
					break;
				case 3:
					mStringFormatBefore = "";
					mStringFormatAfter = " $GEEE";
					mAfterSmall = false;
					break;
				case 4:
					mStringFormatBefore = "";
					mStringFormatAfter = " $GEEE";
					mAfterSmall = true;
					break;
			}
		}

		try {
			placeClock();
			updateClock();
		} catch (Throwable ignored) {
		}
		//endregion clock settings


		//region vo_data
		VolteIconEnabled = Xprefs.getBoolean("VolteIconEnabled", false);
		VowifiIconEnabled = Xprefs.getBoolean("VowifiIconEnabled", false);
		//endregion


		if (Key.length > 0) {
			switch (Key[0]) {
				case "statusbarPaddings":
					updateStatusbarHeight();
					break;
				case "VolteIconEnabled":
				case "VowifiIconEnabled":
					if (VolteIconEnabled || VowifiIconEnabled) {
						initVoData();

						if (!VolteIconEnabled) removeSBIconSlot(VO_LTE_SLOT);
						if (!VowifiIconEnabled) removeSBIconSlot(VO_WIFI_SLOT);
					} else
						removeVoDataCallback();
					break;
			}
		}
	}

	@SuppressLint("DiscouragedApi")
	private int getIntegerResource(String resourceName, int defaultValue) {
		try {
			return mContext.getResources().getInteger(mContext.getResources().getIdentifier(resourceName, "integer", mContext.getPackageName()));
		} catch (Throwable ignored) {
			return defaultValue;
		}
	}

	private void updateClock() {
		try {
			mClockView.post(() -> { //the builtin update method doesn't care about the format. Just the text sadly
				callMethod(getObjectField(mClockView, "mCalendar"), "setTimeInMillis", System.currentTimeMillis());

				mClockView.setText((CharSequence) callMethod(mClockView, "getSmallTime"));
			});
		} catch (Throwable ignored) {
		}
	}

	private void placeNTQS() {
		if (networkTrafficQS == null) {
			return;
		}
		try {
			((ViewGroup) networkTrafficQS.getParent()).removeView(networkTrafficQS);
		} catch (Throwable ignored) {
		}
		if (!networkOnQSEnabled) return;

		try {
			NTQSHolder.addView(networkTrafficQS);
		} catch (Throwable ignored) {
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_PROFILE_SWITCH_AVAILABLE);
		mContext.registerReceiver(mAppProfileSwitchReceiver, filter, Context.RECEIVER_EXPORTED);

		//region needed classes
		ReflectedClass QSSecurityFooterUtilsClass = ReflectedClass.of("com.android.systemui.qs.QSSecurityFooterUtils");
		ReflectedClass KeyguardStatusBarViewControllerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.KeyguardStatusBarViewController");
//      ReflectedClass QuickStatusBarHeaderControllerClass = ReflectedClass.of("com.android.systemui.qs.QuickStatusBarHeaderController");
		ReflectedClass QuickStatusBarHeaderClass = ReflectedClass.of("com.android.systemui.qs.QuickStatusBarHeader");
		ReflectedClass ClockClass = ReflectedClass.of("com.android.systemui.statusbar.policy.Clock");
		ReflectedClass PhoneStatusBarViewClass = ReflectedClass.of("com.android.systemui.statusbar.phone.PhoneStatusBarView");
		ReflectedClass NotificationIconContainerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.NotificationIconContainer");
//		ReflectedClass StatusBarIconViewClass = ReflectedClass.of("com.android.systemui.statusbar.StatusBarIconView");
		ReflectedClass CollapsedStatusBarFragmentClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment");
		ReflectedClass PrivacyItemControllerClass = ReflectedClass.of("com.android.systemui.privacy.PrivacyItemController");
//		ReflectedClass KeyguardUpdateMonitorClass = ReflectedClass.of("com.android.keyguard.KeyguardUpdateMonitor");
		ReflectedClass TunerServiceImplClass = ReflectedClass.of("com.android.systemui.tuner.TunerServiceImpl");
		ReflectedClass ConnectivityCallbackHandlerClass = ReflectedClass.of("com.android.systemui.statusbar.connectivity.CallbackHandler");
		ReflectedClass HeadsUpStatusBarViewClass = ReflectedClass.of("com.android.systemui.statusbar.HeadsUpStatusBarView");
		ReflectedClass NotificationIconContainerAlwaysOnDisplayViewModelClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.notification.icon.ui.viewmodel.NotificationIconContainerAlwaysOnDisplayViewModel");
		ReflectedClass NotificationIconContainerStatusBarViewModelClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.notification.icon.ui.viewmodel.NotificationIconContainerStatusBarViewModel");
		StatusBarIconClass = ReflectedClass.of("com.android.internal.statusbar.StatusBarIcon");
		StatusBarIconHolderClass = ReflectedClass.of("com.android.systemui.statusbar.phone.StatusBarIconHolder");
		SystemUIDialogClass = ReflectedClass.of("com.android.systemui.statusbar.phone.SystemUIDialog");
		ReflectedClass NotifyChangesToCallbackClass = ReflectedClass.ofIfPossible("com.android.systemui.privacy.PrivacyItemController$NotifyChangesToCallback");
		//endregion


		if (NotificationIconContainerAlwaysOnDisplayViewModelClass.getClazz() != null) //Viewbinder implementation of the notification icon container
		{
			NotificationIconContainerAlwaysOnDisplayViewModelClass
					.afterConstruction()
					.run(param -> {
						AODNIC = param.thisObject;
						setObjectField(AODNIC, "maxIcons", NotificationAODIconLimit);
					});

			NotificationIconContainerStatusBarViewModelClass
					.afterConstruction()
					.run(param -> {
						SBNIC = param.thisObject;
						setObjectField(SBNIC, "maxIcons", NotificationIconLimit);
					});
		}

		initSwitchIcon();

		//forcing a refresh on statusbar once the charging chip goes away to avoid layout issues
		//only needed if chip is shown on lockscreen and device is unlocked quickly afterwards


		// Placing the headsUp text right next to the icon. if it's double row, it needs to shift down
		HeadsUpStatusBarViewClass
				.after("onLayout")
				.run(param -> {
					View headsUpView = (View) param.thisObject;
					int[] headsUpLocation = new int[2];
					headsUpView.getLocationOnScreen(headsUpLocation);

					int[] notificationContainerLocation = new int[2];
					mNotificationIconContainer.getLocationOnScreen(notificationContainerLocation);

					((View) getObjectField(param.thisObject, "mTextView"))
							.setTranslationY(
									(notificationContainerLocation[1] - headsUpLocation[1])
											/ 2f);
				});

		//region combined signal icons
		TunerServiceImplClass
				.afterConstruction()
				.run(param -> {
					mTunerService = param.thisObject;
					ReflectedClass.of(getObjectField(param.thisObject, "mObserver").getClass())
							.after("onChange")
							.run(param2 -> wifiVisibleChanged());
				});

		TunerServiceImplClass
				.after("addTunable")
				.run(param -> {
					if (param.args[1].getClass().equals(String[].class)
							&& Arrays.asList((String[]) param.args[1]).contains(ICON_HIDE_LIST)) {
						wifiVisibleChanged();
					} else if (ICON_HIDE_LIST.equals(param.args[1])) {
						wifiVisibleChanged();
					}
				});

		ConnectivityCallbackHandlerClass
				.after("setWifiIndicators")
				.run(param -> {
					boolean wifiVisible = getBooleanField(getObjectField(param.args[0], "statusIcon"), "visible");
					if (wifiVisible != mWifiVisible) {
						mWifiVisible = wifiVisible;
						if (CombineSignalIcons) {
							wifiVisibleChanged();
						}
					}
				});
		//endregion

		//region privacy chip
		PrivacyItemControllerClass
				.afterConstruction()
				.run(param ->
						{
						mPrivacyItemController = param.thisObject;
						ReflectedClass.of(getObjectField(param.thisObject, "notifyChanges").getClass())
								.before("run")
								.run(param1 -> {
									if (HidePrivacyChip) {
										try { //It's sometimes a readonly collection
											((List<?>) getObjectField(param1.thisObject, "privacyList"))
													.clear();
										} catch (Throwable ignored) {
										}
									}
								});
						});

		NotifyChangesToCallbackClass //A15QPR2 + A16
				.before("run")
				.run(param -> {
					if (HidePrivacyChip) {
						((List<?>) getObjectField(mPrivacyItemController, "callbacks")).clear();
					}
				});
		//endregion

		//region SB Padding
		PhoneStatusBarViewClass
				.afterConstruction()
				.run(param -> PSBV = param.thisObject);

		PhoneStatusBarViewClass
				.after("updateStatusBarHeight")
				.run(param -> {
					@SuppressLint("DiscouragedApi")
					View sbContentsView = ((View) param.thisObject).findViewById(mContext.getResources().getIdentifier("status_bar_contents", "id", listenPackage));

					if (SBPaddingStart == PADDING_DEFAULT && SBPaddingEnd == PADDING_DEFAULT)
						return;

					int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

					int paddingStart = SBPaddingStart == PADDING_DEFAULT
							? sbContentsView.getPaddingStart()
							: Math.round(SBPaddingStart * screenWidth / 100f);

					int paddingEnd = SBPaddingEnd == PADDING_DEFAULT
							? sbContentsView.getPaddingEnd()
							: Math.round(SBPaddingEnd * screenWidth / 100f);

					sbContentsView.setPaddingRelative(paddingStart, sbContentsView.getPaddingTop(), paddingEnd, sbContentsView.getPaddingBottom());
				});
		//endregion

		//region multi row statusbar
		//bypassing the max icon limit during measurement
		NotificationIconContainerClass
				.before("onMeasure")
				.run(param -> setObjectField(param.thisObject, "mIsStaticLayout", false));

		NotificationIconContainerClass
				.after("onMeasure")
				.run(param -> setObjectField(param.thisObject, "mIsStaticLayout", true));

		NotificationIconContainerClass
				.before("calculateIconXTranslations")
				.run(param -> {
					NotificationIconContainerOverride.calculateIconXTranslations(param);
					param.setResult(null);
				});

		//endregion

		//getting statusbar class for further use
		CollapsedStatusBarFragmentClass
				.afterConstruction()
				.run(param -> mCollapsedStatusBarFragment = param.thisObject);

		//update statusbar
		PhoneStatusBarViewClass
				.after("onConfigurationChanged")
				.run(param -> new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						if (BatteryBarView.hasInstance()) {
							BatteryBarView.getInstance().post(() -> refreshBatteryBar(BatteryBarView.getInstance()));
						}
					}
				}, 2000));

		//getting activitity starter for further use
		QuickStatusBarHeaderClass
				.afterConstruction()
				.run(param -> {
					QSBH = param.thisObject;
					NTQSHolder = new FrameLayout(mContext);
					FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
					lp.gravity = Gravity.CENTER_HORIZONTAL;
					NTQSHolder.setLayoutParams(lp);
					((FrameLayout) QSBH).addView(NTQSHolder);
					placeNTQS();
				});

		//stealing a working activity starter
		QSSecurityFooterUtilsClass
				.afterConstruction()
				.run(param -> mActivityStarter = getObjectField(param.thisObject, "mActivityStarter"));

		final ClickListener clickListener = new ClickListener();

		//marking clock instances for recognition and setting click actions on some icons
		QuickStatusBarHeaderClass
				.after("onFinishInflate")
				.run(param -> {
					//Getting QS text color for Network traffic
					@SuppressLint("DiscouragedApi") int fillColor = getColorAttrDefaultColor(
							mContext,
							mContext.getResources().getIdentifier("@android:attr/textColorPrimary", "attr", mContext.getPackageName()));

					NetworkTraffic.setTintColor(fillColor, false);

					try {
						//Clickable icons
						Object mBatteryRemainingIcon = getObjectField(param.thisObject, "mBatteryRemainingIcon");
						Object mDateView = getObjectField(param.thisObject, "mDateView");
						Object mClockViewQS = getObjectField(param.thisObject, "mClockView");

						callMethod(mBatteryRemainingIcon, "setOnClickListener", clickListener);
						callMethod(mClockViewQS, "setOnClickListener", clickListener);
						callMethod(mClockViewQS, "setOnLongClickListener", clickListener);
						callMethod(mDateView, "setOnClickListener", clickListener);
						callMethod(mDateView, "setOnLongClickListener", clickListener);
					} catch (Throwable ignored) {
					}
				});

		try {
			//QPR3
			ReflectedClass ShadeHeaderControllerClass = ReflectedClass.ofIfPossible("com.android.systemui.shade.ShadeHeaderController");

			if (ShadeHeaderControllerClass.getClazz() == null) //QPR2
			{
				ShadeHeaderControllerClass = ReflectedClass.of("com.android.systemui.shade.LargeScreenShadeHeaderController");
			}

			ShadeHeaderControllerClass
					.after("onInit")
					.run(param -> {
						View mView = (View) getObjectField(param.thisObject, "mView");

						mView.findViewById(mContext.getResources().getIdentifier("clock", "id", mContext.getPackageName())).setOnClickListener(clickListener);
						mView.findViewById(mContext.getResources().getIdentifier("clock", "id", mContext.getPackageName())).setOnLongClickListener(clickListener);
						mView.findViewById(mContext.getResources().getIdentifier("date", "id", mContext.getPackageName())).setOnClickListener(clickListener);
						mView.findViewById(mContext.getResources().getIdentifier("batteryRemainingIcon", "id", mContext.getPackageName())).setOnClickListener(clickListener);
					});

		} catch (Throwable ignored) {
		}

		//show/hide vibration icon from system icons
		KeyguardStatusBarViewControllerClass
				.afterConstruction()
				.run(param -> {
					//Removing vibration icon from blocked icons in lockscreen
					if (showVibrationIcon && (findFieldIfExists(KeyguardStatusBarViewControllerClass.getClazz(), "mBlockedIcons") != null)) { //Android 12 doesn't have such thing at all
						@SuppressWarnings("unchecked") List<String> OldmBlockedIcons = (List<String>) getObjectField(param.thisObject, "mBlockedIcons");

						List<String> NewmBlockedIcons = new ArrayList<>();
						for (String item : OldmBlockedIcons) {
							if (!item.equals("volume")) {
								NewmBlockedIcons.add(item);
							}
						}
						setObjectField(param.thisObject, "mBlockedIcons", NewmBlockedIcons);
					}
				});

		//restoring batterybar and network traffic: when clock goes back to life
		CollapsedStatusBarFragmentClass
				.before("animateShow")
				.run(param -> {
					if (param.args[0] != mClockView) return;
					for (ClockVisibilityCallback c : clockVisibilityCallbacks) {
						try {
							c.OnVisibilityChanged(true);
						} catch (Exception ignored) {
						}
					}
				});

		CollapsedStatusBarFragmentClass
				.after("animateHiddenState")
				.run(param -> {
					if (param.args[(param.args[1] instanceof View) ? 1 : 0] != mClockView)
						return; //view can be the 2nd arg sometimes
					for (ClockVisibilityCallback c : clockVisibilityCallbacks) {
						try {
							c.OnVisibilityChanged(false);
						} catch (Exception ignored) {
						}
					}
				});

		//modding clock, adding additional objects,
		CollapsedStatusBarFragmentClass
				.after("onViewCreated")
				.run(param -> {
					mStatusBarIconController = getObjectField(param.thisObject, "mStatusBarIconController");

					if (findMethodExactIfExists(mStatusBarIconController.getClass(), "removeAllIconsForSlot", String.class, boolean.class) != null) {
						mRemoveAllIconsForSlotParams = 2;
					}

					try {
						mClockView = (TextView) getObjectField(param.thisObject, "mClockView");
						updateClockColor();
					} catch (Throwable ignored) {
					}

					mStatusBar = (ViewGroup) getObjectField(mCollapsedStatusBarFragment, "mStatusBar");

					mStatusBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> setHeights());

					mStatusbarStartSide = mStatusBar.findViewById(mContext.getResources().getIdentifier("status_bar_start_side_except_heads_up", "id", mContext.getPackageName()));

					mSystemIconArea = mStatusBar.findViewById(mContext.getResources().getIdentifier("statusIcons", "id", mContext.getPackageName()));

					fullStatusbar = (FrameLayout) mStatusBar.getParent();

					try {
						mCenteredIconArea = (View) ((View) getObjectField(param.thisObject, "mCenteredIconArea")).getParent();
					} catch (Throwable ignored) {
						mCenteredIconArea = new LinearLayout(mContext);
						FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
						lp.gravity = Gravity.CENTER;
						mCenteredIconArea.setLayoutParams(lp);
						mStatusBar.addView(mCenteredIconArea);
					}

					makeLeftSplitArea();

					if (BBarEnabled) //in case we got the config but view wasn't ready yet
					{
						placeBatteryBar();
					}

					if (VolteIconEnabled || VowifiIconEnabled) //in case we got the config but context wasn't ready yet
					{
						initVoData();
					}

					if (networkOnSBEnabled) {
						networkTrafficSB = NetworkTraffic.getInstance(mContext, true);
						placeNTSB();
					}

					//<Showing vibration icon in collapsed statusbar>
					if (showVibrationIcon) {
						setShowVibrationIcon();
					}
					//</Showing vibration icon in collapsed statusbar>

					//<modding clock>
					placeClock();

					if (mNotificationIconContainer.getChildCount() == 0) {
						mNotificationContainerContainer.setVisibility(GONE);
					}
					setHeights();
				});
		//clock mods

		ClockClass
				.before("getSmallTime")
				.run(param -> {
					setObjectField(param.thisObject, "mAmPmStyle", AM_PM_STYLE_GONE);
					setObjectField(param.thisObject, "mShowSeconds", mShowSeconds);
				});

		ClockClass
				.after("getSmallTime")
				.run(param -> {
					if (param.thisObject != mClockView)
						return; //We don't want custom format in QS header. do we?

					SpannableStringBuilder result = new SpannableStringBuilder();
					result.append(getFormattedString(mStringFormatBefore, mBeforeSmall, mBeforeClockColor)); //before clock
					SpannableStringBuilder clockText = SpannableStringBuilder.valueOf((CharSequence) param.getResult()); //THE clock
					if (clockColor != null) {
						clockText.setSpan(new NetworkTraffic.TrafficStyle(clockColor), 0, (clockText).length(),
								Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					result.append(clockText);
					if (mAmPmStyle != AM_PM_STYLE_GONE) {
						result.append(getFormattedString("$Ga", mAmPmStyle == AM_PM_STYLE_SMALL, clockColor));
					}
					result.append(getFormattedString(mStringFormatAfter, mAfterSmall, mAfterClockColor)); //after clock

					if (getAdditionalInstanceField(param.thisObject, "stringFormatCallBack") == null) {
						FormattedStringCallback callback = () -> {
							if (!mShowSeconds) //don't update again if it's going to do it every second anyway
								updateClock();
						};

						stringFormatter.registerCallback(callback);
						setAdditionalInstanceField(param.thisObject, "stringFormatCallBack", callback);
					}
					param.setResult(result);
				});

		//using clock colors for network traffic and battery bar
		ClockClass
				.after("onDarkChanged")
				.run(param -> {
					if (param.thisObject != mClockView)
						return; //We don't want colors of QS header. only statusbar

					updateClockColor();
					if (BatteryBarView.hasInstance()) {
						refreshBatteryBar(BatteryBarView.getInstance());
					}
				});

		//region mobile roaming
		//A14QPR1 and prior
		ReflectedClass.of(ServiceState.class)
				.before("getRoaming")
				.run(param -> {
					if (HideRoamingState)
						param.setResult(false);
				});

		//A14QPR2
		ReflectedClass.of(TelephonyDisplayInfo.class)
				.before("isRoaming")
				.run(param -> {
					if (HideRoamingState)
						param.setResult(false);
				});

		try { //A14QPR3
			ReflectedClass MobileIconInteractorImplClass = ReflectedClass.of("com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MobileIconInteractorImpl");

			//we must use the classes defined in the apk. using our own will fail
			ReflectedClass StateFlowImplClass = ReflectedClass.of("kotlinx.coroutines.flow.StateFlowImpl");
			ReflectedClass ReadonlyStateFlowClass = ReflectedClass.of("kotlinx.coroutines.flow.ReadonlyStateFlow");

			MobileIconInteractorImplClass
					.afterConstruction()
					.run(param -> {
						if (HideRoamingState) {
							Object notRoamingFlow = StateFlowImplClass.getClazz().getConstructor(Object.class).newInstance(false);
							setObjectField(param.thisObject, "isRoaming", ReadonlyStateFlowClass.getClazz().getConstructors()[0].newInstance(notRoamingFlow));
						}
					});
		} catch (Throwable ignored) {
		}
		//endregion
	}

	private void updateClockColor() {
		currentClockColor = mClockView.getTextColors().getDefaultColor();

		for (StatusbarTextColorCallback callback : mTextColorCallbacks) {
			callback.onTextColorChanged(currentClockColor);
		}
	}

	public static @ColorInt int getCurrentClockColor() {
		return currentClockColor;
	}

	public static int registerTextColorCallback(StatusbarTextColorCallback callback) {
		mTextColorCallbacks.add(callback);
		return currentClockColor;
	}

	private void updateStatusbarHeight() {
		try {
			callMethod(PSBV, "updateStatusBarHeight");
		} catch (Throwable ignored) {
		}
	}

	//region double row left area
	@SuppressLint("DiscouragedApi")
	private void makeLeftSplitArea() {
		mNotificationIconContainer = mStatusBar.findViewById(mContext.getResources().getIdentifier("notificationIcons", "id", mContext.getPackageName()));

		mNotificationContainerContainer = new LinearLayout(mContext);
		mNotificationContainerContainer.setClipChildren(false); //allowing headsup icon to go beyond

		if (mLeftVerticalSplitContainer == null) {
			mLeftVerticalSplitContainer = new LinearLayout(mContext);
			mLeftVerticalSplitContainer.setClipChildren(false); //allowing headsup icon to go beyond
		} else {
			mLeftVerticalSplitContainer.removeAllViews();
			if (mLeftVerticalSplitContainer.getParent() != null)
				((ViewGroup) mLeftVerticalSplitContainer.getParent()).removeView(mLeftVerticalSplitContainer);
		}

		mLeftVerticalSplitContainer.setOrientation(VERTICAL);
		mLeftVerticalSplitContainer.setLayoutParams(new LinearLayoutCompat.LayoutParams(MATCH_PARENT, MATCH_PARENT));
		mLeftVerticalSplitContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> setHeights());

		LayoutTransition layoutTransition = new LayoutTransition();
		layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
		layoutTransition.setDuration(200);
		mLeftVerticalSplitContainer.setLayoutTransition(layoutTransition);

		mLeftExtraRowContainer = new ShyLinearLayout(mContext);
		mLeftVerticalSplitContainer.addView(mLeftExtraRowContainer, 0);

		ViewGroup parent = (ViewGroup) mNotificationIconContainer.getParent();

		parent.addView(mLeftVerticalSplitContainer, parent.indexOfChild(mNotificationIconContainer));
		parent.removeView(mNotificationIconContainer);
		mLeftVerticalSplitContainer.addView(mNotificationContainerContainer);

		repositionOngoingChips();

		mNotificationContainerContainer.addView(mNotificationIconContainer);

		((LinearLayout.LayoutParams) mNotificationIconContainer.getLayoutParams()).weight = 100;
		mNotificationIconContainer.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				mNotificationContainerContainer.setVisibility(VISIBLE);
				setHeights();
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
				if (mNotificationIconContainer.getChildCount() == 0) {
					mNotificationContainerContainer.setVisibility(GONE);
					setHeights();
				}
			}
		});

		((View) mStatusbarStartSide.getParent()).getLayoutParams().height = MATCH_PARENT;
		mStatusbarStartSide.getLayoutParams().height = MATCH_PARENT;
		mLeftVerticalSplitContainer.getLayoutParams().height = MATCH_PARENT;
	}

	private void repositionOngoingChips() {
		repositionOngoingChip("ongoing_activity_chip"); //A15
		repositionOngoingChip("ongoing_call_chip"); //Pre A15
		repositionOngoingChip("ongoing_activity_chip_primary"); //A15 QPR1
		repositionOngoingChip("ongoing_activity_chip_secondary"); //A15 QPR1
	}

	private void repositionOngoingChip(String chipName) {
		@SuppressLint("DiscouragedApi")
		View ongoingActivityChipView = mStatusBar.findViewById(mContext.getResources().getIdentifier(chipName, "id", mContext.getPackageName()));
		if (ongoingActivityChipView != null) {
			((ViewGroup) ongoingActivityChipView.getParent()).removeView(ongoingActivityChipView);
			mNotificationContainerContainer.addView(ongoingActivityChipView);
		}
	}

	private void setHeights() {
		Resources res = mContext.getResources();
		@SuppressLint("DiscouragedApi") int statusbarHeight = mStatusBar.getLayoutParams().height - res.getDimensionPixelSize(res.getIdentifier("status_bar_padding_top", "dimen", mContext.getPackageName()));

		mNotificationContainerContainer.getLayoutParams().height = (mLeftExtraRowContainer.getVisibility() == VISIBLE) ? statusbarHeight / 2 : MATCH_PARENT;
		mLeftExtraRowContainer.getLayoutParams().height = ((mNotificationContainerContainer.getVisibility() == VISIBLE) ? statusbarHeight / 2 : MATCH_PARENT);
		if (networkOnSBEnabled) {
			networkTrafficSB.getLayoutParams().height = statusbarHeight / ((networkTrafficPosition == POSITION_LEFT && notificationAreaMultiRow) ? 2 : 1);
		}
	}
	//endregion

	//region battery bar related
	private void refreshBatteryBar(BatteryBarView instance) {
		BatteryBarView.setStaticColor(batteryLevels, batteryColors, indicateCharging, chargingColor, indicateFastCharging, fastChargingColor, indicatePowerSave, powerSaveColor, BBarTransitColors, BBAnimateCharging);
		instance.setVisibility((BBarEnabled) ? VISIBLE : GONE);
		instance.setColorful(BBarColorful);
		instance.setOnlyWhileCharging(BBOnlyWhileCharging);
		instance.setOnTop(!BBOnBottom);
		instance.setAlphaPct(BBOpacity);
		instance.setBarHeight(Math.round(BBarHeight / 10f) + 5);
		instance.setCenterBased(BBSetCentered);
		instance.refreshLayout();
	}

	private void placeBatteryBar() {
		try {
			BatteryBarView batteryBarView = BatteryBarView.getInstance(mContext);
			try {
				((ViewGroup) batteryBarView.getParent()).removeView(batteryBarView);
			} catch (Throwable ignored) {
			}
			fullStatusbar.addView(batteryBarView);
			refreshBatteryBar(BatteryBarView.getInstance());
		} catch (Throwable ignored) {
		}
	}
	//endregion

	//region statusbar icon holder
	private Object getStatusbarIconFor(Icon icon, String slotName) {
		try {
			Object statusbarIcon = ObjenesisHelper.newInstance(StatusBarIconClass.getClazz());

			setObjectField(statusbarIcon, "visible", true);

			//noinspection JavaReflectionMemberAccess
			setObjectField(statusbarIcon, "user", UserHandle.class.getDeclaredConstructor(int.class).newInstance(0));
			setObjectField(statusbarIcon, "pkg", BuildConfig.APPLICATION_ID);
			setObjectField(statusbarIcon, "icon", icon);
			setObjectField(statusbarIcon, "iconLevel", 0);
			setObjectField(statusbarIcon, "number", 0);
			setObjectField(statusbarIcon, "contentDescription", slotName);

			return statusbarIcon;
		} catch (Throwable ignored) {
			return null;
		}
	}

	private Object getStatusbarIconHolderFor(Object statusbarIcon) {
		Object holder = ObjenesisHelper.newInstance(StatusBarIconHolderClass.getClazz());
		String[] iconFiled = new String[1];
		Arrays.stream(StatusBarIconHolderClass.getClazz().getFields()).forEach(field ->
		{
			if (field.getName().toLowerCase().contains("icon"))
				iconFiled[0] = field.getName();
		});

		setObjectField(holder, iconFiled[0], statusbarIcon);

		return holder;
	}

	//endregion

	//region vo_data related
	private void initVoData() {
		try {
			if (!telephonyCallbackRegistered) {

				Icon volteIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_volte);
				Object volteStatusbarIcon = getStatusbarIconFor(volteIcon, VO_LTE_SLOT);
				volteStatusbarIconHolder = getStatusbarIconHolderFor(volteStatusbarIcon);

				Icon vowifiIcon = Icon.createWithResource(BuildConfig.APPLICATION_ID, R.drawable.ic_vowifi);
				Object vowifiStatusbarIcon = getStatusbarIconFor(vowifiIcon, VO_WIFI_SLOT);
				vowifiStatusbarIconHolder = getStatusbarIconHolderFor(vowifiStatusbarIcon);

				//noinspection DataFlowIssue
				SystemUtils.TelephonyManager().registerTelephonyCallback(voDataExec, voDataCallback);
				telephonyCallbackRegistered = true;
			}
		} catch (Exception ignored) {
		}

		updateVoData(true);
	}

	private void removeVoDataCallback() {
		try {
			//noinspection DataFlowIssue
			SystemUtils.TelephonyManager().unregisterTelephonyCallback(voDataCallback);
			telephonyCallbackRegistered = false;
		} catch (Exception ignored) {
		}
		removeSBIconSlot(VO_LTE_SLOT);
		removeSBIconSlot(VO_WIFI_SLOT);
	}

	private class serverStateCallback extends TelephonyCallback implements
			TelephonyCallback.ServiceStateListener {
		@Override
		public void onServiceStateChanged(@NonNull ServiceState serviceState) {
			updateVoData(false);
		}
	}

	private void updateVoData(boolean force) {
		boolean voWifiAvailable = (Boolean) callMethod(SystemUtils.TelephonyManager(), "isWifiCallingAvailable");
		boolean volteStateAvailable = (Boolean) callMethod(SystemUtils.TelephonyManager(), "isVolteAvailable");

		if (lastVolteAvailable != volteStateAvailable || force) {
			lastVolteAvailable = volteStateAvailable;
			if (volteStateAvailable && VolteIconEnabled) {
				mStatusBar.post(() -> {
					try {
						callMethod(mStatusBarIconController, "setIcon", VO_LTE_SLOT, volteStatusbarIconHolder);
					} catch (Exception ignored) {
					}
				});
			} else {
				removeSBIconSlot(VO_LTE_SLOT);
			}
		}

		if (lastVowifiAvailable != voWifiAvailable || force) {
			lastVowifiAvailable = voWifiAvailable;
			if (voWifiAvailable && VowifiIconEnabled) {
				mStatusBar.post(() -> {
					try {
						callMethod(mStatusBarIconController, "setIcon", VO_WIFI_SLOT, vowifiStatusbarIconHolder);
					} catch (Exception ignored) {
					}
				});
			} else {
				removeSBIconSlot(VO_WIFI_SLOT);
			}
		}
	}

	private void removeSBIconSlot(String slot) {
		if (mStatusBar == null) return; //probably it's too soon to have a statusbar

		mStatusBar.post(() -> {
			try {
				if (mRemoveAllIconsForSlotParams == 2) {
					callMethod(mStatusBarIconController, "removeAllIconsForSlot", slot, false);
				} else {
					callMethod(mStatusBarIconController, "removeAllIconsForSlot", slot);
				}
			} catch (Throwable ignored) {
			}
		});
	}
	//endregion

	//region vibrationicon related
	private void setShowVibrationIcon() {
		try {
			@SuppressWarnings("unchecked") List<String> mBlockedIcons = (List<String>) getObjectField(mCollapsedStatusBarFragment, "mBlockedIcons");
			Object mStatusBarIconController = getObjectField(mCollapsedStatusBarFragment, "mStatusBarIconController");
			Object mDarkIconManager = getObjectField(mCollapsedStatusBarFragment, "mDarkIconManager");

			if (showVibrationIcon) {
				mBlockedIcons.remove("volume");
			} else {
				mBlockedIcons.add("volume");
			}
			callMethod(mDarkIconManager, "setBlockList", mBlockedIcons);
			callMethod(mStatusBarIconController, "refreshIconGroups");
		} catch (Throwable ignored) {
		}
	}
	//endregion

	//region network traffic related
	private void placeNTSB() {
		if (networkTrafficSB == null) {
			return;
		}
		try {
			((ViewGroup) networkTrafficSB.getParent()).removeView(networkTrafficSB);
		} catch (Exception ignored) {
		}
		if (!networkOnSBEnabled) return;

		try {
			LinearLayout.LayoutParams ntsbLayoutP;
			switch (networkTrafficPosition) {
				case POSITION_RIGHT:
					((ViewGroup) mSystemIconArea.getParent()).addView(networkTrafficSB, 0);
					networkTrafficSB.setPadding(rightClockPadding, 0, leftClockPadding, 0);
					break;
				case POSITION_LEFT:
					if (notificationAreaMultiRow) {
						mLeftExtraRowContainer.addView(networkTrafficSB, mLeftExtraRowContainer.getChildCount());
					} else {
						mStatusbarStartSide.addView(networkTrafficSB, 1);
					}
					networkTrafficSB.setPadding(0, 0, leftClockPadding, 0);
					break;
				case POSITION_CENTER:
					mStatusbarStartSide.addView(networkTrafficSB);
					networkTrafficSB.setPadding(rightClockPadding, 0, leftClockPadding, 0);
					break;
			}
			ntsbLayoutP = (LinearLayout.LayoutParams) networkTrafficSB.getLayoutParams();
			ntsbLayoutP.gravity = Gravity.CENTER_VERTICAL;
			networkTrafficSB.setLayoutParams(ntsbLayoutP);
		} catch (Throwable ignored) {
		}
	}
	//endregion

	//region icon tap related
	class ClickListener implements View.OnClickListener, View.OnLongClickListener {
		public ClickListener() {
		}

		@Override
		public void onClick(View v) {
			String name = mContext.getResources().getResourceName(v.getId());

			if (name.endsWith("clock")) {
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(AlarmClock.ACTION_SHOW_ALARMS), 0);
			} else if (name.endsWith("date")) {
				Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
				builder.appendPath("time");
				builder.appendPath(Long.toString(System.currentTimeMillis()));
				Intent todayIntent = new Intent(Intent.ACTION_VIEW, builder.build());
				callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", todayIntent, 0);
			} else if (name.endsWith("batteryRemainingIcon")) {

				if (BatteryDataProvider.isCharging()) {
					try {
						showChargingDialog();
						return;
					} catch (Throwable ignored) {
					}
				}
				showBatteryPage();
			}
		}

		@Override
		public boolean onLongClick(View v) {
			String name = mContext.getResources().getResourceName(v.getId());

			if (name.endsWith("clock") || name.endsWith("date")) {
				Intent mIntent = new Intent(Intent.ACTION_MAIN);
				mIntent.setClassName("com.android.settings",
						"com.android.settings.Settings$DateTimeSettingsActivity");
				callMethod(mActivityStarter, "startActivity", mIntent, true /* dismissShade */);
				return true;
			}
			return false;
		}
	}

	private void showChargingDialog() throws Throwable {
		AlertDialog dialog = (AlertDialog) SystemUIDialogClass.getClazz().getConstructor(Context.class).newInstance(mContext);

		dialog.setMessage(KeyguardMods.getPowerIndicationString());

		dialog.setButton(BUTTON_NEUTRAL,
				modRes.getText(R.string.battery_info_button_title),
				(dialog1, which) -> showBatteryPage());

		dialog.show();
	}

	private void showBatteryPage() {
		callMethod(mActivityStarter, "postStartActivityDismissingKeyguard", new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 0);
	}
	//endregion

	//region clock and date related
	private void placeClock() {
		ViewGroup parent = (ViewGroup) mClockView.getParent();
		ViewGroup targetArea = null;
		Integer index = null;

		switch (clockPosition) {
			case POSITION_LEFT:
				if (notificationAreaMultiRow) {
					targetArea = mLeftExtraRowContainer;
					index = 0;
				} else {
					targetArea = mStatusbarStartSide;
					index = 1;
				}
				mClockView.setPadding(0, 0, leftClockPadding, 0);
				break;
			case POSITION_CENTER:
				targetArea = (ViewGroup) mCenteredIconArea;
				mClockView.setPadding(rightClockPadding, 0, rightClockPadding, 0);
				break;
			case POSITION_RIGHT:
				mClockView.setPadding(rightClockPadding, 0, 0, 0);
				targetArea = ((ViewGroup) mSystemIconArea.getParent());
				break;
		}
		parent.removeView(mClockView);
		if (index != null) {
			targetArea.addView(mClockView, index);
		} else {
			//noinspection DataFlowIssue
			targetArea.addView(mClockView);
		}
	}

	private final StringFormatter stringFormatter = new StringFormatter();

	private CharSequence getFormattedString(String dateFormat, boolean small, @Nullable @ColorInt Integer textColor) {
		if (dateFormat.isEmpty()) return "";

		//There's some format to work on
		SpannableStringBuilder formatted = new SpannableStringBuilder(stringFormatter.formatString(dateFormat));

		if (small) {
			//small size requested
			CharacterStyle style = new RelativeSizeSpan(0.7f);
			formatted.setSpan(style, 0, formatted.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (textColor != null) {
			formatted.setSpan(new NetworkTraffic.TrafficStyle(textColor), 0, (formatted).length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		return formatted;
	}

	//endregion
	//region callbacks
	public static void registerClockVisibilityCallback(ClockVisibilityCallback callback) {
		clockVisibilityCallbacks.add(callback);
	}

	@SuppressWarnings("unused")
	public static void unRegisterClockVisibilityCallback(ClockVisibilityCallback callback) {
		clockVisibilityCallbacks.remove(callback);
	}

	public interface ClockVisibilityCallback extends Callback {
		void OnVisibilityChanged(boolean isVisible);
	}
	//endregion

	//region combined signal icons
	private void wifiVisibleChanged() {
		try { //don't crash the system if failed
			//inspired from from TunerServiceImpl#reloadAll
			String hideListString = Settings.Secure.getString(
					(ContentResolver) getObjectField(mTunerService, "mContentResolver")
					, ICON_HIDE_LIST);

			if (CombineSignalIcons && mWifiVisible) {
				if (hideListString == null || hideListString.isEmpty()) {
					hideListString = "mobile";
				} else if (!hideListString.contains("mobile")) {
					hideListString = hideListString + ",mobile";
				}
			}
			@SuppressWarnings("unchecked")
			Set<Object> tunables = (Set<Object>) callMethod(getObjectField(mTunerService, "mTunableLookup"), "get", ICON_HIDE_LIST);

			String finalHideListString = hideListString;
			mStatusBar.post(() -> {
				for (Object tunable : tunables) {
					callMethod(tunable, "onTuningChanged", ICON_HIDE_LIST, finalHideListString);
				}
			});
		} catch (Throwable ignored) {
		}
	}
	//endregion

	public interface StatusbarTextColorCallback {
		void onTextColorChanged(int textColor);
	}
}
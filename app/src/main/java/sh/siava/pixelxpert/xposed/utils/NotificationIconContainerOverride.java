package sh.siava.pixelxpert.xposed.utils;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;

public class NotificationIconContainerOverride {
	public static final int STATE_ICON = 0;
	public static final int STATE_DOT = 1;
	public static final int STATE_HIDDEN = 2;
	private static final int MAX_DOTS = 1;
	private static final int NO_VALUE = Integer.MIN_VALUE;
	public static final int LEGACY_MAX_ICONS = 4;
	private static Class<?> xTranslationFieldType;

	public static void calculateIconXTranslations(XC_MethodHook.MethodHookParam param) throws Throwable {//A13
		ViewGroup thisObject = (ViewGroup) param.thisObject;

		String xTranslationField = null;

		Object mIconStates = getObjectField(thisObject, "mIconStates");

		float translationX = (float) callMethod(thisObject, "getActualPaddingStart");
		int firstOverflowIndex = -1;
		int childCount = thisObject.getChildCount();

		float layoutEnd = getLayoutEnd(thisObject);
		setObjectField(thisObject, "mVisualOverflowStart", 0);
		setObjectField(thisObject, "mFirstVisibleIconState", null);
		for (int i = 0; i < childCount; i++) {
			View view = thisObject.getChildAt(i);
			Object iconState = callMethod(mIconStates, "get", view);

			if(xTranslationField == null)  //A13 QPR1 - QPR2 compatibility check
			{
				xTranslationField = (findFieldIfExists(iconState.getClass().getSuperclass(), "mXTranslation") == null)
						? "xTranslation"
						: "mXTranslation";
					xTranslationFieldType = iconState.getClass().getField(xTranslationField).getType();

			}

			if (getFloatField(iconState, "iconAppearAmount") == 1.0f) {
				// We only modify the mXTranslation if it's fully inside of the container
				// since during the transition to the shelf, the translations are controlled
				// from the outside
				setObjectField(iconState, xTranslationField, translationX);
			}
			if (getObjectField(thisObject, "mFirstVisibleIconState") == null) {
				setObjectField(thisObject, "mFirstVisibleIconState", iconState);
			}
			int mSpeedBumpIndex = getIntField(thisObject, "mSpeedBumpIndex");

			int maxIcons = LEGACY_MAX_ICONS; //legacy versions
			try { //viewmodel versions of the class
				maxIcons = getIntField(thisObject, "mMaxIcons");
			}
			catch (Throwable ignored){}

			boolean forceOverflow = shouldForceOverflow(i, mSpeedBumpIndex, getFloatField(iconState, "iconAppearAmount"), maxIcons);

			boolean isLastChild = i == childCount - 1;
			float drawingScale;

			try { //older versions
				drawingScale = getBooleanField(thisObject, "mOnLockScreen") && view.getClass().getName().endsWith("StatusBarIconView")
						? getIconScaleIncreased(view)
						: 1f;
			}
			catch (Throwable ignored) //A15 Beta 2
			{
				drawingScale = getBooleanField(thisObject, "mUseIncreasedIconScale") && view.getClass().getName().endsWith("StatusBarIconView")
						? getIconScaleIncreased(view)
						: 1f;
			}

			setObjectField(iconState, "visibleState", getBooleanField(iconState, "hidden")
					? STATE_HIDDEN
					: STATE_ICON);

			final float overflowDotX = layoutEnd - getIntField(thisObject, "mIconSize");
			boolean isOverflowing = translationX > overflowDotX;

			if (firstOverflowIndex == -1 && (forceOverflow || isOverflowing)) {
				firstOverflowIndex = isLastChild && !forceOverflow
						? i - 1
						: i;
				setObjectField(thisObject, "mVisualOverflowStart", layoutEnd - getIntField(thisObject, "mIconSize"));
				if (forceOverflow || getBooleanField(thisObject, "mIsStaticLayout")) {
					setObjectField(thisObject, "mVisualOverflowStart", Math.min(translationX, getFloatField(thisObject, "mVisualOverflowStart")));
				}
			}
			translationX += getFloatField(iconState, "iconAppearAmount") * view.getWidth() * drawingScale;
		}
		int mNumDots = 0;
//		setObjectField(thisObject, "mNumDots", 0);
		if (firstOverflowIndex != -1) {
			translationX = getFloatField(thisObject, "mVisualOverflowStart");
			for (int i = firstOverflowIndex; i < childCount; i++) {
				View view = thisObject.getChildAt(i);
				Object iconState = callMethod(mIconStates, "get", view);
				setObjectField(iconState, xTranslationField, translationX);
				if (mNumDots < MAX_DOTS) {
					if (mNumDots == 0 && getFloatField(iconState, "iconAppearAmount") < 0.8f) {
						setObjectField(iconState, "visibleState", STATE_ICON);
					} else {
						setObjectField(iconState, "visibleState", STATE_DOT);
						mNumDots++;
						//setObjectField(thisObject, "mNumDots", getIntField(thisObject, "mNumDots") + 1);
					}
					translationX += getFloatField(iconState, "iconAppearAmount");

					try {
						setObjectField(thisObject, "mLastVisibleIconState", iconState);
					} catch (Throwable ignored) {
					} //unfortunately classloader can't locate this field
				} else {
					setObjectField(iconState, "visibleState", STATE_HIDDEN);
				}
			}
		} else if (childCount > 0) {
			View lastChild = thisObject.getChildAt(childCount - 1);
			try {
				setObjectField(thisObject, "mLastVisibleIconState", callMethod(mIconStates, "get", lastChild));
			} catch (Throwable ignored) {
			} //unfortunately classloader can't locate this field

			setObjectField(thisObject, "mFirstVisibleIconState", callMethod(mIconStates, "get", thisObject.getChildAt(0)));
		}
		if ((boolean) callMethod(thisObject, "isLayoutRtl")) {
			for (int i = 0; i < childCount; i++) {
				View view = thisObject.getChildAt(i);
				Object iconState = callMethod(mIconStates, "get", view);
				int xTranslation = xTranslationFieldType.equals(float.class)
						? Math.round(getFloatField(iconState, xTranslationField))
						: getIntField(iconState, xTranslationField);

				setObjectField(iconState, xTranslationField, thisObject.getWidth() - xTranslation - view.getWidth());
			}
		}
	}

	private static boolean shouldForceOverflow(int i, int speedBumpIndex, float iconAppearAmount,
	                                           int maxVisibleIcons) {
		return speedBumpIndex != -1 && i >= speedBumpIndex
				&& iconAppearAmount > 0.0f || i >= maxVisibleIcons;
	}
//Since classloader can't find these methods, we implement them ourselves
	private static float getLayoutEnd(View v) {
		return getActualWidth(v) - getActualPaddingEnd(v);
	}

	private static int getActualWidth(View v) {
		int mActualLayoutWidth = getIntField(v, "mActualLayoutWidth");
		if (mActualLayoutWidth == NO_VALUE) {
			return v.getWidth();
		}
		return mActualLayoutWidth;
	}

	private static float getActualPaddingEnd(View v) {
		float mActualPaddingEnd = getFloatField(v, "mActualPaddingEnd");
		if (mActualPaddingEnd == NO_VALUE) {
			return v.getPaddingEnd();
		}
		return mActualPaddingEnd;
	}

	private static float getIconScaleIncreased(View v) {
		return (float) getIntField(v, "mStatusBarIconDrawingSizeIncreased") / getIntField(v, "mStatusBarIconDrawingSize");
	}
}

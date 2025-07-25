package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static android.view.View.GONE;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import java.lang.reflect.Method;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.ResourceManager;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
@LauncherModPack
public class ClearAllButtonMod extends XposedModPack {
	private Object recentView;
	private static boolean RecentClearAllReposition = false;
	private ImageView clearAllIcon;
	private FrameLayout clearAllButton;

	public ClearAllButtonMod(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		if (Key.length > 0 && Key[0].equals("RecentClearAllReposition")) {
			SystemUtils.killSelf();
		}

		RecentClearAllReposition = Xprefs.getBoolean("RecentClearAllReposition", false);
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass OverviewActionsViewClass = ReflectedClass.of("com.android.quickstep.views.OverviewActionsView");
		ReflectedClass RecentsViewClass = ReflectedClass.of("com.android.quickstep.views.RecentsView");
		Method dismissAllTasksMethod = findMethodBestMatch(RecentsViewClass.getClazz(), "dismissAllTasks", View.class);

		RecentsViewClass
				.afterConstruction()
				.run(param -> recentView = param.thisObject);

		RecentsViewClass
				.after("setColorTint")
				.run(param -> {
					if (!RecentClearAllReposition) return;

					clearAllIcon.getDrawable().setTintList(getThemedColor(mContext));
				});

		RecentsViewClass
				.after("setVisibility")
				.run(param -> {
					if (clearAllButton == null) return;

					clearAllButton.setVisibility((Integer) param.args[0]);
				});

		OverviewActionsViewClass
				.before("onFinishInflate")
				.run(param -> {
					if (!RecentClearAllReposition) return;

					clearAllButton = new FrameLayout(mContext);

					clearAllIcon = new ImageView(mContext);
					clearAllIcon.setImageDrawable(ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_clear_all, mContext.getTheme()));
					clearAllIcon.getDrawable().setTintList(getThemedColor(mContext));
					clearAllButton.addView(clearAllIcon);

					FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

					params.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, Resources.getSystem().getDisplayMetrics());

					params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;

					clearAllButton.setLayoutParams(params);
					clearAllButton.setOnClickListener(v -> {
						if (recentView != null) {
							try {
								dismissAllTasksMethod.invoke(recentView, v);
							} catch (Throwable ignored) {
							}
						}
					});

//				clearAllButton.setPadding(margins/2,0,margins/2,0);
					FrameLayout parent = (FrameLayout) param.thisObject;
					parent.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT; //resize to whole screen
					parent.addView(clearAllButton);
					clearAllButton.setVisibility(GONE);
				});
	}

	public static ColorStateList getThemedColor(Context context) {
		return getSystemAttrColor(context, SystemUtils.isDarkMode()
				? android.R.attr.textColorPrimaryInverse
				: android.R.attr.textColorPrimary);
	}

	public static ColorStateList getSystemAttrColor(Context context, int attr) {
		try(TypedArray a = context.obtainStyledAttributes(new int[]{attr}))
		{
			return a.getColorStateList(a.getIndex(0));
		}
	}
}
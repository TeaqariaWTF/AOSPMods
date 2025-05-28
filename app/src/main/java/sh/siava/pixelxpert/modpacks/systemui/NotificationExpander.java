package sh.siava.pixelxpert.modpacks.systemui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ObjectTools.tryParseInt;
import static sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectionTools.reAddView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.Collection;
import java.util.regex.Pattern;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.ResourceManager;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;

@SuppressWarnings("RedundantThrows")
public class NotificationExpander extends XposedModPack {
	private static final int DEFAULT = 0;
	private static final int EXPAND_ALWAYS = 1;
	/** @noinspection unused*/
	private static final int COLLAPSE_ALWAYS = 2;

	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	public static boolean notificationExpandallHookEnabled = true;
	public static boolean notificationExpandallEnabled = false;
	private static int notificationDefaultExpansion = DEFAULT;
	private Button ExpandBtn, CollapseBtn;
	private FrameLayout FooterView;
	private LinearLayout BtnLayout;
	private Object Scroller;
	private Object NotifCollection = null;

	public NotificationExpander(Context context) {
		super(context);
	}

	@Override
	public void updatePrefs(String... Key) {
		notificationExpandallEnabled = Xprefs.getBoolean("notificationExpandallEnabled", false);
		notificationExpandallHookEnabled = Xprefs.getBoolean("notificationExpandallHookEnabled", true);
		notificationDefaultExpansion = tryParseInt(Xprefs.getString("notificationDefaultExpansion", "0"), 0);

		if (Key.length > 0 && Key[0].equals("notificationExpandallEnabled")) {
			updateFooterBtn();
		}
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!listenPackage.equals(lpParam.packageName) || !notificationExpandallHookEnabled) return;

		ReflectedClass NotificationStackScrollLayoutClass = ReflectedClass.of("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout");
		ReflectedClass FooterViewButtonClass = ReflectedClass.of("com.android.systemui.statusbar.notification.row.FooterViewButton");
		ReflectedClass NotifCollectionClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.notification.collection.NotifCollection");
		ReflectedClass NotificationPanelViewControllerClass = ReflectedClass.of("com.android.systemui.shade.NotificationPanelViewController");
		ReflectedClass FooterViewClass = ReflectedClass.of("com.android.systemui.statusbar.notification.footer.ui.view.FooterView");

		//region default notification state
		NotificationPanelViewControllerClass
				.before("notifyExpandingStarted")
				.run(param -> {
					if(notificationDefaultExpansion != DEFAULT)
						expandAll(notificationDefaultExpansion == EXPAND_ALWAYS);
				});
		//endregion

		//Notification Footer, where shortcuts should live
		FooterViewClass
				.after(Pattern.compile("updateColors.*"))
				.run(param -> updateFooterBtn());

		FooterViewClass
				.after("onFinishInflate")
				.run(param -> {
					FooterView = (FrameLayout) param.thisObject;

					FooterView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
						@Override
						public void onViewAttachedToWindow(@NonNull View v) {
							updateFooterBtn();
						}
						@Override
						public void onViewDetachedFromWindow(@NonNull View v) {
						}
					});

					BtnLayout = new LinearLayout(mContext);
					BtnLayout.setClipChildren(false);

					LinearLayout.LayoutParams BtnFrameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					BtnFrameParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
					BtnLayout.setLayoutParams(BtnFrameParams);

					reAddView(BtnLayout,FooterView.findViewById(idOf("history_button")));

					ExpandBtn = (Button) FooterViewButtonClass.getClazz().getConstructor(Context.class).newInstance(mContext);
					Drawable expandArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_expand, mContext.getTheme());
					ExpandBtn.setForeground(expandArrows);

					BtnLayout.addView(ExpandBtn);

					reAddView(BtnLayout, FooterView.findViewById(idOf("dismiss_text")));

					ExpandBtn.setOnClickListener(v -> expandAll(true));

					CollapseBtn = (Button) FooterViewButtonClass.getClazz().getConstructor(Context.class).newInstance(mContext);
					Drawable collapseArrows = ResourcesCompat.getDrawable(ResourceManager.modRes, R.drawable.ic_collapse, mContext.getTheme());
					CollapseBtn.setForeground(collapseArrows);


					BtnLayout.addView(CollapseBtn);

					reAddView(BtnLayout,FooterView.findViewById(idOf("settings_button")));

					CollapseBtn.setOnClickListener(v -> expandAll(false));

					updateFooterBtn();
					FooterView.addView(BtnLayout);
				});

		//theme changed
		FooterViewClass
				.after("updateColors")
				.run(param -> updateFooterBtn());

		//grab notification container manager
		if (NotifCollectionClass.getClazz() != null) {
			NotifCollectionClass
					.afterConstruction()
					.run(param -> NotifCollection = param.thisObject);
		}

		//grab notification scroll page
		NotificationStackScrollLayoutClass
				.afterConstruction()
				.run(param -> Scroller = param.thisObject);
	}

	@SuppressLint("DiscouragedApi")
	private int idOf(String name) {
		return mContext.getResources().getIdentifier(name, "id", mContext.getPackageName());
	}

	private void updateFooterBtn() {
		if(FooterView == null) return; //Footer not inflated yet

		Resources res = mContext.getResources();

		Button clearAllButton = BtnLayout.findViewById(idOf("dismiss_text"));

		LinearLayout.LayoutParams dismissParams = (LinearLayout.LayoutParams) clearAllButton.getLayoutParams();
		dismissParams.width = 0;
		dismissParams.weight = 1;

		@SuppressLint({"UseCompatLoadingForDrawables", "DiscouragedApi"})
		Drawable backgroundShape = res.getDrawable(res.getIdentifier("notif_footer_btn_background", "drawable", mContext.getPackageName()), mContext.getTheme());

		ExpandBtn.setBackground(backgroundShape);
		CollapseBtn.setBackground(backgroundShape);


		int textColor =  clearAllButton.getCurrentTextColor();
		ExpandBtn.getForeground().setTint(textColor);
		CollapseBtn.getForeground().setTint(textColor);

		setLayouParams(ExpandBtn, true);
		setLayouParams(CollapseBtn, false);

		ExpandBtn.setVisibility(notificationExpandallEnabled ? VISIBLE : GONE);
		CollapseBtn.setVisibility(ExpandBtn.getVisibility());
	}

	private void setLayouParams(Button button, boolean marginToLeft) {
		int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mContext.getResources().getDisplayMetrics());

		LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(size, size);
		btnParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
		int margin = (int)
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
						8,
						mContext.getResources().getDisplayMetrics());

		if(marginToLeft)
			btnParams.setMarginStart(margin);
		else
			btnParams.setMarginEnd(margin);

		button.post(() -> button.setLayoutParams(btnParams));
	}

	public void expandAll(boolean expand) {
		if (NotifCollection == null) return;

		if (!expand) {
			callMethod(
					Scroller,
					"setOwnScrollY",
					/* pisition */0,
					/* animate */ true);
		}

		Collection<Object> entries;
		//noinspection unchecked
		entries = (Collection<Object>) getObjectField(NotifCollection, "mReadOnlyNotificationSet");
		for (Object entry : entries.toArray()) {
			Object row = getObjectField(entry, "row");
			if (row != null) {
				setRowExpansion(row, expand);
			}
		}
	}

	private void setRowExpansion(Object row, boolean expand) {
		callMethod(row, "setUserExpanded", expand, true);
	}
}

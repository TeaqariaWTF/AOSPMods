package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;


import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.modpacks.Constants;
import sh.siava.pixelxpert.modpacks.XPLauncher;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.SystemUtils;
import sh.siava.pixelxpert.modpacks.utils.toolkit.FakeIntegerResource;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ComposeFontUtils;

@SuppressWarnings("RedundantThrows")
public class QSTileGrid extends XposedModPack {
	public static final String listenPackage = Constants.SYSTEM_UI_PACKAGE;

	private static final int NOT_SET = 0;
	private static final int QS_COL_NOT_SET = 1;

	private static int QSRowQty = NOT_SET;
	private static int QSColQty = QS_COL_NOT_SET;

	private static int QSRowQtyL = NOT_SET;
	private static int QSColQtyL = QS_COL_NOT_SET;

	private static float QSLabelScaleFactor = 1, QSSecondaryLabelScaleFactor = 1;
	private static boolean QSHapticEnabled = false;

	private static int QQSTileRows = NOT_SET;

	public QSTileGrid(Context context) {
		super(context);
	}

	public static boolean isQSHapticEnabled()
	{
		return QSHapticEnabled;
	}
	@Override
	public void updatePrefs(String... Key) {
		if (Xprefs == null) return;

		if(Key.length > 0 && Key[0].equals("VerticalQSTile"))
		{
			SystemUtils.doubleToggleDarkMode();
		}

		QSHapticEnabled = Xprefs.getBoolean("QSHapticEnabled", false);

		QSRowQty = Xprefs.getSliderInt( "QSRowQty", NOT_SET);
		QSColQty = Xprefs.getSliderInt( "QSColQty", QS_COL_NOT_SET);
		if(QSColQty < QS_COL_NOT_SET) QSColQty = QS_COL_NOT_SET;
//		QQSTileQty = Xprefs.getSliderInt( "QQSTileQty", QQS_NOT_SET);

		QSRowQtyL = Xprefs.getSliderInt( "QSRowQtyL", NOT_SET);
		QSColQtyL = Xprefs.getSliderInt( "QSColQtyL", QS_COL_NOT_SET);
		if(QSColQtyL < QS_COL_NOT_SET) QSColQtyL = QS_COL_NOT_SET;
//		QQSTileQtyL = Xprefs.getSliderInt( "QQSTileQtyL", QQS_NOT_SET);

		//TODO Add this pref
		QQSTileRows = Xprefs.getSliderInt( "QQSRows", NOT_SET);

		QSLabelScaleFactor = (Xprefs.getSliderFloat( "QSLabelScaleFactor", 0) + 100) / 100f;
		QSSecondaryLabelScaleFactor = (Xprefs.getSliderFloat( "QSSecondaryLabelScaleFactor", 0) + 100) / 100f;

		if (Key.length > 0 && (Key[0].equals("QSRowQty") || Key[0].equals("QSColQty") || Key[0].equals("QQSTileQty") || Key[0].equals("QSRowQtyL") || Key[0].equals("QSColQtyL") || Key[0].equals("QQSTileQtyL") || Key[0].equals("QQSRows"))) {
			SystemUtils.doubleToggleDarkMode();
		}
	}

	@SuppressLint("DiscouragedApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		if (!lpParam.packageName.equals(listenPackage)) return;

		ReflectedClass PaginatedGridLayoutClass = ReflectedClass.ofIfPossible("com.android.systemui.qs.panels.ui.compose.PaginatedGridLayout");
		ReflectedClass QSColumnsRepositoryClass = ReflectedClass.ofIfPossible("com.android.systemui.qs.panels.data.repository.QSColumnsRepository");
		ReflectedClass QuickQuickSettingsRowRepositoryClass = ReflectedClass.ofIfPossible("com.android.systemui.qs.panels.data.repository.QuickQuickSettingsRowRepository");
		ReflectedClass CommonTileKtClass = ReflectedClass.ofIfPossible("com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileKt");

		//region expressive compose UI rows
		//noinspection unchecked
		final Set<XC_MethodHook.Unhook>[] QSRowsHooks = new Set[1];

		PaginatedGridLayoutClass
				.before("TileGrid")
				.run(param ->
						QSRowsHooks[0] = ReflectedClass.of(Resources.class)
								.before("getInteger")
								.run(param1 -> {
									if(param1.args[0].equals(mContext.getResources().getIdentifier("quick_settings_paginated_grid_num_rows", "integer", mContext.getPackageName()))) {
										boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

										if(isLandscape && QSRowQtyL != NOT_SET)
										{
											param1.setResult(QSRowQtyL);
										}
										if(!isLandscape && QSRowQty != NOT_SET)
										{
											param1.setResult(QSRowQty);
										}
									}
								}));

		//region expressive compose tile label size
		CommonTileKtClass
				.before("TileLabel")
				.run(param -> {
					Object spanStyle = getObjectField(param.args[2], "spanStyle");

					if(Boolean.valueOf(true).equals(getAdditionalInstanceField(param.args[2], "Scaled")))
					{
						return;
					}

					setAdditionalInstanceField(param.args[2], "Scaled", true);

					long longFontSize = (long) getObjectField(spanStyle, "fontSize");

					setObjectField(spanStyle, "fontSize", ComposeFontUtils.Companion.scaleTileFont(mContext, longFontSize, QSLabelScaleFactor, QSSecondaryLabelScaleFactor));
				});
		//endregion

		PaginatedGridLayoutClass
				.after("TileGrid")
				.run(param -> {
					try {
						QSRowsHooks[0].forEach(XC_MethodHook.Unhook::unhook);
					}
					finally {
						QSRowsHooks[0] = null;
					}
				});
		//endregion

		//region expressive compose UI cols
		QSColumnsRepositoryClass
				.beforeConstruction()
				.run(param ->
						param.args[0] = new FakeIntegerResource(mContext) {
							@Override
							public int getInteger(int id) {
								if(mContext.getResources().getResourceName(id).endsWith("quick_settings_infinite_grid_num_columns")) {
									boolean isLandscape = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
									//TODO must remove *2 after UI supported it
									if (isLandscape && QSColQtyL != NOT_SET) {
										return 8; //QSColQtyL;
									}
									if (!isLandscape && QSColQty != NOT_SET) {
										return 5; //QSColQty;
									}
								}

								return mContext.getResources().getInteger(id);
							}
						});
		//endregion

		//region expressive compose UI QQS rows
		QuickQuickSettingsRowRepositoryClass
				.beforeConstruction()
				.run(param ->
					param.args[0] = new FakeIntegerResource(mContext) {
						@Override
						public int getInteger(int id) {
							if(getResourceName(id).endsWith("quick_qs_paginated_grid_num_rows") && QQSTileRows != NOT_SET)
							{
								return QQSTileRows;
							}
							return mContext.getResources().getInteger(id);
						}
					});
	}

	@Override
	public boolean listensTo(String packageName) {
		return listenPackage.equals(packageName) && !XPLauncher.isChildProcess;
	}
}
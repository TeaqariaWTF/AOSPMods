package sh.siava.pixelxpert.xposed.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.modpacks.systemui.StatusbarMods.APP_SWITCH_SLOT;

import android.content.Context;
import android.util.ArraySet;
import android.view.View;

import java.util.ArrayList;
import java.util.Set;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.xposed.annotations.SystemUIModPack;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;

@SystemUIModPack
public class StatusIconTuner extends XposedModPack {
	private static Set<String> SBIgnoredIcons = new ArraySet<>();
	private static Set<String> KGIgnoredIcons = new ArraySet<>();
	private static Set<String> QSIgnoredIcons = new ArraySet<>();

	private Object mSBIconContainer, mQSIconContainer, mKGIconContainer;

	public StatusIconTuner(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		SBIgnoredIcons = Xprefs.getStringSet("SBIgnoredIcons", new ArraySet<>());
		KGIgnoredIcons = Xprefs.getStringSet("KGIgnoredIcons", new ArraySet<>());
		QSIgnoredIcons = Xprefs.getStringSet("QSIgnoredIcons", new ArraySet<>());

		KGIgnoredIcons.add(APP_SWITCH_SLOT); //we don't need it on keyguard

		setIgnoredIcons(mSBIconContainer, SBIgnoredIcons);
		setIgnoredIcons(mKGIconContainer, KGIgnoredIcons);
		setIgnoredIcons(mQSIconContainer, QSIgnoredIcons);
	}

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass IconManagerClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.phone.ui.IconManager");
		if(IconManagerClass.getClazz() == null) //pre 15beta3
		{
			IconManagerClass = ReflectedClass.ofIfPossible("com.android.systemui.statusbar.phone.StatusBarIconController$IconManager");
		}
//		ReflectedClass StatusIconContainerClass = ReflectedClass.of("com.android.systemui.statusbar.phone.StatusIconContainer");

		IconManagerClass
				.beforeConstruction()
				.run(param -> {
					try {
						View iconContainer = (View) param.args[0];

						String id = mContext.getResources().getResourceName(((View) iconContainer.getParent().getParent()).getId()); //helps getting exception if it's in QS

						if (id.contains("status_bar_end_side_content")) //statusbar specific id
						{
							mSBIconContainer = iconContainer;
							setIgnoredIcons(iconContainer, SBIgnoredIcons);
						}
						else if(id.contains("shade_header_system_icons")) //QS specific id
						{
							mQSIconContainer = iconContainer;
							setIgnoredIcons(iconContainer, QSIgnoredIcons);
						}
						else if(id.contains("system_icons_container")) //KG specific id
						{
							mKGIconContainer = iconContainer;
							setIgnoredIcons(iconContainer, KGIgnoredIcons);
						}
					}
					catch (Throwable ignored){}
				});
	}

	private void setIgnoredIcons(Object container, Set<String> ignorableSlots){
		try
		{
			//noinspection unchecked
			ArrayList<String> ignoredSlots = (ArrayList<String>) getObjectField(container, "mIgnoredSlots");

			ignoredSlots.clear();

			for (String slot : ignorableSlots) {
				if(!ignoredSlots.contains(slot))
				{
					ignoredSlots.add(slot);
				}
			}

			((View)container).requestLayout();
		}
		catch (Throwable ignored){
		}
	}
}
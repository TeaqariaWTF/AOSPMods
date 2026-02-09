package sh.siava.pixelxpert.xposed.utils;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;

public class SystemUIDependencyProvider {
	private static Object sDependency;
	public static Object get(String fullClassName)
	{
		if(sDependency == null)
			getInstance();

		try {
			return callMethod(sDependency, "getDependencyInner", ReflectedClass.of(fullClassName).getClazz());
		}
		catch (Throwable ignored)
		{
			return null;
		}
	}

	private static void getInstance() {
		ReflectedClass d = ReflectedClass.of("com.android.systemui.Dependency");
		sDependency = getStaticObjectField(d.getClazz(), "sDependency");
	}
}
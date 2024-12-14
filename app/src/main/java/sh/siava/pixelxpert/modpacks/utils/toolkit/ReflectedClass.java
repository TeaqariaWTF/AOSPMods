package sh.siava.pixelxpert.modpacks.utils.toolkit;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import android.util.ArraySet;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** @noinspection unused*/
public class ReflectedClass
{
	Class<?> clazz;
	public ReflectedClass(Class<?> clazz)
	{
		this.clazz = clazz;
	}
	public static ReflectedClass of(Class<?> clazz)
	{
		return new ReflectedClass(clazz);
	}

	public static ReflectedClass of(String name, ClassLoader loader)
	{
		return new ReflectedClass(findClass(name, loader));
	}

	public Class<?> getClazz()
	{
		return clazz;
	}

	public static ReflectedClass ofIfPossible(String name, ClassLoader loader)
	{
		return new ReflectedClass(findClassIfExists(name, loader));
	}

	public BeforeMethodData before(String methodName)
	{
		return new BeforeMethodData(clazz, methodName, false);
	}

	public BeforeMethodData beforeConstruction()
	{
		return new BeforeMethodData(clazz, null, true);
	}

	public AfterMethodData after(String methodName)
	{
		return new AfterMethodData(clazz, methodName, false);
	}

	public AfterMethodData afterConstruction()
	{
		return new AfterMethodData(clazz, null, true);
	}

	public Object callStaticMethod(String methodName, Object... args)
	{
		return XposedHelpers.callStaticMethod(clazz, methodName, args);
	}

	private static class MethodData
	{
		String methodName;
		Class<?> clazz;
		boolean isConstructor;
		private MethodData(Class<?> clazz, String name, boolean isConstructor)
		{
			this.clazz = clazz;
			this.methodName = name;
			this.isConstructor = isConstructor;
		}

		protected Set<XC_MethodHook.Unhook> runBefore(ReflectionConsumer consumer)
		{
			if(clazz == null) return new ArraySet<>();

			if(isConstructor)
			{
				return hookAllConstructors(clazz, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						consumer.run(param);
					}
				});
			}
			else
			{
				return hookAllMethods(clazz, methodName, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						consumer.run(param);
					}
				});
			}
		}

		protected Set<XC_MethodHook.Unhook> runAfter(ReflectionConsumer consumer)
		{
			if(clazz == null) return new ArraySet<>();

			if(isConstructor)
			{
				return hookAllConstructors(clazz, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						consumer.run(param);
					}
				});
			}
			else
			{
				return hookAllMethods(clazz, methodName, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						consumer.run(param);
					}
				});
			}
		}
	}

	public class BeforeMethodData extends MethodData
	{
		private BeforeMethodData(Class<?> clazz, String name, boolean isConstructor)
		{
			super(clazz, name, isConstructor);
		}

		public Set<XC_MethodHook.Unhook> run(ReflectionConsumer consumer)
		{
			return runBefore(consumer);
		}
	}

	public class AfterMethodData extends MethodData
	{
		private AfterMethodData(Class<?> clazz, String name, boolean isConstructor)
		{
			super(clazz, name, isConstructor);
		}

		public Set<XC_MethodHook.Unhook> run(ReflectionConsumer consumer)
		{
			return runAfter(consumer);
		}
	}

	public interface ReflectionConsumer
	{
		void run(XC_MethodHook.MethodHookParam param) throws Throwable;
	}
}
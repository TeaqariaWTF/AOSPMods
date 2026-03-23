package sh.siava.pixelxpert.xposed.utils.reflection;

import androidx.collection.ArraySet;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

public class HookHelper {
	public static Set<XposedInterface.HookHandle> hookAllConstructors(Class<?> hookClass, ReflectedClass.ReflectionConsumer callback, boolean runBefore, XposedInterface xposedInterface) {
		ArraySet<XposedInterface.HookHandle> result = new ArraySet<>();
		for(Executable constructor : findConstructors(hookClass)) {
			result.add(
			xposedInterface.hook(constructor)
					.intercept(chain -> {
						RunParam param = new RunParam();
						param.args = chain.getArgs().toArray();
						param.thisObject = chain.getThisObject();
						param.method = chain.getExecutable();
						if(runBefore)
						{
							callback.run(param);
							if(param.isResultSet)
								return param.result; //we won't proceed if result is already set

							return chain.proceed(param.args);
						}
						else
						{
							param.result = chain.proceed(param.args);
							callback.run(param);
							return param.result;
						}
					}));
		}
		return result;
	}

	public static Set<XposedInterface.HookHandle> hookAllMethods(Class<?> hookClass, String methodName, ReflectedClass.ReflectionConsumer callback, boolean runBefore, XposedInterface xposedInterface) {
		ArraySet<XposedInterface.HookHandle> result = new ArraySet<>();
		for(Executable method : findMethods(hookClass, methodName)) {
			result.add(hookMethod(method, callback, runBefore, xposedInterface));
		}
		return result;
	}

	public static XposedInterface.HookHandle hookMethod(Executable hookMethod, ReflectedClass.ReflectionConsumer callback, boolean runBefore, XposedInterface xposedInterface) {
		return xposedInterface.hook(hookMethod)
				.intercept(chain -> {
					RunParam param = new RunParam();
					param.args = chain.getArgs().toArray();
					param.thisObject = chain.getThisObject();
					param.method = chain.getExecutable();

					if(runBefore)
					{
						callback.run(param);
						if(param.isResultSet)
							return param.result; //we won't proceed if result is already set

						return chain.proceed(param.args);
					}
					else
					{
						param.result = chain.proceed(param.args);
						callback.run(param);
						return param.result;
					}
				});
	}

	private static Set<Executable> findConstructors(Class<?> clazz)
	{
		return new ArraySet<>(clazz.getDeclaredConstructors());
	}

	private static Set<Method> findMethods(Class<?> clazz, String name) {
		return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getName().equals(name)).collect(ArraySet::new, ArraySet::add, ArraySet::addAll);
	}

	public static class RunParam
	{
		public Object thisObject;
		public Executable method;
		public Object[] args;
		private Object result;
		private boolean isResultSet = false;
		public void setResult(Object result)
		{
			isResultSet = true;
			this.result = result;
		}
		public Object getResult()
		{
			return result;
		}
	}
}
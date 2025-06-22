package sh.siava.pixelxpert.xposed;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static de.robv.android.xposed.XposedBridge.log;
import static sh.siava.pixelxpert.BuildConfig.APPLICATION_ID;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.BootLoopProtector.isBootLooped;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dalvik.system.DexFile;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.IRootProviderProxy;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.annotations.CommonModPack;
import sh.siava.pixelxpert.xposed.annotations.DialerModPack;
import sh.siava.pixelxpert.xposed.annotations.FrameworkModPack;
import sh.siava.pixelxpert.xposed.annotations.KSUModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.annotations.SettingsModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIChildProcessModPack;
import sh.siava.pixelxpert.xposed.annotations.SystemUIMainProcessModPack;
import sh.siava.pixelxpert.xposed.annotations.TelecomServerModPack;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.service.RootProviderProxy;

@SuppressWarnings("RedundantThrows")
public class XPLauncher implements ServiceConnection {

	private boolean mIsChildProcess = false;
	public static String processName = "";

	public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
	public Context mContext = null;
	@SuppressLint("StaticFieldLeak")
	static XPLauncher instance;

	private CountDownLatch rootProxyCountdown = new CountDownLatch(1);
	private static IRootProviderProxy rootProxyIPC;
	private static final Queue<ProxyRunnable> proxyQueue = new LinkedList<>();

	/**
	 * @noinspection FieldCanBeLocal
	 */
	public XPLauncher() {
		instance = this;
	}

	public static IRootProviderProxy getRootProviderProxy() {
		if (rootProxyIPC == null) {
			instance.rootProxyCountdown = new CountDownLatch(1);
			instance.forceConnectRootService();
			try {
				//noinspection ResultOfMethodCallIgnored
				instance.rootProxyCountdown.await(5, TimeUnit.SECONDS);
			} catch (Throwable ignored) {
			}
		}
		return rootProxyIPC;
	}

	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		try {
			mIsChildProcess = lpParam.processName.contains(":");
			processName = lpParam.processName;
		} catch (Throwable ignored) {
			mIsChildProcess = false;
		}

		if (lpParam.packageName.equals(Constants.SYSTEM_FRAMEWORK_PACKAGE)) {
			ReflectedClass PhoneWindowManagerClass = ReflectedClass.of("com.android.server.policy.PhoneWindowManager");

			PhoneWindowManagerClass
					.before("init")
					.run(param -> {
						try {
							if (mContext == null) {
								mContext = (Context) param.args[0];

								ResourceManager.modRes = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
										.getResources();

								XPrefs.init(mContext);

								CompletableFuture.runAsync(() -> waitForXprefsLoad(lpParam));
							}
						} catch (Throwable t) {
							log(t);
						}
					});
		} else {
			ReflectedClass.of(Instrumentation.class)
					.after("newApplication")
					.run(param -> {
						try {
							if (mContext == null || lpParam.packageName.equals(Constants.TELECOM_SERVER_PACKAGE)) { //telecom service launches as a secondary process in framework, but has its own package name. context is not null when it loads
								mContext = (Context) param.args[param.args.length - 1];

								ResourceManager.modRes = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
										.getResources();

								XPrefs.init(mContext);

								waitForXprefsLoad(lpParam);
							}
						} catch (Throwable t) {
							log(t);
						}

					});
		}
	}

	private void onXPrefsReady(XC_LoadPackage.LoadPackageParam lpParam) {
		if (isBootLooped(lpParam.packageName)) {
			log(String.format("PixelXpert: Possible bootloop in %s. Will not load for now", lpParam.packageName));
			return;
		}

		new SystemUtils(mContext);
		XPrefs.setPackagePrefs(lpParam.packageName);

		loadModpacks(lpParam);
	}

	private void loadModpacks(XC_LoadPackage.LoadPackageParam lpParam) {
		if (Arrays.asList(ResourceManager.modRes.getStringArray(R.array.root_requirement)).contains(lpParam.packageName)) {
			forceConnectRootService();
		}

		Class<? extends Annotation> annotation = getModMapping(lpParam);

		try {
			//noinspection deprecation
			DexFile moduleDex = new DexFile(ResourceManager.getModulePath());

			ClassLoader moduleClassloader = this.getClass().getClassLoader();
			//noinspection deprecation
			Enumeration<String> moduleClasses = moduleDex.entries();
			while (moduleClasses.hasMoreElements()) {
				String className = moduleClasses.nextElement();
				if (className.startsWith(APPLICATION_ID + ".xposed.modpacks")) {
					try {
						//noinspection DataFlowIssue
						Class<?> thisClass = moduleClassloader.loadClass(className);

						if (thisClass.isAnnotationPresent(CommonModPack.class) || thisClass.isAnnotationPresent(annotation)) {
							//noinspection ControlFlowStatementWithoutBraces,DataFlowIssue
							if(annotation.equals(SystemUIChildProcessModPack.class)
									&& !lpParam.processName.contains(thisClass.getAnnotation(SystemUIChildProcessModPack.class).processNameContains()))
								continue;

							//noinspection unchecked
							loadModPack((Class<? extends XposedModPack>) thisClass, lpParam);
						}
					} catch (Throwable ignored) {
					}
				}
			}
		}
		catch (Throwable ignored){}
	}

	private Class<? extends Annotation> getModMapping(XC_LoadPackage.LoadPackageParam lpParam) {
		HashMap<String, Class<? extends Annotation>> modMapping = new HashMap<>();
		modMapping.put(Constants.LAUNCHER_PACKAGE, LauncherModPack.class);
		modMapping.put(Constants.SETTINGS_PACKAGE, SettingsModPack.class);
		modMapping.put(Constants.DIALER_PACKAGE, DialerModPack.class);
		modMapping.put(Constants.TELECOM_SERVER_PACKAGE, TelecomServerModPack.class);
		modMapping.put(Constants.SYSTEM_FRAMEWORK_PACKAGE, FrameworkModPack.class);
		modMapping.put(Constants.KSU_PACKAGE, KSUModPack.class);
		modMapping.put(Constants.KSU_NEXT_PACKAGE, KSUModPack.class);

		return lpParam.packageName.equals(Constants.SYSTEM_UI_PACKAGE)
				? mIsChildProcess
					? SystemUIChildProcessModPack.class
					: SystemUIMainProcessModPack.class
				:modMapping.get(lpParam.packageName);
	}

	private void loadModPack(Class<? extends XposedModPack> thisClass, XC_LoadPackage.LoadPackageParam lpParam) {
		try {
			XposedModPack instance = thisClass.getConstructor(Context.class).newInstance(mContext);
			try {
				instance.onPreferenceUpdated();
			} catch (Throwable ignored) {
			}
			instance.onPackageLoadedInternal(lpParam);
			runningMods.add(instance);
		} catch (Throwable T) {
			log("Start Error Dump - Occurred in " + thisClass.getName());
			log(T);
		}
	}

	private void forceConnectRootService() {
		new Thread(() -> {
			while (SystemUtils.UserManager() == null
					|| !SystemUtils.UserManager().isUserUnlocked()) //device is still CE encrypted
			{
				SystemUtils.threadSleep(2000);
			}
			SystemUtils.threadSleep(5000); //wait for the unlocked account to settle down a bit

			while (rootProxyIPC == null) {
				connectRootService();
				SystemUtils.threadSleep(5000);
			}
		}).start();
	}

	private void connectRootService() {
		try {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(APPLICATION_ID, RootProviderProxy.class.getName()));
			mContext.bindService(intent, instance, Context.BIND_AUTO_CREATE | Context.BIND_ADJUST_WITH_ACTIVITY);
		} catch (Throwable t) {
			log(t);
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		rootProxyIPC = IRootProviderProxy.Stub.asInterface(service);
		rootProxyCountdown.countDown();

		synchronized (proxyQueue) {
			while (!proxyQueue.isEmpty()) {
				try {
					Objects.requireNonNull(proxyQueue.poll()).run(rootProxyIPC);
				} catch (Throwable ignored) {
				}
			}
		}
	}

	private void waitForXprefsLoad(XC_LoadPackage.LoadPackageParam lpParam) {
		while (true) {
			try {
				Xprefs.getBoolean("LoadTestBooleanValue", false);
				break;
			} catch (Throwable ignored) {
				SystemUtils.threadSleep(1000);
			}
		}

		log(String.format("Loading PixelXpert version: %s on %s", BuildConfig.VERSION_NAME, lpParam.packageName));
		try {
			log("PixelXpert Records: " + Xprefs.getAll().size());
		} catch (Throwable ignored) {
		}

		onXPrefsReady(lpParam);
	}


	@Override
	public void onServiceDisconnected(ComponentName name) {
		rootProxyIPC = null;

		forceConnectRootService();
	}

	public static void enqueueProxyCommand(ProxyRunnable runnable) {
		if (rootProxyIPC != null) {
			try {
				runnable.run(rootProxyIPC);
			} catch (RemoteException ignored) {
			}
		} else {
			synchronized (proxyQueue) {
				proxyQueue.add(runnable);
			}
			instance.forceConnectRootService();
		}
	}

	public interface ProxyRunnable {
		void run(IRootProviderProxy proxy) throws RemoteException;
	}
}
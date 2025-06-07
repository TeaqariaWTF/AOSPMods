package sh.siava.pixelxpert.modpacks.utils;

import com.topjohnwu.superuser.Shell;

import sh.siava.pixelxpert.modpacks.XPLauncher;

public class ModuleFolderOperations {
	public static void applyVolumeSteps(int volumeStps, String rootPath, boolean runFromApp) {
		if (volumeStps <= 10) {
			runRootCommand("rm -Rf " + rootPath + "/system.prop", runFromApp);
			return;
		}
		runRootCommand("echo ro.config.media_vol_steps=" + volumeStps + " > " + rootPath + "/system.prop", runFromApp);
	}

	private static void runRootCommand(String command, boolean runFromApp)
	{
		try
		{
			if(runFromApp)
			{
				Shell.cmd(command).exec();
			}
			else
			{
				XPLauncher.enqueueProxyCommand(proxy -> proxy.runCommand(command));
			}
		}
		catch (Throwable ignored){}
	}
}
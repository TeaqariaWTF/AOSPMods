package sh.siava.pixelxpert.xposed.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If defined, it means the mod pack is targeting the child process, and not main process
 * If both child AND main process are targeted, @MainProcessModPack must be defined alongside this annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChildProcessModPack {
	String processNameContains();
}

package sh.siava.pixelxpert.xposed.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * By default, all mod packs are assumed @MainProcessModPack. However, if @ChildProcessModPack is defined, use this annotation
 * to show that main process must be targeted too. Otherwise, it's optional
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MainProcessModPack {
}

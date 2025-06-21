package sh.siava.pixelxpert.modpacks.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static sh.siava.pixelxpert.modpacks.XPrefs.Xprefs;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.pixelxpert.annotations.SystemUIMainProcessModPack;
import sh.siava.pixelxpert.modpacks.XposedModPack;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass;
import sh.siava.pixelxpert.modpacks.utils.toolkit.ReflectedClass.ReflectionConsumer;

@SuppressWarnings("RedundantThrows")
@SystemUIMainProcessModPack
public class KeyGuardPinScrambler extends XposedModPack {
	private static boolean shufflePinEnabled = false;

	public KeyGuardPinScrambler(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		shufflePinEnabled = Xprefs.getBoolean("shufflePinEnabled", false);
	}

	final List<Integer> digits = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);

	@Override
	public void onPackageLoaded(XC_LoadPackage.LoadPackageParam lpParam) throws Throwable {
		ReflectedClass KeyguardPinBasedInputViewClass = ReflectedClass.of("com.android.keyguard.KeyguardPinBasedInputView");

		ReflectionConsumer pinShuffleHook = param -> {
			if (!shufflePinEnabled) return;

			Collections.shuffle(digits);

			Object[] mButtons = (Object[]) getObjectField(param.thisObject, "mButtons");

			for(Object button : mButtons)
			{
				int mDigit = getIntField(button, "mDigit");
				setObjectField(button, "mDigit", digits.get(mDigit));

				callMethod(
						getObjectField(button, "mDigitText"),
						"setText",
						Integer.toString(digits.get(mDigit)));
			}
		};


		KeyguardPinBasedInputViewClass.after("onFinishInflate").run(pinShuffleHook);
		KeyguardPinBasedInputViewClass.after("resetPasswordText").run(pinShuffleHook);
	}
}
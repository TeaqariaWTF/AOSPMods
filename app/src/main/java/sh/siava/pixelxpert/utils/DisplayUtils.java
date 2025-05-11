package sh.siava.pixelxpert.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import sh.siava.pixelxpert.PixelXpert;

public class DisplayUtils {
	public static boolean isTablet() {
		return PixelXpert.get().getResources().getConfiguration().smallestScreenWidthDp >= 600;
	}
}

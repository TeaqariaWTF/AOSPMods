package sh.siava.pixelxpert.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import sh.siava.pixelxpert.PixelXpert;

public class DisplayUtils {

    @SuppressWarnings("deprecation")
    public static boolean isTablet() {
        DisplayMetrics metrics = new DisplayMetrics();

        WindowManager windowManager = (WindowManager) PixelXpert.get().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);

            float yInches = metrics.heightPixels / metrics.ydpi;
            float xInches = metrics.widthPixels / metrics.xdpi;
            double diagonalInches = Math.sqrt(Math.pow(xInches, 2) + Math.pow(yInches, 2));

            return diagonalInches >= 7.0;
        }

        return false;
    }
}

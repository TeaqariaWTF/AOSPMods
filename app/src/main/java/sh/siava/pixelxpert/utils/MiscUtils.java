package sh.siava.pixelxpert.utils;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import sh.siava.pixelxpert.PixelXpert;
import sh.siava.pixelxpert.R;

public class MiscUtils {

	public static @ColorInt int getColorFromAttribute(Context context, int attr) {
		TypedValue typedValue = new TypedValue();
		context.getTheme().resolveAttribute(attr, typedValue, true);
		return typedValue.data;
	}

	public static String intToHex(int colorValue) {
		return String.format("#%06X", (0xFFFFFF & colorValue));
	}

	public static int dpToPx(int dp) {
		return dpToPx((float) dp);
	}

	public static int dpToPx(float dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, PixelXpert.get().getResources().getDisplayMetrics());
	}

	public static void setupToolbar(AppCompatActivity baseContext, Toolbar toolbar, String title, boolean isBackButtonEnabled) {
		if (baseContext != null) {
			if (toolbar != null) {
				baseContext.setSupportActionBar(toolbar);
				toolbar.setTitle(title);
			}
			if (baseContext.getSupportActionBar() != null) {
				baseContext.getSupportActionBar().setDisplayHomeAsUpEnabled(isBackButtonEnabled);
				baseContext.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_toolbar_chevron);
			}
		}
	}
}

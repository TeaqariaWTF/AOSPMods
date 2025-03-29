package sh.siava.pixelxpert.ui.activities;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.shape.MaterialShapeDrawable;

import java.util.Locale;

import sh.siava.pixelxpert.R;

public class BaseActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupEdgeToEdge();
	}

	private void setupEdgeToEdge() {
		try {
			((AppBarLayout) findViewById(R.id.appBarLayout)).setStatusBarForeground(MaterialShapeDrawable.createWithElevationOverlay(getApplicationContext()));
		} catch (Exception ignored) {
		}

		Window window = getWindow();
		WindowCompat.setDecorFitsSystemWindows(window, false);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			ViewGroup viewGroup = getWindow().getDecorView().findViewById(android.R.id.content);
			ViewCompat.setOnApplyWindowInsetsListener(viewGroup, (v, windowInsets) -> {
				Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

				ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
				v.setPadding(
						params.leftMargin + insets.left,
						0,
						params.rightMargin + insets.right,
						0
				);
				params.bottomMargin = 0;
				v.setLayoutParams(params);

				return windowInsets;
			});
		}
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(newBase);

		SharedPreferences prefs = getDefaultSharedPreferences(newBase.createDeviceProtectedStorageContext());

		String localeCode = prefs.getString("appLanguage", "");
		Locale locale = !localeCode.isEmpty() ? Locale.forLanguageTag(localeCode) : Locale.getDefault();

		Resources res = newBase.getResources();
		Configuration configuration = res.getConfiguration();

		configuration.setLocale(locale);

		LocaleList localeList = new LocaleList(locale);
		LocaleList.setDefault(localeList);
		configuration.setLocales(localeList);

		applyOverrideConfiguration(configuration);
	}
}

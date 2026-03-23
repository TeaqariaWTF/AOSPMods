package sh.siava.pixelxpert.xposed.utils.toolkit;

import android.util.Log;
public class Logger {
	public static String TAG = "PixelXpert Lsposed Module";
	public static synchronized void log(String text) {
		Log.w(TAG, text);
	}

	public static synchronized void log(String text, Throwable t) {
		Log.e(TAG, text, t);
	}

	public static synchronized void log(Throwable t) {
		Log.e(TAG, "", t);
	}
}

package sh.siava.pixelxpert.xposed.utils.toolkit;

import android.util.Log;
public class Logger {
	public static String TAG = "PixelXpert Lsposed Module";

	/**
	 * Logs to logcat. tagged with {@link #TAG}
	 * @param text
	 */
	public static synchronized void log(String text) {
		Log.w(TAG, text);
	}
	/**
	 * Logs to logcat. tagged with {@link #TAG}
	 * @param text
	 */
	public static synchronized void log(String text, Throwable t) {
		Log.e(TAG, text, t);
	}
	/**
	 * Logs to logcat. tagged with {@link #TAG}
	 * @param t
	 */
	public static synchronized void log(Throwable t) {
		Log.e(TAG, "", t);
	}
}

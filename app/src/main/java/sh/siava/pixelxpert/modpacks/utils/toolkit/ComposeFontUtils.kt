package sh.siava.pixelxpert.modpacks.utils.toolkit

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp
import de.robv.android.xposed.XposedHelpers

class ComposeFontUtils {
	companion object {
		@SuppressLint("DiscouragedApi")
		fun scaleTileFont(
			context: Context,
			currentSize: Long,
			labelScale: Float,
			secondaryLabelScale: Float
		) : Long
		{
			var res = context.resources

			var originalSpanStyle = SpanStyle(fontSize = 1.sp)
			XposedHelpers.setObjectField(originalSpanStyle, "fontSize", currentSize)

			var originalFontSizeSP = originalSpanStyle.fontSize.value

			var scaledFontSizeSP = originalFontSizeSP

			if(originalFontSizeSP == getSpFromDimen(res, res.getIdentifier("content_text_size_for_large", "dimen", context.packageName)))
			{
				scaledFontSizeSP = originalFontSizeSP * labelScale
			}
			else if(originalFontSizeSP == getSpFromDimen(res, res.getIdentifier("content_text_size_for_medium", "dimen", context.packageName)))
			{
				scaledFontSizeSP = originalFontSizeSP * secondaryLabelScale
			}

			var scaledSpanStyle = SpanStyle(fontSize = scaledFontSizeSP.sp)

			return XposedHelpers.getObjectField(scaledSpanStyle.fontSize, "packedValue") as Long
		}

		fun getSpFromDimen(
			res: Resources,
			dimenResId: Int
		): Float? {
			var typedValue = TypedValue()
			try {
				res.getValue(dimenResId, typedValue, true)
				if (typedValue.type == TypedValue.TYPE_DIMENSION &&
					typedValue.complexUnit == TypedValue.COMPLEX_UNIT_SP) {
					return TypedValue.complexToFloat(typedValue.data)
				}
			} catch (_ : Throwable) {
				return null
			}
			return null
		}
	}
}
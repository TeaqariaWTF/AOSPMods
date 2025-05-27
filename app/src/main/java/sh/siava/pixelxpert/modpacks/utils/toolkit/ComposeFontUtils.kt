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
		var TILE_LABEL_STYLE = "TextAppearance.QS.TileLabel"
		var TILE_SUBTITLE_STYLE = "TextAppearance.TileDetailsEntrySubTitle"
		@SuppressLint("DiscouragedApi")
		fun scaleTileFont(
			context: Context,
			currentSize: Long,
			labelScale: Float,
			secondaryLabelScale: Float
		) : Long
		{
			var originalSpanStyle = SpanStyle(fontSize = 1.sp)
			XposedHelpers.setObjectField(originalSpanStyle, "fontSize", currentSize)

			var originalFontSizeSP = originalSpanStyle.fontSize.value

			var scaledFontSizeSP = originalFontSizeSP

			if(originalFontSizeSP == ResourceTools.getSpTextSizeFromStyle(context, TILE_LABEL_STYLE))
			{
				scaledFontSizeSP = originalFontSizeSP * labelScale
			}
			else if(originalFontSizeSP == ResourceTools.getSpTextSizeFromStyle(context, TILE_SUBTITLE_STYLE))
			{
				scaledFontSizeSP = originalFontSizeSP * secondaryLabelScale
			}

			var scaledSpanStyle = SpanStyle(fontSize = scaledFontSizeSP.sp)

			return XposedHelpers.getObjectField(scaledSpanStyle.fontSize, "packedValue") as Long
		}
	}
}
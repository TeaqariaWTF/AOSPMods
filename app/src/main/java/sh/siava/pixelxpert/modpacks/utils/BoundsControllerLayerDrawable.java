package sh.siava.pixelxpert.modpacks.utils;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoundsControllerLayerDrawable extends LayerDrawable {
	final List<Drawable> mLayers = new ArrayList<>();

	public BoundsControllerLayerDrawable(Drawable[] layers) {
		super(layers);
		Collections.addAll(mLayers, layers);
	}

	@Override
	public int addLayer(Drawable layer)
	{
		mLayers.add(layer);
		return super.addLayer(layer);
	}

	@Override
	public void setBounds(@NonNull Rect bounds)
	{
		mLayers.forEach(l -> l.setBounds(bounds));
	}

	@Override
	public void setBounds(int a, int b, int c, int d)
	{
		mLayers.forEach(l -> l.setBounds(a, b, c, d));
	}
}

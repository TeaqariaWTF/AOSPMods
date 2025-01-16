package sh.siava.pixelxpert.modpacks.systemui;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Objects;

public class SlidingTileController {
	private static final HashMap<String, HashMap<WeakReference<Object>,WeakReference<Object>>> controlledTiles = new HashMap<>();

	public static void addTile(@NonNull String spec, @NonNull Object tile, @NonNull Object tileView)
	{
		if(!controlledTiles.containsKey(spec))
		{
			controlledTiles.put(spec, new HashMap<>());
		}

		Objects.requireNonNull(controlledTiles.get(spec)).put(new WeakReference<>(tile), new WeakReference<>(tileView));
	}

	public static HashMap<Object, Object> getTiles(String spec)
	{
		HashMap<WeakReference<Object>, WeakReference<Object>> tiles = controlledTiles.get(spec);
		HashMap<Object, Object> result = new HashMap<>();

		for(WeakReference<Object> tileRef : tiles.keySet())
		{
			Object tile = tileRef.get();
			Object tileView = tiles.get(tileRef).get();

			if(tile != null && tileView != null) {
				result.put(tile, tileView);
			}
			else
			{
				tiles.remove(tileRef);
			}
		}

		return result;
	}
}

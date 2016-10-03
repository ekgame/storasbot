package lt.ekgame.storasbot.plugins.beatmap_cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.tillerino.osuApiModel.OsuApiBeatmap;

import lt.ekgame.storasbot.StorasDiscord;

public class OsuBeatmapCatche {
	
	private static final long CACHE_TIMEOUT = 1000*60*60;
	private static final OsuBeatmapCatche INSTANCE = new OsuBeatmapCatche();
	
	private Map<Integer, CachedBeatmap> cache = new HashMap<>();
	
	private OsuBeatmapCatche() {}
	
	public static CachedBeatmap getCachedBeatmap(String beatmapId) {
		return INSTANCE.getEntry(Integer.parseInt(beatmapId));
	}
	
	public static CachedBeatmap getCachedBeatmap(int beatmapId) {
		return INSTANCE.getEntry(beatmapId);
	}
	
	public static boolean hasBeatmap(int beatmapId) {
		return INSTANCE.cache.containsKey(beatmapId) && INSTANCE.cache.get(beatmapId) != null;
	}
	
	public static void addEntry(int beatmapId, OsuApiBeatmap beatmap) {
		if (beatmap != null)
			INSTANCE.cache.put(beatmapId, new CachedBeatmap(System.currentTimeMillis(), beatmap));
	}
	
	private CachedBeatmap getEntry(int beatmapId) {
		if (cache.containsKey(beatmapId)) {
			CachedBeatmap entry = cache.get(beatmapId);
			if (entry != null && System.currentTimeMillis() - entry.getUpdateTimestamp() > CACHE_TIMEOUT) {
				entry = updateEntry(entry);
				cache.put(beatmapId, entry);
			}
			else if (entry == null) {
				entry = discoverEntry(beatmapId);
			}
			return entry;
		}
		else {
			CachedBeatmap entry = discoverEntry(beatmapId);
			cache.put(beatmapId, entry);
			return entry;
		}
	}

	private CachedBeatmap updateEntry(CachedBeatmap entry) {
		int beatmapId = entry.getApiBeatmap().getBeatmapId();
		CachedBeatmap updated = discoverEntry(beatmapId);
		if (updated == null) // failed to update
			return entry;    // return catched
		
		if (updated.getApiBeatmap().getLastUpdate() != entry.getApiBeatmap().getLastUpdate()) {
			entry.update(System.currentTimeMillis(), updated.getApiBeatmap());
		}
		
		return entry;
	}
	
	private CachedBeatmap discoverEntry(int beatmapId) {
		try {
			OsuApiBeatmap beatmap = StorasDiscord.getOsuApi().getBeatmap(String.valueOf(beatmapId));
			return new CachedBeatmap(System.currentTimeMillis(), beatmap);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}

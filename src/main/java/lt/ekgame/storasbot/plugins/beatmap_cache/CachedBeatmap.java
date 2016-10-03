package lt.ekgame.storasbot.plugins.beatmap_cache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.tillerino.osuApiModel.OsuApiBeatmap;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.parser.BeatmapException;
import lt.ekgame.beatmap_analyzer.parser.BeatmapParser;
import lt.ekgame.storasbot.utils.osu.OsuMode;

public class CachedBeatmap {
	
	private static final String osuBeatmapDownloadURL = "http://osu.ppy.sh/osu/%d";
	private static final RequestConfig defaultRequestConfig = RequestConfig.custom()
			.setSocketTimeout(10000).setConnectTimeout(10000)
			.setConnectionRequestTimeout(10000).build();
	private static final HttpClient httpClient = HttpClients.custom()
			.setDefaultRequestConfig(defaultRequestConfig).build();
	
	private long updatedTime;
	private OsuApiBeatmap beatmap;
	private Beatmap parsedBeatmap;
	
	public CachedBeatmap(long updatedTime, OsuApiBeatmap beatmap) {
		this.updatedTime = updatedTime;
		this.beatmap = beatmap;
	}
	
	public OsuMode getMode() {
		return OsuMode.fromValue(beatmap.getMode());
	}
	
	public OsuApiBeatmap getApiBeatmap() {
		return beatmap;
	}
	
	public Beatmap getParsedBeatmap() throws IllegalStateException, BeatmapException, IOException, URISyntaxException {
		if (parsedBeatmap == null)
			parsedBeatmap = downloadBeatmap(beatmap.getBeatmapId());
		return parsedBeatmap;
	}
	
	public long getUpdateTimestamp() {
		return updatedTime;
	}
	
	private Beatmap downloadBeatmap(int beatmapId) throws IllegalStateException, BeatmapException, IOException, URISyntaxException {
		HttpGet request = new HttpGet(new URI(String.format(osuBeatmapDownloadURL, beatmapId)));
		try {
			HttpResponse response = httpClient.execute(request);
			BeatmapParser parser = new BeatmapParser();
			return parser.parse(response.getEntity().getContent());
		}
		finally {
			request.releaseConnection();
		}
	}

	public void update(long updateTime, OsuApiBeatmap apiBeatmap) {
		this.updatedTime = updateTime;
		this.beatmap = apiBeatmap;
		this.parsedBeatmap = null;
	}
}

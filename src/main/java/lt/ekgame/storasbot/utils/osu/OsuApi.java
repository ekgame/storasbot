package lt.ekgame.storasbot.utils.osu;

import java.io.IOException;
import java.util.List;

import org.tillerino.osuApiModel.Downloader;
import org.tillerino.osuApiModel.OsuApiBeatmap;
import org.tillerino.osuApiModel.OsuApiScore;

import com.typesafe.config.Config;

public class OsuApi {
	
	private Downloader downloader;
	
	public OsuApi(Config config) {
		downloader = new Downloader(config.getString("api.osu"));
	}

	public List<OsuApiScore> getUserTop(String userId, OsuMode mode, int num) throws NumberFormatException, IOException {
		return downloader.getUserTop(Integer.parseInt(userId), mode.getValue(), num, OsuApiScore.class);
	}

	public OsuUser getUser(String userId, OsuMode mode) throws NumberFormatException, IOException {
		return downloader.getUser(Integer.parseInt(userId), mode.getValue(), OsuUser.class);
	}

	public OsuApiBeatmap getBeatmap(String beatmapId) throws NumberFormatException, IOException {
		return downloader.getBeatmap(Integer.parseInt(beatmapId), OsuApiBeatmap.class);
	}

	public List<OsuApiBeatmap> getBeatmapSet(String setId) throws NumberFormatException, IOException {
		return downloader.getBeatmapSet(Integer.parseInt(setId), OsuApiBeatmap.class);
	}

	public OsuUser getUserByUsername(String username, OsuMode mode) throws IOException {
		return downloader.getUser(username, mode.getValue(), OsuUser.class);
	}

}

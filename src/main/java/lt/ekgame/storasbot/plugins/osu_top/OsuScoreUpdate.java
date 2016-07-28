package lt.ekgame.storasbot.plugins.osu_top;

import org.tillerino.osuApiModel.OsuApiBeatmap;

public class OsuScoreUpdate {
	
	private OsuPlayer oldPlayer, newPlayer;
	private OsuScore oldScore, newScore;
	
	public OsuScoreUpdate(OsuPlayer oldPlayer, OsuPlayer newPlayer, OsuScore oldScore, OsuScore newScore) {
		this.oldPlayer = oldPlayer;
		this.newPlayer = newPlayer;
		this.oldScore = oldScore;
		this.newScore = newScore;
	}

	public OsuPlayer getOldPlayer() {
		return oldPlayer;
	}

	public OsuPlayer getNewPlayer() {
		return newPlayer;
	}

	public OsuScore getOldScore() {
		return oldScore;
	}

	public OsuScore getNewScore() {
		return newScore;
	}

	public OsuApiBeatmap getBeamap() {
		return BeatmapCatche.getBeatmap(newScore.getBeatmapId());
	}
}

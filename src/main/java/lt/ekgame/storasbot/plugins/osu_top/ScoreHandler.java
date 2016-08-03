package lt.ekgame.storasbot.plugins.osu_top;

import java.util.List;

public interface ScoreHandler {
	
	
	void handleScoreUpdates(List<OsuScoreUpdate> scores);
	
}

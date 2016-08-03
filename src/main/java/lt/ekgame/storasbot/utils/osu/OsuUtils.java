package lt.ekgame.storasbot.utils.osu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tillerino.osuApiModel.OsuApiScore;

import lt.ekgame.storasbot.StorasBot;

public class OsuUtils {
	
	public static List<OsuScore> getScores(String userId, OsuMode mode) throws NumberFormatException, IOException {
		List<OsuApiScore> scores = StorasBot.getOsuApi().getUserTop(userId, mode, 100);
		List<OsuScore> scoresConverted = new ArrayList<>();
		int i = 1; 
		for (OsuApiScore score : scores) {
			scoresConverted.add(new OsuScore(score, i));
			i++;
		}
		return scoresConverted;
	}
}

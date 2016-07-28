package lt.ekgame.storasbot.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.tillerino.osuApiModel.OsuApiScore;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.plugins.osu_top.OsuScore;

public class OsuUtils {
	
	public static List<OsuScore> getScores(String userId, OsuMode mode) throws NumberFormatException, IOException {
		List<OsuApiScore> scores = StorasBot.osuApi.getUserTop(Integer.parseInt(userId), mode.getValue(), 100, OsuApiScore.class);
		List<OsuScore> scoresConverted = new ArrayList<>();
		int i = 1; 
		for (OsuApiScore score : scores) {
			scoresConverted.add(new OsuScore(score, i));
			i++;
		}
		return scoresConverted;
	}

}

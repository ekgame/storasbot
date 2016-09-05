package lt.ekgame.storasbot.utils.osu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tillerino.osuApiModel.OsuApiScore;

import lt.ekgame.storasbot.StorasDiscord;

public class OsuUtils {
	
	public static List<OsuScore> getScores(OsuPlayerIdentifier identifier) throws NumberFormatException, IOException {
		List<OsuApiScore> scores = StorasDiscord.getOsuApi().getUserTop(identifier.getUserId(), identifier.getMode(), 100);
		List<OsuScore> scoresConverted = new ArrayList<>();
		int i = 1; 
		for (OsuApiScore score : scores) {
			scoresConverted.add(new OsuScore(score, i));
			i++;
		}
		return scoresConverted;
	}
}

package lt.ekgame.storasbot.plugins.osu_top;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.math3.util.Precision;
import org.tillerino.osuApiModel.Mods;
import org.tillerino.osuApiModel.OsuApiBeatmap;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import lt.ekgame.storasbot.utils.Utils;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuScore;
import net.dv8tion.jda.entities.Guild;

public class MessageFormatter {
	
	private static final String BEATMAP_LINK = "https://osu.ppy.sh/b/%d";
	private static final String format = 
			  "__New score by **{{{player}}}**! • **{{performance_new}}** • #{{personal_top}} personal best__\n"
			+ "⬥ {{mode}} • #{{global_rank}} • {{country}}#{{country_rank}} • {{performance}}\n"
			+ "⬥ {{combo}} • {{rank}} • {{score}} • {{accuracy}} • {{mods}}\n"
            + "{{{artist}}} - {{{title}}} [{{{version}}}]\n"
            + "⬥ {{length}} • {{bpm}} BPM • ★ **{{stars}}** • <{{beatmap_link}}>";
            
	private static final Mustache scoreFormat;
	private static final DecimalFormat decimalFormat;
	
	static {
		MustacheFactory mf = new DefaultMustacheFactory();
		scoreFormat = mf.compile(new StringReader(format), "score");
		
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.UK);
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(','); 
		decimalFormat = new DecimalFormat("###,###.##", otherSymbols);
	}
	
	public String format(Guild guild, OsuScoreUpdate scoreUpdate, OsuApiBeatmap beatmap) {
		OsuPlayer oldPlayer = scoreUpdate.getOldPlayer();
		OsuPlayer player = scoreUpdate.getNewPlayer();
		OsuScore oldScore = scoreUpdate.getOldScore();
		OsuScore score = scoreUpdate.getNewScore();
		OsuMode gamemode = player.getGamemode();
		
		Map<String, String> scope = new HashMap<>();
		scope.put("player", Utils.escapeMarkdown(player.getUsername()));
		scope.put("mode", gamemode.getName());
		scope.put("global_rank", player.getGlobalRank() + getDiff(oldPlayer.getGlobalRank(), player.getGlobalRank(), true));
		scope.put("country", player.getCountry().toUpperCase());
		scope.put("country_rank", player.getCountryRank() + getDiff(oldPlayer.getCountryRank(), player.getCountryRank(), true));
		scope.put("performance", decimalFormat.format(player.getPerformance())+"pp" + getDiff(oldPlayer.getPerformance(), player.getPerformance(), false));
		
		scope.put("artist", Utils.escapeMarkdown(beatmap.getArtist()));
		scope.put("title", Utils.escapeMarkdown(beatmap.getTitle()));
		scope.put("version", Utils.escapeMarkdown(beatmap.getVersion()));
		//TODO: mods should affect od/cs/hp/ar values.
		scope.put("od", decimalFormat.format(beatmap.getOverallDifficulty()));
		scope.put("cs", decimalFormat.format(beatmap.getCircleSize()));
		scope.put("hp", decimalFormat.format(beatmap.getHealthDrain()));
		scope.put("ar", decimalFormat.format(beatmap.getApproachRate()));
		scope.put("length", getLength(beatmap, score.getMods()));
		scope.put("bpm", getBPM(beatmap, score.getMods()));
		scope.put("stars", decimalFormat.format(beatmap.getStarDifficulty()));
		scope.put("beatmap_link", String.format(BEATMAP_LINK, beatmap.getBeatmapId()));
		scope.put("max_combo", String.valueOf(beatmap.getMaxCombo()));
		scope.put("combo", "x" + (beatmap.getMaxCombo() == score.getMaxCombo() ? String.valueOf(score.getMaxCombo()) : score.getMaxCombo() + "/" + beatmap.getMaxCombo()));
		
		scope.put("rank", getRankString(score));
		scope.put("score", decimalFormat.format(score.getScore()));
		
		scope.put("accuracy", getAccuracy(oldScore, score, gamemode));
		scope.put("mods", getModsString(score.getMods()));
		double performaceOld = oldScore == null ? score.getPerformance() : oldScore.getPerformance();
		scope.put("performance_new", decimalFormat.format(score.getPerformance()) + "pp" + getDiff(performaceOld, score.getPerformance(), false));
		scope.put("personal_top", decimalFormat.format(score.getPersonalTopPlace()));
		
		StringWriter writer = new StringWriter();
		scoreFormat.execute(writer, scope);
		writer.flush();
		return writer.toString();
	}
	
	private String getLength(OsuApiBeatmap beatmap, long mods) {
		int length = beatmap.getTotalLength();
		int lengthWithMods = beatmap.getTotalLength(mods);
		String result = Utils.compactTimeString(beatmap.getTotalLength());
		if (length < lengthWithMods) result += "⇧";
		if (length > lengthWithMods) result += "⇩";
		return result;
	}
	
	private String getBPM(OsuApiBeatmap beatmap, long mods) {
		double bpm = beatmap.getBpm();
		double bpmWithMods = beatmap.getBpm(mods);
		String result = decimalFormat.format(bpmWithMods);
		if (bpm < bpmWithMods) result += "⇧";
		if (bpm > bpmWithMods) result += "⇩";
		return result;
	}
	
	private String getAccuracy(OsuScore oldScore, OsuScore score, OsuMode gamemode) {
		// The current SQL structure for scores only saves x300, x100, x50 and misses.
		// Some gamemodes need gekis and katus, so we just ignore them for now.
		double accuracyOld = score.getAccuracy(gamemode);
		if (oldScore != null && (gamemode == OsuMode.OSU || gamemode == OsuMode.TAIKO))
			accuracyOld = oldScore.getAccuracy(gamemode);
		
		return decimalFormat.format(score.getAccuracy(gamemode)) + "%" + getDiff(accuracyOld, score.getAccuracy(gamemode), false);
	}
	
	private String getDiff(double oldVal, double newVal, boolean invert) {
		double diff = (newVal - oldVal) * (invert ? -1 : 1);
		if (Precision.equals(newVal - oldVal, 0, 0.001)) return "";
		return " (" + (diff>0?"+":"") + decimalFormat.format(diff) + ")";
	}
	
	private String getRankString(OsuScore score) {
		String rank = score.getRank().replace("X", "SS");
		if (score.getCountMiss() > 0)
			rank += " CHOKE";
		return rank;
	}
	
	private String getModsString(long rawMods) {
		List<Mods> mods = Mods.getMods(rawMods);
		if (mods.size() > 0) {
			String modsString = "";
			for (Mods mod : mods)
				modsString += mod.getShortName();
			return modsString;
		}
		return "NOMOD";
	}

}

package lt.ekgame.storasbot.plugins.osu_top;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.tillerino.osuApiModel.Mods;
import org.tillerino.osuApiModel.OsuApiBeatmap;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import lt.ekgame.storasbot.utils.Utils;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuScore;
import net.dv8tion.jda.entities.Guild;

public class MessageFormatter {
	
	private static final String BEATMAP_LINK = "https://osu.ppy.sh/b/%d";
	private static final String format = 
			  "New score by **{{{player}}}**! • {{mode}} • **#{{global_rank}}** • `{{country}}`#{{country_rank}}\n"
            + "_{{{artist}}} - {{{title}}} [{{{version}}}]_\n"
            + "(OD: **{{od}}**, CS: **{{cs}}**, HP: **{{hp}}**, AR: **{{ar}}**)\n"
            + "(Length: **{{length}}**, BPM: **{{bpm}}**, SD: **{{stars}}**)\n"
            + "<{{beatmap_link}}>\n"
            + "x{{combo}}/{{max_combo}} • {{rank}} • {{score}} • {{accuracy}}% • {{mods}} • **{{performance}}pp** • #{{personal_top}} personal best";
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
		OsuPlayer player = scoreUpdate.getNewPlayer();
		OsuScore score = scoreUpdate.getNewScore();
		
		Map<String, String> scope = new HashMap<>();
		scope.put("player", Utils.escapeMarkdown(player.getUsername()));
		scope.put("mode", player.getGamemode().getName());
		scope.put("global_rank", String.valueOf(player.getGlobalRank()));
		scope.put("country", player.getCountry().toUpperCase());
		scope.put("country_rank", String.valueOf(player.getCountryRank()));
		
		scope.put("artist", Utils.escapeMarkdown(beatmap.getArtist()));
		scope.put("title", Utils.escapeMarkdown(beatmap.getTitle()));
		scope.put("version", Utils.escapeMarkdown(beatmap.getVersion()));
		scope.put("od", decimalFormat.format(beatmap.getOverallDifficulty()));
		scope.put("cs", decimalFormat.format(beatmap.getCircleSize()));
		scope.put("hp", decimalFormat.format(beatmap.getHealthDrain()));
		scope.put("ar", decimalFormat.format(beatmap.getApproachRate()));
		scope.put("length", Utils.compactTimeString(beatmap.getTotalLength()));
		scope.put("bpm", decimalFormat.format(beatmap.getBpm()));
		scope.put("stars", decimalFormat.format(beatmap.getStarDifficulty()));
		scope.put("beatmap_link", String.format(BEATMAP_LINK, beatmap.getBeatmapId()));
		scope.put("max_combo", String.valueOf(beatmap.getMaxCombo()));
		
		scope.put("combo", String.valueOf(score.getMaxCombo()));
		scope.put("rank", getRankString(score));
		scope.put("score", decimalFormat.format(score.getScore()));
		scope.put("accuracy", decimalFormat.format(score.getAccuracy()));
		scope.put("mods", getModsString(score.getMods()));
		scope.put("performance", decimalFormat.format(score.getPerformance()));
		scope.put("personal_top", decimalFormat.format(score.getPersonalTopPlace()));
		
		StringWriter writer = new StringWriter();
		scoreFormat.execute(writer, scope);
		writer.flush();
		return writer.toString();
	}
	
	private String getRankString(OsuScore score) {
		String rank = score.getRank().replace("X", "SS");
		if (score.getCountMiss() > 0)
			rank += " **CHOKE**";
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

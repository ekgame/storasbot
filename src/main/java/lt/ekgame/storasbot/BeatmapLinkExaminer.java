package lt.ekgame.storasbot;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tillerino.osuApiModel.OsuApiBeatmap;

import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class BeatmapLinkExaminer extends ListenerAdapter {
	
	private static Pattern matcherSingle    = Pattern.compile("((https?:\\/\\/)?osu\\.ppy\\.sh)\\/b\\/(\\d*)");
	private static Pattern matcherSet       = Pattern.compile("((https?:\\/\\/)?osu\\.ppy\\.sh)\\/s\\/(\\d*)");
	private static Pattern matcherNewSingle = Pattern.compile("((https?:\\/\\/)?new\\.ppy\\.sh)\\/s\\/(\\d*)#(\\d*)");

	private DecimalFormat diffFormat;
	
	private String TITLE = "_%s - %s_ | mapset by %s";
	private String VERSION = "\n[%s] (OD: **%s**, CS: **%s**, HP: **%s**, AR: **%s**, BPM: **%s**, SD: **%s**)";
	
	public BeatmapLinkExaminer() {
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.UK);
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(','); 
		
		diffFormat = new DecimalFormat("###,###.##", otherSymbols);
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getAuthor().getId().equals(StorasBot.client.getSelfInfo().getId()))
			return; // ignore own messages
		
		String content = event.getMessage().getContent();
		List<MatchResult> maps = new LinkedList<>();
		Utils.addAllUniques(maps, matchBeatmaps(content, matcherSingle, 3, true));
		Utils.addAllUniques(maps, matchBeatmaps(content, matcherSet, 3, false));
		Utils.addAllUniques(maps, matchBeatmaps(content, matcherNewSingle, 4, true));
		
		if (maps.size() > 0) {
			String message = "";
			for (MatchResult map : maps) {
				try {
					String temp = generateMessage(map);
					if (!message.isEmpty())
						message += "\n\n";
					message += temp;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			StorasBot.sendMessage((TextChannel)event.getMessage().getChannel(), message);
		}
	}
	
	String generateMessage(MatchResult map) throws IOException {
		if (map.single) {
			OsuApiBeatmap beatmap = StorasBot.osuApi.getBeatmap(map.id, OsuApiBeatmap.class);
			if (beatmap == null) return null;
			return generateTitle(beatmap) + generateVersion(beatmap);
		}
		else {
			List<OsuApiBeatmap> beatmaps = StorasBot.osuApi.getBeatmapSet(map.id, OsuApiBeatmap.class);
			if (beatmaps == null) return null;
			
			beatmaps.sort((a, b) -> ((int)a.getStarDifficulty()*100 - (int)b.getStarDifficulty()*100));
			String result = generateTitle(beatmaps.get(0));
			for (OsuApiBeatmap beatmap : beatmaps)
				result += generateVersion(beatmap);
			return result;
		}
	}
	
	String generateTitle(OsuApiBeatmap beatmap) {
		return String.format(TITLE, Utils.escapeMarkdown(beatmap.getArtist()), 
				Utils.escapeMarkdown(beatmap.getTitle()), Utils.escapeMarkdown(beatmap.getCreator()));
	}
	
	String generateVersion(OsuApiBeatmap beatmap) {
		return String.format(VERSION, Utils.escapeMarkdown(beatmap.getVersion()), 
				diffFormat.format(beatmap.getOverallDifficulty()),
				diffFormat.format(beatmap.getCircleSize()),
				diffFormat.format(beatmap.getHealthDrain()),
				diffFormat.format(beatmap.getApproachRate()),
				diffFormat.format(beatmap.getBpm()),
				diffFormat.format(beatmap.getStarDifficulty()));
	}
	
	private List<MatchResult> matchBeatmaps(String content, Pattern pattern, int group, boolean single) {
		List<MatchResult> result = new LinkedList<>();
		Matcher matcher = pattern.matcher(content);
		while (matcher.find())
			result.add(new MatchResult(single, Integer.parseInt(matcher.group(group))));
		return result;
	}
	
	class MatchResult {
		boolean single;
		int id;
		
		MatchResult(boolean single, int id) {
			this.single = single;
			this.id = id;
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof MatchResult) {
				MatchResult other = (MatchResult) obj;
				return other.single == single && other.id == id;
			}
			return false;
		}
	}
}

package lt.ekgame.storasbot.plugins;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.RandomStringUtils;
import org.tillerino.osuApiModel.OsuApiBeatmap;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.parser.BeatmapException;
import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.plugins.beatmap_cache.CachedBeatmap;
import lt.ekgame.storasbot.plugins.beatmap_cache.OsuBeatmapCatche;
import lt.ekgame.storasbot.utils.TableRenderer;
import lt.ekgame.storasbot.utils.Utils;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class BeatmapLinkExaminer extends ListenerAdapter {
	
	private static Pattern matcherSingle    = Pattern.compile("((https?:\\/\\/)?osu\\.ppy\\.sh)\\/b\\/(\\d*)");
	private static Pattern matcherSet       = Pattern.compile("((https?:\\/\\/)?osu\\.ppy\\.sh)\\/s\\/(\\d*)");
	private static Pattern matcherNewSingle = Pattern.compile("((https?:\\/\\/)?new\\.ppy\\.sh)\\/s\\/(\\d*)#(\\d*)");

	private DecimalFormat diffFormat;
	
	private String TITLE = "___%s - %s___ (mapset by **%s**) | DL: <%s>";
	
	public BeatmapLinkExaminer() {
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.UK);
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(','); 
		
		diffFormat = new DecimalFormat("###,###.##", otherSymbols);
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getAuthor().equals(StorasDiscord.getJDA().getSelfInfo()))
			return; // ignore own messages
		
		
		Settings settings = StorasDiscord.getSettings(event.getGuild());
		Boolean enabled = settings.get(Setting.BEATMAP_EXAMINER, Boolean.class);
		if (!enabled)
			return; // don't process if disabled
		
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
						message += "\n";
					message += temp;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			TextChannel channel = (TextChannel)event.getMessage().getChannel();
			if (maps.size() == 1 && maps.get(0).single && settings.get(Setting.BEATMAP_ANALYZER, Boolean.class)) {
				if (!Utils.hasCommandPermission(channel, StorasDiscord.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES)) {
					StorasDiscord.sendMessage((TextChannel)event.getMessage().getChannel(), message + "\n_Cant't display beatmap analysis, because I don't have permissions to attach files._");
				}
				else processSingleBeatmap((TextChannel)event.getMessage().getChannel(), message, maps.get(0).beatmap);
			}
			else {
				StorasDiscord.sendMessage((TextChannel)event.getMessage().getChannel(), message);
			}
		}
	}
	
	private void processSingleBeatmap(TextChannel channel, String message, CachedBeatmap beatmap) {
		if (beatmap.getMode() == OsuMode.CATCH) {
			StorasDiscord.sendMessage(channel, message + "\n_Performance information for osu!catch is unavailable. Will implement if there is demand._");
		}
		else {
			try {
				Beatmap parsedBeatmap = beatmap.getParsedBeatmap();
				BeatmapAnalyzer analyzer = new BeatmapAnalyzer(parsedBeatmap);
				
				File tempFile;
				do  {
					tempFile = new File(RandomStringUtils.randomAlphanumeric(16) + ".png");
				} while (tempFile.exists());
				final File file = tempFile;
				
				BufferedImage image = analyzer.getChartImage(600, 200);
				ImageIO.write(image, "PNG", file);
				
				StorasDiscord.sendFile(channel, message, file, (msg) -> {
					file.delete();
				});
			} catch (IllegalStateException | BeatmapException | IOException | URISyntaxException e) {
				e.printStackTrace();
				StorasDiscord.sendMessage(channel, message + "\n_Failed to analyze the beatmap._");
			}
		}
	}

	String generateMessage(MatchResult map) throws IOException {
		TableRenderer table = new TableRenderer();
		table.setHeader("Version", "Stars", "Combo", "BPM", "OD", "CS", "HP", "AR");
		String title = null;
		String addition = "";
		if (map.single) {
			CachedBeatmap beatmap = OsuBeatmapCatche.getCachedBeatmap(map.id);
			if (beatmap == null) return null;
			map.beatmap = beatmap;
			title = generateTitle(beatmap.getApiBeatmap());
			generateVersion(table, beatmap.getApiBeatmap());
		}
		else {
			List<OsuApiBeatmap> beatmaps = StorasDiscord.getOsuApi().getBeatmapSet(map.id);
			if (beatmaps == null) return null;
			
			List<OsuApiBeatmap> beatmapsSorted = beatmaps.stream()
				.sorted((a, b) -> ((int)b.getStarDifficulty()*100 - (int)a.getStarDifficulty()*100))
				.limit(10)
				.collect(Collectors.toList());
			
			title = generateTitle(beatmaps.get(0));
			
			for (OsuApiBeatmap beatmap : beatmapsSorted)
				generateVersion(table, beatmap);
			
			if (beatmapsSorted.size() < beatmaps.size())
				addition = "\nAnd " + (beatmaps.size() - beatmapsSorted.size()) + " more maps.";
		}
		return title + "```" + table.build() + addition + "```";
	}
	
	String generateTitle(OsuApiBeatmap beatmap) {
		return String.format(TITLE, Utils.escapeMarkdown(beatmap.getArtist()), 
			Utils.escapeMarkdown(beatmap.getTitle()), Utils.escapeMarkdown(beatmap.getCreator()),
			"http://osu.ppy.sh/d/" + beatmap.getSetId());
	}
	
	void generateVersion(TableRenderer table, OsuApiBeatmap beatmap) {
		table.addRow(
			Utils.escapeMarkdown(Utils.trimLength(beatmap.getVersion(), 20, "...")), 
			diffFormat.format(beatmap.getStarDifficulty()),
			diffFormat.format(beatmap.getMaxCombo()),
			diffFormat.format(beatmap.getBpm()),
			diffFormat.format(beatmap.getOverallDifficulty()),
			diffFormat.format(beatmap.getCircleSize()),
			diffFormat.format(beatmap.getHealthDrain()),
			diffFormat.format(beatmap.getApproachRate())
		);
	}
	
	private List<MatchResult> matchBeatmaps(String content, Pattern pattern, int group, boolean single) {
		List<MatchResult> result = new LinkedList<>();
		Matcher matcher = pattern.matcher(content);
		while (matcher.find())
			result.add(new MatchResult(single, matcher.group(group)));
		return result;
	}
	
	class MatchResult {
		boolean single;
		String id;
		CachedBeatmap beatmap;
		
		MatchResult(boolean single, String id) {
			this.single = single;
			this.id = id;
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof MatchResult) {
				MatchResult other = (MatchResult) obj;
				return other.single == single && other.id.equals(id);
			}
			return false;
		}
	}
}

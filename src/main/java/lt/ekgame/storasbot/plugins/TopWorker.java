package lt.ekgame.storasbot.plugins;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tillerino.osuApiModel.Mods;
import org.tillerino.osuApiModel.OsuApiBeatmap;
import org.tillerino.osuApiModel.OsuApiScore;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.Utils;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.utils.SimpleLog;

public class TopWorker extends Thread implements EventListener {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Top Worker");
	
	private boolean enabled;
	private String country;
	private List<String> tagLines;
	private String postChannel;
	
	DecimalFormat diffFormat;
	
	private static String BEATMAP_LINK = "https://osu.ppy.sh/b/%d";
	private static String RANK_LISTING = "http://osu.ppy.sh/p/pp?c=%s&page=%d";
	private static String MESSAGE_FORMAT = "New top play by **%s**!\n"
			                            + "_%s - %s [%s]_\n"
			                            + "(OD: **%s**, CS: **%s**, HP: **%s**, AR: **%s**)\n"
			                            + "(Length: **%s** (%s), BPM: **%s**, SD: **%s**)\n"
			                            + "<%s>\n" //link
			                            + "%s/%s • %s Rank%s • %s • %s%s • **%spp** • #%d personal best\n\n";
	private boolean started = false;
	
	String formMessage(int scoreNum, RankedUser user, OsuApiScore score) {
		try {
			OsuApiBeatmap bm = StorasBot.osuApi.getBeatmap(score.getBeatmapId(), OsuApiBeatmap.class);
			String modsString = "";
			List<Mods> mods = score.getModsList();
			
			if (mods.size() > 0) {
				modsString = " • ";
				for (Mods mod : mods)
					modsString += mod.getShortName();
			}
			
			String choke = "";
			if (score.getCountMiss() > 0)
				choke = " **CHOKE**";
			
			return String.format(MESSAGE_FORMAT,
					user.username, 
					Utils.escapeMarkdown(bm.getArtist()),
					Utils.escapeMarkdown(bm.getTitle()),
					Utils.escapeMarkdown(bm.getVersion()),
					diffFormat.format(bm.getOverallDifficulty()), diffFormat.format(bm.getCircleSize()),
					diffFormat.format(bm.getHealthDrain()), diffFormat.format(bm.getApproachRate()),
					Utils.compactTimeString(bm.getTotalLength()), Utils.compactTimeString(bm.getHitLength()),
					diffFormat.format(bm.getBpm()), diffFormat.format(bm.getStarDifficulty()),
					String.format(BEATMAP_LINK, score.getBeatmapId()),
					"x" + score.getMaxCombo(), bm.getMaxCombo(),
					score.getRank(), choke, diffFormat.format(score.getScore()), 
					score.getPercentagePretty(), modsString, score.getPp(), scoreNum);
		} catch (IOException e) {
			LOG.warn("Failed to generate message (osu api down?)");
			e.printStackTrace();
			return null;
		}
	}
	
	public TopWorker() {
		enabled = StorasBot.config.getBoolean("top-updater.enabled");
		country = StorasBot.config.getString("top-updater.country");
		tagLines = StorasBot.config.getStringList("top-updater.tags");
		postChannel = StorasBot.config.getString("top-updater.channel");
		
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.UK);
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(','); 
		
		diffFormat = new DecimalFormat("###,###.##", otherSymbols);
	}
	
	public void onEvent(Event event) {
		if (event instanceof ReadyEvent) {
			LOG.info("Discord ready.");
			if (enabled) start();
		}
	}
	
	public void run() {
		if (started) return;
		started = true;
		LOG.info("Top updater started.");
		
		while (true) {
				try {
					List<RankedUser> changes = scanCountry(country);
					String message = checkPlayers(changes);
					
					if (!message.isEmpty()){
						message += "*" + tagLines.get(new Random().nextInt(tagLines.size())) + "*";
						for (Guild guild : StorasBot.client.getGuilds()) {
							for (TextChannel channel : guild.getTextChannels()) {
								if (channel.getName().equals(postChannel)) {
									StorasBot.sendMessage(channel, message);
									break;
								}
							}
						}
					}
				}
				catch (SocketTimeoutException e) {
					LOG.warn("Request timeout.");
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private List<RankedUser> scanCountry(String country) throws IOException, SQLException {
		List<RankedUser> users = scrapeTopPlayers(country);
		List<RankedUser> checkPlayers = new ArrayList<>();
		for (RankedUser user : users) {
			if (StorasBot.database.userExists(user.userId)) {
				int lastPerformance = StorasBot.database.getPerformance(user.userId);
				if (lastPerformance < user.performance)
					checkPlayers.add(user);
				StorasBot.database.updateUser(user.userId, user.username, user.performance);
			}
			else {
				StorasBot.database.addUser(user.userId, user.username, user.performance);
				checkPlayers.add(user);
				LOG.info(user.username + " doesn't exist.");
			}
		}
		return checkPlayers;
	}
	
	private String checkPlayers(List<RankedUser> players) throws SQLException {
		String message = "";
		for (RankedUser user : players) {
			LOG.info(user.username + " is being checked");
			try {
				List<OsuApiScore> scores = getTopScores(user.userId, 0, 100);
				int scoreNum = 0;
				for (OsuApiScore score : scores) {
					scoreNum++;
					boolean updateDB = true;
					if (!StorasBot.database.scoreExists(user.userId, score.getBeatmapId(), score.getPp())) {
						LOG.info("New top score by " + user.username + " " + score.getPp() + "pp! #" + scoreNum);
						if (System.currentTimeMillis() - score.getDate() < 86400000 && user.rank <= 100 && (scoreNum <= 30 || score.getPp() >= 300)) {
							String msg = formMessage(scoreNum, user, score);
							if (msg == null)
								updateDB = false; // failed to form message, try again next cycle.
							else 
								message += msg;
						}
					}
					if (updateDB)
						StorasBot.database.updateScore(user.userId, score.getBeatmapId(), score.getPp());
				}
			}
			catch (IOException e) {
				LOG.warn(e.getMessage());
			}
		}
		return message;
	}
	
	List<RankedUser> scrapeTopPlayers(String country) throws IOException {
		List<RankedUser> result = new ArrayList<>();
		result.addAll(scrapeTopPlayers(country, 1));
		result.addAll(scrapeTopPlayers(country, 2));
		result.addAll(scrapeTopPlayers(country, 3));
		return result;
	}
	
	List<RankedUser> scrapeTopPlayers(String country, int page) throws IOException {
		List<RankedUser> result = new ArrayList<>();
		String url = String.format(RANK_LISTING, country, page);
		Document doc = Jsoup.connect(url).get();
		Elements table = doc.select(".beatmapListing tbody");
		if (table.size() < 1) {
			LOG.fatal("No table found.");
			return result;
		}
		Element tbody = table.get(0);
		if (tbody.children().size() != 51) {
			LOG.fatal("Invalid table size.");
			return result;
		}
		for (int i = 1; i < tbody.children().size(); i++) {
			Element row = tbody.children().get(i);
			String onclick = row.attr("onclick");
			
			String userIdRaw = onclick.substring(22, onclick.length() - 1);
			int userId = Integer.parseInt(userIdRaw);
			
			String username = row.children().get(1).text();
			
			String performanceRaw = row.children().get(4).text();
			int performance = Integer.parseInt(performanceRaw.substring(0, performanceRaw.length()-2).replace(",", ""));

			result.add(new RankedUser((page-1)*50+i, userId, username, performance));
		}
		
		return result;
	}
	
	List<OsuApiScore> getTopScores(int userId, int mode, int limit) throws IOException {
		return StorasBot.osuApi.getUserTop(userId, mode, limit, OsuApiScore.class);
	}
	
	class RankedUser {
		int rank;
		int userId;
		String username;
		int performance;
		
		RankedUser(int rank, int userId, String username, int performance) {
			this.rank = rank;
			this.userId = userId;
			this.username = username;
			this.performance = performance;
		}
	}
}

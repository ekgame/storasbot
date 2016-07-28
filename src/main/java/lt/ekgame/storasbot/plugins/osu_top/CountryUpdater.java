package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tillerino.osuApiModel.OsuApiScore;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.OsuMode;
import lt.ekgame.storasbot.utils.OsuUtils;
import net.dv8tion.jda.utils.SimpleLog;

public class CountryUpdater {
	
	private static final SimpleLog LOG = SimpleLog.getLog("Top Worker");
	private static final String RANK_LISTING = "http://osu.ppy.sh/p/pp?c=%s&m=%d&page=%d";
	private static final String RANK_LISTING_GLOBAL = "http://osu.ppy.sh/p/pp?&m=%d&page=%d";
	
	private String country;
	private OsuMode mode;
	private OsuTracker tracker;
	private List<TrackedCountry> trackers;
	private Map<String, OsuPlayer> catchedPlayers;
	
	public CountryUpdater(String country, OsuMode mode, OsuTracker tracker, List<TrackedCountry> trackers) {
		this.country = country;
		this.mode = mode;
		this.tracker = tracker;
		this.trackers = trackers;
	}
	
	public OsuMode getGamemode() {
		return mode;
	}
	
	public void update(OsuUserUpdater userUpdater) {
		try {
			if (country == null)
				LOG.info("Checking global " + mode.toString());
			else
				LOG.info("Checking country " + country + " " + mode.toString());
			
			List<OsuPlayer> players = scrapeTopPlayers();
			for (OsuPlayer player : players) {
				OsuPlayer catched = getCatchedPlayer(player);
				if (catched == null) {
					// Update new players in a different thread
					// It's a lot faster
					userUpdater.addPlayer(player, mode);
				}
				else {
					int catchedPP = (int) Math.round(catched.getPerformance());
					int roundedPP = (int) Math.round(player.getPerformance());
					boolean differentPP = catchedPP != roundedPP;
					
					double catchedAcc = catched.getAccuracy();
					double currentAcc = player.getAccuracy();
					boolean differentAcc = !Precision.equals(catchedAcc, currentAcc, 0.01);
					
					if (differentPP || differentAcc) {
						LOG.info(player.getUsername() + ": checking scores " + differentAcc + " " + differentPP + " " + catchedAcc + " " + currentAcc);
						OsuPlayer updated = player.getFromAPI(mode);
						List<OsuScore> scores = OsuUtils.getScores(updated.getUserId(), mode);
						Map<String, OsuScore> catchedScores = getCatchedScores(player.getUserId()).stream()
							.collect(Collectors.toMap(OsuScore::getBeatmapId, b->b));
						
						List<OsuScore> newScores = new ArrayList<>();
						List<OsuScore> updatedScores = new ArrayList<>();
						List<OsuScoreUpdate> scoreUpdates = new ArrayList<>();
						
						for (OsuScore score : scores) {
							OsuScore catchedScore = catchedScores.get(score.getBeatmapId());
							if (catchedScore == null) {
								newScores.add(score);
								scoreUpdates.add(new OsuScoreUpdate(player, updated, null, score));
								LOG.info("New score = " + score.getPerformance());
							}
							else if (!score.equals(catchedScore)) {
								updatedScores.add(score);
								scoreUpdates.add(new OsuScoreUpdate(player, updated, catchedScore, score));
								LOG.info("Updated score = " + catchedScore.getPerformance() + " -> " + score.getPerformance());
							}
						}
						
						StorasBot.database.addScores(player.getUserId(), mode, newScores);
						StorasBot.database.updateScores(player.getUserId(), mode, updatedScores);
						StorasBot.database.updateTrackedUser(updated);
						
						// very unlikely that someone makes 3 scores in between updates
						// it's more likely that the updater was inactive or pp was recalculated
						// in an attempt to not spam too much, don't notify when this happens
						if (scoreUpdates.size() <= 3) { 
							for (TrackedCountry tracked : trackers)
								tracked.handleScoreUpdates(tracker, scoreUpdates);
						}
						else {
							LOG.info(scoreUpdates.size() + " new/updated scores - skipping notification");
						}
					}
				}
			}
		} catch (SocketTimeoutException e) {
			LOG.warn("Request timeout for " + (country == null ? "global" : country) + ".");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private List<OsuScore> getCatchedScores(String userId) throws SQLException {
		return StorasBot.database.getUserScores(userId, mode);
	}

	private OsuPlayer getCatchedPlayer(OsuPlayer target) throws SQLException {
		if (catchedPlayers == null) {
			catchedPlayers = StorasBot.database.getTrackedCountryPlayers(country, mode).stream()
				.collect(Collectors.toMap(OsuPlayer::getUserId, p -> p));
		}
		return catchedPlayers.get(target.getUserId());
	}
	
	private List<OsuPlayer> scrapeTopPlayers() throws IOException {
		List<OsuPlayer> result = new ArrayList<>();
		result.addAll(scrapeTopPlayers(1));
		result.addAll(scrapeTopPlayers(2));
		result.addAll(scrapeTopPlayers(3));
		return result;
	}
	
	private List<OsuPlayer> scrapeTopPlayers(int page) throws IOException {
		List<OsuPlayer> result = new ArrayList<>();
		String url;
		if (country == null)
			url = String.format(RANK_LISTING_GLOBAL, mode.getValue(), page);
		else
			url = String.format(RANK_LISTING, country, mode.getValue(), page);
				
		Document doc = Jsoup.connect(url).get();
		Elements table = doc.select(".beatmapListing tbody");
		if (table.size() < 1) {
			LOG.fatal("No table found. " + country);
			return result;
		}
		
		Element tbody = table.get(0);
		if (tbody.children().size() == 0) {
			LOG.fatal("Invalid table size." + country);
			return result;
		}
		
		for (int i = 1; i < tbody.children().size(); i++) {
			Element row = tbody.children().get(i);
			String onclick = row.attr("onclick");
			String userId = onclick.substring(22, onclick.length() - 1);
			String performanceRaw = row.children().get(4).text();
			String accuracyRaw = row.children().get(2).text();
			String username = row.children().get(1).text();
			double accuracy = Double.parseDouble(accuracyRaw.substring(0, accuracyRaw.length()-1));
			int performance = Integer.parseInt(performanceRaw.substring(0, performanceRaw.length()-2).replace(",", ""));
			result.add(new OsuPlayer(userId, username, mode, country, (page-1)*50+i, performance, accuracy));
		}
		return result;
	}
}

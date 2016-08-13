package lt.ekgame.storasbot.plugins.osu_top;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import lt.ekgame.storasbot.utils.osu.OsuUserCatche;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuTracker extends Thread {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Osu Tracker");
	public static final int TOP_OVERTAKE = 10;
	public static MessageFormatter messageFormatter = new MessageFormatter();
	
	private OsuUserCatche userCatche;
	
	public OsuTracker(OsuUserCatche osuUserCatche) {
		this.userCatche = osuUserCatche;
	}

	public void run() {
		LOG.info("Starting osu! tracker.");
		long loops = 0;
		while (true) {
			try {
				loops++;
				if (loops % 10 == 0)
					userCatche.updateCatche();
				
				// Registered trackers
				List<TrackedCountry> countryTrackers = StorasBot.getDatabase().getTrackedCountries();
				List<TrackedPlayer> playerTrackers = StorasBot.getDatabase().getTrackedPlayers();
				
				// Trackers grouped by leaderboard and gamemode
				Map<CountryGroup, List<TrackedCountry>> countries = groupCountries(countryTrackers);
				
				// Players that require an update
				OsuUpdatablePlayers updatablePlayers = new OsuUpdatablePlayers(loops == 1);
				OsuLeaderboardScraper scraper = new OsuLeaderboardScraper(20);
				
				// Handle scraping in parallel
				for (Entry<CountryGroup, List<TrackedCountry>> entry : countries.entrySet()) {
					int top = getMaxCountryTop(entry.getValue());
					scraper.submit(new OsuUpdatableLeaderboard(entry.getKey().country, entry.getKey().mode, top, entry.getValue()));
				}
				
				List<OsuUpdatableLeaderboard> leaderboards = scraper.getLeaderboards();
				
				// Schedule players to be checked
				for (OsuUpdatableLeaderboard leaderboard : leaderboards) {
					for (OsuPlayer player : leaderboard.getPlayers()) {
						OsuUpdatablePlayer updatable = updatablePlayers.get(player);
						updatable.addScoreHandlers(leaderboard.getTrackers());
					}
				}
				
				// Add individual trackers (new or append to country tracker)
				for (TrackedPlayer tracker : playerTrackers) {
					OsuUpdatablePlayer updatable = updatablePlayers.get(tracker.getIdentifier());
					updatable.addScoreHandler(tracker);
				}
				
				// Update players in parallel
				OsuUserUpdater userUpdater = new OsuUserUpdater(20, userCatche);
				updatablePlayers.submitPlayers(userUpdater);
				
				LOG.debug("Waiting to complete updates");
				userUpdater.awaitTermination();
				
				LOG.debug("Sleeping for 5 seconds.");
				Thread.sleep(5000);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private int getMaxCountryTop(List<TrackedCountry> trackers) {
		return trackers.stream().mapToInt(e->e.getCountryTop()).max().getAsInt() + TOP_OVERTAKE;
	}
	
	private Map<CountryGroup, List<TrackedCountry>> groupCountries(List<TrackedCountry> trackers) {
		return trackers.stream()
				.collect(Collectors.groupingBy(t->new CountryGroup(t.getCountry(), t.getGamemode())));
	}
	
	private class CountryGroup {
		private String country;
		private OsuMode mode;

		CountryGroup(String country, OsuMode mode) {
			this.country = country;
			this.mode = mode;
		}

		public int hashCode() {
			return new HashCodeBuilder(9, 89).append(country).append(mode).toHashCode();
		}
		
		public boolean equals(Object object) {
			if (object instanceof CountryGroup) {
				CountryGroup other = (CountryGroup) object;
				return country == null ? (other.country == null) : country.equals(other.country) && mode == other.mode;
			}
			return false;
		}
 	}
	
	private class OsuUpdatablePlayers {
		
		boolean firstScan;
		Map<OsuPlayerIdentifier, OsuUpdatablePlayer> players = new HashMap<>();
		
		OsuUpdatablePlayers(boolean firstScan) {
			this.firstScan = firstScan;
		}
		
		public OsuUpdatablePlayer get(OsuPlayerIdentifier identifier) {
			OsuUpdatablePlayer updatable;
			if (!players.containsKey(identifier)) {
				updatable = new OsuUpdatablePlayer(identifier);
				players.put(identifier, updatable);
			}
			else {
				updatable = players.get(identifier);
			}
			return updatable;
		}
		
		public OsuUpdatablePlayer get(OsuPlayer player) {
			OsuUpdatablePlayer updatable;
			if (!players.containsKey(player.getIdentifier())) {
				updatable = new OsuUpdatablePlayer(player);
				players.put(player.getIdentifier(), updatable);
			}
			else {
				updatable = players.get(player.getIdentifier());
			}
			return updatable;
		}

		public void submitPlayers(OsuUserUpdater userUpdater) {
			for (Entry<OsuPlayerIdentifier, OsuUpdatablePlayer> entry : players.entrySet())
				userUpdater.submit(entry.getValue(), !firstScan);
		}
	}
}

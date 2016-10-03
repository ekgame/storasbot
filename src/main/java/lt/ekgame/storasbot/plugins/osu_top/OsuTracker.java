package lt.ekgame.storasbot.plugins.osu_top;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import lt.ekgame.storasbot.utils.osu.OsuUserCatche;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuTracker extends Thread implements EventListener {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Osu Tracker");
	public static final int TOP_OVERTAKE = 10;
	public static MessageFormatter messageFormatter = new MessageFormatter();
	
	private OsuUserCatche userCatche;
	
	public OsuTracker(OsuUserCatche osuUserCatche) {
		this.userCatche = osuUserCatche;
	}
	
	@Override
	public void onEvent(Event event) {
		if (event instanceof ReadyEvent && StorasDiscord.getConfig().getBoolean("tracker.enabled")) {
			start();
		}
	}

	public void run() {
		try {
			// Wait 10 seconds before starting.
			// This is to avoid potentially removing trackers
			// for channels that haven't loaded yet.
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		LOG.info("Starting osu! tracker.");
		long loops = 0;
		while (true) {
			try {
				loops++;
				if (loops % 10 == 0)
					userCatche.updateCatche();
				
				// Registered trackers
				List<TrackedCountry> countryTrackers = StorasDiscord.getDatabase().getTrackedCountries();
				List<TrackedPlayer> playerTrackers = StorasDiscord.getDatabase().getTrackedPlayers();
				
				// check if channels for country trackers exist, remove then if not
				Iterator<TrackedCountry> countryIterator = countryTrackers.iterator();
				while (countryIterator.hasNext()) {
					TrackedCountry tracker = countryIterator.next();
					TextChannel channel = StorasDiscord.getJDA().getTextChannelById(tracker.getChannelId());
					if (channel == null) {
						//tracker.removeTracker();
						countryIterator.remove();
					}
				}
				
				// check if channels for player trackers exist, remove then if not
				Iterator<TrackedPlayer> playerIterator = playerTrackers.iterator();
				while (playerIterator.hasNext()) {
					TrackedPlayer tracker = playerIterator.next();
					TextChannel channel = StorasDiscord.getJDA().getTextChannelById(tracker.getChannelId());
					if (channel == null) {
						//tracker.removeTracker();
						playerIterator.remove();
					}
				}
				
				// Trackers grouped by leaderboard and gamemode
				Map<CountryGroup, List<TrackedCountry>> countries = groupCountries(countryTrackers);
				
				// Players that require an update
				OsuUpdatablePlayers updatablePlayers = new OsuUpdatablePlayers(loops == 1);
				OsuLeaderboardScraper scraper = new OsuLeaderboardScraper(10);
				
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
				OsuUserUpdater userUpdater = new OsuUserUpdater(15, userCatche);
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

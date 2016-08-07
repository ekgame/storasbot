package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuTracker extends Thread {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Top Worker");
	public static final int TOP_OVERTAKE = 10;
	public static MessageFormatter messageFormatter = new MessageFormatter();
	
	private OsuUserCatche userCatche;
	
	public OsuTracker(OsuUserCatche osuUserCatche) {
		this.userCatche = osuUserCatche;
	}

	public void run() {
		LOG.info("Starting osu! tracker.");
		while (true) {
			try {
				List<TrackedCountry> countryTrackers = StorasBot.getDatabase().getTrackedCountries();
				List<TrackedPlayer> playerTrackers = StorasBot.getDatabase().getTrackedPlayers();
				
				Map<CountryGroup, List<TrackedCountry>> countries = groupCountries(countryTrackers);
				Map<OsuPlayerIdentifier, OsuUpdatablePlayer> players = new HashMap<>();
				
				for (Entry<CountryGroup, List<TrackedCountry>> entry : countries.entrySet()) {
					try {
						int top = entry.getValue().stream().mapToInt(e->e.getCountryTop()).max().getAsInt() + TOP_OVERTAKE;
						LOG.info("Country " + entry.getKey().country + " " + entry.getKey().mode);
						List<OsuPlayer> countryPlayers = OsuLeaderboardScraper.scrapePlayers(entry.getKey().country, entry.getKey().mode, top);
						for (OsuPlayer player : countryPlayers) {
							OsuUpdatablePlayer updatable;
							if (!players.containsKey(player.getIdentifier())) {
								updatable = new OsuUpdatablePlayer(player);
								players.put(player.getIdentifier(), updatable);
							}
							else {
								updatable = players.get(player.getIdentifier());
							}
							
							for (TrackedCountry tracker : entry.getValue())
								updatable.addScoreHandler(tracker);
						}
						
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				for (TrackedPlayer tracker : playerTrackers) {
					OsuUpdatablePlayer updatable;
					if (!players.containsKey(tracker.getIdentifier())) {
						updatable = new OsuUpdatablePlayer(tracker.getIdentifier());
						players.put(tracker.getIdentifier(), updatable);
					}
					else {
						updatable = players.get(tracker.getIdentifier());
					}
					updatable.addScoreHandler(tracker);
				}
				
				OsuUserUpdater userUpdater = new OsuUserUpdater(20, userCatche);
				for (Entry<OsuPlayerIdentifier, OsuUpdatablePlayer> entry : players.entrySet())
					userUpdater.submit(entry.getValue());
				
				LOG.info("Waiting to complete updates");
				userUpdater.awaitTermination();
				
				LOG.info("Sleeping for 5 seconds.");
				Thread.sleep(5000);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
		
		public boolean equals(Object other) {
			return EqualsBuilder.reflectionEquals(this, other);
		}
 	}
}

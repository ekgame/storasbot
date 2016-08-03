package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuTracker extends Thread implements EventListener {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Top Worker");
	public static final int TOP_OVERTAKE = 10;
	
	private OsuPlayerUpdater userUpdater = new OsuPlayerUpdater(20, 500);
	public static MessageFormatter messageFormatter = new MessageFormatter();
	private ListMerger<ScoreHandler> listMerger = new ListMerger<>();
	
	public OsuTracker() {
		
	}
	
	@Override
	public void onEvent(Event event) {
		if (event instanceof ReadyEvent) {
			start();
		}
	}

	public void run() {
		LOG.info("Starting osu! tracker.");
		while (true) {
			try {
				List<TrackedCountry> countryTrackers = StorasBot.getDatabase().getTrackedCountries();
				List<TrackedPlayer> playerTrackers = StorasBot.getDatabase().getTrackedPlayers();
				
				Map<CountryGroup, List<TrackedCountry>> countries = groupCountries(countryTrackers);
				Map<OsuUpdatablePlayer, List<? extends ScoreHandler>> players = new HashMap<>();
				
				for (Entry<CountryGroup, List<TrackedCountry>> entry : countries.entrySet()) {
					try {
						int top = entry.getValue().stream().mapToInt(e->e.getCountryTop()).max().getAsInt() + TOP_OVERTAKE;
						List<OsuPlayer> countryPlayers = OsuLeaderboardScraper.scrapePlayers(entry.getKey().country, entry.getKey().mode, top);
						for (OsuPlayer player : countryPlayers)
							players.merge(new OsuUpdatablePlayer(player.getUserId(), player.getGamemode(), player), entry.getValue(), listMerger);
						
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				
				
				LOG.info("Sleeping for 5 seconds.");
				Thread.sleep(5000);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
			// TODO: Get players of tracked countries
			// TODO: Check players of countries
			// TODO: Send out updates
			// TODO: Get tracked players
			// TODO: Check unchecked tracked players
			// TODO: Send out updates
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
	
	private class OsuUpdatablePlayer {
		private String userId;
		private OsuMode mode;
		private OsuPlayer countryPlayer;

		OsuUpdatablePlayer(String userId, OsuMode mode, OsuPlayer countryPlayer) {
			this.userId = userId;
			this.mode = mode;
			this.countryPlayer = countryPlayer;
		}

		public int hashCode() {
			return new HashCodeBuilder(55, 23).append(userId).append(mode).toHashCode();
		}
 	}
	
	private class ListMerger<T> implements BiFunction<List<? extends T>, List<? extends T>, List<? extends T>> {
		@Override
		public List<T> apply(List<? extends T> t, List<? extends T> u) {
			List<T> result = new ArrayList<>();
			result.addAll(t);
			result.addAll(u);
			return result;
		}
	}
}

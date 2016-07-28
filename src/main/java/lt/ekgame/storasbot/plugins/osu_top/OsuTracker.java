package lt.ekgame.storasbot.plugins.osu_top;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.OsuMode;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuTracker extends Thread implements EventListener {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Top Worker");
	
	private OsuUserUpdater userUpdater = new OsuUserUpdater(20, 2000);
	private MessageFormatter messageFormatter = new MessageFormatter();
	
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
			//Map<String, OsuPlayer> checkedPlayers = new HashMap<>(); 
			try {
				List<TrackedCountry> trackers = StorasBot.database.getTrackedCountries();
				List<CountryUpdater> updaters = groupCountries(trackers);
				
				for (CountryUpdater updater : updaters) 
					updater.update(userUpdater);
				
				
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
	
	private List<CountryUpdater> groupCountries(List<TrackedCountry> trackers) {
		return trackers.stream()
				.collect(Collectors.groupingBy(t->new TrackedCountryGroup(t.getCountry(), t.getGamemode())))
				.entrySet().stream().map(t->new CountryUpdater(t.getKey().country, t.getKey().mode, this,t.getValue()))
				.collect(Collectors.toList());
	}
	
	public MessageFormatter getFormatter() {
		return messageFormatter;
	}
	
	private class TrackedCountryGroup {
		private String country;
		private OsuMode mode;

		TrackedCountryGroup(String country, OsuMode mode) {
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

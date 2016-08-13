package lt.ekgame.storasbot.plugins.osu_top;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;

public class OsuUpdatablePlayer implements ScoreHandler {
	
	private OsuPlayerIdentifier identifier;
	private OsuPlayer countryPlayer;
	private List<ScoreHandler> scoreHandlers = new ArrayList<>();

	/**
	 * Update from country.
	 */
	public OsuUpdatablePlayer(OsuPlayer countryPlayer) {
		this.countryPlayer = countryPlayer;
		this.identifier = countryPlayer.getIdentifier();
	}
	
	/**
	 * Update individual.
	 */
	public OsuUpdatablePlayer(OsuPlayerIdentifier identifier) {
		this.identifier = identifier;
	}
	
	public void addScoreHandler(ScoreHandler handler) {
		scoreHandlers.add(handler);
	}
	
	public void addScoreHandlers(List<? extends ScoreHandler> handlers) {
		scoreHandlers.addAll(handlers);
	}
	
	public OsuPlayerIdentifier getIdentifier() {
		return identifier;
	}
	
	public OsuPlayer getCountryPlayer() {
		return countryPlayer;
	}

	public int hashCode() {
		return new HashCodeBuilder(55, 23)
			.append(countryPlayer.getUserId())
			.append(countryPlayer.getGamemode())
			.toHashCode();
	}

	@Override
	public void handleScoreUpdates(List<OsuScoreUpdate> scores) {
		for (ScoreHandler handler : scoreHandlers)
			handler.handleScoreUpdates(scores);
	}
}

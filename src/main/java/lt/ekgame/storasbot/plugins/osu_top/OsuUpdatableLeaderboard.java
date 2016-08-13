package lt.ekgame.storasbot.plugins.osu_top;

import java.util.ArrayList;
import java.util.List;

import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;

public class OsuUpdatableLeaderboard {
	
	private String country;
	private OsuMode mode;
	private int numTop;
	
	private List<OsuPlayer> players = new ArrayList<>();
	private List<TrackedCountry> trackers = new ArrayList<>();
	
	public OsuUpdatableLeaderboard(String country, OsuMode mode, int numTop, List<TrackedCountry> trackers) {
		this.country = country;
		this.mode = mode;
		this.numTop = numTop;
		this.trackers = trackers;
	}
	
	public String getCountry() {
		return country;
	}
	
	public OsuMode getMode() {
		return mode;
	}

	public int getNumTop() {
		return numTop;
	}
	
	public void setPlayers(List<OsuPlayer> players) {
		this.players.addAll(players);
	}
	
	public List<OsuPlayer> getPlayers() {
		return players;
	}
	
	public List<TrackedCountry> getTrackers() {
		return trackers;
	}
}

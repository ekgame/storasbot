package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.OsuMode;
import lt.ekgame.storasbot.utils.OsuUser;

public class OsuPlayer {
	
	private String userId;
	private OsuMode mode;
	private String username;
	private String country;
	private double performance;
	private double accuracy;
	private int globalRank;
	private int countryRank;
	
	public OsuPlayer(String userId, OsuMode mode) {
		this(userId, null, mode, null, -1, -1, -1);
	}
	
	public OsuPlayer(String userId, String username, OsuMode mode, String country, int countryRank, double performance, double accuracy) {
		this(userId, mode, username, country, performance, -1, countryRank, accuracy);
	}
	
	public OsuPlayer(String userId, OsuMode mode, String username, String country, double performance, int globalRank, int countryRank, double accuracy) {
		try {Integer.parseInt(userId);} catch (NumberFormatException e) {
			throw new IllegalArgumentException("userId has to be an integer string.");
		}
		
		this.userId = userId;
		this.mode = mode;
		this.username = username;
		this.country = country;
		this.performance = performance;
		this.globalRank = globalRank;
		this.countryRank = countryRank;
		this.accuracy = accuracy;
	}

	public OsuPlayer getFromAPI(OsuMode mode) throws IOException {
		OsuUser user = StorasBot.osuApi.getUser(Integer.parseInt(userId), mode.getValue(), OsuUser.class);
		return new OsuPlayer(""+user.getUserId(), mode, user.getUserName(), user.getCountry(), user.getPp(), user.getRank(), user.getCountryRank(), user.getAccuracy());
	}
	
	public OsuPlayer getFromDatabase(OsuMode mode) throws IOException {
		OsuPlayer user = StorasBot.database.getTrackedUser(userId, mode);
		return new OsuPlayer(""+user.getUserId(), mode, user.getUsername(), user.getCountry(), user.getPerformance(), user.getGlobalRank(), user.getCountryRank(), user.getAccuracy());
	}

	public String getUserId() {
		return userId;
	}

	public String getUsername() {
		return username;
	}

	public String getCountry() {
		return country;
	}

	public double getPerformance() {
		return performance;
	}
	
	public double getAccuracy() {
		return accuracy;
	}

	public int getGlobalRank() {
		return globalRank;
	}

	public int getCountryRank() {
		return countryRank;
	}
	
	public OsuMode getGamemode() {
		return mode;
	}
	
	public boolean equals(Object object) {
		if (object instanceof OsuPlayer) {
			OsuPlayer other = (OsuPlayer) object;
			return other.userId.equals(this.userId) && other.mode == this.mode;
		}
		return false;
	}
}

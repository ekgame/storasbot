package lt.ekgame.storasbot.utils.osu;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import lt.ekgame.storasbot.StorasDiscord;

public class OsuPlayer {
	
	private OsuPlayerIdentifier identifier;
	private String username;
	private String country;
	private double performance;
	private double accuracy;
	private int globalRank;
	private int countryRank;
	
	public OsuPlayer(OsuPlayerIdentifier identifier) {
		this(identifier, null, null, -1, -1, -1, -1);
	}
	
	public OsuPlayer(OsuPlayerIdentifier identifier, String username, String country, int countryRank, double performance, double accuracy) {
		this(identifier, username, country, performance, -1, countryRank, accuracy);
	}
	
	public OsuPlayer(OsuPlayerIdentifier identifier, String username, String country, double performance, int globalRank, int countryRank, double accuracy) {
		if (identifier == null)
			throw new IllegalArgumentException("identifier can not be null");
		
		this.identifier = identifier;
		this.username = username;
		this.country = country;
		this.performance = performance;
		this.globalRank = globalRank;
		this.countryRank = countryRank;
		this.accuracy = accuracy;
	}

	public OsuPlayer getFromAPI() throws IOException {
		OsuUser user = StorasDiscord.getOsuApi().getUser(getUserId(), getGamemode());
		return new OsuPlayer(identifier, user.getUserName(), user.getCountry(), user.getPp(), user.getRank(), user.getCountryRank(), user.getAccuracy());
	}
	
	public OsuPlayer getFromDatabase() throws IOException {
		OsuPlayer user = StorasDiscord.getDatabase().getTrackedUser(identifier);
		return new OsuPlayer(identifier, user.getUsername(), user.getCountry(), user.getPerformance(), user.getGlobalRank(), user.getCountryRank(), user.getAccuracy());
	}
	
	public List<OsuScore> getCatchedScores() throws SQLException {
		return StorasDiscord.getDatabase().getUserScores(identifier);
	}

	public String getUserId() {
		return identifier.getUserId();
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
		return identifier.getMode();
	}
	
	public OsuPlayerIdentifier getIdentifier() {
		return identifier;
	}
	
	/**
	 * @return true if user IDs and modes match.
	 */
	public boolean equals(Object object) {
		if (object instanceof OsuPlayer) {
			OsuPlayer other = (OsuPlayer) object;
			return identifier.equals(other.identifier);
		}
		return false;
	}
}

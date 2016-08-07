package lt.ekgame.storasbot.plugins.osu_top;

import java.util.List;

import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;

public class TrackedPlayer implements ScoreHandler {

	private String guildId, channelId;
	private int personalTop, minPP;
	private OsuPlayerIdentifier identifier;
	
	public TrackedPlayer(String guildId, String channelId, OsuPlayerIdentifier identifier, int personalTop, int minPP) {
		this.guildId = guildId;
		this.channelId = channelId;
		this.identifier = identifier;
		this.personalTop = personalTop;
		this.minPP = minPP;
	}

	public String getGuildId() {
		return guildId;
	}

	public String getChannelId() {
		return channelId;
	}

	public OsuPlayerIdentifier getIdentifier() {
		return identifier;
	}

	public int getPersonalTop() {
		return personalTop;
	}
	
	public int getMinPerformance() {
		return minPP;
	}

	@Override
	public void handleScoreUpdates(List<OsuScoreUpdate> scores) {
		// TODO Auto-generated method stub
	}
}

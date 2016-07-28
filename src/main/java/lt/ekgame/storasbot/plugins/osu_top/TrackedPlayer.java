package lt.ekgame.storasbot.plugins.osu_top;

import lt.ekgame.storasbot.utils.OsuMode;

public class TrackedPlayer {

	private String guildId, channelId, userId;
	private int personalTop, minPP;
	private OsuMode gamemode;
	
	public TrackedPlayer(String guildId, String channelId, String userId, OsuMode gamemode, int personalTop, int minPP) {
		this.guildId = guildId;
		this.channelId = channelId;
		this.userId = userId;
		this.gamemode = gamemode;
		this.personalTop = personalTop;
		this.minPP = minPP;
	}

	public String getGuildId() {
		return guildId;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getUserId() {
		return userId;
	}

	public OsuMode getGamemode() {
		return gamemode;
	}

	public int getPersonalTop() {
		return personalTop;
	}
	
	public int getMinPerformance() {
		return minPP;
	}
}

package lt.ekgame.storasbot.plugins.osu_top;

import java.util.List;

import org.tillerino.osuApiModel.OsuApiBeatmap;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.OsuMode;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;

public class TrackedCountry {
	/*
	 * is country == null then it's global
	 */
	private String guildId, channelId, country;
	private int countryTop, personalTop, minPerformance;
	private OsuMode gamemode;
	
	public TrackedCountry(Guild guild, TextChannel channel, String country, OsuMode gamemode, int countryTop, int personalTop, int minPerformance) {
		this.guildId = guild.getId();
		this.channelId = channel.getId();
		this.country = country;
		this.countryTop = countryTop;
		this.gamemode = gamemode;
		this.personalTop = personalTop;
		this.minPerformance = minPerformance;
	}
	
	public TrackedCountry(String guildId, String channelId, String country, OsuMode gamemode, int countryTop, int personalTop, int minPerformance) {
		this.guildId = guildId;
		this.channelId = channelId;
		this.country = country;
		this.countryTop = countryTop;
		this.gamemode = gamemode;
		this.personalTop = personalTop;
		this.minPerformance = minPerformance;
	}
	
	public String getGuildId() {
		return guildId;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getCountry() {
		return country;
	}

	public OsuMode getGamemode() {
		return gamemode;
	}

	public int getCountryTop() {
		return countryTop;
	}

	public int getPersonalTop() {
		return personalTop;
	}

	public int getMinPerformance() {
		return minPerformance;
	}
	
	public void handleScoreUpdates(OsuTracker tracker, List<OsuScoreUpdate> scores) {
		for (OsuScoreUpdate score : scores) {
			int leaderboardRank = country == null ? score.getNewPlayer().getGlobalRank() : score.getNewPlayer().getCountryRank();
			boolean isCountryTop  = leaderboardRank <= countryTop;
			boolean isPersonalTop = personalTop == 0 ? false : score.getNewScore().getPersonalTopPlace() <= personalTop;
			boolean isPerformance = minPerformance == 0 ? false : score.getNewScore().getPerformance() >= minPerformance;
			boolean isNew = (System.currentTimeMillis() - score.getNewScore().getTimestamp()) < 1000*60*60*24;
			
			if (isNew && isCountryTop && (isPersonalTop || isPerformance)) {
				Guild guild = StorasBot.client.getGuildById(guildId);
				TextChannel channel = StorasBot.client.getTextChannelById(channelId);
				if (guild == null || channel == null) {
					// TODO remove tracker from db
					break;
				}
				OsuApiBeatmap beatmap = score.getBeamap();
				if (beatmap == null) continue;
				String message = tracker.getFormatter().format(guild, score, beatmap);
				StorasBot.sendMessage(channel, message);
			}
		}
	}
}

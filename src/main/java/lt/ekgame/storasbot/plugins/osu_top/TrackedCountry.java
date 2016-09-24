package lt.ekgame.storasbot.plugins.osu_top;

import java.util.List;

import org.tillerino.osuApiModel.OsuApiBeatmap;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.utils.Tracker;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.utils.SimpleLog;

public class TrackedCountry implements ScoreHandler, Tracker {
	
	private static SimpleLog LOG = SimpleLog.getLog("TrackedCountry");
	
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
	
	public void removeTracker() {
		//if (StorasDiscord.getDatabase().removeCountryTrackerByChannel(channelId))
		//	LOG.info("Removing tracker (" + channelId + "): " + country + " " + gamemode);
	}
	
	public void handleScoreUpdates(List<OsuScoreUpdate> scores) {
		for (OsuScoreUpdate score : scores) {
			int leaderboardRank = country == null ? score.getNewPlayer().getGlobalRank() : score.getNewPlayer().getCountryRank();
			boolean isCountryTop  = leaderboardRank <= countryTop;
			boolean isPersonalTop = personalTop == 0 ? false : score.getNewScore().getPersonalTopPlace() <= personalTop;
			boolean isPerformance = minPerformance == 0 ? false : Math.round(score.getNewScore().getPerformance()) >= minPerformance;
			boolean isNew = (System.currentTimeMillis() - score.getNewScore().getTimestamp()) < 1000*60*10;
			
			if (isNew && isCountryTop && (isPersonalTop || isPerformance)) {
				Guild guild = StorasDiscord.getJDA().getGuildById(guildId);
				TextChannel channel = StorasDiscord.getJDA().getTextChannelById(channelId);
				if (guild == null || channel == null) {
					/*if (StorasDiscord.getDatabase().removeCountryTrackerByChannel(channelId))
						LOG.info("Removing tracker (" + channelId + "): " + country + " " + gamemode);
					*/
					break;
				}
				OsuApiBeatmap beatmap = score.getBeamap();
				if (beatmap == null) continue;
				String message = OsuTracker.messageFormatter.format(guild, score, beatmap);
				StorasDiscord.sendMessage(channel, message);
			}
		}
	}
}

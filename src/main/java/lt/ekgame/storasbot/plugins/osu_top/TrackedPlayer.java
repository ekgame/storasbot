package lt.ekgame.storasbot.plugins.osu_top;

import java.util.List;

import org.tillerino.osuApiModel.OsuApiBeatmap;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.utils.Tracker;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.utils.SimpleLog;

public class TrackedPlayer implements ScoreHandler, Tracker {
	
	private static SimpleLog LOG = SimpleLog.getLog("TrackedPlayer");

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

	public TrackedPlayer(Guild guild, TextChannel channel, OsuPlayerIdentifier identifier, int personalTop, int minPP) {
		this(guild.getId(), channel.getId(), identifier, personalTop, minPP);
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
	
	public void removeTracker() {
		//if (StorasDiscord.getDatabase().removePlayerTrackerByChannel(channelId))
		//	LOG.info("Removing tracker (" + channelId + "): " + identifier.getUserId() + " " + identifier.getMode());
	}

	@Override
	public void handleScoreUpdates(List<OsuScoreUpdate> scores) {
		for (OsuScoreUpdate score : scores) {
			boolean isPersonalTop = personalTop == 0 ? false : score.getNewScore().getPersonalTopPlace() <= personalTop;
			boolean isPerformance = minPP == 0 ? false : Math.round(score.getNewScore().getPerformance()) >= minPP;
			boolean isNew = (System.currentTimeMillis() - score.getNewScore().getTimestamp()) < 1000*60*60*24;
			
			if (isNew && (isPersonalTop || isPerformance)) {
				Guild guild = StorasDiscord.getJDA().getGuildById(guildId);
				TextChannel channel = StorasDiscord.getJDA().getTextChannelById(channelId);
				if (guild == null || channel == null) {
					//removeTracker();
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

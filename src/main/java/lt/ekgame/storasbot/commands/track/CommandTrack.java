package lt.ekgame.storasbot.commands.track;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.plugins.osu_top.TrackedCountry;
import lt.ekgame.storasbot.plugins.osu_top.TrackedPlayer;
import lt.ekgame.storasbot.utils.TableRenderer;
import lt.ekgame.storasbot.utils.Utils;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.MessageChannel;

@CommandReference(isGuild=true, labels = {"track", "tracking"})
public class CommandTrack implements Command<BotCommandContext>  {
	
	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$tracking\n"
			 + "\n"
			 + "Shows information about what's being tracked in this channel.";
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		Guild guild = context.getGuild();
		MessageChannel channel = context.getChannel();
		StringBuilder builder = new StringBuilder();
		
		formatOsuLeaderboard(builder, guild, channel);
		formatOsuPlayers(builder, guild, channel);
		
		if (builder.length() == 0) {
			context.reply("No trackers defined for this channel.");
			return CommandResult.OK;
		}
		else {
			List<String> result = Utils.messageSplit(builder.toString(), 2000 - 6);
			for (String part : result) {
				context.getChannel().sendMessage("```" + part + "```");
			}
			return CommandResult.OK;
		}
	}
	
	private void handleSeperation(StringBuilder builder) {
		if (builder.length() != 0)
			builder.append("\n\n");
	}
	
	private void formatOsuLeaderboard(StringBuilder builder, Guild guild, MessageChannel channel) {
		try {
			List<TrackedCountry> trackers = StorasDiscord.getDatabase().getTrackedCountries(guild)
					.stream().filter(o -> o.getChannelId().equals(channel.getId()))
					.collect(Collectors.toList());
			
			if (trackers.size() == 0)
				return;
			handleSeperation(builder);
			
			TableRenderer table = new TableRenderer();
			table.setHeader("Leaderboard", "TOP", "P-TOP", "Min PP");
			for (TrackedCountry tracker : trackers) {
				String leaderboard = tracker.getCountry() == null ? "global" : tracker.getCountry(); 
				String personalTop = tracker.getPersonalTop() == 0 ? "-" : (""+tracker.getPersonalTop());
				String minPP = tracker.getMinPerformance() == 0 ? "-" : (""+tracker.getMinPerformance());
				table.addRow(leaderboard + " " + tracker.getGamemode().getName(), tracker.getCountryTop(), personalTop, minPP);
			}
			
			builder.append("⬥ Tracked osu! leaderboards:\n");
			builder.append(table.build());
		}
		catch (SQLException e) {
			builder.append("Failed to retrieve tracked leaderboards.\n");
		}
	}
	
	private void formatOsuPlayers(StringBuilder builder, Guild guild, MessageChannel channel) {
		try {
			List<TrackedPlayer> trackers = StorasDiscord.getDatabase().getTrackedPlayers(guild)
					.stream().filter(o -> o.getChannelId().equals(channel.getId()))
					.collect(Collectors.toList());
			
			if (trackers.size() == 0)
				return;
			handleSeperation(builder);
			
			TableRenderer table = new TableRenderer();
			table.setHeader("Player", "Mode", "P-TOP", "Min PP");
			for (TrackedPlayer tracker : trackers) {
				OsuPlayer player = StorasDiscord.getOsuUserCatche().getPlayer(tracker.getIdentifier());
				String username = player == null ? "???" : player.getUsername();
				String personalTop = tracker.getPersonalTop() == 0 ? "-" : (""+tracker.getPersonalTop());
				String minPP = tracker.getMinPerformance() == 0 ? "-" : (""+tracker.getMinPerformance());
				table.addRow(username, tracker.getIdentifier().getMode().getName(), personalTop, minPP);
			}
			
			builder.append("⬥ Tracked osu! players:\n");
			builder.append(table.build());
		}
		catch (SQLException e) {
			builder.append("Failed to retrieve tracked leaderboards.\n");
		}
	}
}

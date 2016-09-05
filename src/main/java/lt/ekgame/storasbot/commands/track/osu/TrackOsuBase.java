package lt.ekgame.storasbot.commands.track.osu;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import com.neovisionaries.i18n.CountryCode;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.commands.engine.DuplicateFlagException;
import lt.ekgame.storasbot.plugins.osu_top.TrackedCountry;
import lt.ekgame.storasbot.plugins.osu_top.TrackedPlayer;
import lt.ekgame.storasbot.utils.Utils;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuUser;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;

public class TrackOsuBase {
	
	private static final int DEFAULT_PERSONAL_TOP = 15;
	private static final int DEFAULT_LEADERBOARD_TOP = 50;
	private static final int DEFAULT_PP_THRESHOLD = 0;
	
	private static final String TRACKING_COUNTRY = "Now tracking **%s** %s **TOP %d** leaderboard for new scores ";
	private static final String TRACKING_PLAYER = "Now tracking **%s** %s for new scores ";
	
	private static final String TRACKING_PERSONAL_TOP = "from players' personal **TOP %d**";
	private static final String TRACKING_INDIVIDUAL_PERSONAL_TOP = "from their personal **TOP %d**";
	private static final String TRACKING_MIN_PP = "worth at least **%dpp**";
	private static final String TRACKING_BOTH = TRACKING_PERSONAL_TOP + " OR scores " + TRACKING_MIN_PP;
	private static final String TRACKING_INDIVIDUAL_BOTH = TRACKING_INDIVIDUAL_PERSONAL_TOP + " OR scores " + TRACKING_MIN_PP;
	
	protected String getHelp(String label) {
		return "Allows server admins to manage osu! ranking tracking for leaderboards and individual users for any gamemode.\n"
			 + "\n"
			 + "Usage:\n"
			 + "$" + label + " player \"<username>\" [flags p, pp]\n"
			 + "- Will post notifications every time the player gets a new score in their personal top (default: 15, changed with \"-p\" flag).\n"
			 + "- If you want to track scores by pp worth (ex: 200), use add these flags: \"-p:0 -pp:200\"\n"
			 + "- If both \"-p\" and \"-pp\" flags are defined, scores that match either one or both conditions will be displayed.\n"
			 + "- \"<username>\" - The quotes are only required for usernames that have spaces.\n"
			 + "\n"
			 + "$" + label + " country <country> [flags t, p, pp]\n"
			 + "- Will post notifications every time a player from the country top 50 (changed with \"-t\" flag).\n"
			 + "- Other flags work the same way as they do for an individual user.\n"
			 + "- <country> expects a 2 letter code ex.: US, RU, CA, etc. \n"
			 + "\n"
			 + "$" + label + " global [flags t, p, pp]\n"
			 + "- Works exactly the same as country tracking, but for the global leaderboard.\n"
			 + "\n"
			 + "$" + label + " remove user \"<user>\"     - Stop tracking a user rankings.\n"
			 + "$" + label + " remove country <country> - Stop tracking a country rankings\n"
			 + "$" + label + " remove global            - Stop tracking the global rankings.\n"
			 + "\n"
			 + "Note: if you want to change tracking settings for something already defined, you DON'T have to remove it first.\n"
			 + "\n"
			 + "Flags:\n"
			 + "-p:15  - Notify about new score from personal top (1-100). 0 to ignore.\n"
			 + "-t:50  - Notify only about new score from top NUM players in the leaderboard (1-100).\n"
			 + "-pp:0  - Notify about new score worth atleast NUM pp. 0 to ignore.\n"
			 + "\n"
			 + "Examples:\n"
			 + "To track a player's personal top 10:\n"
			 + "- $" + label + " user \"My Aim Sucks\" -p:10 \n"
			 + "- $" + label + " user ExGon -p:10 \n"
			 + "\n"
			 + "To track country's top 100 player's personal top 20 OR scores worth atleast 400pp:\n"
			 + "- $" + label + " country ID -t:100 -p:20 -pp:400\n"
			 + "\n"
			 + "To track new score from global top 100 worth atleast 500pp:\n"
			 + "- $" + label + " global -t:100 -pp:500\n";
	}
	
	protected CommandResult handleOsuTrack(CommandIterator command, BotCommandContext context, OsuMode mode) {
		Optional<String> subCommand = command.getToken();
		if (!subCommand.isPresent())
			return context.replyError("What do you want me to do?");
		
		try {
			String sub = subCommand.get().toLowerCase();
			if (sub.equals("user") || sub.equals("player") || sub.equals("u") || sub.equals("p"))
				return handleUser(command, context, mode);
			else if (sub.equals("country") || sub.equals("c"))
				return handleCountry(command, context, mode);
			else if (sub.equals("global") || sub.equals("g"))
				return handleGlobal(command, context, mode);
			else if (sub.equals("remove") || sub.equals("r"))
				return handleRemove(command, context, mode);
			
			return context.replyError("What is \"" + Utils.escapeMarkdownBlock(sub) + "\"?");
		} catch (DuplicateFlagException e) {
			return context.replyError(e.getMessage());
		}
	}

	private CommandResult handleUser(CommandIterator command, BotCommandContext context, OsuMode mode) throws DuplicateFlagException {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		Optional<String> oUsername = command.getString();
		CommandFlags flags = command.getFlags();
		
		if (!oUsername.isPresent()) 
			return context.replyError("Who do you want me to track? Try `$help track -e`.");
		
		int personalTop = flags.getFlagInt("p", DEFAULT_PERSONAL_TOP);
		int ppThreshold = flags.getFlagInt("pp", DEFAULT_PP_THRESHOLD);
		
		if (personalTop < 0 || personalTop > 100)
			return context.replyError("Invalid \"p\" flag. Should be between 1 and 100. 0 to disable.");
		
		if (ppThreshold < 0)
			return context.replyError("Invalid \"pp\" flag. Should be a positive integer. 0 to disable.");
		
		if (personalTop == 0 && ppThreshold == 0)
			return context.replyError("Both \"p\" and \"pp\" flags can not be set to 0.");
		
		try {
			OsuUser user = StorasDiscord.getOsuApi().getUserByUsername(oUsername.get(), mode);
			if (user == null) {
				return context.replyError("That is not a player I know of.");
			}
			else {
				TrackedPlayer tracker = new TrackedPlayer(context.getGuild(), context.getTextChannel(), user.getIdentifier(), personalTop, ppThreshold);
				try {
					StorasDiscord.getDatabase().addOrUpdateTrackedPlayer(tracker);
					return context.replyOk(responseTrackingPlayer(user.getUserName(), tracker));
				} catch (SQLException e) {
					e.printStackTrace();
					return context.replyError("Something went horribly wrong.");
				}
			}
		} catch (IOException e) {
			return context.replyError("Something went wrong, try again later. (osu! API down?)");
		}
	}

	private CommandResult handleCountry(CommandIterator command, BotCommandContext context, OsuMode mode) throws DuplicateFlagException {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		Optional<String> oCountry = command.getString();
		if (!oCountry.isPresent())
			return context.replyError("What do you want me to track?");
		
		String country = oCountry.get();
		CountryCode countryCode = CountryCode.getByCodeIgnoreCase(oCountry.get());
		if (countryCode == null || countryCode == CountryCode.UNDEFINED)
			return context.replyError("\"" + country + "\" is not a country that I know of.");
		
		return handleAddCountry(command, context, countryCode, mode);
	}

	private CommandResult handleGlobal(CommandIterator command, BotCommandContext context, OsuMode mode) throws DuplicateFlagException {
		return handleAddCountry(command, context, null, mode);
	}
	
	private CommandResult handleAddCountry(CommandIterator command, BotCommandContext context, CountryCode country, OsuMode mode) throws DuplicateFlagException {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		CommandFlags flags = command.getFlags();
		int personalTop    = flags.getFlagInt("p", DEFAULT_PERSONAL_TOP);
		int leaderboardTop = flags.getFlagInt("t", DEFAULT_LEADERBOARD_TOP);
		int ppThreshold    = flags.getFlagInt("pp", DEFAULT_PP_THRESHOLD);
		
		if (personalTop < 0 || personalTop > 100)
			return context.replyError("Invalid \"p\" flag. Should be between 1 and 100. 0 to disable.");
		
		if (leaderboardTop < 1 || leaderboardTop > 100)
			return context.replyError("Invalid \"t\" flag. Should be between 1 and 100.");
		
		if (ppThreshold < 0)
			return context.replyError("Invalid \"pp\" flag. Should be a positive integer. 0 to disable.");
		
		if (personalTop == 0 && ppThreshold == 0)
			return context.replyError("Both \"p\" and \"pp\" flags can not be set to 0.");
		
		TrackedCountry tracker = new TrackedCountry(context.getGuild(), context.getTextChannel(),
				country == null ? null : country.getAlpha2(), mode, leaderboardTop, personalTop, ppThreshold);
		
		try {
			StorasDiscord.getDatabase().addOrUpdateTrackedCountry(tracker);
			return context.replyOk(responseTrackingCountry(country, tracker));
		} catch (SQLException e) {
			e.printStackTrace();
			return context.replyError("Something went horribly wrong.");
		}
	}
	
	private CommandResult handleRemove(CommandIterator command, BotCommandContext context, OsuMode mode) {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		Guild guild = context.getGuild();
		TextChannel channel = context.getTextChannel();
		
		Optional<String> subCommand = command.getToken();
		if (!subCommand.isPresent())
			return context.replyError("What do you want me to remove? Try `$help track -r`.");
		
		String sub = subCommand.get().toLowerCase();
		Optional<String> oToRemove = command.getString();
		if (sub.equals("user") || sub.equals("player") || sub.equals("u") || sub.equals("p")) {
			if (!oToRemove.isPresent())
				return context.replyError("Who do you want me to remove? Try `$help track -r`.");
			
			try {
				OsuUser user = StorasDiscord.getOsuApi().getUserByUsername(oToRemove.get(), mode);
				if (user == null) 
					return context.replyError("That's not a player I know of.");
				
				if (StorasDiscord.getDatabase().removeTrackedPlayer(guild, channel, ""+user.getUserId(), mode)) {
					return context.replyOk("No longer tracking **" + user.getUserName() + "** " + mode.getName() + ".");
				}
				else return context.replyError("I'm not even tracking **" + user.getUserName() + "**.");
				
			} catch (IOException e) {
				e.printStackTrace();
				return context.replyError("Something went wrong. (osu!api down?)");
			}
		}
		else if (sub.equals("country") || sub.equals("c")) {
			if (!oToRemove.isPresent())
				return context.replyError("What do you want me to remove?");
			
			String country = oToRemove.get();
			CountryCode countryCode = CountryCode.getByCodeIgnoreCase(oToRemove.get());
			if (countryCode == null || countryCode == CountryCode.UNDEFINED)
				return context.replyError("\"" + country + "\" is not a country that I know of.");
			
			if (StorasDiscord.getDatabase().removeTrackedCountry(guild, channel, countryCode.getAlpha2(), mode)) 
				return context.replyOk("No longer tracking " + countryCode.getName() + " " + mode.getName() + " leaderboard.");
			else
				return context.replyError("I'm not even tracking the " + mode.getName() + " global leaderboard.");
			
		}
		else if (sub.equals("global") || sub.equals("g")) {
			if (StorasDiscord.getDatabase().removeTrackedCountry(guild, channel, null, mode))
				return context.replyOk("No longer tracking the global " + mode.getName() + " leaderboard.");
			else
				return context.replyError("I'm not even tracking the global leaderboard.");
		}
		
		return context.replyError("What is \"" + Utils.escapeMarkdownBlock(sub) + "\"?");
	}
	
	private String responseTrackingCountry(CountryCode country, TrackedCountry tracker) {
		String countryName = country == null ? "global" : country.getName();
		if (tracker.getMinPerformance() == 0) 
			return String.format(TRACKING_COUNTRY + TRACKING_PERSONAL_TOP, countryName, 
				tracker.getGamemode().getName(), tracker.getCountryTop(), tracker.getPersonalTop());
		else if (tracker.getPersonalTop() == 0) 
			return String.format(TRACKING_COUNTRY + TRACKING_MIN_PP, countryName, 
				tracker.getGamemode().getName(), tracker.getCountryTop(), tracker.getMinPerformance());
		else 
			return String.format(TRACKING_COUNTRY + TRACKING_BOTH, countryName, 
				tracker.getGamemode().getName(), tracker.getCountryTop(), tracker.getPersonalTop(), tracker.getMinPerformance());
	}
	
	private String responseTrackingPlayer(String username, TrackedPlayer tracker) {
		if (tracker.getMinPerformance() == 0) 
			return String.format(TRACKING_PLAYER + TRACKING_INDIVIDUAL_PERSONAL_TOP, username,
				tracker.getIdentifier().getMode().getName(), tracker.getPersonalTop());
		else if (tracker.getPersonalTop() == 0) 
			return String.format(TRACKING_PLAYER + TRACKING_MIN_PP, username,
				tracker.getIdentifier().getMode().getName(), tracker.getMinPerformance());
		else 
			return String.format(TRACKING_PLAYER + TRACKING_INDIVIDUAL_BOTH, username,
				tracker.getIdentifier().getMode().getName(), tracker.getPersonalTop(), tracker.getMinPerformance());
	}
}

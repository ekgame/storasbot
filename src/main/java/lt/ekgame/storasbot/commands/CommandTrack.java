package lt.ekgame.storasbot.commands;

import java.sql.SQLException;
import java.util.Optional;

import com.neovisionaries.i18n.CountryCode;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.plugins.osu_top.TrackedCountry;
import lt.ekgame.storasbot.utils.Utils;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;

@CommandReference(isGuild=true, labels = {"track"})
public class CommandTrack implements Command<BotCommandContext>  {
	
	private static final String DEFAULT_GAMEMODE = "osu";
	private static final int DEFAULT_PERSONAL_TOP = 15;
	private static final int DEFAULT_LEADERBOARD_TOP = 50;
	private static final int DEFAULT_PP_THRESHOLD = 0;
	
	private static final String TRACKING_COUNTRY = ":white_check_mark: Now tracking **%s** %s **TOP %d** leaderboard for new scores ";
	private static final String TRACKING_PLAYER = ":white_check_mark: Now tracking **%s** for new scores ";
	
	private static final String TRACKING_PERSONAL_TOP = "from players' personal **TOP %d**";
	private static final String TRACKING_MIN_PP = "worth at least **%dpp**";
	private static final String TRACKING_BOTH = TRACKING_PERSONAL_TOP + " OR scores " + TRACKING_MIN_PP;
	
	@Override
	public String getHelp(CommandFlags flags) {
		// TODO help page
		return "TODO";
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		Optional<String> subCommand = command.getToken();
		if (!subCommand.isPresent()) {
			context.reply("_What do you want me to do? Try `$help track`._");
			return CommandResult.FAIL;
		}
		
		String sub = subCommand.get().toLowerCase();
		if (sub.equals("user"))
			return handleUser(command, context);
		else if (sub.equals("country"))
			return handleCountry(command, context);
		else if (sub.equals("global"))
			return handleGlobal(command, context);
		else if (sub.equals("remove"))
			return handleRemove(command, context);
		else if (sub.equals("info"))
			return handleInfo(command, context);
		
		context.reply("_What is \"" + Utils.escapeMarkdownBlock(sub) + "\"? Try `$help track`._");
		return CommandResult.FAIL;
	}
	
	private CommandResult handleInfo(CommandIterator command, BotCommandContext context) {
		context.reply("_Not implemented._");
		return CommandResult.FAIL;
	}

	private CommandResult handleUser(CommandIterator command, BotCommandContext context) {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		Optional<String> oUsername = command.getString();
		CommandFlags flags = command.getFlags();
		
		if (!oUsername.isPresent()) 
			return context.replyError("Who do you want me to track? Try `$help track -e`.");
		
		OsuMode gamemode = parseGamemode(flags.getFlag("m", DEFAULT_GAMEMODE));
		int personalTop = flags.getFlagInt("p", DEFAULT_PERSONAL_TOP);
		int ppThreshold = flags.getFlagInt("pp", DEFAULT_PP_THRESHOLD);
		
		if (gamemode == null)
			return context.replyError("Unknown gamemode.");
		
		if (personalTop < 0 || personalTop > 100)
			return context.replyError("Invalid \"p\" flag. Should be between 1 and 100. 0 to disable.");
		
		if (ppThreshold < 0)
			return context.replyError("Invalid \"pp\" flag. Should be a positive integer. 0 to disable.");
		
		if (personalTop == 0 && ppThreshold == 0)
			return context.replyError("Both \"p\" and \"pp\" flags can not be set to 0.");
		
		context.reply("TODO: add track player " + oUsername.get());
		return CommandResult.OK;
	}

	private CommandResult handleCountry(CommandIterator command, BotCommandContext context) {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		Optional<String> oCountry = command.getString();
		if (!oCountry.isPresent()) {
			context.reply("What do you want me to track? Try `$help track -e`.");
			return CommandResult.FAIL;
		}
		
		String country = oCountry.get();
		CountryCode countryCode = CountryCode.getByCodeIgnoreCase(oCountry.get());
		if (countryCode == null || countryCode == CountryCode.UNDEFINED)
			return context.replyError("\"" + country + "\" is not a country that I know of.");
		
		return handleAddCountry(command, context, countryCode);
	}

	private CommandResult handleGlobal(CommandIterator command, BotCommandContext context) {
		return handleAddCountry(command, context, null);
	}
	
	public CommandResult handleAddCountry(CommandIterator command, BotCommandContext context, CountryCode country) {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		CommandFlags flags = command.getFlags();
		OsuMode gamemode   = parseGamemode(flags.getFlag("m", DEFAULT_GAMEMODE));
		int personalTop    = flags.getFlagInt("p", DEFAULT_PERSONAL_TOP);
		int leaderboardTop = flags.getFlagInt("t", DEFAULT_LEADERBOARD_TOP);
		int ppThreshold    = flags.getFlagInt("pp", DEFAULT_PP_THRESHOLD);
		
		if (gamemode == null)
			return context.replyError("Unknown gamemode.");
		
		if (personalTop < 0 || personalTop > 100)
			return context.replyError("Invalid \"p\" flag. Should be between 1 and 100. 0 to disable.");
		
		if (leaderboardTop < 1 || leaderboardTop > 100)
			return context.replyError("Invalid \"t\" flag. Should be between 1 and 100.");
		
		if (ppThreshold < 0)
			return context.replyError("Invalid \"pp\" flag. Should be a positive integer. 0 to disable.");
		
		if (personalTop == 0 && ppThreshold == 0)
			return context.replyError("Both \"p\" and \"pp\" flags can not be set to 0.");
		
		TrackedCountry tracker = new TrackedCountry(context.getGuild(), context.getTextChannel(),
				country == null ? null : country.getAlpha2(), gamemode, leaderboardTop, personalTop, ppThreshold);
		
		try {
			StorasBot.getDatabase().addOrUpdateTrackedCountry(tracker);
			context.reply(responseTrackingCountry(country, tracker));
		} catch (SQLException e) {
			e.printStackTrace();
			return context.replyError("Something went horribly wrong.");
		}
		return CommandResult.OK;
	}
	
	private CommandResult handleRemove(CommandIterator command, BotCommandContext context) {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You must have the \"Manage Server\" permissions to use `track` commands.");
		
		Guild guild = context.getGuild();
		TextChannel channel = context.getTextChannel();
		
		Optional<String> subCommand = command.getToken();
		if (!subCommand.isPresent())
			return context.replyError("What do you want me to remove? Try `$help track -r`.");
		
		String sub = subCommand.get().toLowerCase();
		Optional<String> oToRemove = command.getString();
		Optional<String> oMode = command.getString();
		if (sub.equals("user")) {
			if (!oToRemove.isPresent())
				return context.replyError("Who do you want me to remove? Try `$help track -r`.");
			
			context.reply("TODO: remove user " + oToRemove.get());
			return CommandResult.OK;
		}
		else if (sub.equals("country")) {
			if (!oToRemove.isPresent())
				return context.replyError("What do you want me to remove? Try `$help track -r`.");
			OsuMode mode = parseGamemode(oMode);
			
			String country = oToRemove.get();
			CountryCode countryCode = CountryCode.getByCodeIgnoreCase(oToRemove.get());
			if (countryCode == null || countryCode == CountryCode.UNDEFINED)
				return context.replyError("\"" + country + "\" is not a country that I know of.");
			
			if (StorasBot.getDatabase().removeTrackedCountry(guild, channel, countryCode.getAlpha2(), mode)) {
				context.reply("No longer tracking " + countryCode.getName() + " " + mode.getName() + " leaderboard.");
				return CommandResult.OK;
			}
			else return context.replyError("I'm not even tracking the " + mode.getName() + " global leaderboard. Try `$help track -r`");
			
		}
		else if (sub.equals("global")) {
			OsuMode mode = parseGamemode(oToRemove);
			if (StorasBot.getDatabase().removeTrackedCountry(guild, channel, null, mode)) {
				context.reply("No longer tracking the global " + mode.getName() + " leaderboard.");
				return CommandResult.OK;
			}
			else return context.replyError("I'm not even tracking the global leaderboard.");
		}
		
		return context.replyError("What is \"" + Utils.escapeMarkdownBlock(sub) + "\"? Try `$help track +r`.");
	}
	
	private OsuMode parseGamemode(Optional<String> gamemode) {
		if (gamemode.isPresent()) {
			OsuMode mode = parseGamemode(gamemode.get());
			return mode == null ? OsuMode.OSU : mode;
		}
		return OsuMode.OSU;
	}
	
	private OsuMode parseGamemode(String gamemode) {
		if (gamemode.equals("osu") || gamemode.equals("std") || gamemode.equals("standard") || gamemode.equals("o") || gamemode.equals("circles"))
			return OsuMode.OSU;
		if (gamemode.equals("taiko") || gamemode.equals("t") || gamemode.equals("drums"))
			return OsuMode.TAIKO;
		if (gamemode.equals("ctb") || gamemode.equals("c") || gamemode.equals("catch") || gamemode.equals("fruits"))
			return OsuMode.CATCH;
		if (gamemode.equals("mania") || gamemode.equals("m") || gamemode.equals("notes"))
			return OsuMode.MANIA;
		return null;
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
}

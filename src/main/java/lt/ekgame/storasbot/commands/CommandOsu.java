package lt.ekgame.storasbot.commands;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.utils.TableRenderer;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuUser;

@CommandReference(isPrivate=true, isGuild=true, labels={"osu", "oss", "taiko", "catch", "ctb", "mania"})
public class CommandOsu implements Command<BotCommandContext> {
	
	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$osu <user>[,<user>,...]\n"
			 + "$taiko <user>[,<user>,...]\n"
			 + "$catch <user>[,<user>,...]\n"
			 + "$mania <user>[,<user>,...]\n"
			 + "\n"
			 + "Displays osu! statistics for a user. Multiple users may be seperated by commas (up to 6) for comparison. Players will be sorted by rank.";
	}
	
	private <T, V> Object[] getObjects(String title, List<T> objects, Function<T, V> function) {
		Object[] result = new Object[objects.size() + 1];
		result[0] = title;
		List<V> list = objects.stream().map(function).collect(Collectors.toList());
		for (int i = 0; i < list.size(); i++)
			result[i+1] = list.get(i);
		return result;
	}
	
	private OsuMode getMode(String label) {
		if (label.startsWith("o")) return OsuMode.OSU;
		if (label.startsWith("t")) return OsuMode.TAIKO;
		if (label.startsWith("c")) return OsuMode.CATCH;
		if (label.startsWith("m")) return OsuMode.MANIA;
		return null;
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		Optional<String> argument = command.getEverything();
		OsuMode mode = getMode(context.getLabel());
		if (mode == null) 
			return context.replyError("_Something went very wrong. I'm sorry..._");
		
		if (argument.isPresent()) {
			List<String> usernames = Arrays.asList(argument.get().split(",")).stream()
				.map(str -> str.trim())
				.filter(str -> !str.isEmpty())
				.limit(6)
				.collect(Collectors.toList());
			
			if (usernames.isEmpty())
				return context.replyError("I need atleast one username. Work with me here.");
			
			List<String> failed = new ArrayList<>();
			List<OsuUser> users = new ArrayList<>();
			
			if (usernames.size() > 2)            // It will take some time to get all the data
				context.reply("Please wait..."); // so let's give some instant feedback.
			
			for (String username : usernames) {
				try {
					OsuUser user = StorasDiscord.getOsuApi().getUserByUsername(username, mode);
					if (user != null) 
						users.add(user);
					else
						failed.add(username);
				} catch (IOException e) {
					failed.add(username);
				}
			}
			
			users = users.stream().distinct().collect(Collectors.toList());
			
			String message = "";
			if (users.size() > 0) {
				TableRenderer table = new TableRenderer();
				users.sort((a,b) -> a.getRank() - b.getRank());
				NumberFormat number = NumberFormat.getNumberInstance(Locale.UK);
				table.setHeader(getObjects("", users,          user -> user.getUserName()));
				table.addRow(getObjects("Global rank", users,  user -> "#"+user.getRank()));
				table.addRow(getObjects("Country rank", users, user -> user.getCountry()+"#"+user.getCountryRank()));
				table.addRow(getObjects("Performance", users,  user -> String.format(Locale.UK, "%.2fpp", user.getPp())));
				table.addRow(getObjects("Play count" , users,  user -> number.format(user.getPlayCount())));
				table.addRow(getObjects("PP per play", users,     user -> String.format(Locale.UK, "%.2f", user.getPp()/user.getPlayCount())));
				table.addRow(getObjects("Accuracy", users,     user -> String.format(Locale.UK, "%.2f%%", user.getAccuracy())));
				table.addRow(getObjects("Total score" , users, user -> number.format(user.getTotalScore())));
				table.addRow(getObjects("Ranked score", users, user -> number.format(user.getRankedScore())));
				table.addRow(getObjects("Level", users,        user -> user.formatLevel()));
				message += "```cs\n" + table.build() + "```";
			}
			
			if (failed.size() > 0)
				message += "_Failed to find **" + failed.stream().collect(Collectors.joining(", ")) + "**_.";
			
			context.reply(message);
			return failed.size() > 0 ? CommandResult.FAIL : CommandResult.OK;
		}
		else {
			return context.replyError("You don't know what you're doing, do you? Try `$help osu`.");
		}
	}
}

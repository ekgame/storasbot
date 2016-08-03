package lt.ekgame.storasbot.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.utils.BinTag;
import lt.ekgame.storasbot.utils.Utils;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;

@CommandReference(isGuild=true, labels = {"bin"})
public class CommandBin implements Command<BotCommandContext> {

	private static List<String> subCommands = new ArrayList<>();
	private static int PAGE_SIZE = 50;
	
	static {
		subCommands.addAll(Arrays.asList("list", "search", "add", "create", "edit", "change", "delete", "remove", "source", "raw", "random"));
	}

	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$bin add <tag> <text>  - Create a new bin.\n"
			 + "$bin edit <tag> <text> - Change bin's content.\n"
			 + "$bin delete <tag>  - - - Deletes a bin.\n"
		 	 + "$bin list [<page>] - - - Shows a list of bins.\n"
			 + "$bin raw <tag> - - - - - Shows the raw content of a bin.\n"
			 + "$bin random  - - - - - - Content of a random bin.\n"
			 + "$bin <tag> - - - - - - - Content of a bin.\n"
			 + "\n"
			 + "Bin is a feature to save snippets of text and/or links"
			 + " to recall them later. Think of it as a time capsule or"
			 + " a way to quickly answer frequently asked questions.";
	}
	
	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		Optional<String> oSub = command.getToken();
		if (!oSub.isPresent()) {
			context.reply("*You don't know what you're doing, do you? Try `$help bin`.*");
			return CommandResult.FAIL;
		}
		
		String sub = oSub.get().toLowerCase();
		if (sub.equals("list") || sub.equals("search"))        return list(command,   context);
		else if (sub.equals("add") || sub.equals("create"))    return add(command,    context);
		else if (sub.equals("edit") || sub.equals("change"))   return edit(command,   context);
		else if (sub.equals("delete") || sub.equals("remove")) return delete(command, context);
		else if (sub.equals("source") || sub.equals("raw"))    return source(command, context);
		else if (sub.equals("random"))                         return random(command, context);
		else return display(command, sub, context);
	}

	private boolean canEdit(Guild guild, User user, String tagOwner) {
		return user.getId().equals(tagOwner) || Utils.hasCommandPermission(guild, user, Permission.MANAGE_SERVER);
	}
	
	private CommandResult delete(CommandIterator command, BotCommandContext context) {
		User sender = context.getSender();
		Guild guild = context.getGuild();
		
		Optional<String> oTag = command.getToken();
		if (!oTag.isPresent()) {
			context.reply("_What would you like me to delete?_");
			return CommandResult.FAIL;
		}
		String tag = oTag.get();
		
		try {
			BinTag bin = StorasBot.getDatabase().getBin(guild, tag);
			if (bin == null)  {
				context.reply("_Can't delete: bin doesn't exist._");
				return CommandResult.FAIL;
			}
			
			if (canEdit(guild, sender, bin.getUserId())) {
				StorasBot.getDatabase().deleteBin(guild, bin.getId());
				context.reply(":ok_hand:");
				return CommandResult.OK;
			}
			else {
				context.reply("_I'm sorry, " + sender.getAsMention() + ", I can't let you do that._");
				return CommandResult.FAIL;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}

	private CommandResult edit(CommandIterator command, BotCommandContext context) {
		User sender = context.getSender();
		Guild guild = context.getGuild();
		
		Optional<String> oTag = command.getToken();
		Optional<String> oContent = command.getEverything();
		
		if (!oTag.isPresent()) {
			context.reply("_What would you like me to edit?_");
			return CommandResult.FAIL;
		}
		
		if (!oContent.isPresent()) {
			context.reply("_What would you like me to change that to?_");
			return CommandResult.FAIL;
		}
		
		String tag = oTag.get();
		String content = oContent.get();
		
		try {
			BinTag bin = StorasBot.getDatabase().getBin(guild, tag);
			if (bin == null)  {
				context.reply("_That bin doesn't exist. Use `add` to create it._");
				return CommandResult.FAIL;
			}
			
			if (canEdit(guild, sender, bin.getUserId())) {
				StorasBot.getDatabase().editBin(guild, bin.getId(), content);
				context.reply(":ok_hand:");
				return CommandResult.OK;
			}
			else {
				context.reply("_I'm sorry, " + sender.getAsMention() + ", I can't let you do that._");
				return CommandResult.FAIL;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}

	private CommandResult add(CommandIterator command, BotCommandContext context) {
		User sender = context.getSender();
		Guild guild = context.getGuild();
		
		Optional<String> oTag = command.getToken();
		Optional<String> oContent = command.getEverything();
		
		if (!oTag.isPresent()) {
			context.reply("_You have to give a name to the thing you want to add._");
			return CommandResult.FAIL;
		}
		
		if (!oContent.isPresent()) {
			context.reply("_What would you like me to add?_");
			return CommandResult.FAIL;
		}
		
		String tag = oTag.get();
		String content = oContent.get();
		
		try {
			BinTag bin = StorasBot.getDatabase().getBin(guild, tag);
			if (bin != null)  {
				context.reply("_That bin already taken._");
				return CommandResult.FAIL;
			}
			
			if (subCommands.contains(tag)) {
				context.reply("_That bin tag is not allowed._");
				return CommandResult.FAIL;
			}

			StorasBot.getDatabase().addBin(guild, sender.getId(), tag, content);
			context.reply(":ok_hand:");
			return CommandResult.OK;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}
	
	private CommandResult display(CommandIterator command, String tag, BotCommandContext context) {
		Guild guild = context.getGuild();
		try {
			BinTag bin = StorasBot.getDatabase().getBin(guild, tag);
			if (bin == null)  {
				context.reply("*That bin doesn't exist.*");
				return CommandResult.FAIL;
			}
			
			StorasBot.getDatabase().incrementBinUsage(guild, bin.getId());
			context.reply(bin.getContent());
			return CommandResult.OK;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}

	private CommandResult list(CommandIterator command, BotCommandContext context) {
		int page = 1;
		Optional<Integer> oPage = command.getInteger();
		if (oPage.isPresent() && oPage.get() > 1) page = oPage.get();
		
		try {
			Guild guild = context.getGuild();
			long count = StorasBot.getDatabase().getBinsCount(guild);
			if (count == 0) {
				context.reply("_The bin is empty._");
				return CommandResult.OK;
			}
			long pages = count/PAGE_SIZE + 1;
			if (page > pages) {
				context.reply("_That page doesn't exist._");
				return CommandResult.FAIL;
			}
			else {
				List<String> tags = StorasBot.getDatabase().getBinList(guild, PAGE_SIZE, page);
				String result = Utils.escapeMarkdownBlock(tags.stream().collect(Collectors.joining(", ")));
				context.reply(String.format("**Page %d/%d:** ```%s```", page, pages, result));
				return CommandResult.OK;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}
	

	private CommandResult source(CommandIterator command, BotCommandContext context) {
		Optional<String> oTag = command.getToken();
		if (!oTag.isPresent()) {
			context.reply("_What do you want to see?_");
			return CommandResult.FAIL;
		}
		String tag = oTag.get();
		Guild guild = context.getGuild();
		try {
			BinTag bin = StorasBot.getDatabase().getBin(guild, tag);
			if (bin == null)  {
				context.reply("_That bin doesn't exist._");
				return CommandResult.FAIL;
			}
			
			context.reply("```" + Utils.escapeMarkdownBlock(bin.getContent()) + "```");
			return CommandResult.OK;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}
	

	private CommandResult random(CommandIterator command, BotCommandContext context) {
		try {
			Guild guild = context.getGuild();
			long count = StorasBot.getDatabase().getBinsCount(guild);
			if (count == 0) {
				context.reply("_The bin is empty._");
				return CommandResult.OK;
			}
			
			int offset = new Random().nextInt((int)count);
			BinTag bin = StorasBot.getDatabase().getBinOffset(guild, offset);
			if (bin == null) {
				context.reply("*You have to `add` bins first.*");
				return CommandResult.FAIL;
			}
			context.reply(String.format("`%s`\n%s", bin.getTag(), bin.getContent()));
			return CommandResult.OK;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}
}

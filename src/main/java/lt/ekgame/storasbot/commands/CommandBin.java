package lt.ekgame.storasbot.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import lt.ekgame.storasbot.StorasDiscord;
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
		if (!oSub.isPresent())
			return context.replyError("You don't know what you're doing, do you? Try `$help bin`.");
		
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
		if (!oTag.isPresent())
			return context.replyError("What would you like me to delete?");
		
		String tag = oTag.get();
		
		try {
			BinTag bin = StorasDiscord.getDatabase().getBin(guild, tag);
			if (bin == null)  {
				return context.replyError("Can't delete: bin doesn't exist.");
			}
			
			if (canEdit(guild, sender, bin.getUserId())) {
				StorasDiscord.getDatabase().deleteBin(guild, bin.getId());
				context.reply(":ok_hand:");
				return CommandResult.OK;
			}
			else {
				return context.replyError("I'm sorry, " + sender.getAsMention() + ", I can't let you do that.");
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
		
		if (!oTag.isPresent())
			return context.replyError("What would you like me to edit?");
		
		if (!oContent.isPresent())
			return context.replyError("What would you like me to change that to");
		
		String tag = oTag.get();
		String content = oContent.get();
		
		try {
			BinTag bin = StorasDiscord.getDatabase().getBin(guild, tag);
			if (bin == null)
				return context.replyError("That bin doesn't exist. Use `add` to create it.");
			
			if (canEdit(guild, sender, bin.getUserId())) {
				StorasDiscord.getDatabase().editBin(guild, bin.getId(), content);
				context.reply(":ok_hand:");
				return CommandResult.OK;
			}
			else {
				return context.replyError("I'm sorry, " + sender.getAsMention() + ", I can't let you do that.");
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
		
		if (!oTag.isPresent())
			return context.replyError("You have to give a name to the thing you want to add.");
		
		if (!oContent.isPresent())
			return context.replyError("What would you like me to add?");
		
		String tag = oTag.get();
		String content = oContent.get();
		
		try {
			BinTag bin = StorasDiscord.getDatabase().getBin(guild, tag);
			if (bin != null)
				return context.replyError("That bin already taken.");
			
			if (subCommands.contains(tag))
				return context.replyError("That bin tag is not allowed.");

			StorasDiscord.getDatabase().addBin(guild, sender.getId(), tag, content);
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
			BinTag bin = StorasDiscord.getDatabase().getBin(guild, tag);
			if (bin == null)
				return context.replyError("That bin doesn't exist.");
			
			StorasDiscord.getDatabase().incrementBinUsage(guild, bin.getId());
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
			long count = StorasDiscord.getDatabase().getBinsCount(guild);
			if (count == 0)
				
				return context.replyOk("The bin is empty.");
			long pages = count/PAGE_SIZE + 1;
			if (page > pages)
				return context.replyError("That page doesn't exist.");
			else {
				List<String> tags = StorasDiscord.getDatabase().getBinList(guild, PAGE_SIZE, page);
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
		if (!oTag.isPresent())
			return context.replyError("What do you want to see?");
		
		String tag = oTag.get();
		Guild guild = context.getGuild();
		try {
			BinTag bin = StorasDiscord.getDatabase().getBin(guild, tag);
			if (bin == null)
				return context.replyError("That bin doesn't exist.");
			
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
			long count = StorasDiscord.getDatabase().getBinsCount(guild);
			if (count == 0)
				return context.replyOk("The bin is empty.");
			
			int offset = new Random().nextInt((int)count);
			BinTag bin = StorasDiscord.getDatabase().getBinOffset(guild, offset);
			if (bin == null)
				return context.replyError("You have to `add` bins first.");
				
			context.reply(String.format("`%s`\n%s", bin.getTag(), bin.getContent()));
			return CommandResult.OK;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return CommandResult.FAIL;
	}
}

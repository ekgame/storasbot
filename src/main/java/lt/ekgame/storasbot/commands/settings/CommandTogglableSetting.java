package lt.ekgame.storasbot.commands.settings;

import java.util.Optional;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.commands.engine.DuplicateFlagException;
import lt.ekgame.storasbot.utils.Utils;
import net.dv8tion.jda.Permission;

public abstract class CommandTogglableSetting implements Command<BotCommandContext> {

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) throws DuplicateFlagException {
		if (!Utils.hasCommandPermission(context.getGuild(), context.getSender(), Permission.MANAGE_SERVER))
			return context.replyError("You don't have the \"Manage server\" permission.");
		
		Optional<String> token = command.getToken();
		CommandFlags flags = command.getFlags();
		if (token.isPresent()) {
			String raw = token.get().toLowerCase();
			if (raw.equals("enable") || raw.equals("e") || raw.equals("true") || raw.equals("t")) {
				return context.replyOk(onEnable(flags, context));
			}
			else if (raw.equals("disable") || raw.equals("d") || raw.equals("false") || raw.equals("f")) {
				return context.replyOk(onDisable(flags, context));
			}
			else {
				return context.replyError("You don't know what you're doing, do you? Try `$help " + context.getLabel() + "`");
			}
		}
		else {
			return context.replyError("You don't know what you're doing, do you? Try `$help " + context.getLabel() + "`");
		}
	}

	@Override
	public abstract String getHelp(CommandFlags flags);
	
	public abstract String onEnable(CommandFlags flags, BotCommandContext context);
	
	public abstract String onDisable(CommandFlags flags, BotCommandContext context);

}

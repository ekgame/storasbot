package lt.ekgame.storasbot.commands;

import java.util.Optional;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandListener.CommandDefinition;
import lt.ekgame.storasbot.utils.Utils;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.commands.engine.DuplicateFlagException;

@CommandReference(isPrivate=true, isGuild=true, labels = {"help", "?"})
public class CommandHelp implements Command<BotCommandContext> {

	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$help <command>\n"
			 + "\n"
			 + "Displays usage and the functionality of a command.";
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		Optional<String> token = command.getToken();
		if (token.isPresent()) {
			String commandLabel = token.get();
			Optional<CommandDefinition> oCommand = StorasDiscord.getCommandHandler().getCommandByName(commandLabel);
			if (oCommand.isPresent()) {
				try {
					CommandFlags flags = command.getFlags();
					Command<BotCommandContext> theCommand = oCommand.get().getInstance();
					String help = "```" + Utils.escapeMarkdownBlock(theCommand.getHelp(flags)) + "```";
					context.reply(help);
					return CommandResult.OK;
				} catch (DuplicateFlagException e) {
					return context.replyError(e.getMessage());
				}
			}
			else {
				return context.replyError("I don't know any **" + commandLabel + "** command.");
			}
		}
		else {
			context.reply("For a complete list of commands, visit <http://bot.ekga.me/docs>.");
			return CommandResult.OK;
		}
	}
}

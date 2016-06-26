package lt.ekgame.storasbot.commands;

import java.util.Optional;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.Utils;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandListener.CommandDefinition;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;

@CommandReference(isPrivate=true, isGuild=true, labels = {"help", "?"})
public class CommandHelp implements Command<BotCommandContext> {

	@Override
	public String getHelp() {
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
			Optional<CommandDefinition> oCommand = StorasBot.commandHandler.getCommandByName(commandLabel);
			if (oCommand.isPresent()) {
				Command<BotCommandContext> theCommand = oCommand.get().getInstance();
				String help = "```" + Utils.escapeMarkdownBlock(theCommand.getHelp()) + "```";
				context.reply(help);
				return CommandResult.OK;
			}
			else {
				context.reply("_I don't know any **" + commandLabel + "** command._");
				return CommandResult.FAIL;
			}
		}
		else {
			context.reply("_For a complete list of commands, visit https://bitbucket.org/ekgame/storasbot/wiki/Home._");
			return CommandResult.OK;
		}
	}
}

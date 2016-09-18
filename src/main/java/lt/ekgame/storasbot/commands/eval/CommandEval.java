package lt.ekgame.storasbot.commands.eval;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;

// Disabled for potential CPU drain
@CommandReference(isGuild=true, labels = {"eval", "exec"})
public class CommandEval implements Command<BotCommandContext>  {

	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$eval <javascript expression/code>\n"
			 + "or\n"
			 + "$eval ```\n"
			 + "<javascript expression/code>\n"
			 + "```\n"
			 + "\n"
			 + "Executes a JavaScript expression or code using Nashorn engine.\n"
			 + "To print something, use print.line(<object>) or print.format(<format>[, objects...]).\n"
			 + "print.format(...) works just like Java's/C++ printf(...).";
		
	}
	
	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		//StorasBot.getJDA().addEventListener(new CodeExecutor(context.getMessage()));
		//return CommandResult.OK; // Content changes are handled by the executor
		
		// Disabled for potential CPU drain
		return context.replyError("This feature is disabled for possible CPU draining problems.");
	}
}

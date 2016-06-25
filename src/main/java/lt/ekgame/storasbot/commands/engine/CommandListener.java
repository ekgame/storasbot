package lt.ekgame.storasbot.commands.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import lt.ekgame.storasbot.commands.CommandBin;
import lt.ekgame.storasbot.commands.CommandHelp;
import lt.ekgame.storasbot.commands.CommandPrune;
import lt.ekgame.storasbot.commands.eval.CommandEval;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
	
	private static String prefix = "$";
	private static List<Command<BotCommandContext>> commands = new ArrayList<>();
	
	private static List<FailedCommand> failedCommands = new LinkedList<>();
	
	static {
		commands.add(new CommandBin());
		commands.add(new CommandPrune());
		commands.add(new CommandHelp());
		commands.add(new CommandEval());
	}
	
	private void handleCommand(Guild guild, Message message, FailedCommand failedCommand) {
		String rawCommand = message.getContent().trim().substring(prefix.length());
		CommandIterator iterator = new CommandIterator(rawCommand);
		
		Optional<String> oLabel = iterator.getToken();
		if (!oLabel.isPresent()) return;
		String label = oLabel.get().toLowerCase();
		
		Optional<Command<BotCommandContext>> oCommand = getCommandByName(label);
		BotCommandContext theContext = failedCommand == null ? new BotCommandContext(message, guild) : failedCommand.context;
		
		if (oCommand.isPresent()) {
			Command<BotCommandContext> command = oCommand.get();
			
			if (message.getChannel() instanceof PrivateChannel && !command.isPrivateCommand()) {
				theContext.reply("_This command can not be used in private chat._");
				return;
			}
			
			if (message.getChannel() instanceof TextChannel && !command.isGuildCommand()) {
				theContext.reply("_This command can only be used in private chat._");
				return;
			}
			
			CommandResult result = command.execute(iterator, theContext);
			if (result == CommandResult.FAIL && failedCommand == null)
				addFailedCommand(message, theContext);
			else if (result == CommandResult.FAIL)
				failedCommand.timestamp = System.currentTimeMillis();
			else if (result == CommandResult.OK && failedCommand != null)
				removeFailedCommand(failedCommand);
		}
		else {
			theContext.reply("_I don't know what you want me to do._");
			if (failedCommand == null)
				addFailedCommand(message, theContext);
			else
				failedCommand.timestamp = System.currentTimeMillis();
		}
	}

	@Override
    public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getMessage().getContent().trim().startsWith(prefix)) {
			handleCommand(event.getGuild(), event.getMessage(), null);
		}
	}
	
	@Override
	public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
		FailedCommand command = getFailedCommand(event.getMessage());
		if (command != null && event.getMessage().getContent().trim().startsWith(prefix)) {
			handleCommand(event.getGuild(), event.getMessage(), command);
		}
	}

	private FailedCommand getFailedCommand(Message message) {
		synchronized (failedCommands) {
			Iterator<FailedCommand> iterator = failedCommands.iterator();
			while (iterator.hasNext()) {
				FailedCommand command = iterator.next();
				if (command.original.equals(message))
					return command;
				
				// remove failed command cache after 60 seconds
				if (System.currentTimeMillis() - command.timestamp > 60*1000)
					iterator.remove();
			}
		}
		return null;
	}

	public Optional<Command<BotCommandContext>> getCommandByName(String name) {
		name = name.toLowerCase().trim();
		for (Command<BotCommandContext> command : commands) {
			List<String> labels = command.getLabels();
			for (String label : labels) 
				if (label.toLowerCase().equals(name))
					return Optional.of(command);
		}
		return Optional.empty();
	}
	
	private void addFailedCommand(Message original, BotCommandContext context) {
		synchronized (failedCommands) {
			failedCommands.add(new FailedCommand(original, context));
		}
	}	
	
	private void removeFailedCommand(FailedCommand command) {
		synchronized (failedCommands) {
			failedCommands.remove(command);
		}
	}
	
	class FailedCommand {
		protected Message original;
		protected BotCommandContext context;
		protected long timestamp = System.currentTimeMillis();
		
		FailedCommand(Message original, BotCommandContext context) {
			this.original = original;
			this.context = context;
		}
	}
}

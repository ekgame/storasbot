package lt.ekgame.storasbot.commands.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.SimpleLog;

@SuppressWarnings("unchecked")
public class CommandListener extends ListenerAdapter {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Command Listener");
	
	private static String prefix = "$";
	private List<CommandDefinition> commands = new ArrayList<>();
	private List<FailedCommand> failedCommands = new LinkedList<>();
	
	public CommandListener() {
		try {
			ClassPath classPath = ClassPath.from(CommandListener.class.getClassLoader());
			Set<ClassInfo> classes = classPath.getTopLevelClassesRecursive("lt.ekgame.storasbot.commands");
			for (ClassInfo info : classes) {
				try {
					CommandReference reference = info.load().getAnnotation(CommandReference.class);
					if (reference != null) {
						Object rawCommand = info.load().newInstance();
						if (rawCommand instanceof Command) {
							commands.add(new CommandDefinition((Command<BotCommandContext>) rawCommand, reference));
							LOG.info("Loaded " + info.getSimpleName() + ".");
						}
						else {
							LOG.warn("Failed to load class \"" + info.getResourceName() + "\" - doesn't extend Command.");
						}
					}
				} catch (InstantiationException | IllegalAccessException e) {
					LOG.fatal("Failed to load class \"" + info.getResourceName() + "\" - " + e.getMessage());
					e.printStackTrace();
				}
				//System.out.println(info.load().getName() + " " + info.getClass().isAnnotationPresent(CommandReference.class));
			}
		} catch (IOException e) {
			LOG.fatal("Failed to get classpath from \"lt.ekgame.storasbot.commands\" - " + e.getMessage());
		}
	}
	
	private void handleCommand(Guild guild, Message message, FailedCommand failedCommand) {
		String rawCommand = message.getContent().trim().substring(prefix.length());
		CommandIterator iterator = new CommandIterator(rawCommand);
		
		Optional<String> oLabel = iterator.getToken();
		if (!oLabel.isPresent()) return;
		String label = oLabel.get().toLowerCase();
		
		Optional<CommandDefinition> oCommand = getCommandByName(label);
		BotCommandContext theContext = failedCommand == null ? new BotCommandContext(message, guild) : failedCommand.context;
		
		if (oCommand.isPresent()) {
			CommandDefinition definition = oCommand.get();
			
			if (message.getChannel() instanceof PrivateChannel && !definition.isPrivateCommand()) {
				theContext.reply("_This command can not be used in private chat._");
				return;
			}
			
			if (message.getChannel() instanceof TextChannel && !definition.isGuildCommand()) {
				theContext.reply("_This command can only be used in private chat._");
				return;
			}
			
			Command<BotCommandContext> command = definition.getInstance();
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

	public Optional<CommandDefinition> getCommandByName(String name) {
		name = name.toLowerCase().trim();
		for (CommandDefinition command : commands) {
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
		
		protected FailedCommand(Message original, BotCommandContext context) {
			this.original = original;
			this.context = context;
		}
	}
	
	public class CommandDefinition {
		private Command<BotCommandContext> command;
		private CommandReference reference;
		private List<String> labels;
		
		private CommandDefinition(Command<BotCommandContext> command, CommandReference reference) {
			this.command = command;
			this.reference = reference;
			this.labels = Arrays.asList(reference.labels());
		}
		
		public Command<BotCommandContext> getInstance() {
			return command;
		}
		
		public boolean isGuildCommand() {
			return reference.isGuild();
		}
		
		public boolean isPrivateCommand() {
			return reference.isPrivate();
		}
		
		public List<String> getLabels() {
			return labels;
		}
	}
}

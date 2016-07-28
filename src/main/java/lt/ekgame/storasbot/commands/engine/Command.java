package lt.ekgame.storasbot.commands.engine;

public interface Command<T> {
	
	CommandResult execute(CommandIterator command, T context);
	
	String getHelp(CommandFlags flags);

}

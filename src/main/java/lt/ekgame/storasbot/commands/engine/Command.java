package lt.ekgame.storasbot.commands.engine;

public interface Command<T> {
	
	CommandResult execute(CommandIterator command, T context) throws DuplicateFlagException;
	
	String getHelp(CommandFlags flags);

}

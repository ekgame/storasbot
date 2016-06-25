package lt.ekgame.storasbot.commands.engine;

import java.util.List;

public interface Command<T> {
	
	CommandResult execute(CommandIterator command, T context);
	
	List<String> getLabels();
	
	String getHelp();
	
	boolean isGuildCommand();
	
	boolean isPrivateCommand();

}

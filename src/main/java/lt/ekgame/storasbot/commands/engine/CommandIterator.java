package lt.ekgame.storasbot.commands.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandIterator {
	
	private static Pattern TOKEN = Pattern.compile("^\\s*([^\\s]+)");
	private static Pattern TOKEN_OR_QUOTED = Pattern.compile("^\\s*(\"(.*?)\"|([^\\s]+))");
	private static Pattern LINE = Pattern.compile("^\\s*(.*)");
	private static Pattern EVERYTHING = Pattern.compile("^\\s*(.*)", Pattern.DOTALL);
	
	private static Pattern FLAG = Pattern.compile("^\\s*\\-([^\\s:]+)");
	private static Pattern FLAG_VALUE = Pattern.compile("^:(\"(.*?)\"|([^\\s]+))");
	
	private String command;
	
	public CommandIterator(String command) {
		this.command = command;
	}
	
	private Optional<String> getFlag() {
		Matcher matcher = FLAG.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			return Optional.of(matcher.group(1));
		}
		return Optional.empty();
	}
	
	private Optional<String> getFlagContent() {
		Matcher matcher = FLAG_VALUE.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			return Optional.of(matcher.group(2) == null ? matcher.group(1) : matcher.group(2));
		}
		return Optional.empty();
	}
	
	public CommandFlags getFlags() throws DuplicateFlagException {
		Map<String, String> flags = new HashMap<>();
		
		while (true) {
			Optional<String> flag = getFlag();
			if (!flag.isPresent()) {
				if (getToken().isPresent()) continue;
				else break;
			}
			
			String key = flag.get().toLowerCase();
			String value = getFlagContent().orElse(null);
			if (flags.containsKey(key))
				throw new DuplicateFlagException("Duplicate flag \""+key+"\".");
			flags.put(key, value);
		}
		
		return new CommandFlags(flags);
	}
	
	public Optional<String> getToken() {
		Matcher matcher = TOKEN.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			return Optional.of(matcher.group(1));
		}
		return Optional.empty();
	}
	
	public Optional<Double> getDouble() {
		Matcher matcher = TOKEN.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			try {
				return Optional.of(Double.parseDouble(matcher.group(1)));
			}
			catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}
	
	public Optional<Integer> getInteger() {
		Matcher matcher = TOKEN.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			try {
				return Optional.of(Integer.parseInt(matcher.group(1)));
			}
			catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}
	
	public Optional<String> getString() {
		Matcher matcher = TOKEN_OR_QUOTED.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			return Optional.of(matcher.group(2) == null ? matcher.group(1) : matcher.group(2));
		}
		return Optional.empty();
	}
	
	public Optional<String> getLine() {
		Matcher matcher = LINE.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			return Optional.of(matcher.group(1));
		}
		return Optional.empty();
	}
	
	public Optional<String> getEverything() {
		Matcher matcher = EVERYTHING.matcher(command);
		if (matcher.find()) {
			command = command.substring(matcher.group(0).length());
			if (matcher.group(1).trim().isEmpty())
				return Optional.empty();
			return Optional.of(matcher.group(1).trim());
		}
		return Optional.empty();
	}
}

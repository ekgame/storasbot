package lt.ekgame.storasbot.commands.engine;

import java.util.Collections;
import java.util.Map;

public class CommandFlags {
	
	private Map<String, String> flags;
	
	public CommandFlags(Map<String, String> flags) {
		this.flags = flags;
	}
	
	public Map<String, String> getRawFlags() {
		return Collections.unmodifiableMap(flags);
	}
	
	public double getFlagDouble(String name, double _default) {
		name = name.toLowerCase();
		if (!flags.containsKey(name) || flags.get(name) == null)
			return _default;
		
		try {
			return Double.parseDouble(flags.get(name));
		}
		catch (NumberFormatException e) {
			return _default; // Not double string, return default
		}
	}
	
	public int getFlagInt(String name, int _default) {
		name = name.toLowerCase();
		if (!flags.containsKey(name) || flags.get(name) == null)
			return _default;
		
		try {
			return Integer.parseInt(flags.get(name));
		}
		catch (NumberFormatException e) {
			return _default; // Not integer string, return default
		}
	}
	
	public String getFlag(String name, String _default) {
		name = name.toLowerCase();
		if (!flags.containsKey(name) || flags.get(name) == null)
			return _default;
		return flags.get(name);
	}
	
	public boolean getFlagBool(String name) {
		name = name.toLowerCase();
		if (!flags.containsKey(name))
			return false;
		
		String value = flags.get(name);
		if (value == null) return true;
		value = value.toLowerCase();
		
		if (value.equals("true") || value.equals("tru") || value.equals("t") || value.equals("yes") || value.equals("y") || value.equals("ok"))
			return true;
		
		if (value.equals("false") || value.equals("f") || value.equals("no") || value.equals("n") || value.equals("nope"))
			return false;
		
		return true; // bool flag exists with weird argument, but still return true because the flag is defined
	}
}

package lt.ekgame.storasbot.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lt.ekgame.storasbot.Database;
import net.dv8tion.jda.entities.Guild;

public class Settings {
	
	private Guild guild;
	private Database database;
	private Map<Setting, Object> settings = new HashMap<>();
	
	public Settings(Guild guild, Database database) {
		this.guild = guild;
		this.database = database;
		List<Map<String, Object>> cached = database.getSettings(guild);
		
		for (Setting setting : Setting.values()) {
			for (Map<String, Object> map : cached) {
				if (map.get("setting").equals(setting.getSQLName())) {
					String raw = (String) map.get("value");
					
					Object casted = null;
					if (setting.getType().equals(String.class))
						casted = raw;
					else if (setting.getType().equals(Integer.class))
						casted = new Integer(raw);
					else if (setting.getType().equals(Double.class))
						casted = new Double(raw);
					else if (setting.getType().equals(Boolean.class))
						casted = new Boolean(raw);
					else 
						throw new IllegalStateException("Unsupported settings class: " + setting.getType());
					
					settings.put(setting, casted);
					continue;
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Setting setting, Class<T> type) {
		if (settings.containsKey(setting))
			return (T) settings.get(setting);
		return (T) setting.getDefault();
	}
	
	public void update(Setting setting, Object value) {
		settings.put(setting, value);
		database.updateSetting(guild, setting, value.toString());
	}

}

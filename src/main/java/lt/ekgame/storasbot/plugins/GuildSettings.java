package lt.ekgame.storasbot.plugins;

import java.util.HashMap;
import java.util.Map;

import lt.ekgame.storasbot.Database;
import net.dv8tion.jda.entities.Guild;

public class GuildSettings {
	
	private Map<Guild, Settings> cache = new HashMap<>();
	private Database database;
	
	public GuildSettings(Database database) {
		this.database = database;
	}
	
	public Settings getSettings(Guild guild) {
		if (cache.containsKey(guild)) {
			return cache.get(guild);
		}
		else {
			Settings settings = new Settings(guild, database);
			cache.put(guild, settings);
			return settings;
		}
	}

}

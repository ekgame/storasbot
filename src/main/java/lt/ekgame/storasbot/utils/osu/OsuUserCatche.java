package lt.ekgame.storasbot.utils.osu;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lt.ekgame.storasbot.Database;
import lt.ekgame.storasbot.StorasDiscord;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuUserCatche {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Player Catche");
	private Map<OsuPlayerIdentifier, OsuPlayer> catche = new HashMap<>();
	
	public OsuUserCatche() throws SQLException {
		updateCatche();
	}
	
	public void updateCatche() throws SQLException {
		catche.clear();
		Database db = StorasDiscord.getDatabase();
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.OSU));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.TAIKO));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.CATCH));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.MANIA));
		LOG.debug("Catche loaded (" + catche.entrySet().size() + ").");
	}
	
	private void addPlayers(List<OsuPlayer> players) {
		for (OsuPlayer player : players)
			catche.put(player.getIdentifier(), player);
	}
	
	public void updatePlayer(OsuPlayer player) {
		synchronized (catche) {
			catche.put(player.getIdentifier(), player);
		}
	}

	public OsuPlayer getPlayer(OsuPlayerIdentifier identifier) {
		synchronized (catche) {
			return catche.get(identifier);
		}
	}
}

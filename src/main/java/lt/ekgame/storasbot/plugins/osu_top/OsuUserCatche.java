package lt.ekgame.storasbot.plugins.osu_top;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lt.ekgame.storasbot.Database;
import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuUserCatche {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Player Catche");
	private Map<OsuPlayerIdentifier, OsuPlayer> catche = new HashMap<>();
	
	public OsuUserCatche() throws SQLException {
		LOG.info("Loading catche.");
		Database db = StorasBot.getDatabase();
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.OSU));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.TAIKO));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.CATCH));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.MANIA));
		LOG.info("Catche loaded (" + catche.entrySet().size() + ").");
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

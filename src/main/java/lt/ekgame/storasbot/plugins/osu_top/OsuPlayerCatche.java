package lt.ekgame.storasbot.plugins.osu_top;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import lt.ekgame.storasbot.Database;
import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;

public class OsuPlayerCatche {
	
	private Map<OsuPlayerIdentifier, OsuPlayer> catche;
	
	public OsuPlayerCatche() throws SQLException {
		Database db = StorasBot.getDatabase();
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.OSU));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.TAIKO));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.CATCH));
		addPlayers(db.getTrackedCountryPlayers(null, OsuMode.MANIA));
	}
	
	private void addPlayers(List<OsuPlayer> players) {
		for (OsuPlayer player : players)
			catche.put(player.getIdentifier(), player);
	}
	
	public void updatePlayer(OsuPlayer player) {
		synchronized (catche) {
			catche.merge(player.getIdentifier(), player, (u, t) -> t);
		}
	}

	public OsuPlayer getPlayer(OsuPlayerIdentifier identifier) {
		synchronized (catche) {
			return catche.get(identifier);
		}
	}
}

package lt.ekgame.storasbot.plugins;

import com.typesafe.config.Config;

import lt.ekgame.storasbot.StorasDiscord;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class GameChanger extends ListenerAdapter {
	
	private String game;
	
	public GameChanger(Config config) {
		game = StorasDiscord.getConfig().getString("general.game");
	}

	@Override
	public void onReady(ReadyEvent event) {
		StorasDiscord.getJDA().getAccountManager().setGame(game);
	}

}

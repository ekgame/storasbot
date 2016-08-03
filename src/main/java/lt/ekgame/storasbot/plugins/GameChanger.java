package lt.ekgame.storasbot.plugins;

import com.typesafe.config.Config;

import lt.ekgame.storasbot.StorasBot;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class GameChanger extends ListenerAdapter {
	
	private String game;
	
	public GameChanger(Config config) {
		game = StorasBot.getConfig().getString("general.game");
	}

	@Override
	public void onReady(ReadyEvent event) {
		StorasBot.getJDA().getAccountManager().setGame(game);
	}

}

package lt.ekgame.storasbot.commands.settings;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.plugins.Setting;

@CommandReference(isGuild=true, labels = {"asih"})
public class CommandASIH extends CommandTogglableSetting {

	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$asih enable - Enable \"Anti shit image hosts\".\n"
			 + "$asih disable - Disable the feature.\n"
			 + "\n"
			 + "\"Anti shit image hosts\" tries to discourage users from posting non-direct image links.";
	}

	@Override
	public String onEnable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.ASIH, true);
		return "\"Anti shit image hosts\" enabled.";
	}

	@Override
	public String onDisable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.ASIH, false);
		return "\"Anti shit image hosts\" disabled.";
	}

}

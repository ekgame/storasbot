package lt.ekgame.storasbot.commands.settings;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.plugins.Setting;

@CommandReference(isGuild=true, labels = {"beatmap-examiner"})
public class CommandBeatmapExaminer extends CommandTogglableSetting {
		
	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$beatmap-examiner enable - Enable beatmap examining.\n"
			 + "$beatmap-examiner disable - Disable beatmap examining.\n"
			 + "\n"
			 + "If enabled, beatmap examiner will post information about a beatmap when a link is posted.";
	}

	@Override
	public String onEnable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.BEATMAP_EXAMINER, true);
		return "Beatmap examining enabled.";
	}

	@Override
	public String onDisable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.BEATMAP_EXAMINER, false);
		return "Beatmap examining disabled.";
	}

}
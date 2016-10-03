package lt.ekgame.storasbot.commands.settings;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.plugins.Setting;

@CommandReference(isGuild=true, labels = {"beatmap-analyzer"})
public class CommandBeatmapAnalyzer extends CommandTogglableSetting {

	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$beatmap-analyzer enable - Enable beatmap analyzing.\n"
			 + "$beatmap-analyzer disable - Disable beatmap analyzing.\n"
			 + "\n"
			 + "If enabled, when a beatmap link is posted, it will attempt to analyze the beatmap."
			 + " Requires beatmap-examiner to be enabled to work."
			 + " Doesn't work with osu!catch maps yet. Only works for \"osu.ppy.sh/b/\" links.";
	}

	@Override
	public String onEnable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.BEATMAP_ANALYZER, true);
		return "Beatmap analyzing enabled.";
	}

	@Override
	public String onDisable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.BEATMAP_ANALYZER, false);
		return "Beatmap analyzing disabled.";
	}

}

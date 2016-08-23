package lt.ekgame.storasbot.commands.settings;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.plugins.Setting;

@CommandReference(isGuild=true, labels = {"bancho-status"})
public class CommandBanchoStatus extends CommandTogglableSetting {
	
	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$bancho-status enable - Enable osu!Bancho status notifications.\n"
			 + "$bancho-status disable - Disable notifications.\n"
			 + "\n"
			 + "Notifies when osu!Bancho goes offline and back up. Only notifies the channel where this was enabled in.";
	}

	@Override
	public String onEnable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.BANCHO_STATUS, true);
		context.getSettings().update(Setting.BANCHO_STATUS_CHANNEL, context.getChannel().getId());
		return "osu!Bancho status notifications enabled for " + context.getTextChannel().getAsMention() + ".";
	}

	@Override
	public String onDisable(CommandFlags flags, BotCommandContext context) {
		context.getSettings().update(Setting.BANCHO_STATUS, false);
		return "osu!Bancho status notifications disabled.";
	}

}

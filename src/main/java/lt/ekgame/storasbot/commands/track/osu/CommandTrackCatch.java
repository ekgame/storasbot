package lt.ekgame.storasbot.commands.track.osu;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.utils.osu.OsuMode;

@CommandReference(isGuild=true, labels = {"track-catch", "track-ctb", "t-c"})
public class CommandTrackCatch extends TrackOsuBase implements Command<BotCommandContext> {
	
	@Override
	public String getHelp(CommandFlags flags) {
		return getHelp("track-catch");
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		return handleOsuTrack(command, context, OsuMode.CATCH);
	}
}
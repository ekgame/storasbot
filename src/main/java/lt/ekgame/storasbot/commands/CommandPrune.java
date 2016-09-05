package lt.ekgame.storasbot.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.utils.Utils;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;

@CommandReference(isGuild=true, labels = {"prune", "clear"})
public class CommandPrune implements Command<BotCommandContext> {

	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$prune <number> [<user>]\n"
			 + "\n"
			 + "Removes last <number> of messages in the channel."
			 + " You can optionally provide a user who's messages should be deleted.";
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		// TODO: refactor to check if enough messages will be deleted before performing the action.
		Guild guild = context.getGuild();
		TextChannel channel = context.getTextChannel();
		User sender = context.getSender();
		
		if (!Utils.hasCommandPermission(channel, StorasDiscord.getJDA().getSelfInfo(), Permission.MESSAGE_MANAGE))
			return context.replyError("I don't have permissions to delete messages.");
		
		if (Utils.hasCommandPermission(channel, sender, Permission.MESSAGE_MANAGE)) {
			Optional<Integer> oNumber = command.getInteger();
			Optional<String> oUserRaw = command.getEverything();
			if (oNumber.isPresent()) {
				int number = oNumber.get();
				if (number < 2 || number > 100)
					return context.replyError("The number has to be between 2 and 100 (inclusive).");
				
				List<Message> recent = channel.getHistory().retrieve();
				List<Message> remove = new ArrayList<>();
				
				if (oUserRaw.isPresent()) {
					Optional<User> user = Utils.getUser(guild, oUserRaw.get());
					if (user.isPresent()) {
						for (Message message : recent) {
							if (remove.size() >= number)
								break;
							if (message.getAuthor().equals(user.get()))
								remove.add(message);
						}
						channel.deleteMessages(remove);
						return context.replyOk("Deleted **" + number + "** messages by " + user.get().getAsMention() + ".");
					}
					else {
						return context.replyError("Unknown user **" + Utils.escapeMarkdown(oUserRaw.get()) + "**.");
					}
				}
				else {
					for (Message message : recent) {
						if (remove.size() >= number)
							break;
						remove.add(message);
					}
					channel.deleteMessages(remove);
					return context.replyOk("Deleted **" + number + "** messages.");
				}
			}
			else {
				return context.replyError("You don't know what you're doing. Try `$help prune`.");
			}
		}
		else {
			return context.replyError("I'm sorry, " + sender.getAsMention() + ", I can't let you do that.");
		}
	}
}

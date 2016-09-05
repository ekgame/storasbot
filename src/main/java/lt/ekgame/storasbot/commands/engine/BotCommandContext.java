package lt.ekgame.storasbot.commands.engine;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.plugins.Settings;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;

public class BotCommandContext {
	
	private Message message;
	private Guild guild;
	private String label;
	
	private Message reply;
	
	public BotCommandContext(Message message, Guild guild, String label) {
		this.message = message;
		this.guild = guild;
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	public Message getMessage() {
		return message;
	}
	
	public MessageChannel getChannel() {
		return message.getChannel();
	}
	
	public PrivateChannel getPrivateChannel() {
		return (PrivateChannel)message.getChannel();
	}
	
	public TextChannel getTextChannel() {
		return (TextChannel)message.getChannel();
	}

	public User getSender() {
		return message.getAuthor();
	}

	public Guild getGuild() {
		return guild;
	}
	
	public Settings getSettings() {
		return StorasDiscord.getSettings(getGuild());
	}
	
	public void reply(String message) {
		if (reply == null) {
			StorasDiscord.sendMessage(getChannel(), message, (msg) -> {
				reply = msg;
			});
		}
		else {
			StorasDiscord.editMessage(reply, message);
		}
	}
	
	public CommandResult replyError(String message) {
		reply(":no_entry_sign: _" + message + "_");
		return CommandResult.FAIL;
	}
	
	public CommandResult replyOk(String message) {
		reply(":white_check_mark:  " + message);
		return CommandResult.FAIL;
	}
}

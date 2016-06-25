package lt.ekgame.storasbot.commands.engine;

import lt.ekgame.storasbot.StorasBot;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;

public class BotCommandContext {
	
	private Message message;
	private Guild guild;
	
	private Message reply;
	
	public BotCommandContext(Message message, Guild guild) {
		this.message = message;
		this.guild = guild;
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
	
	public void reply(String message) {
		if (reply == null) {
			StorasBot.sendMessage(getChannel(), message, (msg) -> {
				reply = msg;
			});
		}
		else {
			StorasBot.editMessage(reply, message);
		}
	}
}

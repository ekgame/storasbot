package lt.ekgame.storasbot.plugins;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lt.ekgame.storasbot.StorasDiscord;

import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class AntiShitImageHosts extends ListenerAdapter {
	
	private static Pattern HTTP_URL = Pattern.compile("((http[s]?):\\/)?\\/?([^:\\/\\s]+)(((\\/\\w+)*\\/)([\\w\\-\\.]+[^#?\\s]+)(.*)?(#[\\w\\-]+)?)");
	
	private static List<String> blacklist = Arrays.asList("prntscr.com", "prnt.sc", "gyazo.com");
	private static List<String> exceptions = Arrays.asList("image.prntscr.com", "i.gyazo.com");
	private static List<String> extensions = Arrays.asList("png", "jpg", "jpeg", "bmp", "gif", "mp4", "mpg", "mpeg", "mp3");
	
	private static String REPLY = "Please use direct links or <https://getsharex.com/>.";
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getAuthor().equals(StorasDiscord.getJDA().getSelfInfo()))
			return; // ignore own messages
		
		Boolean enabled = StorasDiscord.getSettings(event.getGuild()).get(Setting.ASIH, Boolean.class);
		if (!enabled)
			return; // don't process if disabled
		
		String content = event.getMessage().getContent();
		Matcher matcher = HTTP_URL.matcher(content);
		while (matcher.find()) {
			if (isBlacklisted(matcher.group(3).toLowerCase(), matcher.group(0).toLowerCase())) {
				StorasDiscord.sendMessage(event.getChannel(), event.getAuthor().getAsMention() + " " + REPLY);
				break;
			}
		}
	}
	
	private boolean isBlacklisted(String host, String fullLink) {
		for (String item : extensions)
			if (fullLink.endsWith(item))
				return false;
		
		for (String item : exceptions)
			if (host.contains(item))
				return false;
		
		for (String item : blacklist)
			if (host.contains(item))
				return true;
		
		return false;
	}
}

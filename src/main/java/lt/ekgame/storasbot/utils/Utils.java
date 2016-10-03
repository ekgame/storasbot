package lt.ekgame.storasbot.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Splitter;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.MultipartBody;

import lt.ekgame.storasbot.StorasDiscord;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.impl.JDAImpl;
import net.dv8tion.jda.exceptions.PermissionException;
import net.dv8tion.jda.handle.EntityBuilder;
import net.dv8tion.jda.requests.Requester;
import net.dv8tion.jda.utils.PermissionUtil;

public class Utils {
	
	public static List<String> listSplitMaxLength(List<String> list, int maxLength) {
		List<String> result = new ArrayList<>();
		for (String item : list) {
			if (item.length() > maxLength) {
				for (int i = 0; i < item.length(); i += maxLength) {
		        	String part = item.substring(i, Math.min(item.length(), i + maxLength));
		        	result.add(part);
		        }
			}
			else result.add(item);
		}
		return result;
	}
	
	public static List<String> messageSplit(String message, int maxLength) {
		List<String> list = Splitter.on('\n').splitToList(message);
		list = listSplitMaxLength(list, maxLength-1);
		
		ListIterator<String> iterator = list.listIterator();
		
		List<String> result = new ArrayList<>();
		String current = "";
		
		while (iterator.hasNext()) {
			String line = iterator.next();
			if (current.length() + line.length() + 1 > maxLength) {
				iterator.previous();
				result.add(current);
				current = "";
			}
			else {
				if (!current.isEmpty())
					current += "\n";
				current += line;
			}
		}
		result.add(current);
		return result;
	}
	
	
	
	public static String trimLength(String text, int maxLength, String denoter) {
		if (text.length() <= maxLength)
			return text;
		else 
			return text.substring(0, maxLength - denoter.length()) + denoter;
	}
	
	public static String escapeMarkdown(String text) {
		return text.replace("*", "\\*").replace("`", "\u200B`\u200B").replace("&#39;", "'");
	}
	
	public static String escapeMarkdownBlock(String text) {
		return text.replace("`", "\u200B`\u200B").replace("&#39;", "'");
	}
	
	public static <T> void addAllUniques(List<T> master, List<T> donor) {
		for (T o : donor) 
			if (!master.contains(o))
				master.add(o);
	}
	
	public static String toTimeString(long miliseconds) {
		int days = (int) (miliseconds / (1000*60*60*24));
		miliseconds = miliseconds % (1000*60*60*24);
		int hours = (int) (miliseconds / (1000*60*60));
		miliseconds = miliseconds % (1000*60*60);
		int minutes = (int) (miliseconds / (1000*60));
		miliseconds = miliseconds % (1000*60);
		int seconds = (int) (miliseconds / (1000));
		miliseconds = miliseconds % (1000);
		
		String sDays = days <= 0 ? null : (days + " day" + (days > 1 ? "s" : ""));
		String sHours = hours <= 0 ? null : (hours + " hour" + (hours > 1 ? "s" : ""));
		String sMinutes = minutes <= 0 ? null : (minutes + " minute" + (minutes > 1 ? "s" : ""));
		String sSeconds = seconds <= 0 ? null : (seconds + " second" + (seconds > 1 ? "s" : ""));
		
		List<String> strings = new ArrayList<>();
		if (sDays != null) strings.add(sDays);
		if (sHours != null) strings.add(sHours);
		if (sMinutes != null) strings.add(sMinutes);
		if (sSeconds != null) strings.add(sSeconds);
		
		return strings.stream().collect(Collectors.joining(" "));
	}
	
	public static String numberToDual(int number) {
		return (number > 9 ? "": "0") + number;
	}
	
	public static String compactTimeString(int seconds) {
		int mins = seconds / 60;
		int secs = seconds % 60;
		
		return numberToDual(mins) + ":" + numberToDual(secs);
	}
	
	public static Optional<User> getUser(Guild guild, String username) {
		for (User user : guild.getUsers()) {
			String nickname = guild.getNicknameForUser(user);
			if (user.getUsername().equals(username)
			 || ("@"+user.getUsername()).equals(username)
			 ||(nickname != null && nickname.equals(username))
			 || user.getId().equals(username)) {
				return Optional.of(user);
			}
		}
		return Optional.empty();
	}
	
	public static Message sendImage(Guild guild, Channel channel, BufferedImage image, Message message)
    {
		guild.checkVerification();
        if (!PermissionUtil.checkPermission(guild.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, channel))
            throw new PermissionException(Permission.MESSAGE_WRITE);
        if (!PermissionUtil.checkPermission(guild.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES, channel))
            throw new PermissionException(Permission.MESSAGE_ATTACH_FILES);
        if(image == null)
            throw new IllegalArgumentException("Provided image is null!");

        JDAImpl api = (JDAImpl) guild.getJDA();
        try
        {
        	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        	ImageIO.write(image, "png", bytes);
        	bytes.flush();
            MultipartBody body = Unirest.post(Requester.DISCORD_API_PREFIX + "channels/" + channel.getId() + "/messages")
                    .header("authorization", guild.getJDA().getAuthToken())
                    .header("user-agent", Requester.USER_AGENT)
                    .field("empty", "");
            body.field("file", bytes.toByteArray(), ContentType.create("image/png"), "imge.png");
            
            bytes.close();
            
            if (message != null)
                body.field("content", message.getRawContent()).field("tts", message.isTTS());

            String dbg = String.format("Requesting %s -> %s\n\tPayload: image, message: %s, tts: %s\n\tResponse: ",
                    body.getHttpRequest().getHttpMethod().name(), body.getHttpRequest().getUrl(), message == null ? "null" : message.getRawContent(), message == null ? "N/A" : message.isTTS());
            String requestBody = body.asString().getBody();
            Requester.LOG.trace(dbg + body);

            try
            {
                JSONObject messageJson = new JSONObject(requestBody);
                return new EntityBuilder(api).createMessage(messageJson);
            }
            catch (JSONException e)
            {
                Requester.LOG.fatal("Following json caused an exception: " + requestBody);
                Requester.LOG.log(e);
            }
        }
        catch (UnirestException | IOException e)
        {
            Requester.LOG.log(e);
        }
        return null;
    }

    public static void sendImageAsync(Guild guild, Channel channel, BufferedImage image, String message, Consumer<Message> callback)
    {
    	guild.checkVerification();
        if (!PermissionUtil.checkPermission(guild.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE, channel))
            throw new PermissionException(Permission.MESSAGE_WRITE);
        if (!PermissionUtil.checkPermission(guild.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES, channel))
            throw new PermissionException(Permission.MESSAGE_ATTACH_FILES);

        Thread thread = new Thread(() ->
        {
        	Message messageReturn = sendImage(guild, channel, image, new MessageBuilder().appendString(message).build());
			if (callback != null)
				callback.accept(messageReturn);
        });
        thread.setName("TextChannelImpl sendFileAsync Channel: " + channel.getId());
        thread.setDaemon(true);
        thread.start();
    }
    
    public static boolean hasCommandPermission(Guild guild, User user, Permission perm) {
    	if (StorasDiscord.isOperator(user))
    		return true;
    	return PermissionUtil.checkPermission(user, perm, guild);
    }
    
    public static boolean hasCommandPermission(Channel channel, User user, Permission perm) {
    	if (StorasDiscord.isOperator(user))
    		return true;
    	return PermissionUtil.checkPermission(user, perm, channel);
    }
}

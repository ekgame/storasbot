package lt.ekgame.storasbot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;

public class Utils {
	
	public static String escapeMarkdown(String text) {
		return text.replace("*", "\\*").replace("`", "\u200B`\u200B");
	}
	
	public static String escapeMarkdownBlock(String text) {
		return text.replace("`", "\u200B`\u200B");
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
}

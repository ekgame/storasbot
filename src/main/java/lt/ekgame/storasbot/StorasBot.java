package lt.ekgame.storasbot;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.security.auth.login.LoginException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import lt.ekgame.storasbot.commands.engine.CommandListener;
import lt.ekgame.storasbot.plugins.AntiShitImageHosts;
import lt.ekgame.storasbot.plugins.BanchoStatusChecker;
import lt.ekgame.storasbot.plugins.BeatmapLinkExaminer;
import lt.ekgame.storasbot.plugins.GameChanger;
import lt.ekgame.storasbot.plugins.osu_top.OsuTracker;
import lt.ekgame.storasbot.utils.osu.OsuApi;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.utils.SimpleLog;

public class StorasBot {
	
	private static JDA client;
	private static Database database;
	private static Config config;
	private static OsuApi osuApi;
	private static CommandListener commandHandler;
	private static List<String> operators = new ArrayList<>();
	
	public static void main(String... args) throws SQLException, LoginException, IllegalArgumentException {
		try {
			config = ConfigFactory.parseFile(new File(args[0])); // Very important that this is first
			database = new Database(config);
			database.testConnection();
			
			String token = config.getString("api.discord");
			operators = config.getStringList("general.operators");
			client = new JDABuilder().setBotToken(token).buildAsync();
			
			osuApi = new OsuApi(config);
			
			client.addEventListener(new OsuTracker());
			client.addEventListener(commandHandler = new CommandListener());
			client.addEventListener(new BeatmapLinkExaminer());
			client.addEventListener(new AntiShitImageHosts());
			client.addEventListener(new BanchoStatusChecker(config));
			client.addEventListener(new GameChanger(config));
		} catch (Exception e) {
			SimpleLog.getLog("Initialization").fatal("Initialization failed.");
			e.printStackTrace();
		}
	}
	
	public static JDA getJDA() {
		return client;
	}
	
	public static Database getDatabase() {
		return database;
	}
	
	public static Config getConfig() {
		return config;
	}
	
	public static OsuApi getOsuApi() {
		return osuApi;
	}
	
	public static CommandListener getCommandHandler() {
		return commandHandler;
	}
	
	public static boolean isOperator(User user) {
		return operators.contains(user.getId());
	}
	
	public static String getPrefix(Guild guild) {
		String nickname = guild.getNicknameForUser(client.getSelfInfo());
		return "@" + (nickname != null ? nickname : client.getSelfInfo().getUsername());
	}
	
	public static void sendMessage(MessageChannel messageChannel, String message, Consumer<Message> consumer) {
		messageChannel.sendMessageAsync(message, consumer);
	}
	
	public static void sendMessage(MessageChannel messageChannel, String message) {
		messageChannel.sendMessageAsync(message, null);
	}
	
	public static void editMessage(Message message, String newText, Consumer<Message> consumer) {
		message.updateMessageAsync(newText, consumer);
	}
	
	public static void editMessage(Message message, String newText) {
		message.updateMessageAsync(newText, null);
	}
}

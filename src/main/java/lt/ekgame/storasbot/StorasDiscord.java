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
import lt.ekgame.storasbot.plugins.GuildSettings;
import lt.ekgame.storasbot.plugins.Settings;
import lt.ekgame.storasbot.plugins.osu_top.OsuTracker;
import lt.ekgame.storasbot.utils.osu.OsuApi;
import lt.ekgame.storasbot.utils.osu.OsuUserCatche;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.utils.SimpleLog;
import net.dv8tion.jda.utils.SimpleLog.Level;

public class StorasDiscord {
	
	private static JDA client;
	private static Database database;
	private static Config config;
	private static OsuApi osuApi;
	private static OsuUserCatche osuUserCatche;
	private static CommandListener commandHandler;
	private static GuildSettings guildSettings;
	private static List<String> operators = new ArrayList<>();
	
	public static void main(String... args) throws SQLException, LoginException, IllegalArgumentException {
		try {
			if (args.length > 1 && args[1].equalsIgnoreCase("-d"))
				SimpleLog.LEVEL = Level.DEBUG;
			else
				SimpleLog.LEVEL = Level.INFO;
			
			config = ConfigFactory.parseFile(new File(args[0])); // Very important that this is first
			database = new Database(config);
			database.testConnection();
			osuApi = new OsuApi(config);
			osuUserCatche = new OsuUserCatche();
			guildSettings = new GuildSettings(database);
			
			String token = config.getString("api.discord");
			operators = config.getStringList("general.operators");
			client = new JDABuilder().setBotToken(token)
				.addListener(new OsuTracker(osuUserCatche))
				.addListener(commandHandler = new CommandListener())
				.addListener(new BeatmapLinkExaminer())
				.addListener(new AntiShitImageHosts())
				.addListener(new BanchoStatusChecker(config))
				.addListener(new GameChanger(config))
				.buildAsync();
			
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
	
	public static OsuUserCatche getOsuUserCatche() {
		return osuUserCatche;
	}
	
	public static CommandListener getCommandHandler() {
		return commandHandler;
	}
	
	public static Settings getSettings(Guild guild) {
		return guildSettings.getSettings(guild);
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
	
	public static void sendFile(MessageChannel messageChannel, String message, File file, Consumer<Message> consumer) {
		messageChannel.sendFileAsync(file, new MessageBuilder().appendString(message).build(), consumer);
	}
	
	public static void sendFile(MessageChannel messageChannel, String message, File file) {
		messageChannel.sendFileAsync(file, new MessageBuilder().appendString(message).build(), null);
	}
	
	public static void editMessage(Message message, String newText, Consumer<Message> consumer) {
		message.updateMessageAsync(newText, consumer);
	}
	
	public static void editMessage(Message message, String newText) {
		message.updateMessageAsync(newText, null);
	}
}

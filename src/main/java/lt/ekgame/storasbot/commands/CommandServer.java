package lt.ekgame.storasbot.commands;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;
import lt.ekgame.storasbot.utils.Utils;

import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;

@CommandReference(isGuild=true, labels = {"server", "guild"})
public class CommandServer implements Command<BotCommandContext> {
	
	private static Mustache msgServer;
	private static String messageFormat = 
			"```ruby\n"
		  + "{{server-name}}\n"
		  + "users: {{users-online}} online, {{users-away}} away, {{users-offline}} offline, {{users-bots}} bots\n"
		  + "verification_level: {{verification}}\n"
		  + "server_owner: {{owner}}\n"
		  + "most_used_bins: {{bins-info}}\n"
		  + "```";
	
	//private static int IMAGE_WIDTH = 500, IMAGE_HEIGHT = 200;
	
	static {
		MustacheFactory mf = new DefaultMustacheFactory();
		msgServer = mf.compile(new StringReader(messageFormat), "server");
	}
	
	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$server\n"
			 + "\n"
			 + "Displays information about this server.";
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		Guild guild = context.getGuild();
		//TextChannel channel = context.getTextChannel();
		
		long users   = guild.getUsers().size();
		long online  = guild.getUsers().stream().filter(u->u.getOnlineStatus()==OnlineStatus.ONLINE).count();
		long away    = guild.getUsers().stream().filter(u->u.getOnlineStatus()==OnlineStatus.AWAY).count();
		long offline = guild.getUsers().stream().filter(u->u.getOnlineStatus()==OnlineStatus.OFFLINE).count();
		long bots    = guild.getUsers().stream().filter(u->u.isBot()).count();
		
		Map<String, String> scope = new HashMap<>();
		scope.put("server-name", Utils.escapeMarkdownBlock(guild.getName()));
		scope.put("verification", guild.getVerificationLevel().toString());
		scope.put("owner", Utils.escapeMarkdownBlock(guild.getOwner().getUsername() + "#" + guild.getOwner().getDiscriminator()));
		scope.put("users", String.valueOf(users));
		scope.put("users-online", String.valueOf(online));
		scope.put("users-away", String.valueOf(away));
		scope.put("users-offline", String.valueOf(offline));
		scope.put("users-bots", String.valueOf(bots));
		scope.put("bins-info", Utils.escapeMarkdownBlock(getBinsInfo(guild)));
		
		StringWriter writer = new StringWriter();
		msgServer.execute(writer, scope);
		writer.flush();
		String message = writer.toString();
		context.reply(message);
		//Utils.sendImageAsync(guild, channel, getChartImage(guild), message, null);
		
		return CommandResult.OK;
	}
	
	private String getBinsInfo(Guild guild) {
		String binsInfo = "UNKNOWN";
		try {
			List<Pair<String, Long>> mostUsedBins = StorasDiscord.getDatabase().getTopBins(guild);
			if (mostUsedBins.size() == 0) {
				binsInfo = "NONE DEFINED";
			}
			else {
				binsInfo = mostUsedBins.stream()
					.map((o) -> String.format("%s (%d)", o.getLeft(), o.getRight()))
					.collect(Collectors.joining(", "));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return binsInfo;
	}
	
	/*private BufferedImage getChartImage(Guild guild) {
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		getMessageChart(guild).draw(graphics, new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()));
		graphics.dispose();
		return image;
	}
	
	private JFreeChart getMessageChart(Guild guild) {
		final TimeSeries series = new TimeSeries("Random Data");
		Hour current = new Hour();
		double value = 100.0;
		for (int i = 0; i < 400; i++) {
			try {
				value = value + Math.random() - 0.5;
				series.add(current, new Double(value));
				current = (Hour) current.next();
			} catch (SeriesException e) {
				System.err.println("Error adding to series");
			}
		}

		TimeSeriesCollection dataset = new TimeSeriesCollection(series);
		
		return ChartFactory.createTimeSeriesChart("Computing Test", "Seconds", "Value", dataset, false, false, false);
	}*/
}

package lt.ekgame.storasbot.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.typesafe.config.Config;

import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.utils.Utils;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.utils.SimpleLog;

public class BanchoStatusChecker extends Thread implements EventListener {
	
	public static final SimpleLog LOG = SimpleLog.getLog("Bancho Checker");
	
	private boolean enabled;
	private int requiredSuccesses;
	private int requiredFailures;
	private int timeout;
	private Mustache msgOffline, msgOnline;
	private List<List<String>> tags;
	
	private HttpClient httpClient;
	private URI requestUri;
	
	private int successes = 0;
	private int failures = 0;
	private boolean online = true; // assume online by default
	private long timestampOffline = 0;
	private List<String> currentTags;
	
	@SuppressWarnings("unchecked")
	public BanchoStatusChecker(Config config) {
		enabled = config.getBoolean("bancho.enabled");
		timeout = config.getInt("bancho.timeout")*1000;
		String host = config.getString("bancho.host");
		
		requiredSuccesses = config.getInt("bancho.successes");
		requiredFailures = config.getInt("bancho.failures");
		
		MustacheFactory mf = new DefaultMustacheFactory();
		msgOffline = mf.compile(new StringReader(config.getString("bancho.msg-offline")), "offline");
		msgOnline = mf.compile(new StringReader(config.getString("bancho.msg-online")), "online");
		
		tags = (List<List<String>>) config.getAnyRefList("bancho.tags");
	    
		try {
			RequestConfig defaultRequestConfig = RequestConfig.custom()
			    .setSocketTimeout(timeout)
			    .setConnectTimeout(timeout)
			    .setConnectionRequestTimeout(timeout)
			    .build();
		
			httpClient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
			requestUri = new URI(host);
		} catch (URISyntaxException e) {
			LOG.fatal("Invalid URI: " + host);
		}
	}
	
	public void onEvent(Event event) {
		if (event instanceof ReadyEvent) {
			if (requestUri != null && enabled)
				start();
		}
	}
		
	public void run() {
		LOG.info("Bancho checker started: " + requestUri.toString());
		while (true) {
			try {
				HttpGet request = new HttpGet(requestUri);
				try {
					httpClient.execute(request);
					failures = 0;
					successes++;
				} catch (IOException e) {
					failures++;
					LOG.warn("Bancho failed to respoind(" + failures + "): " + e.getMessage());
					successes = 0;
				}
				finally {
					request.reset();
				}
				
				if (successes >= requiredSuccesses && !online) {
					online = true;
					long timeOffline = System.currentTimeMillis() - timestampOffline;
					announceOnline(timeOffline);
					LOG.info("Bancho is back online.");
				}
				
				if (failures >= requiredFailures && online) {
					online = false;
					timestampOffline = System.currentTimeMillis() - failures*timeout;
					currentTags = tags.get(new Random().nextInt(tags.size()));
					announceOffline();
					LOG.info("Bancho is offline.");
				}
				
				if (online && failures == 0) {
					Thread.sleep(10000);
				}
				else {
					Thread.sleep(500);
				}
			} catch (InterruptedException e) {}
		}
	}
	
	private List<TextChannel> getChannels() {
		List<TextChannel> channels = new ArrayList<>();
		for (Guild guild : StorasDiscord.getJDA().getGuilds()) {
			Settings settings = StorasDiscord.getSettings(guild);
			if (settings.get(Setting.BANCHO_STATUS, Boolean.class)) {
				String channelId = settings.get(Setting.BANCHO_STATUS_CHANNEL, String.class);
				TextChannel channel = StorasDiscord.getJDA().getTextChannelById(channelId);
				if (channel != null)
					channels.add(channel);
			}
		}
		LOG.debug("Got channels: " + channels.size() + " - " + channels.stream().map(o->o.getName()).collect(Collectors.joining(", ")));
		
		return channels;
	}
	
	private void announceOffline() {
		Map<String, String> scope = new HashMap<>();
		scope.put("tag-offline", currentTags.get(0));
		StringWriter writer = new StringWriter();
		msgOffline.execute(writer, scope);
		writer.flush();
		String message = writer.toString();
		
		for (TextChannel channel : getChannels()) {
			StorasDiscord.sendMessage(channel, message);
		}
	}
	
	private void announceOnline(long timeOffline) {
		Map<String, String> scope = new HashMap<>();
		scope.put("tag-online", currentTags.get(1));
		scope.put("time", Utils.toTimeString(timeOffline));
		StringWriter writer = new StringWriter();
		msgOnline.execute(writer, scope);
		writer.flush();
		String message = writer.toString();
		
		for (TextChannel channel : getChannels())
			StorasDiscord.sendMessage(channel, message);
	}
}

package lt.ekgame.storasbot.commands;

import java.io.IOException;
import java.util.Optional;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;

@CommandReference(isGuild=true, isPrivate=true, labels = {"xkcd"})
public class CommandXKCD implements Command<BotCommandContext>  {
	
	private static String XKCD_ID = "http://m.xkcd.com/%s/";
	private static String XKCD_RANDOM = "http://c.xkcd.com/random/mobile_comic/";
	
	@Override
	public String getHelp() {
		return "Usage:\n"
			 + "$xkcd [<id>]\n"
			 + "\n"
			 + "Shows a specified xkcd commic. If there is no id specified, a random one will be shown.\n";
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		Optional<String> rawId = command.getToken();
		String link = XKCD_RANDOM;
		if (rawId.isPresent())
			link = String.format(XKCD_ID, rawId.get());
		
		try {
			Document doc = Jsoup.connect(link).get();
			Elements elements = doc.select("img[id=comic]");
			if (elements.size() == 0) {
				context.reply("_That commic can not be diplayed._");
				return CommandResult.FAIL;
			}
			
			Element element = elements.get(0);
			String image = element.attr("src");
			if (image.isEmpty()) {
				context.reply("_That commic can not be diplayed._");
				return CommandResult.FAIL;
			}
			
			context.reply(image);
			return CommandResult.OK;
		}
		catch (HttpStatusException e) {
			context.reply("_xkcd responded with status code: " + e.getStatusCode() + "._");
			return CommandResult.FAIL;
		}
		catch (IOException e) {
			context.reply("_Something went wrong, try again later._");
			return CommandResult.FAIL;
		}
	}
}

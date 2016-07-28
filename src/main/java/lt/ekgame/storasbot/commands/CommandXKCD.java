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
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;

@CommandReference(isGuild=true, isPrivate=true, labels = {"xkcd"})
public class CommandXKCD implements Command<BotCommandContext>  {
	
	private static String XKCD_ID = "http://m.xkcd.com/%s/";
	private static String XKCD_RANDOM = "http://c.xkcd.com/random/mobile_comic/";
	
	@Override
	public String getHelp(CommandFlags flags) {
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
			if (elements.size() == 0)
				return context.replyError("That commic can not be diplayed.");
			
			Element element = elements.get(0);
			String image = element.attr("src");
			if (image.isEmpty())
				return context.replyError("That comic can not be diplayed.");
			
			context.reply(image);
			return CommandResult.OK;
		}
		catch (HttpStatusException e) {
			return context.replyError("xkcd responded with status code: " + e.getStatusCode() + ".");
		}
		catch (IOException e) {
			return context.replyError("Something went wrong, try again later.");
		}
	}
}

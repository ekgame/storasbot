package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuLeaderboardScraper {
	
	private static final SimpleLog LOG = SimpleLog.getLog("Top Worker");
	private static final String RANK_LISTING = "http://osu.ppy.sh/p/pp?c=%s&m=%d&page=%d";
	private static final String RANK_LISTING_GLOBAL = "http://osu.ppy.sh/p/pp?&m=%d&page=%d";
	
	public static List<OsuPlayer> scrapePlayers(String country, OsuMode mode, int numTop) throws IOException {
		int pages = (numTop / 50) + 1;
		List<OsuPlayer> result = new ArrayList<>();
		for (int i = 0; i < pages; i++)
			result.addAll(scrapePlayers(i+1, country, mode));
		return result;
	}
	
	private static List<OsuPlayer> scrapePlayers(int page, String country, OsuMode mode) throws IOException {
		List<OsuPlayer> result = new ArrayList<>();
		String url;
		if (country == null)
			url = String.format(RANK_LISTING_GLOBAL, mode.getValue(), page);
		else
			url = String.format(RANK_LISTING, country, mode.getValue(), page);
				
		Document doc = Jsoup.connect(url).get();
		Elements table = doc.select(".beatmapListing tbody");
		if (table.size() < 1) {
			LOG.fatal("No table found. " + country);
			return result;
		}
		
		Element tbody = table.get(0);
		if (tbody.children().size() == 0) {
			LOG.fatal("Invalid table size." + country);
			return result;
		}
		
		for (int i = 1; i < tbody.children().size(); i++) {
			Element row = tbody.children().get(i);
			String onclick = row.attr("onclick");
			String userId = onclick.substring(22, onclick.length() - 1);
			String performanceRaw = row.children().get(4).text();
			String accuracyRaw = row.children().get(2).text();
			String username = row.children().get(1).text();
			double accuracy = Double.parseDouble(accuracyRaw.substring(0, accuracyRaw.length()-1));
			int performance = Integer.parseInt(performanceRaw.substring(0, performanceRaw.length()-2).replace(",", ""));
			result.add(new OsuPlayer(OsuPlayerIdentifier.of(userId, mode), username, country, (page-1)*50+i, performance, accuracy));
		}
		return result;
	}
}

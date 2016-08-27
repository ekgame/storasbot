package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuLeaderboardScraper {
	
	private static final SimpleLog LOG = SimpleLog.getLog("Leaderboard Scraper");
	private static final String RANK_LISTING = "http://osu.ppy.sh/p/pp?c=%s&m=%d&page=%d";
	private static final String RANK_LISTING_GLOBAL = "http://osu.ppy.sh/p/pp?&m=%d&page=%d";
	
	private ExecutorService workers;
	private List<Future<OsuUpdatableLeaderboard>> futures = new ArrayList<>();
	
	public OsuLeaderboardScraper(int numWorkers) {
		this.workers = Executors.newFixedThreadPool(numWorkers);
	}
	
	public void submit(OsuUpdatableLeaderboard updatable) {
		futures.add(workers.submit(new Worker(updatable)));
	}
	
	public List<OsuUpdatableLeaderboard> getLeaderboards() throws InterruptedException {
		workers.shutdown();
		workers.awaitTermination(1, TimeUnit.HOURS);
		List<OsuUpdatableLeaderboard> result = new ArrayList<>();
		for (Future<OsuUpdatableLeaderboard> future : futures) {
			try {
				result.add(future.get());
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private class Worker implements Callable<OsuUpdatableLeaderboard> {

		private OsuUpdatableLeaderboard updatable;

		public Worker(OsuUpdatableLeaderboard updatable) {
			this.updatable = updatable;
		}

		@Override
		public OsuUpdatableLeaderboard call() {
			try {
				LOG.debug("Country " + updatable.getCountry() + " " + updatable.getMode());
				updatable.setPlayers(scrapePlayers(updatable));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return updatable;
		}
		
		public List<OsuPlayer> scrapePlayers(OsuUpdatableLeaderboard updatable) throws IOException {
			List<OsuPlayer> result = new ArrayList<>();
			int remaining = updatable.getNumTop();
			try {
				for (int i = 0; i < 3; i++) {
					List<OsuPlayer> players = scrapePlayers(i+1, updatable, remaining);
					result.addAll(players);
					remaining -= players.size();
					if (remaining <= 0)
						break;
				}
				Thread.sleep(1000);
			} catch (SocketTimeoutException e) {
				LOG.debug("Timed out while scraping " + updatable.getCountry());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return result;
		}
		
		private List<OsuPlayer> scrapePlayers(int page, OsuUpdatableLeaderboard updatable, int remaining) throws IOException {
			List<OsuPlayer> result = new ArrayList<>();
			String url;
			if (updatable.getCountry() == null)
				url = String.format(RANK_LISTING_GLOBAL, updatable.getMode().getValue(), page);
			else
				url = String.format(RANK_LISTING, updatable.getCountry(), updatable.getMode().getValue(), page);
					
			Document doc = Jsoup.connect(url).timeout(10000).get();
			Elements table = doc.select(".beatmapListing tbody");
			if (table.size() < 1) {
				LOG.fatal("No table found. " + updatable.getCountry());
				return result;
			}
			
			Element tbody = table.get(0);
			if (tbody.children().size() == 0) {
				LOG.fatal("Invalid table size. " + updatable.getCountry());
				return result;
			}
			
			for (int i = 1; i < tbody.children().size(); i++) {
				Element row = tbody.children().get(i);
				String username = row.children().get(1).text();
				boolean isInactive = row.child(1).child(1).attr("style").equals("color:gray");
				if (isInactive) {
					remaining--;
					if (remaining <= 0)
						break;
					// Skip checks for inactive players (no new top play in 2 months?)
					// because their web stats sometimes don't match API results
					// and forces the updater to keep checking the same player over and over again.
					continue;
				}
				String onclick = row.attr("onclick");
				String userId = onclick.substring(22, onclick.length() - 1);
				String performanceRaw = row.children().get(4).text();
				String accuracyRaw = row.children().get(2).text();
				
				double accuracy = Double.parseDouble(accuracyRaw.substring(0, accuracyRaw.length()-1));
				int performance = Integer.parseInt(performanceRaw.substring(0, performanceRaw.length()-2).replace(",", ""));
				result.add(new OsuPlayer(OsuPlayerIdentifier.of(userId, updatable.getMode()), username, updatable.getCountry(), (page-1)*50+i, performance, accuracy));
				remaining--;
				if (remaining <= 0)
					break;
			}
			return result;
		}
	}
}

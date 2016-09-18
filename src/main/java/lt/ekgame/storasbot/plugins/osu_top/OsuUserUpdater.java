package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;

import lt.ekgame.storasbot.Database;
import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuPlayerIdentifier;
import lt.ekgame.storasbot.utils.osu.OsuScore;
import lt.ekgame.storasbot.utils.osu.OsuUserCatche;
import lt.ekgame.storasbot.utils.osu.OsuUtils;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuUserUpdater {
	
	private static SimpleLog LOG = SimpleLog.getLog("User updater");
	
	private ExecutorService workers;
	private OsuUserCatche catche;
	
	public OsuUserUpdater(int numWorkers, OsuUserCatche catche) {
		this.workers = Executors.newFixedThreadPool(numWorkers);
		this.catche = catche;
	}
	
	public void submit(OsuUpdatablePlayer updatable, boolean sendNotifications) {
		workers.submit(new Worker(updatable, sendNotifications));
	}
	
	public void awaitTermination() throws InterruptedException {
		workers.shutdown();
		workers.awaitTermination(1, TimeUnit.HOURS);
	}
	
	private class Worker implements Runnable {
		
		private OsuUpdatablePlayer updatable;
		private OsuPlayerIdentifier identifier;
		private boolean sendNotifications;
		
		public Worker(OsuUpdatablePlayer updatable, boolean sendNotifications) {
			this.updatable = updatable;
			this.identifier = updatable.getIdentifier();
			this.sendNotifications = sendNotifications;
		}

		@Override
		public void run() {
			try {
				OsuPlayer catched = catche.getPlayer(identifier);
				OsuPlayer countryPlayer = updatable.getCountryPlayer();

				if (catched == null)
					updateNewPlayer(new OsuPlayer(identifier));
				else if (countryPlayer == null)
					updateIndividual(catched, updatable);
				else 
					updateFromCountry(catched, countryPlayer, updatable);
				
			} catch (IOException | SQLException e) {
				LOG.info(e.getClass().getName() + ": " + e.getMessage());
			}
		}
		
		private void updateFromCountry(OsuPlayer catched, OsuPlayer known, OsuUpdatablePlayer updatable) throws SQLException, IOException {
			int catchedPP = (int) Math.round(catched.getPerformance());
			int roundedPP = (int) Math.round(known.getPerformance());
			boolean differentPP = catchedPP != roundedPP;
			
			double catchedAcc = catched.getAccuracy();
			double currentAcc = known.getAccuracy();
			boolean differentAcc = !Precision.equals(catchedAcc, currentAcc, 0.01);
			
			if (differentPP || differentAcc) {
				OsuPlayer updated = known.getFromAPI();
				LOG.info(updated.getUsername() + ": (cou) checking scores " + differentAcc + " " + differentPP + " " + catchedAcc + " " + currentAcc);
				List<OsuScoreUpdate> scores = updateScores(catched, updated);
				
				if (sendNotifications)
					updatable.handleScoreUpdates(scores);
			}
		}

		private void updateIndividual(OsuPlayer catched, OsuUpdatablePlayer updatable) throws IOException, NumberFormatException, SQLException {
			OsuPlayer updated = catched.getFromAPI();
			LOG.debug(updated.getUsername() + ": (ind " + updated.getGamemode() + ") quick check");
			
			double catchedPP = catched.getPerformance();
			double knownPP = updated.getPerformance();
			boolean differentPP = !Precision.equals(catchedPP, knownPP, 0.0001);
			
			double catchedAcc = catched.getAccuracy();
			double currentAcc = updated.getAccuracy();
			boolean differentAcc = !Precision.equals(catchedAcc, currentAcc, 0.0001);
			
			if (differentPP || differentAcc) {
				LOG.info(updated.getUsername() + ": (ind) checking scores " + differentAcc + " " + differentPP + " " + catchedAcc + " " + currentAcc);
				List<OsuScoreUpdate> scores = updateScores(catched, updated);
				
				if (sendNotifications)
					updatable.handleScoreUpdates(scores);
			}
		}
		
		private List<OsuScoreUpdate> updateScores(OsuPlayer catched, OsuPlayer updated) throws SQLException, NumberFormatException, IOException {
			List<OsuScore> scores = OsuUtils.getScores(updated.getIdentifier());
			Map<String, OsuScore> catchedScores = updated.getCatchedScores().stream()
				.collect(Collectors.toMap(OsuScore::getBeatmapId, b->b));
			catche.updatePlayer(updated);
			
			List<OsuScore> newScores = new ArrayList<>();
			List<OsuScore> updatedScores = new ArrayList<>();
			List<OsuScoreUpdate> scoreUpdates = new ArrayList<>();
			
			for (OsuScore score : scores) {
				OsuScore catchedScore = catchedScores.get(score.getBeatmapId());
				if (catchedScore == null) {
					newScores.add(score);
					scoreUpdates.add(new OsuScoreUpdate(catched, updated, null, score));
					LOG.info("New score = " + score.getPerformance());
				}
				else if (!score.equals(catchedScore)) {
					updatedScores.add(score);
					scoreUpdates.add(new OsuScoreUpdate(catched, updated, catchedScore, score));
					LOG.info("Updated score = " + catchedScore.getPerformance() + " -> " + score.getPerformance());
				}
			}
			Database db = StorasDiscord.getDatabase();
			db.addScores(updated.getIdentifier(), newScores);
			db.updateScores(updated.getIdentifier(), updatedScores);
			db.updateTrackedUser(updated);
			
			return scoreUpdates;
		}
		
		private void updateNewPlayer(OsuPlayer player) throws IOException, SQLException {
			OsuPlayer updated = player.getFromAPI();
			LOG.info("New user: " + updated.getUsername());
			List<OsuScore> scores = OsuUtils.getScores(player.getIdentifier());
			StorasDiscord.getDatabase().addScores(updated.getIdentifier(), scores);
			StorasDiscord.getDatabase().addTrackedUser(updated);
			catche.updatePlayer(updated);
		}
	}
}

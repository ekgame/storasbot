package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;

import lt.ekgame.storasbot.Database;
import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.osu.OsuMode;
import lt.ekgame.storasbot.utils.osu.OsuPlayer;
import lt.ekgame.storasbot.utils.osu.OsuScore;
import lt.ekgame.storasbot.utils.osu.OsuUtils;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuPlayerUpdater {
	
	private List<UserToUpdate> users = new ArrayList<>();
	private int idleSleep;
	
	public OsuPlayerUpdater(int workers, int idleSleep) {
		this.idleSleep = idleSleep;
		for (int i = 0; i < workers; i++)
			Executors.newSingleThreadExecutor().execute(new Worker(i+1));
	}
	
	public void addPlayer(OsuPlayer user, OsuMode mode, Map<String, OsuPlayer> playerCatche, ScoreHandler handler) {
		synchronized (users) {
			if (users.stream().noneMatch(u->u.player.equals(user)))
				users.add(new UserToUpdate(user, playerCatche, handler));
		}
	}
	
	private void removeUser(UserToUpdate user) {
		synchronized (users) {
			users.remove(user);
		}
	}
	
	public void waitToComplete() {
		while (users.size() != 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}
	
	private UserToUpdate getUser() {
		synchronized (users) {
			Optional<UserToUpdate> oUser = users.stream()
					.filter(u->!u.isTaken())
					.findFirst();
			
			if (oUser.isPresent()) {
				oUser.get().setTaken();
				return oUser.get();
			}
			return null;
		}
	}

	class UserToUpdate {
		OsuPlayer player;
		OsuMode mode;
		Map<String, OsuPlayer> playerCatche;
		ScoreHandler handler;
		
		boolean taken = false;
		
		UserToUpdate(OsuPlayer player, Map<String, OsuPlayer> playerCatche, ScoreHandler handler) {
			this.player = player;
			this.mode = player.getGamemode();
			this.playerCatche = playerCatche;
			this.handler = handler;
		}
		
		public boolean isTaken() {
			return taken;
		}
		
		public void setTaken() {
			taken = true;
		}
	}
	
	class Worker implements Runnable {
		
		private SimpleLog LOG;
		int workerId;
		
		Worker(int workerId) {
			this.workerId = workerId;
			LOG = SimpleLog.getLog("Player updater #" + workerId);
		}
		
		@Override
		public void run() {
			while (true) {
				try {
					UserToUpdate user = getUser();
					if (user == null) {
						Thread.sleep(idleSleep);
						continue;
					}
					
					OsuPlayer catched = user.playerCatche.get(user.player.getUserId());
					if (catched == null) {
						LOG.info("Updating new player: " + user.player.getUsername());
						OsuPlayer updated = user.player.getFromAPI();
						List<OsuScore> scores = OsuUtils.getScores(updated.getUserId(), user.mode);
						StorasBot.getDatabase().addScores(updated.getIdentifier(), scores);
						StorasBot.getDatabase().addTrackedUser(updated);
					}
					else {
						int catchedPP = (int) Math.round(catched.getPerformance());
						int roundedPP = (int) Math.round(user.player.getPerformance());
						boolean differentPP = catchedPP != roundedPP;
						
						double catchedAcc = catched.getAccuracy();
						double currentAcc = user.player.getAccuracy();
						boolean differentAcc = !Precision.equals(catchedAcc, currentAcc, 0.01);
						
						if (differentPP || differentAcc) {
							LOG.info(user.player.getUsername() + ": checking scores " + differentAcc + " " + differentPP + " " + catchedAcc + " " + currentAcc);
							OsuPlayer updated = user.player.getFromAPI();
							List<OsuScore> scores = OsuUtils.getScores(updated.getUserId(), user.player.getGamemode());
							Map<String, OsuScore> catchedScores = user.player.getCatchedScores().stream()
								.collect(Collectors.toMap(OsuScore::getBeatmapId, b->b));
							
							List<OsuScore> newScores = new ArrayList<>();
							List<OsuScore> updatedScores = new ArrayList<>();
							List<OsuScoreUpdate> scoreUpdates = new ArrayList<>();
							
							for (OsuScore score : scores) {
								OsuScore catchedScore = catchedScores.get(score.getBeatmapId());
								if (catchedScore == null) {
									newScores.add(score);
									scoreUpdates.add(new OsuScoreUpdate(user.player, updated, null, score));
									LOG.info("New score = " + score.getPerformance());
								}
								else if (!score.equals(catchedScore)) {
									updatedScores.add(score);
									scoreUpdates.add(new OsuScoreUpdate(user.player, updated, catchedScore, score));
									LOG.info("Updated score = " + catchedScore.getPerformance() + " -> " + score.getPerformance());
								}
							}
							Database db = StorasBot.getDatabase();
							db.addScores(user.player.getIdentifier(), newScores);
							db.updateScores(user.player.getIdentifier(), updatedScores);
							db.updateTrackedUser(updated);
							
							user.handler.handleScoreUpdates(scoreUpdates);
						}
					}
					removeUser(user);
					
				} catch (InterruptedException | IOException | SQLException e) {
					LOG.warn("Failed: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
}

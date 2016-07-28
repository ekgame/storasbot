package lt.ekgame.storasbot.plugins.osu_top;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import lt.ekgame.storasbot.StorasBot;
import lt.ekgame.storasbot.utils.OsuMode;
import lt.ekgame.storasbot.utils.OsuUtils;
import net.dv8tion.jda.utils.SimpleLog;

public class OsuUserUpdater {
	
	private List<UserToUpdate> users = new ArrayList<>();
	private int idleSleep;
	
	public OsuUserUpdater(int workers, int idleSleep) {
		this.idleSleep = idleSleep;
		for (int i = 0; i < workers; i++)
			Executors.newSingleThreadExecutor().execute(new Worker(i+1));
	}
	
	public void addPlayer(OsuPlayer user, OsuMode mode) {
		synchronized (users) {
			if (users.stream().noneMatch(u->u.player.equals(user)))
				users.add(new UserToUpdate(user, mode));
		}
	}
	
	private void removeUser(UserToUpdate user) {
		synchronized (users) {
			users.remove(user);
		}
	}
	
	private UserToUpdate getUser() {
		synchronized (users) {
			Optional<UserToUpdate> oUser = users.stream()
					.filter(u->!u.isTaken())
					.findAny();
			
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
		boolean taken = false;
		
		UserToUpdate(OsuPlayer player, OsuMode mode) {
			this.player = player;
			this.mode = mode;
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
					else {
						LOG.info("Updating new player: " + user.player.getUsername());
						OsuPlayer updated = user.player.getFromAPI(user.mode);
						List<OsuScore> scores = OsuUtils.getScores(updated.getUserId(), user.mode);
						StorasBot.database.addScores(updated.getUserId(), user.mode, scores);
						StorasBot.database.addTrackedUser(updated);
						removeUser(user);
					}
				} catch (InterruptedException | IOException | SQLException e) {
					LOG.warn("Failed: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
}

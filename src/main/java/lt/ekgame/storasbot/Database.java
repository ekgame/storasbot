package lt.ekgame.storasbot;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.mariadb.jdbc.MariaDbDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.PreparedBatch;

import com.typesafe.config.Config;

import lt.ekgame.storasbot.plugins.osu_top.OsuPlayer;
import lt.ekgame.storasbot.plugins.osu_top.OsuScore;
import lt.ekgame.storasbot.plugins.osu_top.TrackedCountry;
import lt.ekgame.storasbot.plugins.osu_top.TrackedPlayer;
import lt.ekgame.storasbot.utils.BinTag;
import lt.ekgame.storasbot.utils.OsuMode;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;

public class Database {
	
	private String host, port, database, username, password;
	private DBI dbi;

	public Database(Config config) throws SQLException {
		this.host     = config.getString("database.host");
		this.port     = config.getString("database.port");
		this.database = config.getString("database.dtbs");
		this.username = config.getString("database.user");
		this.password = config.getString("database.pass");
		
		MariaDbDataSource ds = new MariaDbDataSource();
		ds.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + username + "&password=" + password);
		dbi = new DBI(ds);
	}
	
	// --------- osu! ranking tracker --------- //
	
	public List<TrackedCountry> getTrackedCountries() throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = handle.select("SELECT guild_id, channel_id, country, gamemode, top_number, top_personal, min_pp FROM osu_country_tracker");
			return rawData.stream()
				.map((row)->new TrackedCountry(
					(String) row.get("guild_id"), 
					(String) row.get("channel_id"), 
					(String) row.get("country"), 
					OsuMode.fromValue((int) row.get("gamemode")), 
					(int) row.get("top_number"), 
					(int) row.get("top_personal"), 
					(int) row.get("min_pp")
				)).collect(Collectors.toList());
		}
	}
	
	public void addTrackedCountry(TrackedCountry tracked) throws SQLException {
		try (Handle handle = dbi.open()) {
			handle.execute("INSERT INTO osu_country_tracker (guild_id, channel_id, country, gamemode, top_number, top_personal, min_pp) VALUES (?, ?, ?, ?, ?, ?, ?)",
				tracked.getGuildId(),
				tracked.getChannelId(),
				tracked.getCountry(),
				tracked.getGamemode(),
				tracked.getCountryTop(),
				tracked.getPersonalTop(),
				tracked.getMinPerformance()
			);
		}
	}
	
	public List<TrackedPlayer> getTrackedPlayers() throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = handle.select("SELECT guild_id, channel_id, user_id, gamemode, top_number, min_pp FROM osu_user_tracker");
			return rawData.stream()
				.map((row)->new TrackedPlayer(
					(String) row.get("guild_id"), 
					(String) row.get("channel_id"), 
					(String) row.get("user_id"), 
					OsuMode.fromValue((int) row.get("gamemode")), 
					(int)    row.get("top_number"),
					(int)    row.get("min_pp")
				)).collect(Collectors.toList());
		}
	}
	
	public void addTrackedPlayer(TrackedPlayer tracked) throws SQLException {
		try (Handle handle = dbi.open()) {
			handle.execute("INSERT INTO osu_country_tracker (guild_id, channel_id, user_id, gamemode, top_number) VALUES (?, ?, ?, ?, ?)",
				tracked.getGuildId(),
				tracked.getChannelId(),
				tracked.getUserId(),
				tracked.getGamemode(),
				tracked.getPersonalTop()
			);
		}
	}
	
	public OsuPlayer getTrackedUser(String userId, OsuMode mode) {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = handle.select("SELECT user_id, username, country, performance, global_rank, country_rank, accuracy FROM osu_tracked_users WHERE user_id=? AND gamemode=?", userId, mode.getValue());
			if (rawData.size() == 0)
				return null;
			Map<String, Object> item = rawData.get(0);
			return new OsuPlayer(
					(String)item.get("user_id"),
					 mode,
					(String)item.get("username"),
					(String)item.get("country"),
					(double)item.get("performance"),
					(int)   item.get("global_rank"),
					(int)   item.get("country_rank"),
					(double)item.get("accuracy")
				);
		}
	}
	
	public void addTrackedUser(OsuPlayer user) {
		try (Handle handle = dbi.open()) {
			handle.insert("INSERT INTO osu_tracked_users (user_id, username, country, performance, global_rank, country_rank, gamemode, accuracy) VALUES (?,?,?,?,?,?,?,?)",
				user.getUserId(), user.getUsername(),
				user.getCountry(), user.getPerformance(),
				user.getGlobalRank(), user.getCountryRank(),
				user.getGamemode().getValue(), user.getAccuracy());
		}
	}
	
	public void updateTrackedUser(OsuPlayer user) {
		try (Handle handle = dbi.open()) {
			handle.update("UPDATE osu_tracked_users SET username=?, country=?, performance=?, global_rank=?, country_rank=?, accuracy=? WHERE user_id=? AND gamemode=?",
				user.getUsername(), user.getCountry(), user.getPerformance(),
				user.getGlobalRank(), user.getCountryRank(), user.getAccuracy(),
				user.getUserId(), user.getGamemode().getValue());
		}
	}
	
	public List<OsuPlayer> getTrackedCountryPlayers(String country, OsuMode mode) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = country == null
				? handle.select("SELECT user_id, username, performance, global_rank, country_rank, country, accuracy FROM osu_tracked_users WHERE gamemode=?", mode.getValue())
				: handle.select("SELECT user_id, username, performance, global_rank, country_rank, country, accuracy FROM osu_tracked_users WHERE country=? AND gamemode=?", country, mode.getValue());
			
			return rawData.stream()
				.map((item)->new OsuPlayer(
					(String)item.get("user_id"),
					 mode,
					(String)item.get("username"),
					(String)item.get("country"),
					(double)item.get("performance"),
					(int)   item.get("global_rank"),
					(int)   item.get("country_rank"),
					(double)item.get("accuracy")
				)).collect(Collectors.toList());
		}
	}
	
	public void addScores(String userId, OsuMode mode, List<OsuScore> scores) throws SQLException {
		try (Handle handle = dbi.open()) {
			PreparedBatch batch = handle.prepareBatch("INSERT INTO osu_tracked_plays (user_id, beatmap_id, performance, mods, gamemode, count300, count100, count50, count_miss, score, max_combo, timestamp, rank, personal_top) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			for (OsuScore score : scores) {
				batch.add()
					.bind(0, userId)
					.bind(1, score.getBeatmapId())
					.bind(2, score.getPerformance())
					.bind(3, score.getMods())
					.bind(4, mode.getValue())
					.bind(5, score.getCount300())
					.bind(6, score.getCount100())
					.bind(7, score.getCount50())
					.bind(8, score.getCountMiss())
					.bind(9, score.getScore())
					.bind(10, score.getMaxCombo())
					.bind(11, score.getTimestamp())
					.bind(12, score.getRank())
					.bind(13, score.getPersonalTopPlace());
			}
			batch.execute();
		}
	}
	
	public void updateScores(String userId, OsuMode mode, List<OsuScore> scores) throws SQLException {
		try (Handle handle = dbi.open()) {
			PreparedBatch batch = handle.prepareBatch("UPDATE osu_tracked_plays SET performance=?, mods=?, count300=?, count100=?, count50=?, count_miss=?, score=?, max_combo=?, timestamp=?, rank=?, personal_top=? WHERE user_id=? AND beatmap_id=? AND gamemode=?");
			for (OsuScore score : scores) {
				batch.add()
					.bind(0, score.getPerformance())
					.bind(1, score.getMods())
					.bind(2, score.getCount300())
					.bind(3, score.getCount100())
					.bind(4, score.getCount50())
					.bind(5, score.getCountMiss())
					.bind(6, score.getScore())
					.bind(7, score.getMaxCombo())
					.bind(8, score.getTimestamp())
					.bind(9, userId)
					.bind(10, score.getRank())
					.bind(11, score.getPersonalTopPlace())
					.bind(12, score.getBeatmapId())
					.bind(13, mode.getValue());
			}
			batch.execute();
		}
	}
	
	public List<OsuScore> getUserScores(String userId, OsuMode mode) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = handle.select("SELECT beatmap_id, performance, mods, count300, count100, count50, count_miss, score, max_combo, timestamp, rank, personal_top FROM osu_tracked_plays WHERE user_id=? AND gamemode=?", userId, mode.getValue());
			return rawData.stream()
				.map((item)->new OsuScore(
					(String)item.get("beatmap_id"),
					(long)  item.get("mods"),
					(double)item.get("performance"),
					(int)   item.get("count300"),
					(int)   item.get("count100"),
					(int)   item.get("count50"),
					(int)   item.get("count_miss"),
					(int)   item.get("max_combo"),
					(long)  item.get("score"),
					(long)  item.get("timestamp"),
					(String)item.get("rank"),
					(int)   item.get("personal_top")
				)).collect(Collectors.toList());
		}
	}
	
	/**
	 * @return true if new, false if updated;
	 */
	public boolean addOrUpdateTrackedPlayer(TrackedPlayer tracker) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = handle.select("SELECT id FROM osu_user_tracker WHERE guild_id=? AND channel_id=? AND user_id=? AND gamemode=?",
					tracker.getGuildId(), tracker.getChannelId(), tracker.getUserId(), tracker.getGamemode());
			
			if (rawData.size() == 0) {
				handle.insert("INSERT INTO osu_user_tracker (guild_id, channel_id, user_id, gamemode, top_number, min_pp) VALUES (?,?,?,?,?,?)",
					tracker.getGuildId(), tracker.getChannelId(), tracker.getUserId(), tracker.getGamemode().getValue(),
					tracker.getPersonalTop(), tracker.getMinPerformance());
				return true;
			}
			else {
				handle.update("UPDATE osu_user_tracker SET top_number=?, min_pp=? WHERE id=?",
					tracker.getPersonalTop(), tracker.getMinPerformance(), (int)rawData.get(0).get("id"));
				return false;
			}
		}
	}
	
	/**
	 * @return true if new, false if updated;
	 */
	public boolean addOrUpdateTrackedCountry(TrackedCountry tracker) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = tracker.getCountry() == null
				? handle.select("SELECT id FROM osu_country_tracker WHERE guild_id=? AND channel_id=? AND country IS NULL AND gamemode=?",
					tracker.getGuildId(), tracker.getChannelId(), tracker.getGamemode().getValue())
				: handle.select("SELECT id FROM osu_country_tracker WHERE guild_id=? AND channel_id=? AND country=? AND gamemode=?",
					tracker.getGuildId(), tracker.getChannelId(), tracker.getCountry(), tracker.getGamemode().getValue());
			
			if (rawData.size() == 0) {
				handle.insert("INSERT INTO osu_country_tracker (guild_id, channel_id, country, gamemode, top_number, top_personal, min_pp) VALUES (?,?,?,?,?,?,?)",
					tracker.getGuildId(), tracker.getChannelId(), tracker.getCountry(), tracker.getGamemode().getValue(),
					tracker.getCountryTop(), tracker.getPersonalTop(), tracker.getMinPerformance());
				return true;
			}
			else {
				handle.update("UPDATE osu_country_tracker SET top_number=?, top_personal=?, min_pp=? WHERE id=?",
					tracker.getCountryTop(), tracker.getPersonalTop(), tracker.getMinPerformance(), (int)rawData.get(0).get("id"));
				return false;
			}
		}
	}
	
	public boolean removeTrackedCountry(Guild guild, TextChannel channel, String country, OsuMode mode) {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = country == null
				? handle.select("SELECT id FROM osu_country_tracker WHERE guild_id=? AND channel_id=? AND country IS NULL AND gamemode=?", 
					guild.getId(), channel.getId(), mode.getValue())
				: handle.select("SELECT id FROM osu_country_tracker WHERE guild_id=? AND channel_id=? AND country=? AND gamemode=?", 
					guild.getId(), channel.getId(), country, mode.getValue());
			
			if (rawData.size() != 0) {
				handle.update("DELETE FROM osu_country_tracker WHERE id=?", (int) rawData.get(0).get("id"));
				return true;
			}
			else return false;
		}
	}
	
	// --------- Bin database --------- //
	
	public long getBinsCount(Guild guild) throws SQLException {
		try (Handle handle = dbi.open()) {
			return (long) handle.select("SELECT count(*) as num FROM bin WHERE guild_id=?", guild.getId()).get(0).get("num");
		}
	}
	
	/**
	 * Used to get a random bin.
	 */
	public BinTag getBinOffset(Guild guild, int offset) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> result = handle.select("SELECT id, author_id, tag, content FROM bin WHERE guild_id=? LIMIT ?,1", guild.getId(), offset);
			if (result.size() == 0) return null;
			Map<String, Object> item = result.get(0);
			return new BinTag(
				(int)   item.get("id"),
				(String)item.get("author_id"),
				(String)item.get("tag"),
				(String)item.get("content")
			);
		}
	}
	
	public BinTag getBin(Guild guild, String tag) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> result = handle.select("SELECT id, author_id, tag, content FROM bin WHERE tag=? AND guild_id=?", tag, guild.getId());
			if (result.size() == 0) return null;
			Map<String, Object> item = result.get(0);
			return new BinTag(
				(int)   item.get("id"),
				(String)item.get("author_id"),
				(String)item.get("tag"),
				(String)item.get("content")
			);
		}
	}
	
	public List<String> getBinList(Guild guild, int count, int page) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> result = handle.select("SELECT tag FROM bin WHERE guild_id=? ORDER BY tag DESC LIMIT ?,?", guild.getId(), count*(page-1), count);
			return result.stream()
				.map((obj)->(String)obj.get("tag"))
				.collect(Collectors.toList());
		}
	}
	
	public boolean getBinExists(Guild guild, String tag) throws SQLException {
		try (Handle handle = dbi.open()) {
			return handle.select("SELECT id FROM bin WHERE tag=? AND guild_id=?", tag, guild.getId()).size() > 0;
		}
	}
	
	public void addBin(Guild guild, String submitterId, String tag, String content) throws SQLException {
		try (Handle handle = dbi.open()) {
			handle.execute("INSERT INTO bin (guild_id, author_id, tag, content) VALUES (?, ?, ?, ?)", guild.getId(), submitterId, tag, content);
		}
	}
	
	public void editBin(Guild guild, int id, String content) throws SQLException {
		try (Handle handle = dbi.open()) {
			handle.execute("UPDATE bin SET content=? WHERE id=? AND guild_id=?", content, id, guild.getId());
		}
	}
	
	public void deleteBin(Guild guild, int id) throws SQLException {
		try (Handle handle = dbi.open()) {
			handle.execute("DELETE FROM bin WHERE id=? AND guild_id=?", id, guild.getId());
		}
	}
	
	public void incrementBinUsage(Guild guild, int id) throws SQLException {
		try (Handle handle = dbi.open()) {
			handle.execute("UPDATE bin SET usages = usages + 1 WHERE id=? AND guild_id=?", id, guild.getId());
		}
	}
	
	public List<Pair<String, Long>> getTopBins(Guild guild) throws SQLException {
		try (Handle handle = dbi.open()) {
			List<Map<String, Object>> rawData = handle.select("SELECT tag, usages FROM bin WHERE guild_id=? ORDER BY usages DESC LIMIT 3", guild.getId());
			return rawData.stream()
				.map((item)->Pair.of(
					(String) item.get("tag"),
					(long) item.get("usages")
				)).collect(Collectors.toList());
		}
	}
}

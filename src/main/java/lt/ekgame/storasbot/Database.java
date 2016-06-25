package lt.ekgame.storasbot;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mariadb.jdbc.MariaDbDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.util.IntegerColumnMapper;

import com.typesafe.config.Config;

public class Database {
	
	private String host, port, database, username, password;

	public Database(Config config) throws SQLException {
		this.host = config.getString("database.host");
		this.port = config.getString("database.port");
		this.database = config.getString("database.dtbs");
		this.username = config.getString("database.user");
		this.password = config.getString("database.pass");
	}
	
	private Handle getHandle() throws SQLException {
		MariaDbDataSource ds = new MariaDbDataSource();
		ds.setUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + username + "&password=" + password);
		DBI dbi = new DBI(ds);
		Handle handle = dbi.open();
		return handle;
	}

	public void addUser(int userId, String username, int performance) throws SQLException {
		try (Handle handle = getHandle()) {
			handle.execute("INSERT INTO top_users (userId, username, performance) VALUES (?, ?, ?)", userId, username, performance);
		}
	}

	public void updateUser(int userId, String username, int performance) throws SQLException {
		try (Handle handle = getHandle()) {
			handle.execute("UPDATE top_users SET username=?, performance=? WHERE userId=?", username, performance, userId);
		}
	}

	public boolean userExists(int userId) throws SQLException {
		try (Handle handle = getHandle()) {
			return handle.createQuery("SELECT * FROM top_users WHERE userId=?").bind(0, userId).first() != null;
		}
	}

	public int getPerformance(int userId) throws SQLException {
		try (Handle handle = getHandle()) {
			return handle.createQuery("SELECT performance FROM top_users WHERE userId=?").bind(0, userId).map(IntegerColumnMapper.PRIMITIVE).first();
		}
	}

	public boolean scoreExists(int userId, int beatmapId, double performance) throws SQLException {
		try (Handle handle = getHandle()) {
			return handle.createQuery("SELECT * FROM top_plays WHERE userId=? AND beatmapId=? AND performance=?")
					.bind(0, userId)
					.bind(1, beatmapId)
					.bind(2, performance)
					.first() != null;
		}
	}
	
	public boolean playExists(int userId, int beatmapId) throws SQLException {
		try (Handle handle = getHandle()) {
			return handle.createQuery("SELECT * FROM top_plays WHERE userId=? AND beatmapId=?")
					.bind(0, userId)
					.bind(1, beatmapId)
					.first() != null;
		}
	}

	public void updateScore(int userId, int beatmapId, double performance) throws SQLException {
		try (Handle handle = getHandle()) {
			if (playExists(userId, beatmapId)) {
				handle.execute("UPDATE top_plays SET performance=? WHERE userId=? AND beatmapId=?", performance, userId, beatmapId);
			}
			else {
				handle.execute("INSERT INTO top_plays (userId, beatmapId, performance) VALUES (?, ?, ?)", userId, beatmapId, performance);
			}
		}
	}
	
	public int getBinsCount() throws SQLException {
		try (Handle handle = getHandle()) {
			return handle.createQuery("SELECT count(*) FROM bin").map(IntegerColumnMapper.PRIMITIVE).first();
		}
	}
	
	/**
	 * Used to get a random bin.
	 */
	public BinTag getBinOffset(int offset) throws SQLException {
		try (Handle handle = getHandle()) {
			Map<String, Object> result = handle.createQuery("SELECT id, submitter, tag, content FROM bin LIMIT ?,1")
				.bind(0, offset).first();
			if (result == null) return null;
			return new BinTag((int)result.get("id"), (String)result.get("submitter"), (String)result.get("tag"), (String)result.get("content"));
		}
	}
	
	public BinTag getBin(String tag) throws SQLException {
		try (Handle handle = getHandle()) {
			Map<String, Object> result = handle.createQuery("SELECT id, submitter, tag, content FROM bin WHERE tag=?")
				.bind(0, tag).first();
			if (result == null) return null;
			return new BinTag((int)result.get("id"), (String)result.get("submitter"), (String)result.get("tag"), (String)result.get("content"));
		}
	}
	
	public List<String> getBinList(int count, int page) throws SQLException {
		try (Handle handle = getHandle()) {
			List<String> tags = new ArrayList<>();
			Query<Map<String, Object>> result = handle.createQuery("SELECT tag FROM bin ORDER BY tag DESC LIMIT ?,?").bind(0, count*(page-1)).bind(1, count);
			Iterator<Map<String, Object>> iterator = result.iterator();
			while (iterator.hasNext())
				tags.add((String) iterator.next().get("tag"));
			return tags;
		}
	}
	
	public boolean getBinExists(String tag) throws SQLException {
		try (Handle handle = getHandle()) {
			return handle.createQuery("SELECT id, submitter, tag, content FROM bin WHERE tag=?")
					.bind(0, tag).first() != null;
		}
	}
	
	public void addBin(String submitter, String tag, String content) throws SQLException {
		try (Handle handle = getHandle()) {
			handle.execute("INSERT INTO bin (submitter, tag, content) VALUES (?, ?, ?)", submitter, tag, content);
		}
	}
	
	public void editBin(int id, String content) throws SQLException {
		try (Handle handle = getHandle()) {
			handle.execute("UPDATE bin SET content=? WHERE id=?", content, id);
		}
	}
	
	public void deleteBin(int id) throws SQLException {
		try (Handle handle = getHandle()) {
			handle.execute("DELETE FROM bin WHERE id=?", id);
		}
	}
}

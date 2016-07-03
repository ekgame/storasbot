package lt.ekgame.storasbot.utils;

public class BinTag {
	
	private int id;
	private String userId, tag, content;

	public BinTag(int id, String userId, String tag, String content) {
		this.id = id;
		this.userId = userId;
		this.tag = tag;
		this.content = content;
	}
	
	public int getId() {
		return id;
	}

	public String getUserId() {
		return userId;
	}

	public String getTag() {
		return tag;
	}

	public String getContent() {
		return content;
	}
}

package lt.ekgame.storasbot.utils.osu;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class OsuPlayerIdentifier {
	
	private String userId;
	private OsuMode mode;

	private OsuPlayerIdentifier(String userId, OsuMode mode) {
		try {Integer.parseInt(userId);} catch (NumberFormatException e) {
			throw new IllegalArgumentException("userId has to be an integer string.");
		}
		if (mode == null)
			throw new IllegalArgumentException("mode can not be null.");
		
		this.userId = userId;
		this.mode = mode;
	}
	
	public static OsuPlayerIdentifier of(String userId, OsuMode mode) {
		return new OsuPlayerIdentifier(userId, mode);
	}
	
	public String getUserId() {
		return userId;
	}
	
	public OsuMode getMode() {
		return mode;
	}
	
	public int getModeValue() {
		return mode.getValue();
	}

	public int hashCode() {
		return new HashCodeBuilder(55, 23).append(userId).append(mode).toHashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (obj.getClass() != getClass()) return false;
		OsuPlayerIdentifier other = (OsuPlayerIdentifier) obj;
		return userId.equals(other.userId) && mode == other.mode;
	}
}

package lt.ekgame.storasbot.utils.osu;

public enum OsuMode {
	OSU  (0, "osu!"), 
	TAIKO(1, "osu!taiko"), 
	CATCH(2, "osu!catch"), 
	MANIA(3, "osu!mania");
	
	private int value;
	private String name;
	
	OsuMode(int value, String name) {
		this.value = value;
		this.name = name;
	}
	
	public int getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
	public static OsuMode fromValue(int value) {
		for (OsuMode mode : OsuMode.values())
			if (mode.value == value)
				return mode;
		throw new IllegalArgumentException("Invalid osu! gamemode value '" + value + "'.");
	}
}

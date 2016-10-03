package lt.ekgame.storasbot.plugins;

public enum Setting {
	
	ASIH("enabled_asih", false, Boolean.class),
	BANCHO_STATUS("enabled_bancho_status", false, Boolean.class),
	BANCHO_STATUS_CHANNEL("bancho_status_channel", null, String.class),
	BEATMAP_EXAMINER("enabled_beatmap_examiner", true, Boolean.class),
	BEATMAP_ANALYZER("enabled_beatmap_anzlyzer", true, Boolean.class);
	
	private String sqlName;
	private Object defaultValue;
	private Class<?> klass;
	
	Setting(String sqlName, Object defaultValue, Class<?> klass) {
		this.sqlName = sqlName;
		this.defaultValue = defaultValue;
		this.klass = klass;
	}

	public String getSQLName() {
		return sqlName;
	}

	public Object getDefault() {
		return defaultValue;
	}

	public Class<?> getType() {
		return klass;
	}
}

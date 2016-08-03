package lt.ekgame.storasbot.utils.osu;

import org.tillerino.osuApiModel.OsuApiScore;

public class OsuScore {
	
	private String beatmapId, rank;
	private long mods;
	private double performance;
	
	private int count300, count100, count50, countMiss, maxCombo;
	private int personalTop = -1;
	private long score, timestamp;
	
	public OsuScore(String beatmapId, long mods, double performance, int count300, int count100,
			int count50, int countMiss, int maxCombo, long score, long timestamp, String rank,
			int personalTop) {
		this.beatmapId   = beatmapId;
		this.mods        = mods;
		this.performance = performance;
		
		this.count300  = count300;
		this.count100  = count100;
		this.count50   = count50;
		this.countMiss = countMiss;
		
		this.score     = score;
		this.maxCombo  = maxCombo;
		this.timestamp = timestamp;
		
		this.rank = rank;
		this.personalTop = personalTop;
	}
	
	public OsuScore(OsuApiScore score, int personalTop) {
		this.beatmapId   = ""+score.getBeatmapId();
		this.mods        = score.getMods();
		this.performance = score.getPp();
		
		this.count300  = score.getCount300();
		this.count100  = score.getCount100();
		this.count50   = score.getCount50();
		this.countMiss = score.getCountMiss();
		
		this.score     = score.getScore();
		this.maxCombo  = score.getMaxCombo();
		this.timestamp = score.getDate();
		
		this.rank = score.getRank();
		this.personalTop = personalTop;
	}

	public String getBeatmapId() {
		return beatmapId;
	}

	public long getMods() {
		return mods;
	}

	public double getPerformance() {
		return performance;
	}
	
	public boolean equals(Object o) {
		if (o instanceof OsuScore) {
			OsuScore other = (OsuScore) o;
			boolean different = false;
			different |= !this.beatmapId.equals(other.beatmapId);
			different |= this.mods != other.mods;
			different |= Math.abs(this.performance - other.getPerformance()) > 0.0001;
			return !different;
		}
		return false;
	}

	public int getCount300() {
		return count300;
	}

	public int getCount100() {
		return count100;
	}

	public int getCount50() {
		return count50;
	}

	public int getCountMiss() {
		return countMiss;
	}

	public int getMaxCombo() {
		return maxCombo;
	}
	
	public int getPersonalTopPlace() {
		return personalTop;
	}

	public long getScore() {
		return score;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public double getAccuracy() {
		return 100*(6*count300 + 2*count100 + count50)/(double)(6*(count300 + count100 + count50 + countMiss));
	}

	public String getRank() {
		return rank;
	}
}

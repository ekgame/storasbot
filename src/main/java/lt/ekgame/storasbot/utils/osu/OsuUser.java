package lt.ekgame.storasbot.utils.osu;

import org.tillerino.osuApiModel.OsuApiUser;

import com.google.gson.annotations.SerializedName;

public class OsuUser extends OsuApiUser {
	
	@SerializedName("pp_country_rank")
	private int countryRank;

	public int getCountryRank() {
		return countryRank;
	}
	
	public String formatLevel() {
		int level = (int) getLevel();
		double progress = (getLevel() - level)*100;
		return level + String.format(" (%.0f%%)", progress);
	}
	
	public int getPlayCount() {
		// a cheat to avoid division by zero for osu! command.
		return super.getPlayCount() <= 0 ? 1 : super.getPlayCount();
	}

	public OsuPlayerIdentifier getIdentifier() {
		return OsuPlayerIdentifier.of(""+getUserId(), OsuMode.fromValue(getMode()));
	}
}

package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;

public class SquadPlayer {
    @SerializedName("player")
    private PlayerDetails playerDetails;

    public PlayerDetails getPlayerDetails() {
        return playerDetails;
    }

    public static class PlayerDetails {
        @SerializedName("id")
        private int id;
        @SerializedName("name")
        private String name;

        @SerializedName("position")
        private String position;
        @SerializedName("jerseyNumber")
        private int jerseyNumber;
        public int getId() {
            return id;
        }
        public String getName() {
            return name;
        }
        public String getPosition() {
            return position;
        }
        public int getJerseyNumber() {
            return jerseyNumber;
        }
    }
}

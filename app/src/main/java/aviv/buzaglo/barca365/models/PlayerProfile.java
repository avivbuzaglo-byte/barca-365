package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlayerProfile {
    // בתוך ה-API זה מגיע בתוך אובייקט "player"
    @SerializedName("player")
    private PlayerData player;

    public PlayerData getPlayer() { return player; }

    public static class PlayerData {
        @SerializedName("name")
        private String name;

        @SerializedName("height")
        private int height; // בסנטימטרים

        @SerializedName("dateOfBirthTimestamp")
        private long dateOfBirthTimestamp; // כדי לחשב גיל

        @SerializedName("country")
        private NationalityData country;

        @SerializedName("position")
        private String position; // F, M, D...
        @SerializedName("positionsDetailed")
        private List<String> positionsDetailed;

        public String getName() { return name; }
        public int getHeight() { return height; }
        public long getDateOfBirthTimestamp() { return dateOfBirthTimestamp; }
        public NationalityData getCountry() { return country; }
        public String getPosition() { return position; }
        public List<String> getPositionsDetailed() { return positionsDetailed; }
    }

    public static class NationalityData {
        @SerializedName("name")
        private String name;

        public String getName() { return name; }
    }
}
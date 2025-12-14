package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlayerProfile {
    // בתוך ה-API זה מגיע בתוך אובייקט "player"
    @SerializedName("player")
    private PlayerData player;

    public PlayerData getPlayer() { return player; }

    public static class PlayerData {
        @SerializedName("id")
        private int id;
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
        @SerializedName("injury")
        private Injury injury;
        @SerializedName("proposedMarketValue")
        private long proposedMarketValue;
        @SerializedName("proposedMarketValueRaw")
        private MarketValueRaw proposedMarketValueRaw;

        public String getName() { return name; }
        public int getHeight() { return height; }
        public int getId() {return id; }
        public Injury getInjury() { return injury; }
        public long getDateOfBirthTimestamp() { return dateOfBirthTimestamp; }
        public NationalityData getCountry() { return country; }
        public String getPosition() { return position; }
        public List<String> getPositionsDetailed() { return positionsDetailed; }
        public long getProposedMarketValue() { return proposedMarketValue; }
        public MarketValueRaw getProposedMarketValueRaw() { return proposedMarketValueRaw; }
    }

    public static class NationalityData {
        @SerializedName("name")
        private String name;

        public String getName() { return name; }
    }
    public static class Injury {
        @SerializedName("reason")
        private String reason; // "Knee Injury"

        @SerializedName("status")
        private String status; // "out" / "questionable"

        @SerializedName("endDateTimestamp")
        private Long endDateTimestamp; // תאריך חזרה משוער

        public String getReason() { return reason; }
        public String getStatus() { return status; }
        public Long getEndDateTimestamp() { return endDateTimestamp; }
    }
    public static class MarketValueRaw {
        @SerializedName("value")
        private long value;

        @SerializedName("currency")
        private String currency; // "EUR", "USD" etc.

        public long getValue() { return value; }
        public String getCurrency() { return currency; }
    }
}
package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SofaEventsResponse {

    @SerializedName("events")
    private List<SofaEvent> events;

    public List<SofaEvent> getEvents() {
        return events;
    }

    // --- המחלקה הראשית שמייצגת משחק אחד ---
    public static class SofaEvent {
        @SerializedName("id")
        private int id;

        @SerializedName("startTimestamp")
        private long startTimestamp;

        @SerializedName("tournament")
        private Tournament tournament;

        @SerializedName("homeTeam")
        private Team homeTeam;

        @SerializedName("awayTeam")
        private Team awayTeam;

        @SerializedName("status")
        private Status status;

        @SerializedName("homeScore")
        private Score homeScore;

        @SerializedName("awayScore")
        private Score awayScore;

        // Getters
        public int getId() { return id; }
        public long getStartTimestamp() { return startTimestamp; }
        public Tournament getTournament() { return tournament; }
        public Team getHomeTeam() { return homeTeam; }
        public Team getAwayTeam() { return awayTeam; }
        public Status getStatus() { return status; }
        public Score getHomeScore() { return homeScore; }
        public Score getAwayScore() { return awayScore; }
    }

    // --- מחלקות עזר פנימיות ---

    public static class Tournament {
        @SerializedName("name")
        private String name;

        @SerializedName("uniqueTournament")
        private UniqueTournament uniqueTournament;

        public String getName() { return name; }
        public UniqueTournament getUniqueTournament() { return uniqueTournament; }
    }

    public static class UniqueTournament {
        @SerializedName("id")
        private int id;

        public int getId() { return id; }
    }

    public static class Team {
        @SerializedName("name")
        private String name;

        @SerializedName("id")
        private int id;

        public String getName() { return name; }
        public int getId() { return id; }
    }

    public static class Status {
        @SerializedName("code")
        private int code; // 100=נגמר, 0=לא התחיל, 6/7=לייב

        @SerializedName("description")
        private String description; // למשל "FT", "HT", "35'"

        public int getCode() { return code; }
        public String getDescription() { return description; }
    }

    public static class Score {
        @SerializedName("current")
        private int current; // התוצאה הנוכחית

        public int getCurrent() { return current; }
    }
}
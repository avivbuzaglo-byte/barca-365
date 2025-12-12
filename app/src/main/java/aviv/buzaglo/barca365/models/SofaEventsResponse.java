package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SofaEventsResponse {

    // 1. משמש לקריאת רשימת משחקים (events)
    @SerializedName("events")
    private List<SofaEvent> events;

    public List<SofaEvent> getEvents() {
        return events;
    }

    // 2. משמש לקריאת משחק בודד (event) - הוספנו את זה כאן כדי לא ליצור קובץ חדש
    @SerializedName("event")
    private SofaEvent event;

    public SofaEvent getEvent() {
        return event;
    }

    // --- המחלקה הראשית שמייצגת משחק ---
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

        // --- הוספנו את רשימות הפצועים לכאן ---
        @SerializedName("homeTeamMissingPlayers")
        private List<MissingPlayer> homeTeamMissingPlayers;

        @SerializedName("awayTeamMissingPlayers")
        private List<MissingPlayer> awayTeamMissingPlayers;

        // Getters
        public int getId() { return id; }
        public long getStartTimestamp() { return startTimestamp; }
        public Tournament getTournament() { return tournament; }
        public Team getHomeTeam() { return homeTeam; }
        public Team getAwayTeam() { return awayTeam; }
        public Status getStatus() { return status; }
        public Score getHomeScore() { return homeScore; }
        public Score getAwayScore() { return awayScore; }

        public List<MissingPlayer> getHomeTeamMissingPlayers() { return homeTeamMissingPlayers; }
        public List<MissingPlayer> getAwayTeamMissingPlayers() { return awayTeamMissingPlayers; }
    }

    // --- מחלקה שמייצגת שחקן פצוע (הכנסנו אותה לכאן) ---
    public static class MissingPlayer {
        public MissingPlayer(int id, String name, String reason, Long returnTimestamp) {
            this.player = new TeamPlayer();
            this.player.name = name;
            this.player.id = id; // הנה התיקון!
            this.reason = reason;
            this.willReturnTimestamp = returnTimestamp;
            this.type = "missing";
        }
        @SerializedName("player")
        private TeamPlayer player; // קוראים לזה TeamPlayer כדי לא להתבלבל עם Team

        @SerializedName("type")
        private String type; // missing / doubtful

        @SerializedName("reason")
        private String reason; // "Hamstring Injury"

        @SerializedName("willReturnTimestamp")
        private Long willReturnTimestamp;

        public TeamPlayer getPlayer() { return player; }
        public String getType() { return type; }
        public String getReason() { return reason; }
        public Long getWillReturnTimestamp() { return willReturnTimestamp; }
    }

    // פרטי השחקן עצמו (שם, עמדה, תמונה)
    public static class TeamPlayer {
        @SerializedName("name")
        private String name;

        @SerializedName("id")
        private int id;

        @SerializedName("position")
        private String position;

        public String getName() { return name; }
        public int getId() { return id; }
        public String getPosition() { return position; }
    }

    // --- מחלקות עזר נוספות (כמו שהיו קודם) ---

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
        private int code;
        @SerializedName("description")
        private String description;
        public int getCode() { return code; }
        public String getDescription() { return description; }
    }

    public static class Score {
        @SerializedName("current")
        private int current;
        public int getCurrent() { return current; }
    }
    public class IncidentsResponse {
        private List<SofaIncident> incidents;
        public List<SofaIncident> getIncidents() { return incidents; }
    }
}
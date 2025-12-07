package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StandingsResponse {

    @SerializedName("standings")
    private List<StandingTable> standings;

    public List<StandingTable> getStandings() {
        return standings;
    }

    // מייצג טבלה אחת (בית, חוץ, או סה"כ)
    public static class StandingTable {
        @SerializedName("type")
        private String type; // "TOTAL", "HOME", "AWAY"

        @SerializedName("rows")
        private List<StandingRow> rows;

        public String getType() { return type; }
        public List<StandingRow> getRows() { return rows; }
    }

    // מייצג שורה בטבלה (קבוצה אחת)
    public static class StandingRow {
        @SerializedName("team")
        private Team team;

        @SerializedName("position")
        private int position;

        @SerializedName("matches")
        private int matches;

        @SerializedName("wins")
        private int wins;

        @SerializedName("draws")
        private int draws;

        @SerializedName("losses")
        private int losses;

        @SerializedName("scoresFor")
        private int scoresFor;

        @SerializedName("scoresAgainst")
        private int scoresAgainst;

        @SerializedName("points")
        private int points;

        public Team getTeam() { return team; }
        public int getPosition() { return position; }
        public int getMatches() { return matches; }
        public int getWins() { return wins; }
        public int getDraws() { return draws; }
        public int getLosses() { return losses; }
        public int getPoints() { return points; }

        // חישוב הפרש שערים
        public int getGoalDifference() {
            return scoresFor - scoresAgainst;
        }
    }

    // מייצג את הקבוצה
    public static class Team {
        @SerializedName("name")
        private String name;

        @SerializedName("id")
        private int id;

        public String getName() { return name; }
        public int getId() { return id; }
    }
}
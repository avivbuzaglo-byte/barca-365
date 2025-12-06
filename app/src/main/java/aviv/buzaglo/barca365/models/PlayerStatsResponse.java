package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;

public class PlayerStatsResponse {

    @SerializedName("statistics")
    private StatsData statistics;

    public StatsData getStatistics() {
        return statistics;
    }

    public static class StatsData {
        @SerializedName("appearances")
        private int appearances;

        @SerializedName("goals")
        private int goals;

        @SerializedName("assists")
        private int assists;
        @SerializedName("saves")
        private int saves;

        @SerializedName("cleanSheet")
        private int cleanSheets;

        // Getters
        public int getAppearances() { return appearances; }
        public int getGoals() { return goals; }
        public int getAssists() { return assists; }
        public int getSaves() { return saves; }
        public int getCleanSheets() { return cleanSheets; }
    }
}
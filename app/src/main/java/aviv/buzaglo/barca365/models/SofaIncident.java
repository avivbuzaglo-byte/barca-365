package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;

public class SofaIncident {
    private String incidentType; // "goal", "card"
    private int time; // דקה במשחק
    private boolean isHome; // האם זה קבוצת בית?
    private String incidentClass; // "red", "yellow", "regular" (לגולים)

    @SerializedName("player")
    private Player player;

    // Getters
    public String getType() { return incidentType; }
    public int getTime() { return time; }
    public boolean isHome() { return isHome; }
    public String getIncidentClass() { return incidentClass; }
    public Player getPlayer() { return player; }

    public static class Player {
        private String shortName;
        public String getShortName() { return shortName; }
    }
}
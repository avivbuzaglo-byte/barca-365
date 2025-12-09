package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SofaSquadResponse {

    @SerializedName("players")
    private List<SquadPlayer> players;

    public List<SquadPlayer> getPlayers() {
        return players;
    }

    public static class SquadPlayer {
        @SerializedName("player")
        private SofaEventsResponse.TeamPlayer player; // משתמשים במודל הקיים של שחקן

        // שדות חשובים שאנחנו מחפשים (אחד מהם יכיל את המידע)

        @SerializedName("missing")
        private boolean missing;

        @SerializedName("missingType")
        private String missingType; // למשל "injury"

        @SerializedName("missingReason")
        private String missingReason; // למשל "Knee injury"

        @SerializedName("returnTimestamp")
        private Long returnTimestamp; // צפי חזרה

        public SofaEventsResponse.TeamPlayer getPlayer() {
            return player;
        }

        public boolean isMissing() {
            return missing;
        }

        public String getMissingType() {
            return missingType;
        }

        public String getMissingReason() {
            return missingReason;
        }

        public Long getReturnTimestamp() {
            return returnTimestamp;
        }
    }
}
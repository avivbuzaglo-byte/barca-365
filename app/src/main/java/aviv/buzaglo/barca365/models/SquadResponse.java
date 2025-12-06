package aviv.buzaglo.barca365.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SquadResponse {
    @SerializedName("players")
    private List<SquadPlayer> players;

    public List<SquadPlayer> getPlayers() {
        return players;
    }
}

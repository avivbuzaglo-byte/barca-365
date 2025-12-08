package aviv.buzaglo.barca365.network;

import aviv.buzaglo.barca365.models.PlayerProfile;
import aviv.buzaglo.barca365.models.PlayerStatsResponse;
import aviv.buzaglo.barca365.models.SofaEventsResponse;
import aviv.buzaglo.barca365.models.SquadResponse;
import aviv.buzaglo.barca365.models.StandingsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SofaApiService {
    @GET("team/{teamId}/players")
    Call<SquadResponse> getTeamSquad(@Path("teamId") int teamId);

    @GET("player/{playerId}")
    Call<PlayerProfile> getPlayerDetails(@Path("playerId") int playerId);

    @GET("player/{playerId}/unique-tournament/{tournamentId}/season/{seasonId}/statistics/overall")
    Call<PlayerStatsResponse> getPlayerSeasonStats(
            @Path("playerId") int playerId,
            @Path("tournamentId") int tournamentId,
            @Path("seasonId") int seasonId
    );
    // שליפת טבלאות
    @GET("unique-tournament/{tournamentId}/season/{seasonId}/standings/total")
    Call<StandingsResponse> getStandings(
            @Path("tournamentId") int tournamentId,
            @Path("seasonId") int seasonId
    );
    @GET("team/{teamId}/events/last/{page}")
    Call<SofaEventsResponse> getTeamLastEvents(
            @Path("teamId") int teamId,
            @Path("page") int page
    );

    // קריאה למשחקים שיהיו (לוח משחקים עתידי)
    @GET("team/{teamId}/events/next/{page}")
    Call<SofaEventsResponse> getTeamNextEvents(
            @Path("teamId") int teamId,
            @Path("page") int page
    );
}

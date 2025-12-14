package aviv.buzaglo.barca365.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.adapters.PlayersAdapter;
import aviv.buzaglo.barca365.models.SquadPlayer;
import aviv.buzaglo.barca365.models.SquadResponse;
import aviv.buzaglo.barca365.network.SofaApiService;
import aviv.buzaglo.barca365.network.SofaRetrofitClient;
import aviv.buzaglo.barca365.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayersFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private PlayersAdapter adapter;

    // מזהה הקבוצה של ברצלונה ב-Sofascore

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // מנפח (טוען) את קובץ ה-XML שיצרנו קודם
        return inflater.inflate(R.layout.fragment_players, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // קישור לרכיבים במסך
        recyclerView = view.findViewById(R.id.players_recycler_view);
        progressBar = view.findViewById(R.id.loading_progress_bar);

        // הגדרת ה-RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // קריאה לפונקציה שמביאה נתונים
        fetchPlayers();
    }

    private void fetchPlayers() {
        progressBar.setVisibility(View.VISIBLE);

        SofaApiService apiService = SofaRetrofitClient.getClient().create(SofaApiService.class);
        Call<SquadResponse> call = apiService.getTeamSquad(Constants.BARCELONA_TEAM_ID);

        call.enqueue(new Callback<SquadResponse>() {
            @Override
            public void onResponse(Call<SquadResponse> call, Response<SquadResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<SquadPlayer> playersList = response.body().getPlayers();

                    // --- התיקון כאן: יצירת האדפטר פעם אחת, כולל המאזין ללחיצה ---
                    adapter = new PlayersAdapter(getContext(), playersList, new PlayersAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(int playerId, int jerseyNumber) {
                            // מעבר לפרגמנט הפרטים עם ה-ID של השחקן
                            PlayerDetailsFragment detailsFragment = PlayerDetailsFragment.newInstance(playerId, jerseyNumber);

                            getParentFragmentManager()
                                    .beginTransaction()
                                    // שימוש באנימציות כניסה ויציאה (אופציונלי אך מומלץ)
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                    .replace(R.id.fragment_container, detailsFragment)
                                    .addToBackStack(null) // מאפשר לחזור אחורה לרשימה
                                    .commit();
                        }
                    });

                    recyclerView.setAdapter(adapter);

                } else {
                    Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SquadResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }
}
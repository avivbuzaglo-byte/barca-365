package aviv.buzaglo.barca365.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.adapters.StandingsAdapter;
import aviv.buzaglo.barca365.models.StandingsResponse;
import aviv.buzaglo.barca365.network.SofaApiService;
import aviv.buzaglo.barca365.network.SofaRetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StandingsFragment extends Fragment {

    private RecyclerView recyclerView;
    // לה ליגה ID ועונה 24/25
    private static final int TOURNAMENT_ID = 8;
    private static final int SEASON_ID = 77559; // שים לב: וודא שזה ה-ID העדכני לעונה, זה משתנה

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_standings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.standings_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fetchStandings();
    }

    private void fetchStandings() {
        SofaApiService apiService = SofaRetrofitClient.getClient().create(SofaApiService.class);
        Call<StandingsResponse> call = apiService.getStandings(TOURNAMENT_ID, SEASON_ID);

        call.enqueue(new Callback<StandingsResponse>() {
            @Override
            public void onResponse(Call<StandingsResponse> call, Response<StandingsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StandingsResponse.StandingTable> tables = response.body().getStandings();

                    // חיפוש הטבלה מסוג "TOTAL" (לא בית ולא חוץ)
                    for (StandingsResponse.StandingTable table : tables) {
                        if (table.getType().equals("total")) {
                            StandingsAdapter adapter = new StandingsAdapter(getContext(), table.getRows());
                            recyclerView.setAdapter(adapter);
                            break;
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load standings", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StandingsResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
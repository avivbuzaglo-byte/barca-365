package aviv.buzaglo.barca365.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.adapters.FixturesAdapter;
import aviv.buzaglo.barca365.models.SofaEventsResponse; // שים לב לשימוש במודל החדש
import aviv.buzaglo.barca365.network.SofaApiService;
import aviv.buzaglo.barca365.network.SofaRetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FixturesFragment extends Fragment {

    private static final String TAG = "FixturesFragment";

    // קבועים
    private static final int BARCA_TEAM_ID = 2817;
    private final int TOTAL_API_CALLS = 2;

    // UI Elements
    private RecyclerView recyclerView;
    private FixturesAdapter adapter;
    private ProgressBar progressBar;
    private TextView errorTextView;

    // Data
    private SofaApiService apiService;
    private final List<SofaEventsResponse.SofaEvent> masterEventList = new ArrayList<>();
    private final AtomicInteger apiCallCounter = new AtomicInteger(0);

    // --- אתחול הפרגמנט ---
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // וודא שקובץ ה-XML הראשי נקרא fragment_fixtures
        return inflater.inflate(R.layout.fragment_fixtures, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();

        // יצירת ה-API Service
        apiService = SofaRetrofitClient.getClient().create(SofaApiService.class);

        // התחלת טעינת הנתונים
        fetchAllEvents();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_fixtures);
        progressBar = view.findViewById(R.id.progress_bar_fixtures);
        errorTextView = view.findViewById(R.id.text_view_error_fixtures);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // אנו מעבירים רשימה ריקה בהתחלה
        adapter = new FixturesAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    private void fetchAllEvents() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
        errorTextView.setVisibility(View.GONE);

        masterEventList.clear();
        apiCallCounter.set(0);

        // 1. קריאת משחקי עבר (Last Events)
        apiService.getTeamLastEvents(BARCA_TEAM_ID, 0).enqueue(new Callback<SofaEventsResponse>() {
            @Override
            public void onResponse(Call<SofaEventsResponse> call, Response<SofaEventsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getEvents() != null) {
                    synchronized (masterEventList) {
                        masterEventList.addAll(response.body().getEvents());
                    }
                }
                checkIfAllCallsFinished();
            }

            @Override
            public void onFailure(Call<SofaEventsResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching last events", t);
                checkIfAllCallsFinished();
            }
        });

        // 2. קריאת משחקי עתיד (Next Events)
        apiService.getTeamNextEvents(BARCA_TEAM_ID, 0).enqueue(new Callback<SofaEventsResponse>() {
            @Override
            public void onResponse(Call<SofaEventsResponse> call, Response<SofaEventsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getEvents() != null) {
                    synchronized (masterEventList) {
                        masterEventList.addAll(response.body().getEvents());
                    }
                }
                checkIfAllCallsFinished();
            }

            @Override
            public void onFailure(Call<SofaEventsResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching next events", t);
                checkIfAllCallsFinished();
            }
        });
    }

    private void checkIfAllCallsFinished() {
        // בודק אם הגענו ל-2 קריאות (גם past וגם next)
        if (apiCallCounter.incrementAndGet() == TOTAL_API_CALLS) {
            // אם הפרגמנט עדיין מחובר למסך, נעדכן את ה-UI
            if (isAdded() && getActivity() != null) {
                processData();
            }
        }
    }

    private void processData() {
        progressBar.setVisibility(View.GONE);

        if (masterEventList.isEmpty()) {
            errorTextView.setVisibility(View.VISIBLE);
            return;
        }

        List<SofaEventsResponse.SofaEvent> filteredList = new ArrayList<>();

        for (SofaEventsResponse.SofaEvent event : masterEventList) {
            if (event.getTournament() != null && event.getTournament().getUniqueTournament() != null) {
                    filteredList.add(event);
            }
        }

        if (filteredList.isEmpty()) {
            errorTextView.setText("No matches found for this season.");
            errorTextView.setVisibility(View.VISIBLE);
            return;
        }

        // מיון הרשימה לפי זמן (מהישן לחדש)
        Collections.sort(filteredList, new Comparator<SofaEventsResponse.SofaEvent>() {
            @Override
            public int compare(SofaEventsResponse.SofaEvent o1, SofaEventsResponse.SofaEvent o2) {
                return Long.compare(o1.getStartTimestamp(), o2.getStartTimestamp());
            }
        });

        // עדכון האדפטר והצגת הרשימה
        adapter.updateEvents(filteredList);
        recyclerView.setVisibility(View.VISIBLE);

        // גלילה אוטומטית למשחק הקרוב ביותר
        scrollToFirstFutureMatch(filteredList);
    }

    private void scrollToFirstFutureMatch(List<SofaEventsResponse.SofaEvent> list) {
        int targetIndex = -1;

        for (int i = 0; i < list.size(); i++) {
            SofaEventsResponse.SofaEvent event = list.get(i);
            // קוד סטטוס קטן מ-100 אומר שהמשחק טרם הסתיים (עתידי או לייב)
            if (event.getStatus() != null && event.getStatus().getCode() < 100) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex != -1) {
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(targetIndex, 0);
        } else {
            // אם הכל נגמר, גלול לסוף
            recyclerView.scrollToPosition(list.size() - 1);
        }
    }
}
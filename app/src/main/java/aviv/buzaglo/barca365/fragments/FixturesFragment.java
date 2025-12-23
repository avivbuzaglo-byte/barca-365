package aviv.buzaglo.barca365.fragments;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.adapters.FixturesAdapter;
import aviv.buzaglo.barca365.models.SofaEventsResponse;
import aviv.buzaglo.barca365.network.SofaApiService;
import aviv.buzaglo.barca365.network.SofaRetrofitClient;
import aviv.buzaglo.barca365.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FixturesFragment extends Fragment {

    private static final String TAG = "FixturesFragment";

    // קבועים לקריאות API
    private final int TOTAL_API_CALLS = 2;

    // קבועים ליומן
    private static final int CALENDAR_PERMISSION_CODE = 100;

    // UI Elements
    private RecyclerView recyclerView;
    private FixturesAdapter adapter;
    private ProgressBar progressBar;
    private TextView errorTextView;

    // Data
    private SofaApiService apiService;
    private final List<SofaEventsResponse.SofaEvent> masterEventList = new ArrayList<>();
    private final AtomicInteger apiCallCounter = new AtomicInteger(0);

    // משתנה זמני לשמירת האירוע בזמן בקשת הרשאה
    private SofaEventsResponse.SofaEvent eventToSave;

    // --- אתחול הפרגמנט ---
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        // --- תיקון: העברת ה-Listener לאדפטר ---
        adapter = new FixturesAdapter(getContext(), new ArrayList<>(), event -> {
            // כאשר לוחצים על הכפתור באדפטר, פונקציה זו נקראת
            onAddToCalendarClicked(event);
        });

        recyclerView.setAdapter(adapter);
    }

    // --- לוגיקה של לוח שנה והרשאות ---

    private void onAddToCalendarClicked(SofaEventsResponse.SofaEvent event) {
        this.eventToSave = event; // שומרים את האירוע בצד

        // בדיקה אם יש הרשאה לכתוב ליומן
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            // אם אין הרשאה - מבקשים אותה
            requestPermissions(new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_CODE);
        } else {
            // יש הרשאה - מבצעים שמירה
            saveEventToCalendar(event);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALENDAR_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // המשתמש אישר את ההרשאה
                if (eventToSave != null) {
                    saveEventToCalendar(eventToSave);
                }
            } else {
                Toast.makeText(getContext(), "Permission required to add events to calendar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveEventToCalendar(SofaEventsResponse.SofaEvent event) {
        long calID = getPrimaryCalendarId();
        if (calID == -1) {
            Toast.makeText(getContext(), "No calendar account found on device", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long startMillis = event.getStartTimestamp() * 1000;
            long endMillis = startMillis + (120 * 60 * 1000); // הנחה: שעתיים משחק

            String title = event.getHomeTeam().getName() + " vs " + event.getAwayTeam().getName();
            String description = "Match saved via Barca365 App";

            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, startMillis);
            values.put(CalendarContract.Events.DTEND, endMillis);
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, description);
            values.put(CalendarContract.Events.CALENDAR_ID, calID);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            Uri uri = requireContext().getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);

            if (uri != null) {
                Toast.makeText(getContext(), "Game saved to calendar successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving event", e);
            Toast.makeText(getContext(), "Error saving event", Toast.LENGTH_SHORT).show();
        }
    }

    private long getPrimaryCalendarId() {
        if (getContext() == null) return -1;

        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.IS_PRIMARY
        };

        // ניסיון ראשון: למצוא יומן שמסומן כ-Primary
        try (Cursor cursor = getContext().getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.VISIBLE + " = 1 AND " + CalendarContract.Calendars.IS_PRIMARY + "=1",
                null,
                CalendarContract.Calendars._ID + " ASC"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding primary calendar", e);
        }

        // ניסיון שני: לקחת את היומן הראשון שזמין
        try (Cursor cursor = getContext().getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.VISIBLE + " = 1",
                null,
                CalendarContract.Calendars._ID + " ASC"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding any calendar", e);
        }

        return -1;
    }

    // --- סוף לוגיקה של יומן ---

    private void fetchAllEvents() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
        errorTextView.setVisibility(View.GONE);

        masterEventList.clear();
        apiCallCounter.set(0);

        // 1. קריאת משחקי עבר (Last Events)
        apiService.getTeamLastEvents(Constants.BARCELONA_TEAM_ID, 0).enqueue(new Callback<SofaEventsResponse>() {
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
        apiService.getTeamNextEvents(Constants.BARCELONA_TEAM_ID, 0).enqueue(new Callback<SofaEventsResponse>() {
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
        if (apiCallCounter.incrementAndGet() == TOTAL_API_CALLS) {
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

        Collections.sort(filteredList, new Comparator<SofaEventsResponse.SofaEvent>() {
            @Override
            public int compare(SofaEventsResponse.SofaEvent o1, SofaEventsResponse.SofaEvent o2) {
                return Long.compare(o1.getStartTimestamp(), o2.getStartTimestamp());
            }
        });

        adapter.updateEvents(filteredList);
        recyclerView.setVisibility(View.VISIBLE);

        scrollToFirstFutureMatch(filteredList);
    }

    private void scrollToFirstFutureMatch(List<SofaEventsResponse.SofaEvent> list) {
        int targetIndex = -1;

        for (int i = 0; i < list.size(); i++) {
            SofaEventsResponse.SofaEvent event = list.get(i);
            if (event.getStatus() != null && event.getStatus().getCode() < 100) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex != -1) {
            ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(targetIndex, 0);
        } else {
            recyclerView.scrollToPosition(list.size() - 1);
        }
    }
}
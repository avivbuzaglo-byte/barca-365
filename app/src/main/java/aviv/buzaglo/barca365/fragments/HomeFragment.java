package aviv.buzaglo.barca365.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.UserSettingsActivity;
import aviv.buzaglo.barca365.adapters.InjuriesAdapter;
import aviv.buzaglo.barca365.models.LastViewedPlayer;
import aviv.buzaglo.barca365.models.PlayerProfile;
import aviv.buzaglo.barca365.models.SofaEventsResponse;
import aviv.buzaglo.barca365.models.SofaSquadResponse;
import aviv.buzaglo.barca365.network.SofaApiService;
import aviv.buzaglo.barca365.network.SofaRetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int BARCA_TEAM_ID = 2817;

    // --- משתני UI ---

    // Next Match
    private View nextMatchCard;
    private TextView tvTournament, tvDate, tvHomeName, tvAwayName, tvTime;
    private ImageView imgTournament, imgHome, imgAway;
    private ProgressBar progressNextMatch;

    // Injuries
    private RecyclerView recyclerInjuries;
    private InjuriesAdapter injuriesAdapter;
    // רשימה חיה שתתעדכן בזמן אמת ככל שנמצא פצועים
    private final List<SofaEventsResponse.MissingPlayer> liveInjuredList = new ArrayList<>();

    // Last Viewed Player
    private View lastViewedPlayerCard;
    private ImageView imgLastPlayer;
    private TextView tvLastPlayerName, tvLastPlayerPosition, tvLastPlayerNumber;
    private TextView tvNoPlayerViewed;

    // --- נתונים ---
    private SofaApiService apiService;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    ImageButton btnProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. אתחול
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        apiService = SofaRetrofitClient.getClient().create(SofaApiService.class);

        initViews(view);
        setupInjuriesRecycler();
        setupDateFormat();

        // 2. הפעלת הטעינות (במקביל)
        loadNextMatchData();
        loadInjuriesFromSquad(); // הלוגיקה החדשה!
        loadLastViewedPlayer();
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UserSettingsActivity.class);
            startActivity(intent);
        });
    }

    private void initViews(View view) {
        btnProfile = view.findViewById(R.id.imageButton);
        // Next Match
        nextMatchCard = view.findViewById(R.id.next_match_card);
        progressNextMatch = view.findViewById(R.id.progress_next_match);
        tvTournament = nextMatchCard.findViewById(R.id.text_tournament_name);
        tvDate = nextMatchCard.findViewById(R.id.text_match_date);
        tvHomeName = nextMatchCard.findViewById(R.id.text_home_name);
        tvAwayName = nextMatchCard.findViewById(R.id.text_away_name);
        tvTime = nextMatchCard.findViewById(R.id.text_time);
        imgTournament = nextMatchCard.findViewById(R.id.image_tournament_logo);
        imgHome = nextMatchCard.findViewById(R.id.image_home_logo);
        imgAway = nextMatchCard.findViewById(R.id.image_away_logo);
        nextMatchCard.findViewById(R.id.text_score).setVisibility(View.GONE);
        nextMatchCard.findViewById(R.id.text_match_status).setVisibility(View.GONE);

        // Injuries
        recyclerInjuries = view.findViewById(R.id.recycler_injuries);

        // Last Viewed
        lastViewedPlayerCard = view.findViewById(R.id.last_viewed_player_card);
        tvNoPlayerViewed = view.findViewById(R.id.tv_no_player_viewed);
        imgLastPlayer = lastViewedPlayerCard.findViewById(R.id.player_image);
        tvLastPlayerName = lastViewedPlayerCard.findViewById(R.id.player_name);
        tvLastPlayerPosition = lastViewedPlayerCard.findViewById(R.id.player_position);
        tvLastPlayerNumber = lastViewedPlayerCard.findViewById(R.id.player_number);
    }

    private void setupInjuriesRecycler() {
        recyclerInjuries.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerInjuries.setNestedScrollingEnabled(false);
        injuriesAdapter = new InjuriesAdapter(getContext(), new ArrayList<>());
        recyclerInjuries.setAdapter(injuriesAdapter);
    }

    private void setupDateFormat() {
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    // =========================================================================
    // לוגיקה 1: המשחק הבא
    // =========================================================================

    private void loadNextMatchData() {
        progressNextMatch.setVisibility(View.VISIBLE);
        nextMatchCard.setVisibility(View.INVISIBLE);

        apiService.getTeamNextEvents(BARCA_TEAM_ID, 0).enqueue(new Callback<SofaEventsResponse>() {
            @Override
            public void onResponse(Call<SofaEventsResponse> call, Response<SofaEventsResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().getEvents().isEmpty()) {
                    SofaEventsResponse.SofaEvent nextMatch = response.body().getEvents().get(0);
                    updateNextMatchUI(nextMatch);
                } else {
                    progressNextMatch.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<SofaEventsResponse> call, Throwable t) {
                if (!isAdded()) return;
                progressNextMatch.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load next match", t);
            }
        });
    }

    private void updateNextMatchUI(SofaEventsResponse.SofaEvent match) {
        if (getContext() == null) return;

        // טורניר
        if (match.getTournament() != null) {
            tvTournament.setText(match.getTournament().getName());
            if (match.getTournament().getUniqueTournament() != null) {
                String url = "https://api.sofascore.app/api/v1/unique-tournament/" +
                        match.getTournament().getUniqueTournament().getId() + "/image";
                loadImage(url, imgTournament);
            }
        }

        // זמן
        Date date = new Date(match.getStartTimestamp() * 1000);
        tvTime.setText(timeFormat.format(date));
        tvTime.setVisibility(View.VISIBLE);
        tvDate.setText(getSmartDate(match.getStartTimestamp()));
        // קבוצות
        if (match.getHomeTeam() != null) {
            tvHomeName.setText(match.getHomeTeam().getName());
            loadImage("https://api.sofascore.app/api/v1/team/" + match.getHomeTeam().getId() + "/image", imgHome);
        }
        if (match.getAwayTeam() != null) {
            tvAwayName.setText(match.getAwayTeam().getName());
            loadImage("https://api.sofascore.app/api/v1/team/" + match.getAwayTeam().getId() + "/image", imgAway);
        }

        nextMatchCard.setVisibility(View.VISIBLE);
        progressNextMatch.setVisibility(View.GONE);
    }

    // =========================================================================
    // לוגיקה 2: פצועים (BRUTE FORCE - סריקת כל השחקנים)
    // =========================================================================

    private void loadInjuriesFromSquad() {
        Log.d(TAG, "Starting Full Squad Injury Scan...");

        // ניקוי רשימה קודמת
        liveInjuredList.clear();
        if (injuriesAdapter != null) injuriesAdapter.updateList(liveInjuredList);

        // שלב 1: משיכת כל הסגל
        apiService.getSquadForInjuries(BARCA_TEAM_ID).enqueue(new Callback<SofaSquadResponse>() {
            @Override
            public void onResponse(Call<SofaSquadResponse> call, Response<SofaSquadResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getPlayers() != null) {
                    List<SofaSquadResponse.SquadPlayer> allPlayers = response.body().getPlayers();
                    Log.d(TAG, "Squad fetched. Scanning " + allPlayers.size() + " players for injuries...");

                    // שלב 2: לולאה על כל שחקן כדי לבדוק פרטים מלאים
                    for (SofaSquadResponse.SquadPlayer squadPlayer : allPlayers) {
                        fetchIndividualPlayerDetails(squadPlayer.getPlayer().getId());
                    }
                }
            }

            @Override
            public void onFailure(Call<SofaSquadResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load squad list", t);
            }
        });
    }

    private void fetchIndividualPlayerDetails(int playerId) {
        // שלב 3: בדיקת פרטים לכל שחקן
        apiService.getPlayerDetails(playerId).enqueue(new Callback<PlayerProfile>() {
            @Override
            public void onResponse(Call<PlayerProfile> call, Response<PlayerProfile> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    PlayerProfile.PlayerData data = response.body().getPlayer();

                    // שלב 4: בדיקה אם קיים אובייקט Injury (מצאנו את הזהב!)
                    if (data.getInjury() != null) {
                        Log.d(TAG, "INJURY FOUND: " + data.getName() + " - " + data.getInjury().getReason());

                        // המרה למודל של האדפטר
                        SofaEventsResponse.MissingPlayer injuryModel = new SofaEventsResponse.MissingPlayer(
                                data.getId(),
                                data.getName(),
                                data.getInjury().getReason(), // למשל "Knee Injury"
                                data.getInjury().getEndDateTimestamp() // תאריך חזרה
                        );

                        // הוספה לרשימה ועדכון התצוגה
                        liveInjuredList.add(injuryModel);

                        // עדכון ה-Adapter בזמן אמת (שחקנים יקפצו למסך אחד אחד)
                        if (injuriesAdapter != null) {
                            injuriesAdapter.updateList(liveInjuredList);
                            recyclerInjuries.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<PlayerProfile> call, Throwable t) {
                // התעלמות משגיאות בודדות כדי לא להציף את הלוג
            }
        });
    }


    // =========================================================================
    // לוגיקה 3: שחקן אחרון (Firebase)
    // =========================================================================

    private void loadLastViewedPlayer() {
        if (mAuth.getCurrentUser() == null) {
            showEmptyState();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    if (documentSnapshot.exists()) {
                        LastViewedPlayer player = documentSnapshot.get("lastViewed", LastViewedPlayer.class);
                        if (player != null) updateLastViewedUI(player);
                        else showEmptyState();
                    } else showEmptyState();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) showEmptyState();
                });
    }

    private void updateLastViewedUI(LastViewedPlayer player) {
        tvNoPlayerViewed.setVisibility(View.GONE);
        lastViewedPlayerCard.setVisibility(View.VISIBLE);

        tvLastPlayerName.setText(player.getName());
        tvLastPlayerPosition.setText(player.getPosition());

        // הצגת מספר חולצה אם קיים
        if (player.getShirtNumber() != 0) {
            tvLastPlayerNumber.setText(String.valueOf(player.getShirtNumber()));
        } else {
            tvLastPlayerNumber.setText("");
        }

        if (getContext() != null) {
            Glide.with(this)
                    .load(player.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgLastPlayer);
        }

        // --- הניווט לפרטי השחקן ---
        lastViewedPlayerCard.setOnClickListener(v -> {
            // 1. יצירת המופע של פרגמנט השחקן עם הנתונים
            PlayerDetailsFragment playerFragment = PlayerDetailsFragment.newInstance(
                    player.getId(),
                    player.getShirtNumber()
            );

            // 2. ביצוע המעבר (Transaction)
            getParentFragmentManager().beginTransaction()
                    // אפקטים של מעבר (אופציונלי אבל יפה)
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    // החלפת הפרגמנט הנוכחי בחדש
                    .replace(R.id.fragment_container, playerFragment) // <--- וודא שזה ה-ID הנכון ב-activity_main.xml שלך!
                    // הוספה להיסטוריה (כדי שכפתור חזור יעבוד)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void showEmptyState() {
        lastViewedPlayerCard.setVisibility(View.GONE);
        tvNoPlayerViewed.setVisibility(View.VISIBLE);
    }

    private void loadImage(String url, ImageView imageView) {
        if (getContext() == null) return;
        Glide.with(this).load(url).placeholder(R.drawable.ic_launcher_background).into(imageView);
    }
    private String getSmartDate(long timestamp) {
        Calendar matchCal = Calendar.getInstance();
        matchCal.setTimeInMillis(timestamp * 1000); // המרה משניות למילישניות

        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        if (isSameDay(matchCal, today)) {
            return "Today";
        } else if (isSameDay(matchCal, tomorrow)) {
            return "Tomorrow";
        } else {
            // אם זה לא היום ולא מחר, נשתמש בפורמט הרגיל (dd/MM) שהגדרנו למעלה
            return dateFormat.format(matchCal.getTime());
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
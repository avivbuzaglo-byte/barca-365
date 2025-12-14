package aviv.buzaglo.barca365.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // הוספתי את זה לשמירה בטוחה

import java.util.Calendar;
import java.util.HashMap; // הוספתי
import java.util.Locale;
import java.util.Map; // הוספתי

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.models.LastViewedPlayer;
import aviv.buzaglo.barca365.models.PlayerProfile;
import aviv.buzaglo.barca365.models.PlayerStatsResponse;
import aviv.buzaglo.barca365.network.SofaApiService;
import aviv.buzaglo.barca365.network.SofaRetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerDetailsFragment extends Fragment {

    private int playerId;
    private int shirtNumber;
    private ProgressBar progressBar;
    private ScrollView contentScrollView;

    // UI Elements
    private TextView tvName, tvNumber, tvPosition, tvAge, tvHeight, tvNation, tvValue, tvRating;
    private TextView tvApps, tvGoals, tvAssists;
    private TextView tvLabelGoals, tvLabelAssists;

    private ImageView iconGoals, iconAssists;
    private ImageView imgPlayer, btnBack;

    private boolean isGoalkeeper = false;

    private static final int TOURNAMENT_ID_LALIGA = 8;
    private static final int SEASON_ID_25_26 = 77559;

    public static PlayerDetailsFragment newInstance(int playerId, int shirtNumber) {
        PlayerDetailsFragment fragment = new PlayerDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("PLAYER_ID", playerId);
        args.putInt("PLAYER_NUMBER", shirtNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            playerId = getArguments().getInt("PLAYER_ID");
            shirtNumber = getArguments().getInt("PLAYER_NUMBER");
        }

        initViews(view);
        tvNumber.setText(String.valueOf(shirtNumber));

        fetchPlayerDetails();

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.player_detail_progress_bar);
        contentScrollView = view.findViewById(R.id.player_detail_content_scrollview);

        tvName = view.findViewById(R.id.player_detail_name);
        tvNumber = view.findViewById(R.id.player_detail_number);
        tvPosition = view.findViewById(R.id.player_detail_position);
        tvAge = view.findViewById(R.id.stat_age);
        tvHeight = view.findViewById(R.id.stat_height);
        tvNation = view.findViewById(R.id.stat_nationality);

        imgPlayer = view.findViewById(R.id.player_detail_image);
        btnBack = view.findViewById(R.id.back_arrow_icon);

        tvApps = view.findViewById(R.id.stat_appearances);
        tvGoals = view.findViewById(R.id.stat_goals);
        tvAssists = view.findViewById(R.id.stat_assists);

        tvLabelGoals = view.findViewById(R.id.stat_label_2);
        tvLabelAssists = view.findViewById(R.id.stat_label_3);

        iconGoals = view.findViewById(R.id.icon_stat_goals);
        iconAssists = view.findViewById(R.id.icon_stat_assists);

        tvValue = view.findViewById(R.id.market_value_amount);
        tvRating = view.findViewById(R.id.stat_rating);
    }

    private void fetchPlayerDetails() {
        progressBar.setVisibility(View.VISIBLE);

        SofaApiService apiService = SofaRetrofitClient.getClient().create(SofaApiService.class);
        Call<PlayerProfile> call = apiService.getPlayerDetails(playerId);

        call.enqueue(new Callback<PlayerProfile>() {
            @Override
            public void onResponse(Call<PlayerProfile> call, Response<PlayerProfile> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    contentScrollView.setVisibility(View.VISIBLE);
                    updateUI(response.body().getPlayer());

                    fetchPlayerStats();
                } else {
                    Toast.makeText(getContext(), "Error loading details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PlayerProfile> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(PlayerProfile.PlayerData data) {
        tvName.setText(data.getName());

        int age = calculateAge(data.getDateOfBirthTimestamp());
        tvAge.setText(String.valueOf(age));

        tvHeight.setText(data.getHeight() + " cm");

        if (data.getCountry() != null) {
            tvNation.setText(data.getCountry().getName());
        } else {
            tvNation.setText("-");
        }

        // --- חישוב העמדה ---
        String displayPosition;
        if (data.getPositionsDetailed() != null && !data.getPositionsDetailed().isEmpty()) {
            String firstPosition = data.getPositionsDetailed().get(0);
            displayPosition = getFullDetailedPosition(firstPosition);
            if (firstPosition.equalsIgnoreCase("GK")) {
                isGoalkeeper = true;
            }
        } else {
            displayPosition = convertPosition(data.getPosition());
            if (data.getPosition() != null && data.getPosition().equalsIgnoreCase("G")) {
                isGoalkeeper = true;
            }
        }
        tvPosition.setText(displayPosition);
        long value = data.getProposedMarketValueRaw().getValue();
        String currency = data.getProposedMarketValueRaw().getCurrency();
        tvValue.setText(formatMarketValue(value, currency));

        // טעינת תמונה
        String imageUrl = "https://api.sofascore.app/api/v1/player/" + playerId + "/image";
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(imgPlayer);

        // --- !!! הוספה קריטית: כאן אנחנו קוראים לשמירה ב-FIREBASE !!! ---
        saveLastViewedPlayerToFirebase(playerId, data.getName(), displayPosition, shirtNumber);
    }

    private void fetchPlayerStats() {
        SofaApiService apiService = SofaRetrofitClient.getClient().create(SofaApiService.class);

        Call<PlayerStatsResponse> call = apiService.getPlayerSeasonStats(
                playerId,
                TOURNAMENT_ID_LALIGA,
                SEASON_ID_25_26
        );

        call.enqueue(new Callback<PlayerStatsResponse>() {
            @Override
            public void onResponse(Call<PlayerStatsResponse> call, Response<PlayerStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatistics() != null) {
                    PlayerStatsResponse.StatsData stats = response.body().getStatistics();

                    tvApps.setText(String.valueOf(stats.getAppearances()));
                    double rating = stats.getRating();
                    String shortRating = String.format(Locale.US, "%.1f", rating);
                    tvRating.setText(shortRating);

                    if (isGoalkeeper) {
                        tvLabelGoals.setText("Saves");
                        tvLabelAssists.setText("Clean Sheets");
                        tvGoals.setText(String.valueOf(stats.getSaves()));
                        tvAssists.setText(String.valueOf(stats.getCleanSheets()));
                        iconGoals.setImageResource(R.drawable.ic_glove);
                        iconAssists.setImageResource(R.drawable.ic_shield);
                    } else {
                        tvLabelGoals.setText("Goals");
                        tvLabelAssists.setText("Assists");
                        tvGoals.setText(String.valueOf(stats.getGoals()));
                        tvAssists.setText(String.valueOf(stats.getAssists()));
                        iconGoals.setImageResource(R.drawable.ic_ball);
                        iconAssists.setImageResource(R.drawable.ic_boot);
                    }
                } else {
                    setZeroStats();
                }
            }

            @Override
            public void onFailure(Call<PlayerStatsResponse> call, Throwable t) {
                setZeroStats();
            }
        });
    }

    private void setZeroStats() {
        if (tvApps != null) {
            tvApps.setText("0");
            tvGoals.setText("0");
            tvAssists.setText("0");
        }
    }

    private int calculateAge(long timestamp) {
        Calendar dob = Calendar.getInstance();
        dob.setTimeInMillis(timestamp * 1000);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private String convertPosition(String shortPos){
        if(shortPos == null) return "-";
        switch (shortPos.toUpperCase(Locale.ROOT)){
            case "G": return "Goalkeeper";
            case "D": return "Defender";
            case "M": return "Midfielder";
            case "F": return "Forward";
            default: return shortPos;
        }
    }

    private String getFullDetailedPosition(String abbreviation) {
        if (abbreviation == null) return "";
        switch (abbreviation.toUpperCase()) {
            case "GK": return "Goalkeeper";
            case "DC": case "CB": return "Center Back";
            case "DL": case "LB": return "Left Back";
            case "DR": case "RB": return "Right Back";
            case "LWB": return "Left Wing Back";
            case "RWB": return "Right Wing Back";
            case "DM": case "CDM": return "Defensive Midfielder";
            case "MC": case "CM": return "Central Midfielder";
            case "AM": case "CAM": return "Attacking Midfielder";
            case "LM": return "Left Midfielder";
            case "RM": return "Right Midfielder";
            case "LW": return "Left Winger";
            case "RW": return "Right Winger";
            case "ST": return "Striker";
            case "CF": return "Center Forward";
            case "SS": return "Second Striker";
            default: return abbreviation;
        }
    }
    public static String formatMarketValue(long value, String currencyCode) {
        // 1. המרת קוד מטבע לסימן
        String symbol = currencyCode;
        if ("EUR".equalsIgnoreCase(currencyCode)) {
            symbol = "€";
        } else if ("USD".equalsIgnoreCase(currencyCode)) {
            symbol = "$";
        } else if ("GBP".equalsIgnoreCase(currencyCode)) {
            symbol = "£";
        }

        // 2. חישוב הערך (מיליונים או אלפים)
        if (value >= 1_000_000) {
            // חילוק ב-1.0 מיליון כדי לקבל תוצאה עשרונית (double)
            double inMillions = value / 1_000_000.0;

            // בדיקה אם המספר שלם (למשל 15.0) כדי להציג אותו בלי נקודה
            if (inMillions == (long) inMillions) {
                return String.format(Locale.US, "%s%dM", symbol, (long) inMillions);
            } else {
                // עיצוב עם ספרה אחת אחרי הנקודה (למשל 9.4M)
                return String.format(Locale.US, "%s%.1fM", symbol, inMillions);
            }
        } else if (value >= 1_000) {
            // מקרה גיבוי לשחקנים צעירים (K)
            double inThousands = value / 1_000.0;
            if (inThousands == (long) inThousands) {
                return String.format(Locale.US, "%s%dK", symbol, (long) inThousands);
            } else {
                return String.format(Locale.US, "%s%.1fK", symbol, inThousands);
            }
        }

        // ערך נמוך מאוד
        return symbol + value;
    }

    // --- הפונקציה המתוקנת והבטוחה לשמירה ב-Firebase ---
    private void saveLastViewedPlayerToFirebase(int id, String name, String position, int shirtNumber) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String imageUrl = "https://api.sofascore.app/api/v1/player/" + id + "/image";

        // יצירת מפה לעדכון (כדי לעדכן רק שדה אחד ולא לדרוס את כל היוזר)
        LastViewedPlayer lastViewed = new LastViewedPlayer(id, name, position, shirtNumber, imageUrl);
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastViewed", lastViewed);

        // שימוש ב-set עם Merge הוא הבטוח ביותר: אם המסמך לא קיים הוא ייווצר, ואם כן - יעודכן
        db.collection("Users").document(uid)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Player saved successfully: " + name))
                .addOnFailureListener(e -> Log.e("Firebase", "Error saving player", e));
    }
}
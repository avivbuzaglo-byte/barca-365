package aviv.buzaglo.barca365;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;

import aviv.buzaglo.barca365.models.SofaEventsResponse;
import aviv.buzaglo.barca365.models.SofaIncident;
import aviv.buzaglo.barca365.network.GeminiAiManager;
import aviv.buzaglo.barca365.network.SofaApiService;
import aviv.buzaglo.barca365.network.SofaRetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GeminiSummaryActivity extends AppCompatActivity {

    private TextView tvStory, tvPlayer, tvMoment, tvError;
    private ProgressBar progressBar;
    private LinearLayout contentLayout;
    private ImageButton btnBack;

    private int eventId;
    private String homeTeamName, awayTeamName, scoreStr, tournamentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini_summary);

        initViews();

        eventId = getIntent().getIntExtra("EVENT_ID", -1);
        homeTeamName = getIntent().getStringExtra("HOME_TEAM");
        awayTeamName = getIntent().getStringExtra("AWAY_TEAM");
        scoreStr = getIntent().getStringExtra("SCORE");
        tournamentName = getIntent().getStringExtra("TOURNAMENT");

        btnBack.setOnClickListener(v -> finish());

        if (eventId != -1) {
            fetchIncidentsAndAnalyze();
        } else {
            showError("Error loading match details");
        }
    }

    private void initViews() {
        tvStory = findViewById(R.id.summary_story_text);
        tvPlayer = findViewById(R.id.summary_player_text);
        tvMoment = findViewById(R.id.summary_moment_text);
        tvError = findViewById(R.id.summary_error_text);
        progressBar = findViewById(R.id.summary_progress_bar);
        contentLayout = findViewById(R.id.summary_content_layout);
        btnBack = findViewById(R.id.btn_back);
    }

    private void fetchIncidentsAndAnalyze() {
        showLoading();
        SofaApiService apiService = SofaRetrofitClient.getClient().create(SofaApiService.class);

        apiService.getEventIncidents(eventId).enqueue(new Callback<SofaEventsResponse.IncidentsResponse>() {
            @Override
            public void onResponse(Call<SofaEventsResponse.IncidentsResponse> call, Response<SofaEventsResponse.IncidentsResponse> response) {
                List<SofaIncident> incidents = null;
                if (response.isSuccessful() && response.body() != null) {
                    incidents = response.body().getIncidents();
                    if (incidents != null) {
                        Collections.reverse(incidents);
                    }
                }
                String prompt = buildPrompt(incidents);
                callGemini(prompt);
            }

            @Override
            public void onFailure(Call<SofaEventsResponse.IncidentsResponse> call, Throwable t) {
                Log.e("Incidents", "Error", t);
                String prompt = buildPrompt(null);
                callGemini(prompt);
            }
        });
    }

    private String buildPrompt(List<SofaIncident> incidents) {
        StringBuilder eventsBuilder = new StringBuilder();
        if (incidents != null) {
            for (SofaIncident incident : incidents) {
                if ("goal".equals(incident.getType())) {
                    String teamName = incident.isHome() ? homeTeamName : awayTeamName;
                    String playerName = (incident.getPlayer() != null) ? incident.getPlayer().getShortName() : "Goal";
                    eventsBuilder.append(String.format("- Goal (%d'): %s (%s)\n", incident.getTime(), playerName, teamName));
                }
                if ("card".equals(incident.getType()) && ("red".equals(incident.getIncidentClass()) || "yellowRed".equals(incident.getIncidentClass()))) {
                    String teamName = incident.isHome() ? homeTeamName : awayTeamName;
                    eventsBuilder.append(String.format("- Red Card (%d'): %s\n", incident.getTime(), teamName));
                }
            }
        }

        return String.format(
                "You are a professional football journalist. Analyze this match: %s vs %s (Score: %s) in %s.\n" +
                        "Key Events: \n%s\n\n" +
                        "Please respond in **English**.\n" +
                        "Use the EXACT following format headers:\n\n" +
                        "### Game Story\n" +
                        "[Write a short summary paragraph]\n\n" +
                        "### Key Player\n" +
                        "[Name the best player and explain why]\n\n" +
                        "### Turning Point\n" +
                        "[Identify the moment that changed the game]",
                homeTeamName, awayTeamName, scoreStr, tournamentName, eventsBuilder.toString()
        );
    }

    private void callGemini(String prompt) {
        GeminiAiManager.analyzeMatch(prompt, new GeminiAiManager.GeminiCallback() {
            @Override
            public void onSuccess(String analysis) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    parseAndDisplayResponse(analysis);
                });
            }

            @Override
            public void onError(Throwable t) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    showError("Error generating AI analysis");
                    Log.e("Gemini", "Error", t);
                });
            }
        });
    }

    private void parseAndDisplayResponse(String response) {
        try {
            // ניקוי מוקדם של הטקסט
            response = response.trim();

            String story = "No data available";
            String player = "No data available";
            String moment = "No data available";

            if (response.contains("Game Story")) {
                String[] parts = response.split("Game Story");
                if (parts.length > 1) {
                    String temp = parts[1];
                    if (temp.contains("Key Player")) {
                        story = temp.split("Key Player")[0];
                        String temp2 = temp.split("Key Player")[1];
                        if (temp2.contains("Turning Point")) {
                            player = temp2.split("Turning Point")[0];
                            moment = temp2.split("Turning Point")[1];
                        } else {
                            player = temp2;
                        }
                    } else {
                        story = temp;
                    }
                }
            } else {
                story = response;
            }

            // עדכון המסך עם פונקציית הניקוי המשופרת
            tvStory.setText(cleanText(story));
            tvPlayer.setText(cleanText(player));
            tvMoment.setText(cleanText(moment));

            showContent();

        } catch (Exception e) {
            Log.e("ParseError", "Error parsing Gemini response", e);
            tvStory.setText(cleanText(response));
            showContent();
        }
    }

    // --- הפונקציה המשופרת לניקוי הטקסט ---
    private String cleanText(String text) {
        if (text == null) return "";

        // 1. הסרת רווחים בהתחלה ובסוף
        text = text.trim();

        // 2. הסרת סימנים מיותרים מההתחלה (כמו נקודותיים, מקפים, סולמיות)
        while (text.startsWith(":") || text.startsWith("-") || text.startsWith("#") || text.startsWith("*")) {
            text = text.substring(1).trim();
        }

        // 3. הסרת סימנים מיותרים מהסוף (במיוחד סולמיות שציינת שמפריעות)
        while (text.endsWith("#") || text.endsWith("*") || text.endsWith("-")) {
            text = text.substring(0, text.length() - 1).trim();
        }

        return text;
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }
}
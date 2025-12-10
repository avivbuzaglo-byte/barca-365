package aviv.buzaglo.barca365.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageButton; // הוספתי לייבוא

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.models.SofaEventsResponse;
import aviv.buzaglo.barca365.network.GeminiAiManager; // השם החדש והנכון

public class FixturesAdapter extends RecyclerView.Adapter<FixturesAdapter.FixtureViewHolder> {

    private List<SofaEventsResponse.SofaEvent> events;
    private Context context;

    // פורמטים לתצוגת זמן ותאריך
    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat dateFormat;

    public FixturesAdapter(Context context, List<SofaEventsResponse.SofaEvent> events) {
        this.context = context;
        this.events = events;

        // הגדרת פורמטים
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

        // הגדרת אזור זמן (חשוב כדי שהשעות יהיו נכונות לישראל)
        timeFormat.setTimeZone(TimeZone.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    public void updateEvents(List<SofaEventsResponse.SofaEvent> newEvents) {
        this.events.clear();
        this.events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FixtureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fixture_item_layout, parent, false);
        return new FixtureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FixtureViewHolder holder, int position) {
        SofaEventsResponse.SofaEvent event = events.get(position);
        if (event == null) return;

        // 1. פרטי הטורניר (חלק עליון)
        if (event.getTournament() != null) {
            holder.tvTournamentName.setText(event.getTournament().getName());

            if (event.getTournament().getUniqueTournament() != null) {
                String url = "https://api.sofascore.app/api/v1/unique-tournament/" +
                        event.getTournament().getUniqueTournament().getId() + "/image";
                loadImage(url, holder.imgTournamentLogo);
            }
        }

        // 2. תאריך (בצד ימין למעלה) - שימוש בפונקציה חכמה להיום/מחר
        holder.tvDate.setText(getSmartDate(event.getStartTimestamp()));

        // 3. קבוצת בית
        if (event.getHomeTeam() != null) {
            holder.tvHomeName.setText(event.getHomeTeam().getName());
            String url = "https://api.sofascore.app/api/v1/team/" + event.getHomeTeam().getId() + "/image";
            loadImage(url, holder.imgHomeLogo);
        }

        // 4. קבוצת חוץ
        if (event.getAwayTeam() != null) {
            holder.tvAwayName.setText(event.getAwayTeam().getName());
            String url = "https://api.sofascore.app/api/v1/team/" + event.getAwayTeam().getId() + "/image";
            loadImage(url, holder.imgAwayLogo);
        }

        // 5. ניהול המרכז: תוצאה vs זמן vs סטטוס
        int statusCode = (event.getStatus() != null) ? event.getStatus().getCode() : 0;
        String statusDescription = (event.getStatus() != null) ? event.getStatus().getDescription() : "";

        // איפוס נראות (Reset Visibility)
        holder.tvScore.setVisibility(View.GONE);
        holder.tvTime.setVisibility(View.GONE);
        holder.tvStatus.setVisibility(View.GONE);

        if (statusCode == 0) {
            // --- משחק עתידי (Future) ---
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(timeFormat.format(new Date(event.getStartTimestamp() * 1000)));
            // מסתירים תוצאה וסטטוס

        } else if (statusCode == 100) {
            // --- משחק שהסתיים (Finished) ---
            holder.tvScore.setVisibility(View.VISIBLE);
            if (event.getHomeScore() != null && event.getAwayScore() != null) {
                String score = event.getHomeScore().getCurrent() + " - " + event.getAwayScore().getCurrent();
                holder.tvScore.setText(score);
            }

            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("FT"); // Full Time
            holder.tvStatus.setTextColor(Color.parseColor("#999999")); // אפור

        } else {
            // --- משחק חי (Live) ---
            holder.tvScore.setVisibility(View.VISIBLE);
            if (event.getHomeScore() != null && event.getAwayScore() != null) {
                String score = event.getHomeScore().getCurrent() + " - " + event.getAwayScore().getCurrent();
                holder.tvScore.setText(score);
            }

            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(statusDescription); // למשל "35'" או "HT"
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // ירוק בולט למשחק חי
        }

        // --- ניהול כפתור ה-AI ---
        // נציג אותו רק אם המשחק הסתיים (סטטוס "finished")
        if (event.getStatus() != null && event.getStatus().getCode() == 100) {
            holder.btnAiAnalysis.setVisibility(View.VISIBLE);
        }

        // לחיצה על כפתור ה-AI
        holder.btnAiAnalysis.setOnClickListener(v -> {
            showGeminiDialog(v.getContext(), event);
        });
    }

    // --- פונקציות עזר ---

    private String getSmartDate(long timestamp) {
        Calendar matchCal = Calendar.getInstance();
        matchCal.setTimeInMillis(timestamp * 1000);

        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        if (isSameDay(matchCal, today)) {
            return "Today";
        } else if (isSameDay(matchCal, tomorrow)) {
            return "Tomorrow";
        } else {
            return dateFormat.format(matchCal.getTime());
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void loadImage(String url, ImageView imageView) {
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imageView);
    }

    // --- פונקציה להצגת דיאלוג ה-AI ---
    private void showGeminiDialog(Context context, SofaEventsResponse.SofaEvent event) {
        // יצירת ה-Bottom Sheet
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_gemini_analysis, null);
        bottomSheetDialog.setContentView(view);

        TextView tvResult = view.findViewById(R.id.tv_gemini_result);
        ProgressBar progressBar = view.findViewById(R.id.gemini_progress_bar);

        bottomSheetDialog.show();

        // 3. בניית הפרומפט (השאלה לג'מיני)
        String homeTeam = (event.getHomeTeam() != null) ? event.getHomeTeam().getName() : "Home Team";
        String awayTeam = (event.getAwayTeam() != null) ? event.getAwayTeam().getName() : "Away Team";
        String score = "0-0";
        if (event.getHomeScore() != null && event.getAwayScore() != null) {
            score = event.getHomeScore().getCurrent() + "-" + event.getAwayScore().getCurrent();
        }

        String prompt = "Act as a football analyst. Analyze the match between " + homeTeam + " and " + awayTeam +
                ". The final score was " + score + ". " +
                "Write a short, exciting 3-sentence summary of the match in Hebrew. " +
                "Focus on the result and the teams.";

        // 4. שליחה לג'מיני (שימוש במחלקה GeminiAiManager)
        GeminiAiManager.analyzeMatch(prompt, new GeminiAiManager.GeminiCallback() {
            @Override
            public void onSuccess(String analysis) {
                // חזרה ל-Thread הראשי כדי לעדכן UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setVisibility(View.VISIBLE);
                    tvResult.setText(analysis);
                });
            }

            @Override
            public void onError(Throwable t) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setVisibility(View.VISIBLE);
                    tvResult.setText("שגיאה בניתוח המשחק. נסה שוב מאוחר יותר.");
                    Log.e("Gemini", "Error", t);
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    // --- ViewHolder ---
    public static class FixtureViewHolder extends RecyclerView.ViewHolder {

        ImageView imgTournamentLogo, imgHomeLogo, imgAwayLogo;
        TextView tvTournamentName, tvDate;
        TextView tvHomeName, tvAwayName;
        TextView tvScore, tvTime, tvStatus;

        // --- הוספנו את המשתנה הזה שהיה חסר ---
        View btnAiAnalysis;

        public FixtureViewHolder(@NonNull View itemView) {
            super(itemView);

            // קישור ל-XML
            imgTournamentLogo = itemView.findViewById(R.id.image_tournament_logo);
            tvTournamentName = itemView.findViewById(R.id.text_tournament_name);
            tvDate = itemView.findViewById(R.id.text_match_date);

            imgHomeLogo = itemView.findViewById(R.id.image_home_logo);
            tvHomeName = itemView.findViewById(R.id.text_home_name);

            imgAwayLogo = itemView.findViewById(R.id.image_away_logo);
            tvAwayName = itemView.findViewById(R.id.text_away_name);

            tvScore = itemView.findViewById(R.id.text_score);
            tvTime = itemView.findViewById(R.id.text_time);
            tvStatus = itemView.findViewById(R.id.text_match_status);

            // --- אתחול הכפתור ---
            btnAiAnalysis = itemView.findViewById(R.id.btn_ai_analysis);
        }
    }
}
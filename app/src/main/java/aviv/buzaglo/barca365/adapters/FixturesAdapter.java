package aviv.buzaglo.barca365.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import aviv.buzaglo.barca365.GeminiSummaryActivity;
import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.models.SofaEventsResponse;

public class FixturesAdapter extends RecyclerView.Adapter<FixturesAdapter.FixtureViewHolder> {

    // --- הוספה 1: הגדרת הממשק (Interface) ---
    public interface OnCalendarClickListener {
        void onCalendarClick(SofaEventsResponse.SofaEvent event);
    }

    private List<SofaEventsResponse.SofaEvent> events;
    private Context context;
    private OnCalendarClickListener calendarListener; // --- הוספה 2: משתנה לליסנר ---

    // פורמטים לתצוגת זמן ותאריך
    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat dateFormat;

    // --- הוספה 3: עדכון הבנאי לקבלת הליסנר ---
    public FixturesAdapter(Context context, List<SofaEventsResponse.SofaEvent> events, OnCalendarClickListener listener) {
        this.context = context;
        this.events = events;
        this.calendarListener = listener; // שמירת הליסנר

        // הגדרת פורמטים
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

        // הגדרת אזור זמן
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

        // ... (חלקים ללא שינוי: טורניר, תאריך, קבוצות) ...

        if (event.getTournament() != null) {
            holder.tvTournamentName.setText(event.getTournament().getName());
            if (event.getTournament().getUniqueTournament() != null) {
                String url = "https://api.sofascore.app/api/v1/unique-tournament/" +
                        event.getTournament().getUniqueTournament().getId() + "/image";
                loadImage(url, holder.imgTournamentLogo);
            }
        }

        holder.tvDate.setText(getSmartDate(event.getStartTimestamp()));

        if (event.getHomeTeam() != null) {
            holder.tvHomeName.setText(event.getHomeTeam().getName());
            String url = "https://api.sofascore.app/api/v1/team/" + event.getHomeTeam().getId() + "/image";
            loadImage(url, holder.imgHomeLogo);
        }

        if (event.getAwayTeam() != null) {
            holder.tvAwayName.setText(event.getAwayTeam().getName());
            String url = "https://api.sofascore.app/api/v1/team/" + event.getAwayTeam().getId() + "/image";
            loadImage(url, holder.imgAwayLogo);
        }

        // --- ניהול נראות התחלתי (חשוב כדי למנוע באגים בגלילה) ---
        holder.tvScore.setVisibility(View.GONE);
        holder.tvTime.setVisibility(View.GONE);
        holder.tvStatus.setVisibility(View.GONE);
        holder.btnCalender.setVisibility(View.GONE); // ברירת מחדל: מוסתר
        holder.btnAiAnalysis.setVisibility(View.GONE); // ברירת מחדל: מוסתר

        int statusCode = (event.getStatus() != null) ? event.getStatus().getCode() : 0;
        String statusDescription = (event.getStatus() != null) ? event.getStatus().getDescription() : "";

        if (statusCode == 0) {
            // --- משחק עתידי ---
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(timeFormat.format(new Date(event.getStartTimestamp() * 1000)));

            // הצגת כפתור יומן
            holder.btnCalender.setVisibility(View.VISIBLE);

            // --- הוספה 4: הגדרת הלחיצה על כפתור היומן ---
            holder.btnCalender.setOnClickListener(v -> {
                if (calendarListener != null) {
                    calendarListener.onCalendarClick(event);
                }
            });

        } else if (statusCode == 100) {
            // --- משחק שהסתיים ---
            holder.tvScore.setVisibility(View.VISIBLE);
            if (event.getHomeScore() != null && event.getAwayScore() != null) {
                String score = event.getHomeScore().getCurrent() + " - " + event.getAwayScore().getCurrent();
                holder.tvScore.setText(score);
            }
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("FT");
            holder.tvStatus.setTextColor(Color.parseColor("#999999"));
        } else {
            // --- משחק חי ---
            holder.tvScore.setVisibility(View.VISIBLE);
            if (event.getHomeScore() != null && event.getAwayScore() != null) {
                String score = event.getHomeScore().getCurrent() + " - " + event.getAwayScore().getCurrent();
                holder.tvScore.setText(score);
            }
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(statusDescription);
        }

        // --- ניהול כפתור ה-AI (ללא שינוי, רק וידוא שהוא בתוך הבלוק הנכון) ---
        boolean isFinished = (statusCode == 100) || "finished".equalsIgnoreCase(statusDescription);

        if (isFinished) {
            holder.btnAiAnalysis.setVisibility(View.VISIBLE);
            holder.btnAiAnalysis.setOnClickListener(v -> {
                // ... (הקוד הקיים שלך למעבר ל-AI) ...
                String homeName = (event.getHomeTeam() != null) ? event.getHomeTeam().getName() : "Home";
                String awayName = (event.getAwayTeam() != null) ? event.getAwayTeam().getName() : "Away";
                String scoreStr = "0-0";
                if (event.getHomeScore() != null && event.getAwayScore() != null) {
                    scoreStr = event.getHomeScore().getCurrent() + " - " + event.getAwayScore().getCurrent();
                }
                String tourName = (event.getTournament() != null) ? event.getTournament().getName() : "Tournament";

                Intent intent = new Intent(context, GeminiSummaryActivity.class);
                intent.putExtra("EVENT_ID", event.getId());
                intent.putExtra("HOME_TEAM", homeName);
                intent.putExtra("AWAY_TEAM", awayName);
                intent.putExtra("SCORE", scoreStr);
                intent.putExtra("TOURNAMENT", tourName);
                context.startActivity(intent);
            });
        }
    }

    // ... (פונקציות עזר ו-ViewHolder נשארים זהים) ...

    private String getSmartDate(long timestamp) {
        Calendar matchCal = Calendar.getInstance();
        matchCal.setTimeInMillis(timestamp * 1000);
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        if (isSameDay(matchCal, today)) return "Today";
        else if (isSameDay(matchCal, tomorrow)) return "Tomorrow";
        else return dateFormat.format(matchCal.getTime());
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

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class FixtureViewHolder extends RecyclerView.ViewHolder {
        ImageView imgTournamentLogo, imgHomeLogo, imgAwayLogo;
        TextView tvTournamentName, tvDate, tvHomeName, tvAwayName, tvScore, tvTime, tvStatus;
        View btnAiAnalysis, btnCalender;

        public FixtureViewHolder(@NonNull View itemView) {
            super(itemView);
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
            btnAiAnalysis = itemView.findViewById(R.id.btn_ai_analysis);
            btnCalender = itemView.findViewById(R.id.btn_calender);
        }
    }
}
package aviv.buzaglo.barca365.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.models.StandingsResponse;

public class StandingsAdapter extends RecyclerView.Adapter<StandingsAdapter.ViewHolder> {

    private Context context;
    private List<StandingsResponse.StandingRow> rows;

    public StandingsAdapter(Context context, List<StandingsResponse.StandingRow> rows) {
        this.context = context;
        this.rows = rows;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_standings_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StandingsResponse.StandingRow row = rows.get(position);

        // נתונים בסיסיים
        holder.tvPosition.setText(String.valueOf(row.getPosition()));
        holder.tvTeamName.setText(row.getTeam().getName());
        holder.tvMatches.setText(String.valueOf(row.getMatches()));
        holder.tvWins.setText(String.valueOf(row.getWins()));
        holder.tvDraws.setText(String.valueOf(row.getDraws()));
        holder.tvLosses.setText(String.valueOf(row.getLosses()));
        holder.tvPoints.setText(String.valueOf(row.getPoints()));

        // חישוב ועיצוב הפרש שערים
        int gd = row.getGoalDifference();
        String gdText = gd > 0 ? "+" + gd : String.valueOf(gd);
        holder.tvGD.setText(gdText);

        if (gd > 0) {
            holder.tvGD.setTextColor(Color.parseColor("#4ade80")); // ירוק
        } else if (gd < 0) {
            holder.tvGD.setTextColor(Color.parseColor("#ef4444")); // אדום
        } else {
            holder.tvGD.setTextColor(Color.parseColor("#888888")); // אפור
        }

        // טעינת לוגו
        String imageUrl = "https://api.sofascore.app/api/v1/team/" + row.getTeam().getId() + "/image";
        Glide.with(context).load(imageUrl).into(holder.imgLogo);

        // --- הדגשת ברצלונה ---
        // אם השם מכיל "Barcelona", נשנה את הרקע לטיפה יותר בהיר/כחלחל
        if (row.getTeam().getName().contains("Barcelona")) {
            holder.itemView.setBackgroundColor(Color.parseColor("#331E90FF")); // כחול שקוף מאוד
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPosition, tvTeamName, tvMatches, tvWins, tvDraws, tvLosses, tvGD, tvPoints;
        ImageView imgLogo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.position_text);
            imgLogo = itemView.findViewById(R.id.team_logo);
            tvTeamName = itemView.findViewById(R.id.team_name_text);
            tvMatches = itemView.findViewById(R.id.mp_text);
            tvWins = itemView.findViewById(R.id.wins_text);
            tvDraws = itemView.findViewById(R.id.draws_text);
            tvLosses = itemView.findViewById(R.id.losses_text);
            tvGD = itemView.findViewById(R.id.gd_text);
            tvPoints = itemView.findViewById(R.id.points_text);
        }
    }
}
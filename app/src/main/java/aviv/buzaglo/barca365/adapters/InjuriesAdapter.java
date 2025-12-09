package aviv.buzaglo.barca365.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.models.SofaEventsResponse; // שימוש במודל המאוחד

public class InjuriesAdapter extends RecyclerView.Adapter<InjuriesAdapter.InjuryViewHolder> {

    private Context context;
    private List<SofaEventsResponse.MissingPlayer> injuredList;
    private SimpleDateFormat dateFormat;

    public InjuriesAdapter(Context context, List<SofaEventsResponse.MissingPlayer> injuredList) {
        this.context = context;
        this.injuredList = injuredList;
        // פורמט תאריך קצר (למשל: Dec 20)
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
    }

    public void updateList(List<SofaEventsResponse.MissingPlayer> newList) {
        this.injuredList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InjuryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_injury, parent, false);
        return new InjuryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InjuryViewHolder holder, int position) {
        SofaEventsResponse.MissingPlayer missingPlayer = injuredList.get(position);
        if (missingPlayer == null) return;

        // 1. שם השחקן
        if (missingPlayer.getPlayer() != null) {
            holder.tvName.setText(missingPlayer.getPlayer().getName());

            // 2. תמונת השחקן
            String imageUrl = "https://api.sofascore.app/api/v1/player/" + missingPlayer.getPlayer().getId() + "/image";
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // מומלץ לשים תמונת ברירת מחדל של צללית
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgPlayer);
        }

        // 3. סוג הפציעה (Reason)
        // אם ה-API מחזיר סיבה (למשל "Muscle Injury") נציג אותה. אם לא, נכתוב סתם Injured.
        if (missingPlayer.getReason() != null && !missingPlayer.getReason().isEmpty()) {
            holder.tvInjuryType.setText(missingPlayer.getReason());
        } else {
            holder.tvInjuryType.setText("Injured");
        }

        // 4. תאריך חזרה משוער
        if (missingPlayer.getWillReturnTimestamp() != null) {
            // ה-Timestamp מגיע בשניות, צריך להכפיל ב-1000 למילישניות
            Date date = new Date(missingPlayer.getWillReturnTimestamp() * 1000);
            holder.tvReturnDate.setText("Exp. Return: " + dateFormat.format(date));
            holder.tvReturnDate.setVisibility(View.VISIBLE);
        } else {
            // אם אין תאריך צפי, נסתיר את הטקסט הזה
            holder.tvReturnDate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return injuredList != null ? injuredList.size() : 0;
    }

    // --- ViewHolder ---
    public static class InjuryViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPlayer;
        TextView tvName, tvInjuryType, tvReturnDate;

        public InjuryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlayer = itemView.findViewById(R.id.img_injured_player);
            tvName = itemView.findViewById(R.id.tv_injured_name);
            tvInjuryType = itemView.findViewById(R.id.tv_injury_type);
            tvReturnDate = itemView.findViewById(R.id.tv_return_date);
        }
    }
}
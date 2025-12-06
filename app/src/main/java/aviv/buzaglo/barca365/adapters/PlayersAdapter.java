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

import java.util.List;
import java.util.Locale;

import aviv.buzaglo.barca365.R;
import aviv.buzaglo.barca365.models.SquadPlayer;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder> {

    private Context context;
    private List<SquadPlayer> players;
    private OnItemClickListener listener; // 1. המשתנה למאזין

    // 2. הגדרת הממשק (Interface)
    public interface OnItemClickListener {
        void onItemClick(int playerId, int shirtNumber);
    }

    // 3. עדכון הבנאי לקבל את המאזין
    public PlayersAdapter(Context context, List<SquadPlayer> players, OnItemClickListener listener) {
        this.context = context;
        this.players = players;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.player_item, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        SquadPlayer squadPlayer = players.get(position);
        SquadPlayer.PlayerDetails details = squadPlayer.getPlayerDetails();

        if (details != null) {
            holder.tvName.setText(details.getName());
            holder.tvNumber.setText(String.valueOf(details.getJerseyNumber()));
            holder.tvPosition.setText(convertPosition(details.getPosition()));

            String imageUrl = "https://api.sofascore.app/api/v1/player/" + details.getId() + "/image";

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgPlayer);

            // 4. זיהוי הלחיצה והעברת ה-ID
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(details.getId(), details.getJerseyNumber());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return players != null ? players.size() : 0;
    }

    private String convertPosition(String shortPos){
        if(shortPos == null) return "Unknown";
        switch (shortPos.toUpperCase(Locale.ROOT)){
            case "G": return "Goalkeeper";
            case "D": return "Defender";
            case "M": return "Midfielder";
            case "F": return "Forward";
            default: return "Unknown";
        }
    }

    public static class PlayerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPosition, tvNumber;
        ImageView imgPlayer;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.player_name);
            tvPosition = itemView.findViewById(R.id.player_position);
            tvNumber = itemView.findViewById(R.id.player_number);
            imgPlayer = itemView.findViewById(R.id.player_image);
        }
    }
}
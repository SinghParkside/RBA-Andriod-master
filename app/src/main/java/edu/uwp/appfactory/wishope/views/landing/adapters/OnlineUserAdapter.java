package edu.uwp.appfactory.wishope.views.landing.adapters;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.landing.items.CoachProfileData;

/**
 * <h1>.</h1>
 * <p>
 * </p>
 *
 * @author Allen Rocha
 * @version 1.9.5
 * @since 15-08-2020
 */
public class OnlineUserAdapter extends RecyclerView.Adapter<OnlineUserAdapter.OnlineUserHolder> {
    private final Activity parentActivity;
    private OnItemClickListener itemClickListener;

    public OnlineUserAdapter(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        itemClickListener = listener;
    }

    @NonNull
    @Override
    public OnlineUserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.online_user_item, parent, false);
        return new OnlineUserHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull OnlineUserHolder holder, int position) {
        CoachProfileData user = UserConstants.ONLINE_USERS.get(position);
        final String fullName = toTileCase(user.getFirstName()) + " " + toTileCase(user.getLastName());
        holder.name.setText(fullName);
        if (!user.getProfilePic().isEmpty() && user.getImageBitmap() == null) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference profilePictureRef = storageRef.child(user.getProfilePic());
            final long ONE_MEGABYTE = 1024L * 1024L;
            profilePictureRef
                    .getBytes(ONE_MEGABYTE)
                    .addOnCompleteListener(imageBytesTask -> {
                        if (imageBytesTask.isSuccessful()) {
                            user.setImageBitmap(
                                    BitmapFactory.decodeByteArray(
                                            imageBytesTask.getResult(),
                                            0,
                                            imageBytesTask.getResult().length)
                            );
                            holder.coachProfilePicture.setImageBitmap(user.getImageBitmap());
                        } else {
                            Exception exception = imageBytesTask.getException();
                            Log.e(String.format("CoachProfileData: UID: %s Path: %s", user.getUid(), user.getProfilePic()), "onFailure: ", exception);
                        }
                    });
        } else if (user.getImageBitmap() != null)
            holder.coachProfilePicture.setImageBitmap(user.getImageBitmap());
        holder.bio.setText(user.getBio());
        holder.location.setText(toTileCase(user.getLocation()));
        holder.status.setText(toTileCase(user.getStatus()));
        switch (user.getStatus().toLowerCase()) {
            case "online":
                holder.status.setTextColor(ContextCompat.getColor(parentActivity, R.color.status_online));
                holder.statusIcon.setColorFilter(ContextCompat.getColor(parentActivity, R.color.status_online));
                break;
            case "busy":
                holder.status.setTextColor(ContextCompat.getColor(parentActivity, R.color.status_busy));
                holder.statusIcon.setColorFilter(ContextCompat.getColor(parentActivity, R.color.status_busy));
                break;
            default:
                holder.status.setTextColor(ContextCompat.getColor(parentActivity, R.color.status_offline));
                holder.statusIcon.setColorFilter(ContextCompat.getColor(parentActivity, R.color.status_offline));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return UserConstants.ONLINE_USERS.size();
    }

    private String toTileCase(String str) {
        if (str == null) {
            return null;
        }

        boolean space = true;
        StringBuilder builder = new StringBuilder(str);
        final int len = builder.length();

        for (int i = 0; i < len; ++i) {
            char c = builder.charAt(i);
            if (space) {
                if (!Character.isWhitespace(c)) {
                    // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, Character.toTitleCase(c));
                    space = false;
                }
            } else if (Character.isWhitespace(c)) {
                space = true;
            } else {
                builder.setCharAt(i, Character.toLowerCase(c));
            }
        }

        return builder.toString();
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class OnlineUserHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView status;
        private final TextView bio;
        private final TextView location;
        private final ImageView coachProfilePicture;
        private final ImageView statusIcon;

        public OnlineUserHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            itemView.setOnClickListener(view -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });
            name = itemView.findViewById(R.id.onlineUserNameTextView);
            status = itemView.findViewById(R.id.onlineUserStatusTextView);
            bio = itemView.findViewById(R.id.onlineBioTextView);
            location = itemView.findViewById(R.id.onlineLocationTextView);
            coachProfilePicture = itemView.findViewById(R.id.onlineUserProfilePicture);
            statusIcon = itemView.findViewById(R.id.onlineUserStatusIcon);
        }
    }
}

package edu.uwp.appfactory.wishope.notifications;

import android.app.NotificationChannel;
import android.content.Context;
import android.os.Build;
import android.view.View;

import androidx.core.app.NotificationCompat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <h1>Manages the notification and channel generations</h1>
 * This class creates the channels for the notifications and the creation of notifications.
 * <p>
 * REQUIRES ANDROID VERSION 26+ (OREO+)
 *
 * @author Allen Rocha
 * @version 1.0
 * @since 08-15-2020
 */
public class ManageNotifications {
    /**
     * Returns a notification channel based on the given arguments. This is required to send a
     * notification to an Android phone using Android >= 8.0. This function must be called on
     * application startup.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#Priority
     * for more information on setting the notification channel and priority.
     *
     * @param channelId   ID/Category for the notification.
     *                    This String should be stored in your stored in your String resources file.
     * @param channelName Displayed name in the notifications setting for this app.
     *                    This String should be stored in your stored in your String resources file.
     * @param priority    Intrusiveness of the notification. Once priority has been set, you must
     *                    reinstall the app whenever you wish to change a priority to an already
     *                    set channel.
     * @return If API >= 26 sends a created notification channel, otherwise returns null.
     * <p>
     * Calling sequence (Called in the main activity to create the defined notification channels):
     * NotificationChannel notificationChannel = ManageNotifications.createNotificationChannel(
     * "Channel ID 1",
     * "This is the channel name",
     * NotificationCompat.PRIORITY_MAX
     * );
     * To see any changes done to already created notification channels, you must reinstall the app.
     */
    @Nullable
    public static NotificationChannel createNotificationChannel(@NotNull String channelId,
                                                                @NotNull String channelName,
                                                                int priority) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            return new NotificationChannel(
                    channelId,
                    channelName,
                    priority
            );
        return null;
    }

    /**
     * Creates a notification with the given channel ID, icon, a title, message, priority and category.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#builder
     * for more information on setting the notification content (Icon, text, style, priority, etc.).
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#system-category
     * for information on setting categories.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#click
     * for information on adding tap actions to the notification.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#Actions
     * for information on adding action buttons.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#reply-action
     * for information on adding a `direct reply` action.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#progressbar
     * for information on adding a progress bar.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#Updating
     * for information on updating a notification after it has been sent out.
     * <p>
     * See https://developer.android.com/training/notify-user/build-notification#Removing
     * for information on removing a notification after it has been sent out.
     *
     * @param view      Current view, not having this in the parameters will crash it.
     * @param context   Current context.
     * @param channelID The channel channel assigned to this notification. This String should be
     *                  stored in your String resources file.
     * @param icon      Icon to display in the notification.
     * @param title     Title text for the notification.
     * @param message   Message to display in the notification.
     * @param priority  Intrusiveness of this notification.
     *                  Defined in NotificationCompat as:
     *                  NotificationCompat.PRIORITY_DEFAULT
     *                  NotificationCompat.PRIORITY_HIGH
     *                  NotificationCompat.PRIORITY_LOW
     *                  NotificationCompat.PRIORITY_MAX
     *                  NotificationCompat.PRIORITY_MIN
     * @return A notification builder.
     * <p>
     * Calling sequence
     * NotificationCompat.Builder notificationBuilder = Objects.requireNonNull(ManageNotifications.createNotification(
     * null, this, "CHANNEL ID 1", R.drawable.ic_alert_icon,
     * "NOTIFICATION TITLE", messageBody, MAX, CATEGORY_MESSAGE
     * )
     * )
     * .setAutoCancel(true)
     * .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
     * .setContentIntent(pendingIntent);
     * <p>
     * To show the notification:
     * NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
     * <p>
     * // notificationId is a unique int for each notification that you must define
     * notificationManager.notify(notificationId, notificationBuilder.build());
     * <p>
     * More information: https://developer.android.com/training/notify-user/build-notification#notify
     */
    public static NotificationCompat.Builder createNotification(@Nullable View view,
                                                                @NotNull Context context,
                                                                @NotNull String channelID, int icon,
                                                                @NotNull String title,
                                                                @NotNull String message, int priority) {
        return new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority);
    }
}

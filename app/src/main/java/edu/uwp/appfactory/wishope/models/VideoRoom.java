package edu.uwp.appfactory.wishope.models;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalDataTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.UserConstants;

import static androidx.constraintlayout.widget.Constraints.TAG;

/**
 * This class allows users to connect to rooms.
 * Its implementation is NOT complete.
 */

//TODO implement ability to call a target and make a room, or have someone call you

public class VideoRoom  {

    //Local tracks and related variables used for the sharing of media (voice, video)
    private final boolean enable;
    private final Context context;
    private final LocalAudioTrack localAudioTrack;//= LocalAudioTrack.create(context,enable);
    private final CameraCapturer cameraCapturer;
    private final LocalVideoTrack localVideoTrack;
    private final LocalDataTrack localDataTrack;
    private final VideoView primaryVideoView;
    private final VideoView secondaryVideoView;
    private final Room.Listener roomListener;
    //Hold lists of all tracks
    private final List<LocalAudioTrack> localAudioTracks;
    private final List<LocalVideoTrack> localVideoTracks;
    private final List<LocalDataTrack> localDataTracks;
    private Room room;
    //This string can be generated for a room by going to
    //Twilio programmable video console -> Tools -> Testing tools
    //If trying to connect 2 users, keep code will have to be compiled twice for 2
    //  different devices, so that they represent different users.
    //  Make sure the room being connected to in activity_video.xml.xml.kt matches the room used
    //  to generate this access token.
    //Extremely provisional! This method is for testing only. A backend should generate this for
    //  users based on who they're trying to contact.
    private String accessToken;
    private long endTime = Long.MIN_VALUE;
    private long startTime = Long.MIN_VALUE;

    /**
     * Used to set up tracks and views for a video call.
     *
     * @param context Needs the context from wherever views are hosted.
     *                For now it is activity_video.xml.xml.kt but should
     *                be changed in the future to have its own screen.
     */
    public VideoRoom(Context context) {

        this.enable = true;

        this.context = context;

        this.localAudioTrack = LocalAudioTrack.create(context, enable);

        this.cameraCapturer = new CameraCapturer(context,
                CameraCapturer.CameraSource.FRONT_CAMERA);

        this.localVideoTrack = LocalVideoTrack.create(context, enable, cameraCapturer);

        this.primaryVideoView = ((Activity) context).findViewById(R.id.primaryVideoView);//Get view from activity_video.xml

        this.secondaryVideoView = ((Activity) context).findViewById(R.id.secondaryVideoView);//Get view

        this.localDataTrack = LocalDataTrack.create(context);

        if (primaryVideoView != null) {
            primaryVideoView.setMirror(true);//mirror the image displayed
            localVideoTrack.addRenderer(primaryVideoView);//Add the local video to the renderer (preview of yourself)
        }
        localAudioTracks = new ArrayList<>();
        localVideoTracks = new ArrayList<>();
        localDataTracks = new ArrayList<>();

        localAudioTracks.add(localAudioTrack);
        localVideoTracks.add(localVideoTrack);
        localDataTracks.add(localDataTrack);

        roomListener = roomListener();
    }

    /**
     * Method returns the end time of the call. The end time
     * is recorded when either participant disconnects from the room
     * after they initially both connected.
     * @return
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Method returns the start time of the call. The start time
     * begins when both participants have connected to the room.
     * @return
     */
    public long getStartTime() {
        return startTime;
    }

    public void setAccessToken(String accessToken) {
        System.out.println(accessToken);
        this.accessToken = accessToken;
    }

    /**
     * Method with listeners for different room behaviors
     *
     * @return Room.Listener
     */
    private Room.Listener roomListener() {

        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                //Notifies the local participant when they have connected to the room
                Log.d(TAG, "Connected to " + room.getName());

                //Go off from primary view, go into little view
                if (primaryVideoView != null)
                    localVideoTrack.removeRenderer(primaryVideoView);
                if (secondaryVideoView != null)
                    localVideoTrack.addRenderer(secondaryVideoView);

                //If the room has a remote participants (AKA not you), set listeners on them
                //This is useful for when you join an existing room, because people
                //  already in this room automatically get listeners set for new joins,
                //  but people who join an existing room do not automatically get listeners
                //  for existing participants.
                if (room.getRemoteParticipants().size() >= 1) {
                    room.getRemoteParticipants().get(0).setListener(remoteParticipantListener());
                    if (startTime == Long.MIN_VALUE)
                        startTime = System.currentTimeMillis();
                }
                //Calls the remoteParticipantDeclinedCall() method to determine if the local participant
                // should be notified that the remote participant has either declined the call or failed
                // to join within one minute.
//                if (remoteParticipantDeclinedCall()) {
//                    Toast.makeText(
//                            context,
//                            String.format("%s has declined the call. Please hang up.", UserConstants.THEIR_DISPLAY_NAME),
//                            Toast.LENGTH_LONG
//                    ).show();
//                    hangUp();
//
//                }
            }

            @Override
            public void onConnectFailure(@NonNull Room room, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onReconnected(@NonNull Room room) {

            }

            @Override
            public void onDisconnected(@NonNull Room room, @Nullable TwilioException twilioException) {

                room.disconnect();
                Log.d("Disconnected: ", room.getLocalParticipant().getIdentity());
            }

            /**
             * This method notifies the local participant when the remote participant has connected
             * to their room
             * @param room
             * @param remoteParticipant
             */
            @Override
            public void onParticipantConnected(@NonNull Room room, @NonNull RemoteParticipant remoteParticipant) {
                Log.i("USER CONNECTED", remoteParticipant.getIdentity());

                Toast.makeText(
                        context,
                        String.format("%s has connected", UserConstants.THEIR_DISPLAY_NAME),
                        Toast.LENGTH_LONG
                ).show();
                System.out.println("PARTICIPANT CONNECTED: " + remoteParticipant.getIdentity());
                remoteParticipant.setListener(remoteParticipantListener());
                startTime = System.currentTimeMillis();
            }

            /**
             * This method notifies the remote or local participant if one has left the room
             * @param room
             * @param remoteParticipant
             */
            @Override
            public void onParticipantDisconnected(@NonNull Room room, @NonNull RemoteParticipant remoteParticipant) {
                Toast
                        .makeText(
                                context,
                                String.format("%s has disconnected", UserConstants.THEIR_DISPLAY_NAME),
                                Toast.LENGTH_LONG
                        )
                        .show();
                System.out.println("PARTICIPANT DISCONNECTED: " + remoteParticipant.getIdentity());
                room.disconnect();
            }

            @Override
            public void onRecordingStarted(@NonNull Room room) {

            }

            @Override
            public void onRecordingStopped(@NonNull Room room) {

            }
        };
    }

    /**
     * Method for handling remotely connected participants
     *
     * @return RemoteParticipant.Listener
     */
    private RemoteParticipant.Listener remoteParticipantListener() {
        return new RemoteParticipant.Listener() {
            @Override
            public void onAudioTrackPublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackUnpublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackSubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication, @NonNull RemoteAudioTrack remoteAudioTrack) {

            }

            @Override
            public void onAudioTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onAudioTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication, @NonNull RemoteAudioTrack remoteAudioTrack) {

            }

            @Override
            public void onVideoTrackPublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackUnpublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackSubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication, @NonNull RemoteVideoTrack remoteVideoTrack) {

                //When participant joins, remove preview of yourself from primary view and add them to it
                if (primaryVideoView != null) {
                    primaryVideoView.setMirror(false);
                    localVideoTrack.removeRenderer(primaryVideoView);
                    remoteVideoTrack.addRenderer(primaryVideoView);
                }
            }

            @Override
            public void onVideoTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onVideoTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication, @NonNull RemoteVideoTrack remoteVideoTrack) {

            }

            @Override
            public void onDataTrackPublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {

            }

            @Override
            public void onDataTrackUnpublished(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication) {

            }

            @Override
            public void onDataTrackSubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication, @NonNull RemoteDataTrack remoteDataTrack) {

            }

            @Override
            public void onDataTrackSubscriptionFailed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onDataTrackUnsubscribed(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteDataTrackPublication remoteDataTrackPublication, @NonNull RemoteDataTrack remoteDataTrack) {

            }

            @Override
            public void onAudioTrackEnabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackDisabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onVideoTrackEnabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackDisabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {

            }
        };
    }

    /**
     * Release no loner used data tracks and disconnect from room
     */
    public void hangUp() {
        if (room != null) {
            room.disconnect();
            if (startTime != Long.MIN_VALUE)
                endTime = System.currentTimeMillis();

            if (localAudioTrack != null) {
                localAudioTrack.release();
                localAudioTracks.clear();
            }
        }

        Toast.makeText(
                this.context,
                "Disconnected",
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Mutes the local participant in the room
     */
    public void muteLocalParticipant() {
        localAudioTrack.enable(false);
    }

    /**
     * Unmutes the local participant in the room
     */
    public void unmuteLocalParticipant() {
        localAudioTrack.enable(true);
    }

    /**
     * Disables the local participant's video
     */
    public void disableVideoLocalParticipant() {

        localVideoTrack.enable(false);
    }

    /**
     * Enables the local participant's video
     */
    public void enableVideoLocalParticipant() {
        localVideoTrack.enable(true);
    }

    /**
     * Returns the call duration
     *
     * @return
     */
    public long getCallDuration() {
        return endTime - startTime;
    }


    /**
     * Returns true or false depending if the remote participant answers the calls within
     * a minute. If false than it has a Toast pop up to notify the local participant otherwise
     * the call will be accepted when they join the room.
     * @return
     */
    public boolean remoteParticipantDeclinedCall() {
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        boolean declined = false;
        long waitTime = System.currentTimeMillis();
        long elapsedTime = 0;
        while(room.getRemoteParticipants().size() < 1 && !declined) {
            elapsedTime = System.currentTimeMillis() - waitTime;
            if(elapsedTime/1000 == 60) {
                System.out.println(elapsedTime /1000);
                declined = true;
            }
        }
        return declined;
    }

    /**
     * Connect to room and set up with your data tracks
     *
     * @param roomName Name of target room
     */
    public void connectToRoom(String roomName) {
        ConnectOptions connectOptions = new ConnectOptions.Builder(accessToken)
                .roomName(roomName)
                .audioTracks(localAudioTracks)
                .videoTracks(localVideoTracks)
                .dataTracks(localDataTracks)
                .build();
        room = Video.connect(context, connectOptions, roomListener);
        Log.d(TAG, "connectToRoom: " + roomName);
        Toast.makeText(
                this.context,
                "Connected",
                Toast.LENGTH_SHORT
        ).show();
    }
}


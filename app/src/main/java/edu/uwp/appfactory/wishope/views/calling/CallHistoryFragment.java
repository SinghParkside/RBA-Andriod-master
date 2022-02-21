package edu.uwp.appfactory.wishope.views.calling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import edu.uwp.appfactory.wishope.R;
import edu.uwp.appfactory.wishope.utils.UserConstants;
import edu.uwp.appfactory.wishope.views.calling.adapters.CallHistoryAdapter;

/**
 * <h1>View for the Call History Fragment</h1>
 * This class sets up the view for the `fragment_call_history` layout file. Contains the logic to
 * set load the populate the RecyclerView with the list of previous calls.
 *
 * @author Allen Rocha
 * @version 1.0
 * @since 03-01-2021
 */
public class CallHistoryFragment extends Fragment {
    private final String TAG = "CallHistoryFragment";
    private TextView noCallHistoryTextView;
    private RecyclerView callHistoryRecyclerView;
    private CallHistoryAdapter callHistoryAdapter;
    private RecyclerView.LayoutManager callHistoryLayoutManager;
    private final BroadcastReceiver callEndedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Here you can refresh your listview or other UI
            initializeRecyclerView();
            callHistoryAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Called");
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: Called");
        View view = inflater.inflate(R.layout.fragment_recoveree_call_history, container, false);
        // Initialize the UI elements.
        noCallHistoryTextView = view.findViewById(R.id.noCallHistoryTextView);
        callHistoryRecyclerView = view.findViewById(R.id.callHistoryRecyclerView);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // If the user has previous calls, then UI will update to hide the no call history text and
        // show their call history.
        if (!UserConstants.callHistory.isEmpty()) {
            initializeRecyclerView();
            noCallHistoryTextView.setVisibility(View.GONE);
            callHistoryRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This function initializes the RecyclerView Adapter, LinearLayoutManager, and sets them.
     */
    private void initializeRecyclerView() {
        callHistoryLayoutManager = new LinearLayoutManager(requireContext());
        callHistoryRecyclerView.setLayoutManager(callHistoryLayoutManager);
        callHistoryAdapter = new CallHistoryAdapter();
        callHistoryRecyclerView.setAdapter(callHistoryAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Create the broadcast receiver
        requireContext().registerReceiver(callEndedBroadcastReceiver, new IntentFilter("callEnded"));
        if (callHistoryAdapter != null)
            // Update RecyclerView
            callHistoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Remove the broadcast receiver
        requireContext().unregisterReceiver(callEndedBroadcastReceiver);
    }
}
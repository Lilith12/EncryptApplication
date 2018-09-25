package com.example.aleksandra.encryptapplication.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.example.aleksandra.encryptapplication.EncryptAppSocket;
import com.example.aleksandra.encryptapplication.R;
import com.example.aleksandra.encryptapplication.ServerStatsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class AvailableRoomsFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    Handler handler;

    private OnFragmentInteractionListener mListener;
    private Socket mSocket;
    private ListView rooms;
    private ArrayList<String> roomsList = new ArrayList<>();
    private String jsonObject;

    public AvailableRoomsFragment() {
        // Required empty public constructor
    }

    public static AvailableRoomsFragment newInstance(String param1, String param2) {
        AvailableRoomsFragment fragment = new AvailableRoomsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ((ServerStatsActivity) getActivity()).setActionBarTitle(R.string.group_fragment_title);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.action_bar_add_room, menu);

    }

    private void addRoom() {

        final EditText roomNameField = new EditText(getActivity());
        roomNameField.setTextColor(Color.rgb(255, 255, 255));
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(),
                        android.R.style.Theme_DeviceDefault_Dialog_NoActionBar));
        builder.setMessage(getString(R.string.unique_room_name))
                .setCancelable(true)
                .setView(roomNameField)
                .setPositiveButton(getString(R.string.ok), null)
                .setNegativeButton(getString(R.string.cancel),
                        (dialog, id) -> dialog.cancel()).create();
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Dialog d = dialog;
            String roomName = roomNameField.getText().toString();
            if (!roomsList.contains(roomName)) {
                mSocket.emit("create room", roomName);
                mSocket.emit("connect to room", roomName);
                d.dismiss();
            } else {
                roomNameField.setError(getString(R.string.room_exists));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {

            /** EDIT **/
            case R.id.add_room:
                addRoom();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_available_rooms, container, false);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        mListener = null;
        handler.removeCallbacks(run);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rooms = getView().findViewById(R.id.roomsListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                R.layout.room_item_list_layout, R.id.label, roomsList);
        rooms.setAdapter(adapter);

        rooms.setOnItemClickListener((adapter1, v, position, arg3) -> {
            String value = (String) adapter1.getItemAtPosition(position);
            mSocket.emit("connect to room", value);
            Bundle bundle = new Bundle();
            bundle.putString("roomName", value);

            GroupChatFragment groupChat = new GroupChatFragment();
            groupChat.setArguments(bundle);
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, groupChat);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            // assuming string and if you want to get the value on click of list item
            // do what you intend to do on click of listview row
        });
        EncryptAppSocket app = (EncryptAppSocket) getActivity().getApplication();
        mSocket = app.getSocket();
        mSocket.on("roomsArray", handleRoomsArray);
        handler = new Handler();
        run.run();
    }

    Runnable run = new Runnable() {
        @Override
        public void run() {
            getActivity().runOnUiThread(() -> {
                mSocket.emit("get rooms");
                ((BaseAdapter) rooms.getAdapter()).notifyDataSetChanged();
                handler.postDelayed(this, 5000);
            });
        }
    };

    private Emitter.Listener handleRoomsArray = (Object... args) -> {
        JSONObject data = (JSONObject) args[0];

        try {
            jsonObject = data.getString("rooms");
            if (!jsonObject.equals("{}")) {
                JSONArray jsonArray = new JSONArray(jsonObject);
                synchronized (roomsList) {
                    roomsList.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        roomsList.add(jsonArray.getString(i));
                    }
                }
            }
        } catch (JSONException e) {
            return;
        }
    };
}

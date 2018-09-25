package com.example.aleksandra.encryptapplication.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import com.example.aleksandra.encryptapplication.EncryptAppSocket;
import com.example.aleksandra.encryptapplication.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import io.socket.emitter.Emitter;
import lombok.Data;

public class GroupChatFragment extends AbstractChatFragment {
    public static final String DISCONNECT_FROM_ROOM = "disconnect from room";
    public static final String USER_DISCONNECTED = "user disconnected";
    private OnFragmentInteractionListener mListener;
    private String toUser;


    public GroupChatFragment() {
        isGroupMessage = true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.action_bar_disconnect, menu);
        inflater.inflate(R.menu.action_bar_add_image, menu);
    }

    private void addWarning(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar));
        builder.setMessage(getString(R.string.exit_room))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    mSocket.emit(DISCONNECT_FROM_ROOM, getTarget());
                    mSocket.emit(USER_DISCONNECTED);
                    AvailableRoomsFragment rooms = new AvailableRoomsFragment();
                    android.support.v4.app.FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.fragment_container, rooms);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                })
                .setNegativeButton(getString(R.string.no), (dialog, id) -> dialog.cancel()).create();
        final AlertDialog dialog = builder.create();
        dialog.show();
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
    public void onDestroyView(){
        super.onDestroyView();
        mListener = null;
        removeHandlers();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    void getDataAndAddTyping(JSONObject data) throws JSONException {
        String username = data.getString("username");
        String fromRoomName = data.getString("roomName");
        if (getTarget().equals(fromRoomName)) {
            addTyping(username);
        }
    }

    private Emitter.Listener onConnect = args -> {
        JSONObject data = (JSONObject) args[0];
        final String username;
        try {
            username = data.getString("username");
        } catch (JSONException e) {
            return;
        }
        getActivity().runOnUiThread(() -> Toast.makeText(getActivity().getApplicationContext(),
                getString(R.string.user_connected_to_room, username), Toast.LENGTH_LONG).show());
    };

    private Emitter.Listener onDisconnect = args -> {
        JSONObject data = (JSONObject) args[0];
        final String username;
        try {
            username = data.getString("username");
        } catch (JSONException e) {
            return;
        }
        getActivity().runOnUiThread(() -> Toast.makeText(getActivity().getApplicationContext(),
                getString(R.string.user_disconnected_from_room, username),
                Toast.LENGTH_LONG).show());
    };

    void removeHandlers(){
        mSocket.off("groupMessage");
        mSocket.off("groupTyping");
        mSocket.off("stopGroupTyping");
        mSocket.off("user connected");
        mSocket.off("user disconnected");
        mSocket.off("usersPublicKeys");
    }

    @Override
    String getTarget() {
        return getArguments().getString("roomName");
    }

    @Override
    String getTypingTimeoutName() {
        return "stop typing to group";
    }

    @Override
    String getTypingTimeoutParameters() {
        return getTarget();
    }

    @Override
    String getSendName() {
        return "new group message";
    }

    @Override
    boolean isNameNull() {
        return false;
    }

    @Override
    String getTypingName() {
        return "typing to group";
    }

    @Override
    String getTypingParameters() {
        return getTarget();
    }

    @Override
    void onDisconnect() {
        addWarning();
    }

    @Override
    int getLayoutResource() {
        return R.layout.fragment_group_chat;
    }

    @Override
    void registerSockets() {
        mSocket.on("groupMessage", handleMessage);
        mSocket.on("groupTyping", onTyping);
        mSocket.on("stopGroupTyping", onStopTyping);
        mSocket.on("user connected", onConnect);
        mSocket.on("user disconnected", onDisconnect);
        mSocket.on("usersPublicKeys", this.encryptMessage);
    }

    @Override
    void encodeMessage() {
        mSocket.emit("request users public keys", getTarget());
    }

    @Override
    void emitMessage(String encryptedMessage, String uniqueID) {
        mSocket.emit(getSendName(), toUser, getTarget(), encryptedMessage, wasEdited, position, uniqueID, isImage);
    }

    // it overrides parent listener
    protected Emitter.Listener encryptMessage = args ->
    {
        JSONArray array = (JSONArray) args[0];
        JSONObject object = null;
        List<JsonUser> users = new ArrayList<>();
        for (int i = 0; i < array.length(); i++){
            try {
                object = array.getJSONObject(i);
                users.add(new JsonUser(object.getString("username"), object.getString("key")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for(JsonUser user : users){
            EncryptAppSocket socket = (EncryptAppSocket) getActivity().getApplication();
            if(!socket.getUsername().equals(user.getUsername())){
                toUser = user.getUsername();
                try {
                    encodeWithUserPublicKey(user.getPublicKey());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Data
    private class JsonUser {
        private String username;
        private String publicKey;

        JsonUser(String username, String publicKey){
            this.username = username;
            this.publicKey = publicKey;
        }
    }
}

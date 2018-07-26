package com.example.aleksandra.encryptapplication.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.aleksandra.encryptapplication.EncryptAppSocket;
import com.example.aleksandra.encryptapplication.FileUtils;
import com.example.aleksandra.encryptapplication.R;
import com.example.aleksandra.encryptapplication.model.message.view.Message;
import com.example.aleksandra.encryptapplication.model.message.view.MessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GroupChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GroupChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupChatFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private final String socketPwMessage = "pwMessage";
    private RecyclerView messageView;
    private List<Message> messageList = new ArrayList<Message>();
    private RecyclerView.Adapter adapter;
    private static final int TYPING_TIMER_LENGTH = 600;
    private boolean isTyping = false;
    private Handler typingHandler = new Handler();
    private String roomName;
    private String fromUser;
    private EditText messageField;
    private Socket mSocket;
    private boolean isInBackground;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public GroupChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GroupChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GroupChatFragment newInstance(String param1, String param2) {
        GroupChatFragment fragment = new GroupChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            roomName = bundle.getString("roomName");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.action_bar_disconnect, menu);
        inflater.inflate(R.menu.action_bar_add_image, menu);
    }

    @Override
    public void onStart(){
        super.onStart();
        isInBackground = false;
    }

    @Override
    public void onStop(){
        super.onStop();
        if(!getActivity().isChangingConfigurations()){
            isInBackground = true;
        }
    }

    private void addWarning(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar));
        builder.setMessage(getString(R.string.exit_room))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    mSocket.emit("disconnect from room", roomName);
                    mSocket.emit("user disconnected");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {

            /** EDIT **/
            case R.id.disconnect:
                addWarning();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group_chat, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
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
    public void onAttach(Activity context) {
        super.onAttach(context);
        adapter = new MessageAdapter(getActivity(), messageList);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        messageField = getActivity().findViewById(R.id.messageField);
        messageField.setTextColor(Color.rgb(255, 255, 255));
        Button sendMessageButton = getActivity().findViewById(R.id.sendButton);

        final EncryptAppSocket app = (EncryptAppSocket) getActivity().getApplication();
        mSocket = app.getSocket();
        fromUser = app.getUsername();
        mSocket.on("groupMessage", handleGroupMessage);
        mSocket.on("groupTyping", onTyping);
        mSocket.on("stopGroupTyping", onStopTyping);
        mSocket.on("user connected", onConnect);
        mSocket.on("user disconnected", onDisconnect);
        messageView = getActivity().findViewById(R.id.messages);
        messageView.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageView.setAdapter(adapter);

        setMessageFieldListeners();
        loadPreviousMessages();
        sendMessageButton.setOnClickListener(v -> attemptSend());
    }

    private void setMessageFieldListeners() {
        messageField.setOnEditorActionListener((v, id, event) -> {
            if (id == R.id.send || id == EditorInfo.IME_NULL) {
                attemptSend();
                return true;
            }
            return false;
        });

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == roomName) return;
                if (!mSocket.connected()) return;

                if (!isTyping) {
                    isTyping = true;
                    mSocket.emit("typing to group", roomName);
                }

                typingHandler.removeCallbacks(onTypingTimeout);
                typingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void addLog(String message) {
        messageList.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }
    private void addMessage(String username, String message) {
        messageList.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String username) {
        messageList.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username).build());
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String username) {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message message = messageList.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                messageList.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend() {
        if (null == roomName) return;
        if (!mSocket.connected()) return;

        isTyping = false;

        String message = messageField.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageView.requestFocus();
            return;
        }

        FileUtils.saveMessageToTempFile(FileUtils.createEmptyTempFile(getContext(), roomName), fromUser, message);
        messageField.setText("");
        addMessage(fromUser, message);

        // perform the sending message attempt.
        mSocket.emit("new group message", roomName, message);
    }

    private void scrollToBottom() {
        messageView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private Emitter.Listener handleGroupMessage = args -> getActivity().runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        String username;
        String message;
        try {
            username = data.getString("username");
            message = data.getString("message");
        } catch (JSONException e) {
            return;
        }

        removeTyping(username);
        addMessage(username, message);
    });

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

    private Emitter.Listener onTyping = (Object... args) ->
            getActivity().runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                String username;
                String fromRoomName;
                try {
                    username = data.getString("username");
                    fromRoomName = data.getString("roomName");
                    if (roomName.equals(fromRoomName)) {
                        addTyping(username);
                    }
                } catch (JSONException e) {
                    return;
                }
            });

    private Emitter.Listener onStopTyping = args -> getActivity().runOnUiThread(() -> {
        JSONObject data = (JSONObject) args[0];
        String username;
        try {
            username = data.getString("username");
        } catch (JSONException e) {
            return;
        }
        removeTyping(username);
    });

    private Runnable onTypingTimeout = () -> {
        if (!isTyping) return;

        isTyping = false;
        mSocket.emit("stop typing to group", roomName);
    };

    private void readFromFile(File file){
        String username;
        try(FileReader fileReader = new FileReader(file)) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while((username = bufferedReader.readLine()) != null){
                addMessage(username, bufferedReader.readLine());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void loadPreviousMessages() {
        File outputTempFile = new File(getContext().getCacheDir().getPath() + File.separatorChar + FileUtils.encodeFileName(roomName));
        if(outputTempFile.isFile()){
            readFromFile(outputTempFile);
        }
    }

    private void removeHandlers(){
        mSocket.off("groupMessage");
        mSocket.off("groupTyping");
        mSocket.off("stopGroupTyping");
        mSocket.off("user connected");
        mSocket.off("user disconnected");
    }
}

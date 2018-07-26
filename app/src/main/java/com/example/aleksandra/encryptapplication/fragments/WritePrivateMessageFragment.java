package com.example.aleksandra.encryptapplication.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.aleksandra.encryptapplication.EncryptAppSocket;
import com.example.aleksandra.encryptapplication.R;
import com.example.aleksandra.encryptapplication.ItemClickSupport;
import com.example.aleksandra.encryptapplication.handlers.DatabaseHandler;
import com.example.aleksandra.encryptapplication.handlers.TableMessagesUtils;
import com.example.aleksandra.encryptapplication.model.message.view.Message;
import com.example.aleksandra.encryptapplication.model.message.view.MessageAdapter;
import com.example.aleksandra.encryptapplication.model.message.websocket.PrivateMessageModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class WritePrivateMessageFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String USERNAME = "username";
    private final String socketPwMessage = "pwMessage";
    private RecyclerView messageView;
    private List<Message> messageList = new ArrayList<>();
    private RecyclerView.Adapter adapter;
    private static final int TYPING_TIMER_LENGTH = 600;
    private boolean isTyping = false;
    private Handler typingHandler = new Handler();
    private String toUser;
    private String fromUser;
    private EditText messageField;
    private Socket mSocket;
    private DatabaseHandler db;
    private boolean isInBackground;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public WritePrivateMessageFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static WritePrivateMessageFragment newInstance(String param1, String param2) {
        WritePrivateMessageFragment fragment = new WritePrivateMessageFragment();
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
            toUser = bundle.getString(USERNAME);
        }
        db = DatabaseHandler.getDatabaseHandler(getContext());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {

            /** EDIT **/
            case R.id.disconnect:
                Log.e("aa", "onOptionsItemSelected: ");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_write_message, container, false);
    }

    private String encodeFileName(String username) {
        try {
            byte[] data = username.getBytes("UTF-8");
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
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
    public void onDestroyView() {
        super.onDestroyView();
        mListener = null;
        removeHandlers();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        mSocket.on("pwMessage", handlePrivateMessage);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        messageView = getActivity().findViewById(R.id.messages);
        messageView.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageView.setAdapter(adapter);
        setMessageFieldListeners(sendMessageButton);
        setTextFieldMessageListeners();
        loadPreviousMessagesFromDb();
    }

    private void loadPreviousMessagesFromDb() {
        List<Message> messages = db.getMessagesFromConversation(toUser);
        if (!messages.isEmpty()) {
            for (Message message : messages) {
                addMessage(message, false, null);
            }
        }
    }

    private void setTextFieldMessageListeners() {
        messageView.addOnItemTouchListener(new ItemClickSupport(getContext(),
                messageView, new ItemClickSupport.RecyclerViewClickListener() {

            @Override
            public void onItemClicked(int position, View v) {
                Message selectedMessage = messageList.get(position);
                db.deleteMessage(selectedMessage);
                messageList.remove(position);
                adapter.notifyItemRemoved(position);
            }

            @Override
            public void onItemLongClicked(int position, View v) {
                Message message = messageList.get(position);
                if (message.getUsername().equals(fromUser)) {
                    final boolean edited = message.isEdited();
                    final int positionOnList = position;
                    message = setMessageEdited(position, message, !edited);
                    messageField.setText(edited ? message.getMessage() : "");
                    messageField.setOnEditorActionListener(
                            getEnterListener(edited, positionOnList));
                    Button sendMessageButton = getActivity().findViewById(R.id.sendButton);
                    sendMessageButton.setOnClickListener(v1 -> attemptSend(edited, positionOnList));
                }
            }
        }));
    }


    private Message setMessageEdited(int position, Message message, boolean isEdited) {
        Message.Builder messageBuilder = new Message.Builder(Message.TYPE_MESSAGE);
        Message messageEdited = messageBuilder.id(message.getId()).username(fromUser).message(
                message.getMessage()).isEdited(isEdited).codeMessage(
                message.getCodeMessage()).build();
        messageList.set(position, messageEdited);
        return message;
    }

    private void setMessageFieldListeners(Button sendMessageButton) {
        messageField.setOnEditorActionListener(getEnterListener(false, null));

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == toUser) return;
                if (!mSocket.connected()) return;

                if (!isTyping) {
                    isTyping = true;
                    mSocket.emit("typing PW", toUser);
                }

                typingHandler.removeCallbacks(onTypingTimeout);
                typingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sendMessageButton.setOnClickListener(v -> attemptSend(false, null));
    }

    @NonNull
    private TextView.OnEditorActionListener getEnterListener(final boolean wasEdited,
            final Integer position) {
        return (TextView v, int id, KeyEvent event) -> {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend(wasEdited, position);
                    return true;
                }
                return false;
            };
    }

    private void addLog(String message) {
        messageList.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void addMessage(Message message, boolean wasEdited, Integer position) {
        if (wasEdited && position != null) {
            messageList.set(position, message);
            adapter.notifyItemChanged(position);
        } else {
            messageList.add(message);
            adapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();
        }
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
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(
                    username)) {
                messageList.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend(boolean wasEdited, Integer position) {
        if (null == toUser) return;
        if (!mSocket.connected()) return;

        isTyping = false;

        String message = messageField.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageView.requestFocus();
            return;
        }

//        FileUtils.saveMessageToTempFile(FileUtils.createEmptyTempFile(getContext(), toUser),
// fromUser, message);
        messageField.setText("");
        Message.Builder messageBuilder = new Message.Builder(Message.TYPE_MESSAGE)
                .username(fromUser).message(message);
        String uniqueID;
        long id = -1;
        if (!wasEdited) {
            uniqueID = UUID.randomUUID().toString();
            messageBuilder.codeMessage(uniqueID);
            id = db.addRow(new PrivateMessageModel(fromUser, message, false, position, uniqueID),
                    toUser);
        } else {
            uniqueID = messageList.get(position).getCodeMessage();
        }
        addMessage(messageBuilder.id(id).codeMessage(uniqueID).build(), wasEdited, position);

        // perform the sending message attempt.
        mSocket.emit("new message", toUser, message, wasEdited, position, uniqueID);
    }

    private boolean checkIfActiveConversationWindow(String messageFromUser) {
        return messageFromUser.equals(toUser) && !isInBackground;
    }

    private void scrollToBottom() {
        messageView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private Emitter.Listener handlePrivateMessage = (Object... args) ->
            getActivity().runOnUiThread(() -> {
                try {
                    PrivateMessageModel model = new PrivateMessageModel((JSONObject) args[0]);
                    removeTyping(model.getUsername());
                    boolean wasEditedBool = model.isWasEdited();
                    Integer positionInt = model.getPosition();
                    String senderUsername = model.getUsername();
                    Message.Builder messageBuilder = new Message.Builder(
                            Message.TYPE_MESSAGE).message(model.getMessage()).username(
                            senderUsername).isEdited(wasEditedBool);
                    if (checkIfActiveConversationWindow(senderUsername)) {
                        long id = TableMessagesUtils.addToDatabase(model, db);
                        addMessage(messageBuilder.id(id).build(), wasEditedBool, positionInt);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

    private Emitter.Listener onTyping = (Object... args) ->
            getActivity().runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                String username;
                try {
                    username = data.getString("username");
                    if (toUser.equals(username)) {
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
        mSocket.emit("stop typing PW", toUser);
    };

    private void removeHandlers() {
        mSocket.off("pwMessage");
        mSocket.off("typing");
        mSocket.off("stop typing");
    }
}

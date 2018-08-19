package com.example.aleksandra.encryptapplication.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
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
import com.example.aleksandra.encryptapplication.ItemClickSupport;
import com.example.aleksandra.encryptapplication.R;
import com.example.aleksandra.encryptapplication.ServerStatsActivity;
import com.example.aleksandra.encryptapplication.encrypt.RSA;
import com.example.aleksandra.encryptapplication.handlers.DatabaseHandler;
import com.example.aleksandra.encryptapplication.handlers.TableMessagesUtils;
import com.example.aleksandra.encryptapplication.model.message.view.Message;
import com.example.aleksandra.encryptapplication.model.message.view.MessageAdapter;
import com.example.aleksandra.encryptapplication.model.message.websocket.MessageModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public abstract class AbstractChatFragment extends Fragment {
    private RecyclerView messageView;
    protected List<Message> messageList = new ArrayList<>();
    protected RecyclerView.Adapter adapter;
    private static final int TYPING_TIMER_LENGTH = 600;
    private boolean isTyping = false;
    private Handler typingHandler = new Handler();
    private String fromUser;
    private EditText messageField;
    protected Socket mSocket;
    protected DatabaseHandler db;
    private boolean isInBackground;
    protected boolean isGroupMessage;
    protected String sendMessage;
    protected boolean wasEdited;
    protected Integer position;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new MessageAdapter(getActivity(), messageList);
        setHasOptionsMenu(true);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            getTarget();
        }
        ((ServerStatsActivity) getActivity()).setActionBarTitle(getTarget());
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
    public void onStart() {
        super.onStart();
        isInBackground = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations()) {
            isInBackground = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {

            case R.id.disconnect:
                onDisconnect();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
        registerSockets();
        messageView = getActivity().findViewById(R.id.messages);
        messageView.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageView.setAdapter(adapter);
        setMessageFieldListeners(sendMessageButton);
        setTextFieldMessageListeners();
        loadPreviousMessagesFromDb();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(getLayoutResource(), container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeHandlers();
    }

    private void loadPreviousMessagesFromDb() {
        List<Message> messages = db.getMessagesFromConversation(getTarget());
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
                // Interface needs to implement this
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isNameNull()) return;
                if (!mSocket.connected()) return;

                if (!isTyping) {
                    isTyping = true;
                    mSocket.emit(getTypingName(), getTypingParameters());
                }

                typingHandler.removeCallbacks(onTypingTimeout);
                typingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Interface needs to implement this
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

    protected void addMessage(Message message, boolean wasEdited, Integer position) {
        if (wasEdited && position != null) {
            messageList.set(position, message);
            adapter.notifyItemChanged(position);
        } else {
            messageList.add(message);
            adapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();
        }
    }

    protected void addTyping(String username) {
        messageList.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username).build());
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    protected void removeTyping(String username) {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message message = messageList.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(
                    username)) {
                messageList.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
    }

    void attemptSend(boolean wasEdited, Integer position) {
        if (isNameNull()) return;
        if (!mSocket.connected()) return;

        isTyping = false;

        String message = messageField.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageView.requestFocus();
            return;
        }

        this.sendMessage = message;
        this.wasEdited = wasEdited;
        this.position = position;

        messageField.setText("");
        encodeMessage();
    }

    public void proceedWithMessage(@Nullable boolean wasEdited, @Nullable Integer position,
            String encryptedMessage, String message) {
        Message.Builder messageBuilder = new Message.Builder(Message.TYPE_MESSAGE)
                .username(fromUser).message(message);
        String uniqueID;
        long id = -1;
        if (!wasEdited) {
            uniqueID = UUID.randomUUID().toString();
            messageBuilder.codeMessage(uniqueID);
            MessageModel model = new MessageModel(fromUser, encryptedMessage, false, position,
                    uniqueID);
            setRoomName(model);
            id = db.addRow(model, getTarget());
        } else {
            uniqueID = messageList.get(position).getCodeMessage();
        }
        long finalId = id;
        getActivity().runOnUiThread(() -> addMessage(messageBuilder.id(finalId).codeMessage(
                uniqueID).build(), wasEdited, position));

        // perform the sending sendMessage attempt.
        emitMessage(encryptedMessage, uniqueID);
    }

    private void setRoomName(MessageModel model) {
        if (isGroupMessage) {
            model.setRoomName(getTarget());
        }
    }

    boolean checkIfActiveConversationWindow() {
        return getTarget().equals(getTarget()) && !isInBackground;
    }

    private void scrollToBottom() {
        messageView.scrollToPosition(adapter.getItemCount() - 1);
    }

    protected Emitter.Listener onTyping = (Object... args) ->
            getActivity().runOnUiThread(() -> {
                try {
                    getDataAndAddTyping((JSONObject) args[0]);
                } catch (JSONException e) {
                    return;
                }
            });

    protected Emitter.Listener handleMessage = (Object... args) ->
            getActivity().runOnUiThread(() -> {
                try {
                    MessageModel model = new MessageModel((JSONObject) args[0]);
                    removeTyping(model.getUsername());
                    boolean wasEditedBool = model.isWasEdited();
                    Integer positionInt = model.getPosition();
                    String senderUsername = model.getUsername();
                    setRoomName(model);
                    Message.Builder messageBuilder = new Message.Builder(
                            Message.TYPE_MESSAGE).message(model.decryptMessage()).username(
                            senderUsername).isEdited(wasEditedBool);
                    if (checkIfActiveConversationWindow()) {
                        long id = TableMessagesUtils.addToDatabase(model, db);
                        addMessage(messageBuilder.id(id).build(), wasEditedBool, positionInt);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });

    protected Emitter.Listener onStopTyping = args -> getActivity().runOnUiThread(() -> {
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
        mSocket.emit(getTypingTimeoutName(), getTypingTimeoutParameters());
    };

    protected Emitter.Listener encryptMessage = args ->
    {
        JSONObject data = (JSONObject) args[0];
        try {
            encodeWithUserPublicKey(data.getString("publicKey"));

        } catch (InvalidKeySpecException | NoSuchAlgorithmException | JSONException e) {
            e.printStackTrace();
        }

    };

    protected void encodeWithUserPublicKey(String publicKeyString)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicKeyData;
        publicKeyData = Base64.decode(publicKeyString, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyData);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(spec);
        RSA rsa = RSA.getRSAInstance();
        String encryptedMessage = rsa.encrypt(sendMessage,
                publicKey.getPublicExponent(),
                publicKey.getModulus());
        proceedWithMessage(wasEdited, position, encryptedMessage, sendMessage);
    }

    abstract void getDataAndAddTyping(JSONObject data) throws JSONException;

    abstract void removeHandlers();

    abstract String getTarget();

    abstract String getTypingTimeoutName();

    abstract String getTypingTimeoutParameters();

    abstract String getSendName();

    abstract boolean isNameNull();

    abstract String getTypingName();

    abstract String getTypingParameters();

    abstract void onDisconnect();

    abstract int getLayoutResource();

    abstract void registerSockets();

    abstract void encodeMessage();

    abstract void emitMessage(String encryptedMessage, String uniqueID);
}

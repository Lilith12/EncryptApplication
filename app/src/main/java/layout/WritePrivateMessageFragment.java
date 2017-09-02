package layout;

import android.app.Activity;
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
import com.example.aleksandra.encryptapplication.FileUtils;
import com.example.aleksandra.encryptapplication.Message;
import com.example.aleksandra.encryptapplication.MessageAdapter;
import com.example.aleksandra.encryptapplication.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class WritePrivateMessageFragment extends Fragment {
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
    String toUser;
    String fromUser;
    EditText messageField;
    Socket mSocket;

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
            toUser = bundle.getString("username");
        }
    }

    private void readFromFile(File file){
        String username;
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while((username = bufferedReader.readLine()) != null){
                addMessage(username, bufferedReader.readLine());
            }
        }catch(IOException e){
            e.printStackTrace();
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
    public void onDestroyView(){
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
        messageField = (EditText) getActivity().findViewById(R.id.messageField);
        messageField.setTextColor(Color.rgb(255, 255, 255));
        Button sendMessageButton = (Button) getActivity().findViewById(R.id.sendButton);

        final EncryptAppSocket app = (EncryptAppSocket) getActivity().getApplication();
        mSocket = app.getSocket();
        fromUser = app.getUsername();
        mSocket.on("pwMessage", handlePrivateMessage);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        messageView = (RecyclerView) getActivity().findViewById(R.id.messages);
        messageView.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageView.setAdapter(adapter);

        setMessageFieldListeners(sendMessageButton);
        loadPreviousMessages();
    }

    private void loadPreviousMessages() {
        File outputTempFile = new File(getContext().getCacheDir().getPath() + "/" + encodeFileName(toUser));
        if(outputTempFile.isFile()){
            readFromFile(outputTempFile);
        }
    }

    private void setMessageFieldListeners(Button sendMessageButton) {
        messageField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });

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

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
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
        if (null == toUser) return;
        if (!mSocket.connected()) return;

        isTyping = false;

        String message = messageField.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageView.requestFocus();
            return;
        }

        FileUtils.saveMessageToTempFile(FileUtils.createEmptyTempFile(getContext(), toUser), fromUser, message);
        messageField.setText("");
        addMessage(fromUser, message);

        // perform the sending message attempt.
        mSocket.emit("new message", toUser, message);
    }

    private boolean checkIfActiveConversationWindow(String messageFromUser){
        return messageFromUser.equals(toUser);
    }

    private void scrollToBottom() {
        messageView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private Emitter.Listener handlePrivateMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
                    if (checkIfActiveConversationWindow(username)) {
                        addMessage(username, message);
                    }
                }
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    addTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    removeTyping(username);
                }
            });
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!isTyping) return;

            isTyping = false;
            mSocket.emit("stop typing PW", toUser);
        }
    };

    private void removeHandlers(){
        mSocket.off("pwMessage");
        mSocket.off("typing");
        mSocket.off("stop typing");
    }
}

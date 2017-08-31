package layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.view.ContextThemeWrapper;
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
import android.widget.Toast;

import com.example.aleksandra.encryptapplication.EncryptAppSocket;
import com.example.aleksandra.encryptapplication.Message;
import com.example.aleksandra.encryptapplication.MessageAdapter;
import com.example.aleksandra.encryptapplication.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.opengl.ETC1.encodeImage;

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
    String roomName;
    String fromUser;
    EditText messageField;
    Socket mSocket;

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
    private void addWarning(){

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar));
        builder.setMessage("Czy na pewno chcesz rozłączyć się z pokojem?")
                .setCancelable(true)
                .setPositiveButton("Tak", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSocket.emit("disconnect from room", roomName);
                        mSocket.emit("user disconnected");
                        AvailableRooms rooms = new AvailableRooms();
                        android.support.v4.app.FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.fragment_container, rooms);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                })
                .setNegativeButton("Nie", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).create();
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
        mSocket.on("groupMessage", handleGroupMessage);
        mSocket.on("groupTyping", onTyping);
        mSocket.on("stopGroupTyping", onStopTyping);
        mSocket.on("user connected", onConnect);
        mSocket.on("user disconnected", onDisconnect);
        messageView = (RecyclerView) getActivity().findViewById(R.id.messages);
        messageView.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageView.setAdapter(adapter);

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



        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageField.getText().toString();
                mSocket.emit("new group message", roomName, message);
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
//    public void sendImage(String path)
//    {
//        JSONObject sendData = new JSONObject();
//        try{
//            sendData.put("image", encodeImage(path));
//            Bitmap bmp = decodeImage(sendData.getString("image"));
//            addImage(bmp);
//            mSocket.emit("new group message",sendData);
//        }catch(JSONException e){
//
//        }
//    }
//
//    private void addImage(Bitmap bmp){
//        messageList.add(new Message.Builder(Message.TYPE_MESSAGE)
//                .image(bmp).build());
//        adapter = new MessageAdapter(messageList);
//        adapter.notifyItemInserted(0);
//        scrollToBottom();
//    }
//
//    private String encodeImage(String path)
//    {
//        File imagefile = new File(path);
//        FileInputStream fis = null;
//        try{
//            fis = new FileInputStream(imagefile);
//        }catch(FileNotFoundException e){
//            e.printStackTrace();
//        }
//        Bitmap bm = BitmapFactory.decodeStream(fis);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
//        byte[] b = baos.toByteArray();
//        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
//        //Base64.de
//        return encImage;
//
//    }
//
//    private Bitmap decodeImage(String data)
//    {
//        byte[] b = Base64.decode(data,Base64.DEFAULT);
//        Bitmap bmp = BitmapFactory.decodeByteArray(b,0,b.length);
//        return bmp;
//    }
//
//    private Emitter.Listener handleIncomingMessages = new Emitter.Listener(){
//        @Override
//        public void call(final Object... args){
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    JSONObject data = (JSONObject) args[0];
//                    String message;
//                    String imageText;
//                    try {
//                        message = data.getString("text").toString();
//                        addMessage(message);
//
//                    } catch (JSONException e) {
//                        // return;
//                    }
//                    try {
//                        imageText = data.getString("image");
//                        addImage(decodeImage(imageText));
//                    } catch (JSONException e) {
//                        //retur
//                    }
//
//                }
//            });
//        }
//    };

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

        messageField.setText("");
        addMessage(fromUser, message);

        // perform the sending message attempt.
        mSocket.emit("new group message", roomName, message);
    }

    private void scrollToBottom() {
        messageView.scrollToPosition(adapter.getItemCount() - 1);
    }

    private Emitter.Listener handleGroupMessage = new Emitter.Listener() {
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
                    addMessage(username, message);
                }
            });
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            final String username;
            try {
                username = data.getString("username");
            } catch (JSONException e) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                        Toast.makeText(getActivity().getApplicationContext(), username+" dołączył do pokoju"
                                , Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            final String username;
            try {
                username = data.getString("username");
            } catch (JSONException e) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(), username+ " rozłączył się z pokojem"
                            , Toast.LENGTH_LONG).show();
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
            mSocket.emit("stop typing to group", roomName);
        }
    };
}

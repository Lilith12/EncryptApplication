package com.example.aleksandra.encryptapplication.fragments;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import com.example.aleksandra.encryptapplication.R;

import org.json.JSONException;
import org.json.JSONObject;

public class WritePrivateMessageFragment extends AbstractChatFragment {
    public static final String USERNAME = "username";
    private WritePrivateMessageFragment.OnFragmentInteractionListener mListener;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.action_bar_add_image, menu);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        adapter = new MessageAdapter(getActivity(), messageList);
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    void getDataAndAddTyping(JSONObject data) throws JSONException {
        String username = data.getString("username");
        if (getTarget().equals(username)) {
            addTyping(username);
        }
    }

    @Override
    void removeHandlers() {
        mSocket.off("pwMessage");
        mSocket.off("typing");
        mSocket.off("stop typing");
        mSocket.off("requestedPublicKey");
    }

    @Override
    String getTarget() {
        return getArguments().getString(USERNAME);
    }

    @Override
    String getTypingTimeoutName() {
        return "stop typing PW";
    }

    @Override
    String getTypingTimeoutParameters() {
        return getTarget();
    }

    @Override
    String getSendName() {
        return "new message";
    }

    @Override
    boolean isNameNull() {
        return null == getTarget();
    }

    @Override
    String getTypingName() {
        return "typing PW";
    }

    @Override
    String getTypingParameters() {
        return getTarget();
    }

    @Override
    void onDisconnect() {
        Log.e("aa", "onOptionsItemSelected: ");
    }

    @Override
    int getLayoutResource() {
        return R.layout.fragment_write_message;
    }

    @Override
    void registerSockets() {
        mSocket.on("pwMessage", handleMessage);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        mSocket.on("requestedPublicKey", encryptMessage);
    }

    @Override
    void encodeMessage() {
        mSocket.emit("request public key", getTarget());

    }

    @Override
    void emitMessage(String encryptedMessage, String uniqueID) {
        mSocket.emit(getSendName(), getTarget(), encryptedMessage, wasEdited, position, uniqueID, isImage);
    }
}

package com.example.aleksandra.encryptapplication.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.aleksandra.encryptapplication.EncryptAppSocket;
import com.example.aleksandra.encryptapplication.R;
import com.example.aleksandra.encryptapplication.ServerStatsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Optional;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ConnectedUsersFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConnectedUsersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectedUsersFragment extends Fragment {
    private ListView users;
    private Socket mSocket;
    private ArrayList<String> userList = new ArrayList();
    private ArrayAdapter<String> adapter;
    private static final String getUsers = "get users";
    String jsonObject;
    Handler handler;

    private OnFragmentInteractionListener mListener;

    public ConnectedUsersFragment() {
        // Required empty public constructor
    }

    public static ConnectedUsersFragment newInstance(String param1, String param2) {
        ConnectedUsersFragment fragment = new ConnectedUsersFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ServerStatsActivity) getActivity()).setActionBarTitle(R.string.user_fragment_title);
    }

    private Emitter.Listener handleUserJsonArray = (Object... args) -> {
        JSONObject data = (JSONObject) args[0];
        View view = getView();
        Optional.ofNullable(view).ifPresent(
                presentView -> presentView.post(() -> addNewUserToList(data)));
    };

    private void addNewUserToList(JSONObject data) {
        userList.clear();
        try {
            jsonObject = data.getString("users");
            JSONArray jsonArray = new JSONArray(jsonObject);
            for (int i = 0; i < jsonArray.length(); i++) {
                userList.add(jsonArray.getString(i));
                adapter.notifyDataSetChanged();
            }
        } catch (JSONException e) {
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connected_users, container, false);
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
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        users = getView().findViewById(R.id.userlistView);
        adapter = new ArrayAdapter(getActivity(), R.layout.simple_item_list_layout, R.id.label,
                userList);
        users.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        users.setOnItemClickListener((adapter1, v, position, arg3) -> {
            String value = (String) adapter1.getItemAtPosition(position);

            Bundle bundle = new Bundle();
            bundle.putString("username", value);

            WritePrivateMessageFragment pwMessage = new WritePrivateMessageFragment();
            pwMessage.setArguments(bundle);
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, pwMessage);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
            // assuming string and if you want to get the value on click of list item
            // do what you intend to do on click of listview row
        });
        EncryptAppSocket app = getApplication();
        mSocket = app.getSocket();
        mSocket.on("usersArray", handleUserJsonArray);
        mSocket.emit(getUsers);
        handler = new Handler();
    }

    private EncryptAppSocket getApplication() {
        return (EncryptAppSocket) getActivity().getApplication();
    }
}

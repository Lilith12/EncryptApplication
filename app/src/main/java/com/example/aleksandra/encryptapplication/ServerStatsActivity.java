package com.example.aleksandra.encryptapplication;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.Menu;
import android.view.MenuItem;

import com.example.aleksandra.encryptapplication.fragments.AvailableRoomsFragment;
import com.example.aleksandra.encryptapplication.fragments.ConnectedUsersFragment;
import com.example.aleksandra.encryptapplication.fragments.GroupChatFragment;
import com.example.aleksandra.encryptapplication.fragments.WritePrivateMessageFragment;

public class ServerStatsActivity extends AppCompatActivity implements ConnectedUsersFragment.OnFragmentInteractionListener,
        WritePrivateMessageFragment.OnFragmentInteractionListener, AvailableRoomsFragment.OnFragmentInteractionListener, GroupChatFragment.OnFragmentInteractionListener {
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView nvDrawer;
    android.support.v4.app.FragmentManager fragmentManager;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getSupportFragmentManager();
        setContentView(R.layout.activity_server_stats);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        nvDrawer = findViewById(R.id.nav_view);
        // Setup drawer view
        setupDrawerTitle();
        setupDrawerContent(nvDrawer);

        mDrawerToggle = setupDrawerToggle(toolbar);

        // Tie DrawerLayout events to the ActionBarToggle
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        String fragmentName = getIntent().getStringExtra("fragment");

        if(fragmentName != null && fragmentName.equals("WritePrivateMessageFragment")){
            replacePrivateMessageFragment();
        }
        else if(fragmentName != null && fragmentName.equals("GroupChatFragment")){
            replaceGroupMessageFragment();
        }

        if (findViewById(R.id.fragment_container) != null && fragmentName == null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            ConnectedUsersFragment usersFragment = new ConnectedUsersFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, usersFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
        serviceIntent = new Intent(this, NotificationService.class);
        this.startService(serviceIntent);
    }

    public void setActionBarTitle(int resource){
        getSupportActionBar().setTitle(resource);
    }

    public void setActionBarTitle(String title){
        getSupportActionBar().setTitle(title);
    }

    private void setupDrawerTitle() {
        Menu menu = nvDrawer.getMenu();
        MenuItem messageItem = menu.findItem(R.id.messageTitleNav);
        SpannableString string = new SpannableString(messageItem.getTitle());
        string.setSpan(new TextAppearanceSpan(this, R.style.TextApperanceNavBar), 0, string.length(), 0);
        messageItem.setTitle(string);
        nvDrawer.setNavigationItemSelectedListener(this::onOptionsItemSelected);
    }

    private void replaceGroupMessageFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("roomName", getIntent().getStringExtra("chatView"));
        GroupChatFragment fragment = new GroupChatFragment();
        fragment.setArguments(bundle);
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, "groupChatFragment")
                .addToBackStack(null)
                .commit();
    }

    private void replacePrivateMessageFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("username", getIntent().getStringExtra("chatView"));
        WritePrivateMessageFragment fragment = new WritePrivateMessageFragment();
        fragment.setArguments(bundle);
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, "privateMessageFragment")
                .addToBackStack(null)
                .commit();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopService(serviceIntent);
    }

    private ActionBarDrawerToggle setupDrawerToggle(Toolbar toolbar) {
        return new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, 0, R.string.drawer_open);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void selectDrawerItem(MenuItem item){
        Fragment fragment = null;
        Class fragmentClass;
        switch(item.getItemId()) {
            case R.id.nav_write:
                fragmentClass = ConnectedUsersFragment.class;
                break;
            case R.id.nav_room:
                fragmentClass = AvailableRoomsFragment.class;
                break;
            default:
                fragmentClass = ConnectedUsersFragment.class;
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();

        item.setChecked(true);
        setTitle(item.getTitle());
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //needs to implement this
    }
}

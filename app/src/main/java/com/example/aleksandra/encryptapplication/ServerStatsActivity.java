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
        // NOTE: Make sure you pass in a valid toolbar reference.  ActionBarDrawToggle() does not require it
        // and will not render the hamburger icon without it.
        return new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, 0, R.string.drawer_open);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
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
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void selectDrawerItem(MenuItem item){
        // Create a new fragment and specify the fragment to show based on nav item clicked
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

        // Insert the fragment by replacing any existing fragment
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();

        // Highlight the selected item has been done by NavigationView
        item.setChecked(true);
        // Set action bar title
        setTitle(item.getTitle());
        // Close the navigation drawer
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void setUsersFragment(MenuItem item){
        ConnectedUsersFragment usersFragment = new ConnectedUsersFragment();

        // Insert the fragment by replacing any existing fragment
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_container, usersFragment).commit();

        // Highlight the selected item has been done by NavigationView
        item.setChecked(true);
        // Set action bar title
        setTitle(item.getTitle());
        // Close the navigation drawer
        mDrawerLayout.closeDrawers();
    }

}

package com.maghfoormalkana.fungai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.maghfoormalkana.fungai.adapters.AdapterPosts;
import com.maghfoormalkana.fungai.models.ModelPost;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */

public class HomeFragment extends Fragment {

    //firebase auth
    FirebaseAuth firebaseAuth;
    String myUid;
    RecyclerView recyclerView;
    List<ModelPost> postList;
    AdapterPosts adapterPosts;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        //init
        firebaseAuth = FirebaseAuth.getInstance();

        //RECYCLER VIEW AND ITS PROPERTIES
        recyclerView = view.findViewById(R.id.postsRecyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        //SHOW NEWEST POST FIRST, FOR THIS LOAD FROM LAST
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        //SET LAYOUT TO RECYCLER VIEW
        recyclerView.setLayoutManager(layoutManager);
        //INIT POST LIST
        postList = new ArrayList<>();

        loadPosts();

        return view;
    }

    private void loadPosts() {
        //PATH OF ALL POSTS
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //GET ALL DATA FROM REF
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds: snapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    postList.add(modelPost);

                    //ADAPTER
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //SET ADAPTER TO RECYCLER VIEW
                    recyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //IN CASE OF ERROR
                Toast.makeText(getActivity(), ""+ error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPosts(String searchQuery){
        //PATH OF ALL POSTS
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //GET ALL DATA FROM REF
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds: snapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            modelPost.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())) {
                        postList.add(modelPost);
                    }


                    //ADAPTER
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //refresh adapter
                    adapterPosts.notifyDataSetChanged();
                    //SET ADAPTER TO RECYCLER VIEW
                    recyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //IN CASE OF ERROR
                Toast.makeText(getActivity(), ""+ error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            //set email of logged in user
            //mProfileTv.setText(user.getEmail());
            myUid = user.getUid(); //CURRENT SIGNED IN USER UID
        }
        else {
            //user not signed in, go to main activity
            startActivity(new Intent(getActivity(), Login.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); //Show menu Option in fragment
        super.onCreate(savedInstanceState);
    }


    /* inflate options menu*/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflating menu
        inflater.inflate(R.menu.menu_main, menu);

        //SEARCHVIEW TO SEARCH POSTS BY TITLE/DESCRIPTION
        MenuItem item = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) MenuItemCompat.getActionView(item);
        //SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //SEARCH LISTENER
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //CALLED WHEN USER PRESS SEARCH BUTTON
                if (!TextUtils.isEmpty(query)) {
                    searchPosts(query);
                }
                else {
                    loadPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                //CALLED AS AND WHEN USER PRESS ANY LETTER
                if (!TextUtils.isEmpty(query)) {
                    searchPosts(query);
                }
                else {
                    loadPosts();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    // handle menu item clicks

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item ID
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        if (id == R.id.action_add_post){
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
package com.maghfoormalkana.fungai;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.maghfoormalkana.fungai.adapters.AdapterPosts;
import com.maghfoormalkana.fungai.models.ModelPost;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {


    //FIREBASE
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;
    //path where images of user profile and cover will be stored
    String storagePath = "Users_Profile_Cover_Imgs/";

    //views from xml
    ImageView avatarIv, coverIv;
    TextView nameTv, emailTv, phoneTv;
    FloatingActionButton fab;
    RecyclerView postsRecyclerView;

    ProgressDialog pd;

    //permission constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //arrays of permission to be requested
    String[] cameraPermissions;
    String[] storagePermissions;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    //uri of picked image
    Uri image_uri;

    //for checking profile or cover
    String profileOrCover;
    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //INIT FIREBASE
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference(); //firebase storage reference
        //init arrays of permission
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};



        //init views
        avatarIv = view.findViewById(R.id.avatarIv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);
        postsRecyclerView = view.findViewById(R.id.recyclerview_post);

        //init progress dialog
        pd = new ProgressDialog(getActivity());

        /* Getting info of Currently signed in user using email
            Using OrderByChild query to show the detail from a node whose key named 'email' has equal value as current user
         */
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //check until required data found
                for (DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    String name = ""+ ds.child("name").getValue();
                    String email = ""+ ds.child("email").getValue();
                    String phone = ""+ ds.child("phone").getValue();
                    String image = ""+ ds.child("image").getValue();
                    String cover = ""+ ds.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        //if image is received successfully
                        Picasso.get().load(image).into(avatarIv);
                    }
                    catch (Exception e) {
                        //if there is an error
                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);

                    }
                    try {
                        //if cover is received successfully
                        Picasso.get().load(cover).into(coverIv);
                    }
                    catch (Exception e) {
                        //if there is an error, set default color

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //floating button click
        fab.setOnClickListener(v -> showEditProfileDialog());

        postList = new ArrayList<>();

        checkUserStatus();
        loadMyPosts();

        return view;
    }

    private void loadMyPosts() {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first, load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        //SET LAYOUT TO RECYCLER VIEW
        postsRecyclerView.setLayoutManager(layoutManager);

        //INIT POST LIST
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //QUERY TO LOAD POSTS
        Query query = ref.orderByChild("uid").equalTo(uid);
        //GET ALL DATA FROM THIS REF
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);
                    
                    //ADD TO LIST
                    postList.add(myPosts);
                    
                    //ADAPTER
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //SET ADAPTER
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void searchMyPosts(final String searchQuery) {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first, load from last
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        //SET LAYOUT TO RECYCLER VIEW
        postsRecyclerView.setLayoutManager(layoutManager);

        //INIT POST LIST
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //QUERY TO LOAD POSTS
        Query query = ref.orderByChild("uid").equalTo(uid);
        //GET ALL DATA FROM THIS REF
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            myPosts.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())){
                        //ADD TO LIST
                        postList.add(myPosts);
                    }

                    //ADAPTER
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //SET ADAPTER
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    //-----------------STORAGE PERMISSION--------------------
//    private boolean checkStoragePermission(){
//        //check if storage permission, true if enabled
//        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                == (PackageManager.PERMISSION_GRANTED);
//        return result;
//    }

    private boolean checkStoragePermission() {
        // Check if the fragment is attached to an activity
        if (getActivity() != null) {
            // Check if camera permission and storage permission are granted
            boolean storagePermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

            // Return true if permissions are granted
            return storagePermission;
        } else {
            // If activity is null, display a Toast message to the user
            Toast.makeText(getContext(), "Error: Null Activity", Toast.LENGTH_SHORT).show();
            return false;
        }

    }

    private void requestStoragePermission() {
        // Check if the fragment is attached to an activity
        if (getActivity() != null) {
            // Fragment is attached, request storage permission
            requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
        } else {
            // Fragment is not attached, show a Toast message
            Toast.makeText(getContext(), "Error: Null Activity", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkCameraPermission() {
        // Check if the activity is not null
        if (getActivity() != null) {
            // Check if camera permission and storage permission are granted
            boolean cameraPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
            boolean storagePermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

            // Return true if both permissions are granted
            return cameraPermission && storagePermission;
        } else {
            // If activity is null, display a Toast message to the user
            Toast.makeText(getContext(), "Error: Null Activity", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void requestCameraPermission() {
        // Check if the activity is not null
        if (getActivity() != null) {
            // Request runtime camera permission
            requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
        } else {
            // If activity is null, display a Toast message to the user
            Toast.makeText(getContext(), "Error: Null Activity", Toast.LENGTH_SHORT).show();
        }
    }


    private void showEditProfileDialog() {
        //Show options
        // EDIT PROFILE PICTURE,
        // COVER PHOTO, NAME,
        // PHONE
        String[] options = {"Edit Profile Image","Edit Cover Image","Edit Name","Edit Phone"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Choose Action");
        //set items to dialog
        builder.setItems(options, (dialog, which) -> {

            //handle dialog item clicks
            if (which == 0) {
                //Edit Profile Clicked
                pd.setMessage("Updating Profile Image");
                profileOrCover = "image";
                showImagePicDialog();
            } else if (which == 1) {
                //Edit Cover Clicked
                pd.setMessage("Updating Cover Image");
                profileOrCover = "cover";
                showImagePicDialog();
            } else if (which == 2) {
                //Edit Name Clicked
                pd.setMessage("Updating Name");
                showNamePhoneUpdateDialog("name");
            } else if (which == 3) {
                //Edit Phone Clicked
                pd.setMessage("Updating Phone");
                showNamePhoneUpdateDialog("phone");
            }
        });
        //create and show dialog
        builder.create().show();

    }

    private void showNamePhoneUpdateDialog(String key) {
        //PARAMETER "KEY" WILL CONTAIN VALUE:
        // EITHER "NAME" WHICH IS KEY IN USER"S DATABASE WHILE IS USED TO UPDATE USER'S NAME
        // OR "PHONE" WHICH IS KEY IN USER'S DATABASE WHICH IS USED TO UPDATE USER'S PHONE

        // CUSTOM DIALOG
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update " + key); //UPDATE NAME OR UPDATE PHONE

        //SET LAYOUT OF DIALOG
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);

        //add edit text
        EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+ key);
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add buttons in dialog to update
        builder.setPositiveButton("Update", (dialog, which) -> {
            //input text from edit text
            String value = editText.getText().toString().trim();
            //validate if user has entered something or not
            if (!TextUtils.isEmpty(value)){
                pd.show();
                HashMap<String, Object> result = new HashMap<>();
                result.put(key, value);

                databaseReference.child(user.getUid()).updateChildren(result)
                        .addOnSuccessListener(unused -> {
                            //updated successfully
                            pd.dismiss();
                            Toast.makeText(getActivity(), "Updated Successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            //failed and error
                            pd.dismiss();
                            Toast.makeText(getActivity(),""+ e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                //Update Name in User Posts
                if (key.equals("name")){
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                    Query query = ref.orderByChild("uid").equalTo(uid);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds: snapshot.getChildren()) {
                                String child = ds.getKey();
                                snapshot.getRef().child(child).child("uName").setValue(value);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    //Update Name in Current Users Comments on Posts
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds: snapshot.getChildren()) {
                                String child = ds.getKey();
                                if (snapshot.child(child).hasChild("Comments")) {
                                    String child1 = ""+snapshot.child(child).getKey();
                                    Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments")
                                            .orderByChild("uid").equalTo(uid);
                                    child2.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot ds: snapshot.getChildren()) {
                                                String child = ds.getKey();
                                                snapshot.getRef().child(child).child("uName").setValue(value);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }


            }
            else {
                Toast.makeText(getActivity(), "Please enter "+key, Toast.LENGTH_SHORT).show();
            }

        });
        //add buttons in dialog to cancel
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        //create and show dialog
        builder.create().show();
    }

    private void showImagePicDialog() {
        //show dialog containing options Camera and Gallery to pick the image
        String[] options = {"Camera","Gallery"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Pick Image From");
        //set items to dialog
        builder.setItems(options, (dialog, which) -> {
            //handle dialog item clicks
            if (which == 0) {
                //Camera Clicked

                if (!checkCameraPermission()) {
                    requestCameraPermission();
                    //pickFromCamera();
                }
                else {
                    pickFromCamera();
                }

            } else if (which == 1) {
                //Gallery Clicked
                if (!checkStoragePermission()) {
                    requestStoragePermission();
                    pickFromGallery();
                }
                else {
                    pickFromGallery();
                }
            }
        });
        //create and show dialog
        builder.create().show();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //HANDLING PERMISSION CASES ALLOW AND DENY
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:{
                //picking from camera, first checking permissions
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        //permission enabled
                        pickFromCamera();
                    }
                    else {
                        //permission denied
                        requestCameraPermission();
                        //Toast.makeText(getActivity(), "Enable Camera & Storage Permission", Toast.LENGTH_SHORT).show();

                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //picking from gallery, first checking permissions

                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        //permission enabled
                        pickFromGallery();
                    }
                    else {
                        //permission denied
                        requestStoragePermission();
                        //Toast.makeText(getActivity(), "Enable Storage Permission", Toast.LENGTH_SHORT).show();

                    }
                }

            }
            break;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //THIS METHOD WILL BE CALLED AFTER PICKING IMAGE FROM CAMERA AND GALLERY
        if (resultCode == RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // IMAGE IS PICKED FROM GALLERY. GET URI IMAGE
                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // IMAGE IS PICKED FROM GALLERY. GET URI IMAGE
                uploadProfileCoverPhoto(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(Uri uri) {

        //show progress
        pd.show();
        //CREATING ONE CODE FOR UPLOADING THE PROFILE AND COVER IMAGE
        //path and name of the image to be stored in firebase storage

        String filePathAndName = storagePath +""+ profileOrCover + "_" + user.getUid();
        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    //AFTER IMAGE IS UPLOADED, GET ITS URL AND STORE IN USERS DATABASE
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());
                    Uri downloadUri = uriTask.getResult();

                    //check if image is uploaded or not
                    if (uriTask.isSuccessful()) {
                        //image uploaded
                        //add/update url in users database
                        HashMap<String, Object> results = new HashMap<>();
                        /* First parameter is ProfileOrCover that has value "image" or "cover"
                            which are keys in users database where url of image will be saved in one of them

                            Second Parameter contains the url of the image stored in firebase storage,
                            this url will be saved as value against key "image" or "cover"
                         */
                        results.put(profileOrCover, downloadUri.toString());
                        databaseReference.child(user.getUid()).updateChildren(results)
                                .addOnSuccessListener(aVoid -> {
                                    //url in database of user is added successfully
                                    //dismiss the progress dialog
                                    pd.dismiss();
                                    Toast.makeText(getActivity(), "Image Updated Successfully", Toast.LENGTH_SHORT).show();

                                })
                                .addOnFailureListener(e -> {
                                    //error adding url in database of user
                                    //dismiss the progress dialog
                                    pd.dismiss();
                                    Toast.makeText(getActivity(), "Error Updating Image", Toast.LENGTH_SHORT).show();
                                });
                    }
                    //Update Name in User Posts
                    if (profileOrCover.equals("image")) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = ref.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uImage").setValue(downloadUri);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        //UPDATE IN COMMENTS
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds: snapshot.getChildren()) {
                                    String child = ds.getKey();
                                    if (snapshot.child(child).hasChild("Comments")) {
                                        String child1 = ""+snapshot.child(child).getKey();
                                        Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments")
                                                .orderByChild("uid").equalTo(uid);
                                        child2.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot ds: snapshot.getChildren()) {
                                                    String child = ds.getKey();
                                                    snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }

                    else {
                        // error
                        pd.dismiss();
                        Toast.makeText(getActivity(), "Some error occurred", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    //ERROR IN UPLOADING IMAGE
                    pd.dismiss();
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void pickFromGallery() {

        //pick from gallery
        //Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        //galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);
        //startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);


    }

    private void pickFromCamera() {
        //Intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
        //startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            //set email of logged in user
            //mProfileTv.setText(user.getEmail());
            uid = user.getUid();
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

        //inflater.inflate(R.menu.menu_main, menu);

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
                    searchMyPosts(query);
                }
                else {
                    loadMyPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                //CALLED AS AND WHEN USER PRESS ANY LETTER
                if (!TextUtils.isEmpty(query)) {
                    searchMyPosts(query);
                }
                else {
                    loadMyPosts();
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
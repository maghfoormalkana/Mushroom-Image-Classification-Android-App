package com.maghfoormalkana.fungai;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.maghfoormalkana.fungai.adapters.AdapterComments;
import com.maghfoormalkana.fungai.models.ModelComment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetail extends AppCompatActivity {

    //GET DETAIL OF THE USER AND POST
    String hisUid, myUid, myEmail, myName, myDp,
    postId, pLikes, hisDp, hisName, pImage;
    boolean mProcessComment = false;
    boolean mProcessLike = false;

    ProgressDialog pd;     //PROGRESS BAR

    //VIEWS
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn, commentBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerViewc;

    List<ModelComment> commentList;
    AdapterComments adapterComments;

    //ADD COMMENT VIEWS
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        //ACTION BAR
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //Get ID OF THE POST USING INTENT
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        //INIT VIEWS
        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        commentBtn = findViewById(R.id.commentBtn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerViewc = findViewById(R.id.recyclerViewc);

        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);

        loadPostInfo();
        checkUserStatus();
        loadUserInfo();
        setLikes();

        //SET SUBTITLE OF ACTION BAR
        actionBar.setSubtitle("Signed as: "+myEmail);


        //INIT COMMENT LIST
        commentList = new ArrayList<>();

        loadComments();

        //SEND COMMENT BUTTON CLICK
        sendBtn.setOnClickListener(v -> postComment());

        //LIKE BUTTON CLICK
        likeBtn.setOnClickListener(v -> likePost());

        //COMMENT SECTION ALREADY OPENED
        commentBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comments, 0, 0, 0);
        
        //COMMENT BUTTON CLICK
        commentBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Comment section already opened", Toast.LENGTH_SHORT).show();
        });

        //MORE BUTTON CLICK
        moreBtn.setOnClickListener(v -> showMoreoptions());

    }

    private void loadComments() {
        //LAYOUT LINEAR FOR RECYCLER VIEW
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        //SET LAYOUT TO RECYCLER VIEW
        recyclerViewc.setLayoutManager(layoutManager);

        //INIT COMMENT LIST
        commentList = new ArrayList<>();

        //PATH OF THE POST TO GET COMMENTS
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds: snapshot.getChildren()) {
                    ModelComment modelComment = ds.getValue(ModelComment.class);
                    commentList.add(modelComment);
                }

                // SETUP ADAPTER OUTSIDE THE LOOP (only once)
                adapterComments = new AdapterComments(getApplicationContext(), commentList, myUid, postId);
                recyclerViewc.setAdapter(adapterComments);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMoreoptions() {
        //CREATING POPUP MENU FOR OPTION "DELETE"
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        //SHOW DELETE OPTION IN ONLY POSTS OF CURRENT SIGNED IN USER
        if (hisUid.equals(myUid)) {
            //ADD ITEM IN MENU
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
        }
        else {
            Toast.makeText(this, "Can't Delete Others Post", Toast.LENGTH_SHORT).show();
        }
        
        //ITEM CLICK
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id==0) {
                //DELETE IS CLICKED
                beginDelete();
            }
            return false;
        });
        //SHOW MENU
        popupMenu.show();
    }

    private void beginDelete() {
        //POST CAN BE WITH OR WITHOUT IMAGE
        if (pImage.equals("noImage")) {
            //POST WITHOUT IMAGE
            deleteWithoutImage();
        }
        else {
            //POST WITH IMAGE
            deleteWithImage();
        }
    }

    private void deleteWithImage() {
        //PROGRESSDIALOG
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");

        // 1. DELETE IMAGE USING URL
        // 2. DELETE FROM DATABASE USING POST ID

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(unused -> {
                    //IMAGE DELETED, DELETE FROM DATABASE

                    Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                    fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds: snapshot.getChildren()) {
                                ds.getRef().removeValue(); //REMOVE VALUES FROM FIREBASE WHERE PID MATCHES
                            }
                            //DELETED
                            Toast.makeText(PostDetail.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                            pd.dismiss();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                })
                .addOnFailureListener(e -> {
                    //ERROR OCCURED
                    pd.dismiss();
                    Toast.makeText(PostDetail.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                });
    }

    private void deleteWithoutImage() {
        //PROGRESSDIALOG
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()) {
                    ds.getRef().removeValue(); //REMOVE VALUES FROM FIREBASE WHERE PID MATCHES
                }
                //DELETED
                Toast.makeText(PostDetail.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setLikes() {
        //CHECKING WHETHER USER LIKED THE POST AT THE TIME OF LOADING POSTS
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postId).hasChild(myUid)) {
                    //USER HAS LIKED THIS POST
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    likeBtn.setText("LIKED");

                }
                else {
                    //USER HAS NOT LIKED THIS POST
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    likeBtn.setText("LIKE");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void likePost() {
        //GET TOTAL NUMBER OF LIKES, FOR POST WHOSE LIKE BUTTON CLICKED
        mProcessLike = true;
        //GET ID OF THE POST CLICKED
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessLike) {
                    if (snapshot.child(postId).hasChild(myUid)) {
                        //ALREADY LIKED, SO REMOVE LIKE
                        postsRef.child(postId).child("pLikes").setValue(""+ (Integer.parseInt(pLikes)-1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;
                    }
                    else {
                        //NOT LIKED, LIKE IT
                        postsRef.child(postId).child("pLikes").setValue(""+ (Integer.parseInt(pLikes)+1));
                        likesRef.child(postId).child(myUid).setValue("Liked");
                        mProcessLike = false;
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void postComment() {

        pd = new ProgressDialog(this);
        pd.setMessage("Adding Comment...");

        //GET DATA FROM COMMENT EDIT TEXT
        String comment = commentEt.getText().toString().trim();
        //VALIDATE
        if (TextUtils.isEmpty(comment)) {
            //NO VALUE IS ENTERED
            Toast.makeText(this, "Empty Comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = String.valueOf(System.currentTimeMillis());

        //EACH POST WILL HAVE A CHILD "COMMENT" THAT WILL CONTAIN COMMENTS OF THE POST
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String, Object> hashMap = new HashMap<>();
        //PUT INFO IN HASHMAP
        hashMap.put("cId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timeStamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("uEmail", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);

        //PUT DATA in DATABASE
        ref.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    //ADDED
                    pd.dismiss();
                    Toast.makeText(PostDetail.this, "Comment Added", Toast.LENGTH_SHORT).show();
                    commentEt.setText("");
                    updateCommentCount();
                })
                .addOnFailureListener(e -> {
                    //Failed
                    pd.dismiss();
                    Toast.makeText(PostDetail.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void updateCommentCount() {
        //WHENEVER USER ADD COMMENT INCREASE THE COMMENT COUNT
        mProcessComment = true;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessComment) {
                    String comments = ""+ snapshot.child("pComments").getValue();
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child("pComments").setValue(""+newCommentVal);
                    mProcessComment=false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadUserInfo() {
        //GEt CURRENT USER INFO
        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()) {
                            myName = ""+ds.child("name").getValue();
                            myDp = ""+ds.child("image").getValue();

                            //SET DATA
                            try {
                                //IF IMAGE IS RECEIVED THEN SET
                                Picasso.get().load(myDp)
                                        .placeholder(R.drawable.ic_default_img)
                                        .into(cAvatarIv);
                            }
                            catch (Exception e) {
                                Picasso.get().load(R.drawable.ic_default_img).into(cAvatarIv);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadPostInfo() {
        //GET POST USING POST ID
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //KEEP CHECKING THE POSTS UNTIL GET THE REQUIRED POST
                for (DataSnapshot ds: snapshot.getChildren()) {
                    //GET DATA
                    String pTitle = ""+ds.child("pTitle").getValue();
                    String pDescr = ""+ds.child("pDescr").getValue();
                    pLikes = ""+ds.child("pLikes").getValue();
                    String pTimeStamp = ""+ds.child("pTime").getValue();
                    pImage = ""+ds.child("pImage").getValue();
                    hisDp = ""+ds.child("uDp").getValue();
                    hisUid = ""+ds.child("uid").getValue();
                    String uEmail = ""+ds.child("uEmail").getValue();
                    hisName = ""+ds.child("uName").getValue();
                    String commentCount = ""+ds.child("pComments").getValue();

                    //CONVERT TIMESTAMP TO PROPER FORMAT
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                    //SET DATA
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    if (Integer.parseInt(pLikes) > 1) {
                        pLikesTv.setText(pLikes+" Likes");
                    } else {
                        pLikesTv.setText(pLikes+" Like");
                    }
                    pTimeTv.setText(pTime);
                    if (Integer.parseInt(commentCount) > 1) {
                        pCommentsTv.setText(commentCount+ "Comments");
                    } else {
                        pCommentsTv.setText(commentCount+ "Comment");
                    }


                    uNameTv.setText(hisName);

                    //SET IMAGE OF THE USER WHO POSTED
                    //SET POST IMAGE
                    //IF THERE IS NOT IMAGE THEN HIDE IMAGEVIEW
                    if (pImage.equals("noImage")){
                        //HIDE IMAGEVIEW
                        pImageIv.setVisibility(View.GONE);

                    }
                    else {
                        //SHOW IMAGEVIEW
                        pImageIv.setVisibility(View.VISIBLE);
                        try {
                            Picasso.get().load(pImage)
                                    .into(pImageIv);
                        }
                        catch (Exception e){

                        }
                    }

                    //SET USER IMAGE IN COMMENT
                    try {
                        Picasso.get().load(hisDp)
                                .placeholder(R.drawable.ic_default_img)
                                .into(uPictureIv);
                    }
                    catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img)
                                .into(uPictureIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null) {
            //USER IS SIGNED IN
            myEmail = user.getEmail();
            myUid = user.getUid();
        }
        else {
            //USER IS LOGGED OUT
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //HIDE SOME MENNU ITEM
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item ID
        int id = item.getItemId();
        if (id == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
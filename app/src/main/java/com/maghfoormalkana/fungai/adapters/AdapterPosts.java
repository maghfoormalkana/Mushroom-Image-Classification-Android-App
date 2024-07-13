package com.maghfoormalkana.fungai.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.maghfoormalkana.fungai.OtherProfile;
import com.maghfoormalkana.fungai.PostDetail;
import com.maghfoormalkana.fungai.R;
import com.maghfoormalkana.fungai.models.ModelPost;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder> {

    android.content.Context context;
    List<ModelPost> postList;

    String myUid;

    private DatabaseReference likesRef; //for likes database node
    private DatabaseReference postsRef; //reference of posts

    boolean mProcessLike = false;

    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        //INFLATE LAYOUT ROW_POST.XML
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, viewGroup, false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder myHolder, int position) {
        //GET DATA

        String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        String uDp = postList.get(position).getuDp();
        final String pId = postList.get(position).getpId();
        String pTitle = postList.get(position).getpTitle();
        String pDescription = postList.get(position).getpDescr();
        final String pImage = postList.get(position).getpImage();
        String pTimeStamp = postList.get(position).getpTime();
        String pLikes = postList.get(position).getpLikes(); //Contains total number of likes for a post
        String pComments = postList.get(position).getpComments(); //Contains total number of Comments for a post

        //CONVERT TIMESTAMP TO dd/MM/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();


        //SET DATA
        myHolder.uNameTv.setText(uName);
        myHolder.pTimeTv.setText(pTime);
        myHolder.pTitleTv.setText(pTitle);
        myHolder.pDescriptionTv.setText(pDescription);
        if (Integer.parseInt(pLikes) > 1) {
            myHolder.pLikesTv.setText(pLikes +" Likes");
        } else {
            myHolder.pLikesTv.setText(pLikes +" Like");
        }
        if (Integer.parseInt(pComments) > 1) {
            myHolder.pCommentsTv.setText(pComments +" Comments");
        } else {
            myHolder.pCommentsTv.setText(pComments +" Comment");
        }
        //SET LIKES FOR EACH POST
        setLikes(myHolder, pId);

        //SET USER DP
        try {
            Picasso.get().load(uDp)
                    .placeholder(R.drawable.ic_default_img)
                    .into(myHolder.uPictureIv);
        }
        catch (Exception e){

        }

        //SET POST IMAGE
        //IF THERE IS NOT IMAGE THEN HIDE IMAGEVIEW
        if (pImage.equals("noImage")){
            //HIDE IMAGEVIEW
            myHolder.pImageIv.setVisibility(View.GONE);

        }
        else {
            //SHOW IMAGEVIEW
            myHolder.pImageIv.setVisibility(View.VISIBLE);
            try {
                Picasso.get().load(pImage)
                        .into(myHolder.pImageIv);
            }
            catch (Exception e){

            }
        }

        //HANDLE BUTTON CLICKS
        myHolder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreoptions(myHolder.moreBtn, uid, myUid, pId, pImage);
            }
        });

        myHolder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //GET TOTAL NUMBER OF LIKES, FOR POST WHOSE LIKE BUTTON CLICKED


                int pLikes = Integer.parseInt(postList.get(position).getpLikes());
                mProcessLike = true;
                //GET ID OF THE POST CLICKED
                String postIde = postList.get(position).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mProcessLike) {
                            if (snapshot.child(postIde).hasChild(myUid)) {
                                //ALREADY LIKED, SO REMOVE LIKE
                                postsRef.child(postIde).child("pLikes").setValue(""+ (pLikes-1));
                                likesRef.child(postIde).child(myUid).removeValue();
                                mProcessLike = false;
                            }
                            else {
                                //NOT LIKED, LIKE IT
                                postsRef.child(postIde).child("pLikes").setValue(""+ (pLikes+1));
                                likesRef.child(postIde).child(myUid).setValue("Liked");
                                mProcessLike = false;

                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        myHolder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //START POST DETAILS ACTIVITY
                Intent intent = new Intent(context, PostDetail.class);
                intent.putExtra("postId", pId); //FETCHING USING PID, ID OF THE POST CLICKED
                context.startActivity(intent);
            }
        });

        myHolder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //GO TO CLICKED USER PROFILE USING ITS UID
                //USING UID TO SHOW USER SPECIFIC POST AND DATA
                Intent intent = new Intent(context, OtherProfile.class);
                intent.putExtra("hisUid", uid);
                context.startActivity(intent);
            }
        });

    }

    private void setLikes(MyHolder holder, String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postKey).hasChild(myUid)) {
                    //USER HAS LIKED THIS POST
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    holder.likeBtn.setText("LIKED");

                }
                else {
                    //USER HAS NOT LIKED THIS POST
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    holder.likeBtn.setText("LIKE");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMoreoptions(ImageButton moreBtn, String uid, String myUid, String pId, String pImage) {
        //CREATING POPUP MENU FOR OPTION "DELETE"
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        //SHOW DELETE OPTION IN ONLY POSTS OF CURRENT SIGNED IN USER
        if (uid.equals(myUid)) {
            //ADD ITEM IN MENU
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
        }
        popupMenu.getMenu().add(Menu.NONE, 1, 0, "View Detail");
        //ITEM CLICK
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id==0) {
                    //DELETE IS CLICKED
                    beginDelete(pId, pImage);
                }
                else if (id==1) {
                    //START POST DETAILS ACTIVITY
                    Intent intent = new Intent(context, PostDetail.class);
                    intent.putExtra("postId", pId); //FETCHING USING PID, ID OF THE POST CLICKED
                    context.startActivity(intent);
                }
                return false;
            }
        });
        //SHOW MENU
        popupMenu.show();
    }

    private void beginDelete(String pId, String pImage) {
        //POST CAN BE WITH OR WITHOUT IMAGE
        if (pImage.equals("noImage")) {
            //POST WITHOUT IMAGE
            deleteWithoutImage(pId);
        }
        else {
            //POST WITH IMAGE
            deleteWithImage(pId, pImage);
        }
    }

    private void deleteWithImage(final String pId, String pImage) {
        //PROGRESSDIALOG
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting...");

        // 1. DELETE IMAGE USING URL
        // 2. DELETE FROM DATABASE USING POST ID

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //IMAGE DELETED, DELETE FROM DATABASE

                        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds: snapshot.getChildren()) {
                                    ds.getRef().removeValue(); //REMOVE VALUES FROM FIREBASE WHERE PID MATCHES
                                }
                                //DELETED
                                Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //ERROR OCCURED
                        pd.dismiss();
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void deleteWithoutImage(String pId) {
        //PROGRESSDIALOG
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting...");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()) {
                    ds.getRef().removeValue(); //REMOVE VALUES FROM FIREBASE WHERE PID MATCHES
                }
                //DELETED
                Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views from row_post.xml
        ImageView uPictureIv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn, shareBtn;
        LinearLayout profileLayout;
        public MyHolder (@NonNull View itemView) {
            super(itemView);

            //init views
            uPictureIv = itemView.findViewById(R.id.uPictureIv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            profileLayout = itemView.findViewById(R.id.profileLayout);
        }
    }
}

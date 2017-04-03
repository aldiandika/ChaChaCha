package com.example.aldiandika.chachacha;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ui.email.*;
import com.firebase.ui.auth.ui.email.SignInActivity;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.text.format.DateFormat;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    private static int SIGN_IN_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    private FirebaseListAdapter<ChatMassage> adapter;
    RelativeLayout activity_main;
    FloatingActionButton fab;
    ImageButton addImage;
    private String mUsername;
    private String mPhotoUrl;
    private FirebaseAuth mfirebaseAuth;
    private FirebaseUser mfirebaseUser;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mfirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<FriendlyMassage,MassageViewHolder> mfirebaseAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mMessageRecyclerView;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_signOut){
            AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Snackbar.make(activity_main,"You Have Been Singed Out",Snackbar.LENGTH_LONG).show();
                    finish();
                }
            });
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SIGN_IN_REQUEST_CODE){
            if(resultCode == RESULT_OK ){
                Snackbar.make(activity_main,"Succesfully Signed In...Welcum",Snackbar.LENGTH_LONG).show();
//                displayChatMassage();
            }
            else
            {
                Snackbar.make(activity_main,"Cannot Sign In, Try again",Snackbar.LENGTH_LONG).show();
                finish();
            }
        }
        else if(requestCode == REQUEST_IMAGE){
            if(resultCode == RESULT_OK){
                if(data != null){
                    final Uri uri = data.getData();
                    FriendlyMassage friendlyMassage = new FriendlyMassage(null,mUsername,mPhotoUrl
                            ,LOADING_IMAGE_URL);
                    mfirebaseDatabaseReference.child("messages").push().setValue(friendlyMassage
                            ,new DatabaseReference.CompletionListener(){
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if(databaseError == null){
                                        String key = databaseReference.getKey();
                                        StorageReference storageReference = FirebaseStorage.getInstance()
                                                .getReference(mfirebaseUser.getUid())
                                                .child(key).child(uri.getLastPathSegment());
                                        putImageInStorage(storageReference,uri,key);
                                    }
                                    else{
                                        Snackbar.make(activity_main,"Unable to write massag to database",Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
            }
        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key){
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    FriendlyMassage friendlyMassage = new FriendlyMassage(null,mUsername,mPhotoUrl,
                            task.getResult().getMetadata().getDownloadUrl().toString());
                    mfirebaseDatabaseReference.child("messages").child(key).setValue(friendlyMassage);
                }
                else{
                    Snackbar.make(activity_main,"Image Upload Not Successfull",Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    public static class MassageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public ImageView messageImageView;
        public TextView messengerTextView;
        public CircleImageView messengerImageView;

        public MassageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity_main = (RelativeLayout)findViewById(R.id.activity_main);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = (EditText)findViewById(R.id.input);
                FriendlyMassage friendlyMassage = new FriendlyMassage(input.getText().toString(),mUsername,mPhotoUrl,null);
                mfirebaseDatabaseReference.child("messages").push().setValue(friendlyMassage);
                input.setText("");
                //                EditText input = (EditText)findViewById(R.id.input);
//                FirebaseDatabase.getInstance().getReference().push().setValue(new ChatMassage(input.getText().toString(),
//                        FirebaseAuth.getInstance().getCurrentUser().getEmail()));
//                input.setText("");
            }
        });

        addImage = (ImageButton)findViewById(R.id.addImage);
        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent,2);
            }
        });

        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mfirebaseAuth = FirebaseAuth.getInstance();
        mfirebaseUser = mfirebaseAuth.getCurrentUser();



        if(mfirebaseUser == null){
            startActivity(new Intent(this, com.example.aldiandika.chachacha.SignInActivity.class));
            finish();

            return;
//            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(),SIGN_IN_REQUEST_CODE);
        }
        else
        {
//            Snackbar.make(activity_main,"Welcome "+FirebaseAuth.getInstance().getCurrentUser().getEmail(),
//                    Snackbar.LENGTH_LONG).show();
            Snackbar.make(activity_main,"Welcome "+FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                    Snackbar.LENGTH_LONG).show();
            mUsername = mfirebaseUser.getDisplayName();

            try {
                mPhotoUrl = mfirebaseUser.getPhotoUrl().toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            displayChatMassage();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        mfirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mfirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMassage, MassageViewHolder>(FriendlyMassage.class,
                R.layout.list_item,MassageViewHolder.class,mfirebaseDatabaseReference.child("messages")) {
            @Override
            protected void populateViewHolder(final MassageViewHolder viewHolder, FriendlyMassage model, int position) {
                if(model.getText() != null){
                    viewHolder.messageTextView.setText(model.getText());
                    viewHolder.messageTextView.setVisibility(View.VISIBLE);
                    viewHolder.messageImageView.setVisibility(View.GONE);
                }
                else
                {
                    String imgUrl = model.getImageUrl();
                    if(imgUrl.startsWith("gs://")){
                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imgUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if(task.isSuccessful()){
                                   String downloadUrl = task.getResult().toString();
                                    Glide.with(viewHolder.messengerImageView.getContext())
                                            .load(downloadUrl)
                                            .into(viewHolder.messageImageView);
                                }
                                else{
                                    Log.w("MainActivity","Getting Download URL Wasn't Successfull",task.getException());
                                }

                            }
                        });
                    }
                    else{
                        Glide.with(viewHolder.messengerImageView.getContext())
                                .load(model.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(View.VISIBLE);
                    viewHolder.messageTextView.setVisibility(View.GONE);
                }

                viewHolder.messengerTextView.setText(model.getName());
                if(model.getPhotoUrl() == null){
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.ic_account));
                }
                else {
                    Glide.with(MainActivity.this).load(model.getPhotoUrl()).into(viewHolder.messengerImageView);
                }
            }
        };
        mfirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver(){
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMassageCount = mfirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();

                if((lastVisiblePosition == -1 || (positionStart >= (friendlyMassageCount-1)
                        && lastVisiblePosition == (positionStart-1)))){
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mfirebaseAdapter);
    }

//    private void displayChatMassage(){
//        ListView listOfMassage = (ListView)findViewById(R.id.listOfmassage);
//        adapter = new FirebaseListAdapter<ChatMassage>(this,ChatMassage.class,R.layout.list_item,
//                FirebaseDatabase.getInstance().getReference()) {
//            @Override
//            protected void populateView(View v, ChatMassage model, int position) {
//                TextView massageText, massageUser, massageTime;
//                massageText = (TextView)v.findViewById(R.id.massage_text);
//                massageUser = (TextView)v.findViewById(R.id.massage_user);
//                massageTime = (TextView)v.findViewById(R.id.massage_time);
//
//                massageText.setText(model.getMessageText());
//                massageUser.setText(model.getMessageUser());
//                massageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",model.getMessageTime()));
//            }
//        };
//        listOfMassage.setAdapter(adapter);
//    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}


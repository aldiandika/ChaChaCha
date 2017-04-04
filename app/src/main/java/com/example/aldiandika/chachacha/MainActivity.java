package com.example.aldiandika.chachacha;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
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
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.text.format.DateFormat;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Date;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    private static int SIGN_IN_REQUEST_CODE = 1;
    private FirebaseListAdapter<ChatMassage> adapter;
    RelativeLayout activity_main;
    FloatingActionButton fab;
    private String mUsername;
    private String mPhotoUrl;
    private FirebaseAuth mfirebaseAuth;
    private FirebaseUser mfirebaseUser;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mfirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<FriendlyMassage,MassageViewHolder> mfirebaseAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mMessageRecyclerView;
    private AdView mAdView;

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

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-9108136624493391~6233334860");

        mAdView = (AdView) findViewById(R.id.ads);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
//                Toast.makeText(getApplicationContext(), "Ad is loaded!", Toast.LENGTH_SHORT).show();
                Log.d("ADS","Ad is loaded!");
            }

            @Override
            public void onAdClosed() {
//                Toast.makeText(getApplicationContext(), "Ad is closed!", Toast.LENGTH_SHORT).show();
                Log.d("ADS","Ad is closed!");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
//                Toast.makeText(getApplicationContext(), "Ad failed to load! error code: " + errorCode, Toast.LENGTH_SHORT).show();
                Log.d("ADS","Ad failed to load! error code: " + errorCode);
            }

            @Override
            public void onAdLeftApplication() {
//                Toast.makeText(getApplicationContext(), "Ad left application!", Toast.LENGTH_SHORT).show();
                Log.d("ADS","Ad left application!");
            }

            @Override
            public void onAdOpened() {
//                Toast.makeText(getApplicationContext(), "Ad is opened!", Toast.LENGTH_SHORT).show();
                Log.d("ADS","Ad is opened!");
            }
        });

        String android_id = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Log.d("android id", android_id);

//        AdRequest adRequest = new AdRequest.Builder().build();
        AdRequest request = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
                .build();
        mAdView.loadAd(request);

        activity_main = (RelativeLayout)findViewById(R.id.activity_main);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = (EditText)findViewById(R.id.input);
                FirebaseDatabase.getInstance().getReference().push().setValue(new ChatMassage(input.getText().toString(),
                        FirebaseAuth.getInstance().getCurrentUser().getEmail()));
                input.setText("");
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
//            mPhotoUrl = mfirebaseUser.getPhotoUrl().toString();
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

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    /** Called when returning to the activity */
    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
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


package com.example.cverdetotoo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class advid extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private List<Video> videoList;  // Use your custom Video model

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advid);

        recyclerView = findViewById(R.id.recyclerViewVideos);
        videoList = new ArrayList<>();

        // Create the adapter with an anonymous OnItemClickListener that implements onItemClick(Video)
        adapter = new VideoAdapter(videoList, new VideoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Video video) {
                // Launch YouTube video via Intent
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getYoutubeUrl()));
                startActivity(intent);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchVideos();
    }

    private void fetchVideos() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("videos")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            // Handle the error if needed
                            return;
                        }
                        videoList.clear();
                        if (snapshots != null) {
                            for (DocumentSnapshot document : snapshots.getDocuments()) {
                                String title = document.getString("title");
                                String description = document.getString("description");
                                String youtubeUrl = document.getString("youtubeUrl");
                                videoList.add(new Video(title, description, youtubeUrl));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}

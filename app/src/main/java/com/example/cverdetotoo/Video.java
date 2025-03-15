package com.example.cverdetotoo;

public class Video {
    private String title;
    private String description;
    private String youtubeUrl;

    // Required empty constructor for Firestore deserialization
    public Video() { }

    public Video(String title, String description, String youtubeUrl) {
        this.title = title;
        this.description = description;
        this.youtubeUrl = youtubeUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }
}

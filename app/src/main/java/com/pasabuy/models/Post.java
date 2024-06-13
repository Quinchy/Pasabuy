package com.pasabuy.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Post {
    private String postID;
    private String userID;
    private String userProvince;
    private String title;
    private String description;
    private double price;
    private Timestamp deadline;
    private Timestamp timePosted;
    private String category;
    private List<String> imageIDs;
    private List<String> joinedUserIDs;
    private String thumbnail;
    private String status;

    // No-argument constructor required for Firebase
    public Post() {}

    // Constructor with all fields
    public Post(String postID, String userID, String userProvince, String title, String description, double price, Timestamp deadline, Timestamp timePosted, String category, List<String> imageIDs, List<String> joinedUserIDs, String thumbnail, String status) {
        this.postID = postID;
        this.userID = userID;
        this.userProvince = userProvince;
        this.title = title;
        this.description = description;
        this.price = price;
        this.deadline = deadline;
        this.timePosted = timePosted;
        this.category = category;
        this.imageIDs = imageIDs;
        this.joinedUserIDs = joinedUserIDs;
        this.thumbnail = thumbnail;
        this.status = status;
    }

    // Getters and Setters
    public String getPostID() {
        return postID;
    }

    public void setPostID(String postID) {
        this.postID = postID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserProvince() {
        return userProvince;
    }

    public void setUserProvince(String userProvince) {
        this.userProvince = userProvince;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Timestamp getTimePosted() {
        return timePosted;
    }

    public void setTimePosted(Timestamp timePosted) {
        this.timePosted = timePosted;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getImageIDs() {
        return imageIDs;
    }

    public void setImageIDs(List<String> imageIDs) {
        this.imageIDs = imageIDs;
    }

    public List<String> getJoinedUserIDs() {
        return joinedUserIDs;
    }

    public void setJoinedUserIDs(List<String> joinedUserIDs) {
        this.joinedUserIDs = joinedUserIDs;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

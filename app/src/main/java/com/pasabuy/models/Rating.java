package com.pasabuy.models;

import com.google.firebase.Timestamp;

public class Rating {
    private String ratingID;
    private String orderID;
    private String ratedByUserID;
    private String postUserID;
    private int rating;
    private Timestamp timestamp;

    // Default constructor required for calls to DataSnapshot.getValue(Rating.class)
    public Rating() {}

    public Rating(String ratingID, String orderID, String ratedByUserID, String postUserID, int rating, Timestamp timestamp) {
        this.ratingID = ratingID;
        this.orderID = orderID;
        this.ratedByUserID = ratedByUserID;
        this.postUserID = postUserID;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public String getRatingID() {
        return ratingID;
    }

    public void setRatingID(String ratingID) {
        this.ratingID = ratingID;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getRatedByUserID() {
        return ratedByUserID;
    }

    public void setRatedByUserID(String ratedByUserID) {
        this.ratedByUserID = ratedByUserID;
    }

    public String getPostUserID() {
        return postUserID;
    }

    public void setPostUserID(String postUserID) {
        this.postUserID = postUserID;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}

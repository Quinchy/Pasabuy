package com.pasabuy.models;

public class Image {
    private String imageID;
    private String imageURL;

    public Image() {}

    public Image(String imageID, String imageURL) {
        this.imageID = imageID;
        this.imageURL = imageURL;
    }

    // Getters and setters
    public String getImageID() {
        return imageID;
    }

    public void setImageID(String imageID) {
        this.imageID = imageID;
    }
    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
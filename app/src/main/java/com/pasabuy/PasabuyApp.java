package com.pasabuy;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class PasabuyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
}

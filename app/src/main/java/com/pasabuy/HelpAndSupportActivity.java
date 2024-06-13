package com.pasabuy;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class HelpAndSupportActivity extends AppCompatActivity {

    private WebView webViewChatbot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_support);

        setupBackButton();
        setupWebView();
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButtonHelpSupport);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupWebView() {
        webViewChatbot = findViewById(R.id.webViewChatbot);
        WebSettings webSettings = webViewChatbot.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webViewChatbot.setWebViewClient(new WebViewClient());
        webViewChatbot.loadUrl("https://www.chatbase.co/chatbot-iframe/ziaUGdlJ07RASbZPrZJ2_");
    }
}

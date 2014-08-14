package ru.babay.lib.twitter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.babay.lib.util.WorkerThread;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.lang.String;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 13.01.13
 * Time: 3:03
 */
public class TwitterActivity extends Activity {
    public static final String ICON_TAG = "ru.babay.tw.icon";
    public static final String ERROR_TAG = "ru.babay.tw.error";
    public static final String TOKEN_TAG = "ru.babay.tw.token";
    public static final String TOKEN_SECRET_TAG = "ru.babay.tw.token_secret";
    public static final String VERIFIER_TAG = "ru.babay.tw.verifier";
    public static final String RETRIVE_TOKEN = "ru.babay.tw.retriveToken";
    static twitter4j.Twitter mTwitter;

    static final int TW_BLUE = 0xFFC0DEED;
    static final int MARGIN = 4;
    static final int PADDING = 2;

    private int mIcon;

    private ProgressDialog mSpinner;
    private WebView mWebView;
    private LinearLayout mContent;
    private TextView mTitle;

    private String mUrl;
    private RequestToken mRequestToken;
    boolean retriveToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!loadParams())
            return;

        mSpinner = new ProgressDialog(this);
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");

        mContent = new LinearLayout(this);
        mContent.setOrientation(LinearLayout.VERTICAL);
        mContent.setBackgroundColor(Color.WHITE);
        setUpTitle();
        setUpWebView();

        setContentView(mContent);

        Intent intent = getIntent();
        retriveToken = intent.getBooleanExtra(RETRIVE_TOKEN, false);

        retrieveRequestToken();
    }

    boolean loadParams() {
        Intent intent = getIntent();
        mIcon = intent.getIntExtra(ICON_TAG, 0);
        return true;
    }

    void resultError(Throwable error) {
        Intent intent = new Intent();
        intent.putExtra(ERROR_TAG, error);
        setResult(RESULT_CANCELED, intent);
        if (mSpinner != null && mSpinner.isShowing())
            mSpinner.dismiss();
        finish();
    }

    void resultCancel() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        if (mSpinner != null && mSpinner.isShowing())
            mSpinner.dismiss();
        finish();
    }

    void resultOk(String token, String tokenSecret, String verifier) {
        Intent intent = new Intent();
        intent.putExtra(TOKEN_TAG, token);
        intent.putExtra(TOKEN_SECRET_TAG, tokenSecret);
        intent.putExtra(VERIFIER_TAG, verifier);
        setResult(RESULT_OK, intent);
        if (mSpinner != null && mSpinner.isShowing())
            mSpinner.dismiss();
        finish();
    }

    private void setUpTitle() {
        Drawable icon = getResources().getDrawable(mIcon);
        mTitle = new TextView(this);
        mTitle.setText("Twitter");
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setBackgroundColor(TW_BLUE);
        mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
        mTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mContent.addView(mTitle, lp);
    }

    private void setUpWebView() {
        mWebView = new WebView(this);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new TwWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mContent.addView(mWebView, lp);
    }

    private void retrieveRequestToken() {
        mSpinner.show();
        WorkerThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mTwitter == null)
                        mTwitter = TwitterB.getTwitterInstance();
                    mRequestToken = mTwitter.getOAuthRequestToken(TwitterB.CALLBACK_URI);
                    mWebView.loadUrl(mUrl = mRequestToken.getAuthorizationURL());
                } catch (TwitterException e) {
                    resultError(new DialogError(e.getMessage(), -1, mUrl));
                }
            }
        });
    }

    private void retrieveAccessToken(final String url) {
        final Uri uri = Uri.parse(url);
        final String verifier = uri.getQueryParameter("oauth_verifier");
        if (!retriveToken)
            resultOk(mRequestToken.getToken(), mRequestToken.getTokenSecret(), verifier);
        else {
            new Thread() {
                @Override
                public void run() {
                    try {
                        AccessToken at = mTwitter.getOAuthAccessToken(mRequestToken, verifier);
                        resultOk(at.getToken(), at.getTokenSecret(), null);
                    } catch (TwitterException e) {
                        resultError(new TwitterError(e.getMessage()));
                    }
                }
            }.start();
        }
        //final String token = uri.getQueryParameter("oauth_token");
        //mSpinner.show();
        /*new Thread() {
            @Override
            public void run() {
                try {
                    AccessToken at = mTwitter.getOAuthAccessToken(mRequestToken, verifier);
                    resultOk(at.getToken(), at.getTokenSecret(), verifier);
                } catch (TwitterException e) {
                    resultError(new TwitterError(e.getMessage()));
                }
            }
        }.start();*/
    }

    private class TwWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(TwitterB.CANCEL_URI) || url.startsWith(TwitterB.DENIED_URI)) {
                resultCancel();
                return true;
            }

            if (url.startsWith(TwitterB.CALLBACK_URI)) {
                retrieveAccessToken(url);
                return true;
            }

            mUrl = url;
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            resultError(new ru.babay.lib.twitter.DialogError(description, errorCode, failingUrl));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            try {
                if (!mSpinner.isShowing())
                    mSpinner.show();
            } catch (Exception e) {
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String title = mWebView.getTitle();
            if (title != null && title.length() > 0) {
                mTitle.setText(title);
            }
            try {
                if (mSpinner.isShowing())
                    mSpinner.dismiss();
            } catch (Exception ignored) {
            }
        }
    }
}

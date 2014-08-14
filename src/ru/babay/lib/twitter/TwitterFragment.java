package ru.babay.lib.twitter;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 12.01.13
 * Time: 5:21
 */
public class TwitterFragment extends DialogFragment {

    public static final String TAG = "twitter";

    static final int TW_BLUE = 0xFFC0DEED;
    static final int MARGIN = 4;
    static final int PADDING = 2;

    private int mIcon;
    private String mUrl;
    private TwitterB.LoginListener mListener;
    private ProgressDialog mSpinner;
    private WebView mWebView;
    private LinearLayout mContent;
    private TextView mTitle;

    private twitter4j.Twitter mTwitter;

    private RequestToken mRequestToken;

    public void setParams(int icon, Twitter twitter, TwitterB.LoginListener listener) {
        mIcon = icon;
        mTwitter = twitter;
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSpinner = new ProgressDialog(getActivity());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");

        mContent = new LinearLayout(getActivity());
        mContent.setOrientation(LinearLayout.VERTICAL);
        mContent.setMinimumHeight(300);
        mContent.setBackgroundColor(Color.WHITE);
        setUpTitle();
        setUpWebView();

        retrieveRequestToken();

        return mContent;
    }

    private void retrieveRequestToken() {
        mSpinner.show();
        new Thread() {
            @Override
            public void run() {
                try {
                    mRequestToken = mTwitter.getOAuthRequestToken(TwitterB.CALLBACK_URI);
                    mUrl = mRequestToken.getAuthorizationURL();
                    mWebView.loadUrl(mUrl);

                } catch (TwitterException e) {
                    mListener.onError(new DialogError(e.getMessage(), -1,
                            TwitterB.OAUTH_REQUEST_TOKEN));
                }
            }
        }.start();
    }

    private void setUpTitle() {
        Drawable icon = getResources().getDrawable(mIcon);
        mTitle = new TextView(getActivity());
        mTitle.setText("Twitter");
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setBackgroundColor(TW_BLUE);
        mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
        mTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        mContent.addView(mTitle);
    }

    private void setUpWebView() {
        mWebView = new WebView(getActivity());
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new TwWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        mContent.addView(mWebView, lp);
    }

    private void retrieveAccessToken(String url) {
        mSpinner.show();
        //final Uri uri = Uri.parse(url);
        //final String verifier = uri.getQueryParameter("oauth_verifier");
        new Thread() {
            @Override
            public void run() {
                try {
                    AccessToken at = mTwitter.getOAuthAccessToken(mRequestToken);
                    mListener.onComplete(at.getToken(), at.getTokenSecret(), null);
                } catch (TwitterException e) {
                    mListener.onError(new TwitterError(e.getMessage()));
                }
                mContent.post(new Runnable() {
                    public void run() {
                        mSpinner.dismiss();
                        dismiss();
                    }
                });
            }
        }.start();
    }

    private class TwWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Redirect URL: " + url);

            if (url.startsWith(TwitterB.CANCEL_URI) || url.startsWith(TwitterB.DENIED_URI)) {
                mListener.onCancel();
                TwitterFragment.this.dismiss();
                return true;
            }

            if (url.startsWith(TwitterB.CALLBACK_URI)) {
                retrieveAccessToken(url);
                return true;
            }

            mUrl = url;
            //mWebView.loadUrl(mUrl);
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onError(new DialogError(description, errorCode,
                    failingUrl));
            TwitterFragment.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "WebView loading URL: " + url);
            super.onPageStarted(view, url, favicon);
            if (mSpinner.isShowing()) {
                mSpinner.dismiss();
            }
            mSpinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String title = mWebView.getTitle();
            if (title != null && title.length() > 0) {
                mTitle.setText(title);
            }
            mSpinner.dismiss();
        }
    }
}

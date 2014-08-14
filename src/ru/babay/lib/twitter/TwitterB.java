package ru.babay.lib.twitter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 12.01.13
 * Time: 5:01
 */
public class TwitterB {

    public static final String CALLBACK_URI = "twitter://callback";
    public static final String DENIED_URI = "twitter://callback?denied=";
    public static final String CANCEL_URI = "twitter://cancel";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SECRET_TOKEN = "secret_token";

    public static final String REQUEST = "request";
    public static final String AUTHORIZE = "authorize";

    protected static String REQUEST_ENDPOINT = "https://api.twitter.com/1";

    protected static String OAUTH_REQUEST_TOKEN = "https://api.twitter.com/oauth/request_token";
    protected static String OAUTH_ACCESS_TOKEN = "https://api.twitter.com/oauth/access_token";
    protected static String OAUTH_AUTHORIZE = "https://api.twitter.com/oauth/authorize";

    private int mIcon;
    private twitter4j.Twitter mTwitter;
    private static String consumerKey;
    private static String consumerSecret;

    public static interface LoginListener {
        public void onComplete(String token, String token_secret, String verifier);
        public void onError(Throwable e);
        public void onCancel();
    }

    public TwitterB(int icon, String consumerKey, String consumerSecret) {
        mIcon = icon;

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(consumerKey);
        configurationBuilder.setOAuthConsumerSecret(consumerSecret);
        configurationBuilder.setDebugEnabled(true);
        Configuration configuration = configurationBuilder.build();
        TwitterFactory twitterFactory = new TwitterFactory(configuration);
        mTwitter = twitterFactory.getInstance();
        TwitterB.consumerKey = consumerKey;
        TwitterB.consumerSecret = consumerSecret;
    }

    public static Twitter getTwitterInstance(){
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(consumerKey);
        configurationBuilder.setOAuthConsumerSecret(consumerSecret);
        configurationBuilder.setDebugEnabled(true);
        Configuration configuration = configurationBuilder.build();
        TwitterFactory twitterFactory = new TwitterFactory(configuration);
        return twitterFactory.getInstance();
    }

    public void authorize(Context context, Fragment fragment, int requestCode, boolean retriveOAuthToken){
        Intent intent = new Intent(context, TwitterActivity.class);
        TwitterActivity.mTwitter = mTwitter;
        intent.putExtra(TwitterActivity.ICON_TAG, mIcon);
        intent.putExtra(TwitterActivity.RETRIVE_TOKEN, retriveOAuthToken);
        fragment.startActivityForResult(intent, requestCode);
    }

    public void authorize(Context context, Fragment fragment, int requestCode){
        authorize(context, fragment, requestCode, false);
    }

    public void authorize(Context context, FragmentManager fm, LoginListener listener){
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        TwitterFragment newFragment = new TwitterFragment();
        newFragment.setParams(mIcon, mTwitter, listener);
        newFragment.show(ft, "dialog");
    }

    public static void receiveResult(int resultCode, Intent data, LoginListener callback){
        //Note! twitter do not expire!

        if (resultCode == Activity.RESULT_OK){
            callback.onComplete(data.getStringExtra(TwitterActivity.TOKEN_TAG),
                    data.getStringExtra(TwitterActivity.TOKEN_SECRET_TAG),
                    data.getStringExtra(TwitterActivity.VERIFIER_TAG));
        } else {
            if (data != null && data.hasExtra(TwitterActivity.ERROR_TAG))
                callback.onError((Throwable)data.getSerializableExtra(TwitterActivity.ERROR_TAG));
            else
                callback.onCancel();
        }
    }

    public Twitter getTwitter() {
        return mTwitter;
    }

    public static void logoff(Context context){
        CookieSyncManager.createInstance(context);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }
}

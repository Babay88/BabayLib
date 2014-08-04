package ru.babay.lib;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 07.08.13
 * Time: 22:35
 */
public class MyLocationManager implements LocationListener {
    private static final int MIN_TIME_UPDATE_LOCATION = 30 * 1000;
    private static final int MIN_DISTANCE_UPDATE_LOCATION = 100; // 100 meters

    private static final int LOCATION_OUTDATE_INTERVAL = 1000 * 60 * 5;

    LocationManager systemLocationService;
    Location currentBestLocation;

    static MyLocationManager instance;

    public static void start(Context context){
        getInstance(context).start1();
    }

    public static Location getLocation(Context context){
        return getInstance(context).currentBestLocation;
    }

    public static void stop(){
        if (instance != null)
            instance.stop1();
    }

    public static MyLocationManager getInstance(Context context){
        if (instance == null)
            instance = new MyLocationManager(context);
        return instance;
    }

    public MyLocationManager(Context context) {
        systemLocationService = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void start1(){
        for (String provider: systemLocationService.getAllProviders()){
            onLocationChanged(systemLocationService.getLastKnownLocation(provider));
            systemLocationService.requestLocationUpdates(provider, MIN_TIME_UPDATE_LOCATION, MIN_DISTANCE_UPDATE_LOCATION, this);
        }
    }

    public void stop1(){
        for (String provider: systemLocationService.getAllProviders()){
            onLocationChanged(systemLocationService.getLastKnownLocation(provider));
            systemLocationService.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && isBetterLocation(location, currentBestLocation))
            currentBestLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > LOCATION_OUTDATE_INTERVAL;
        boolean isSignificantlyOlder = timeDelta < -LOCATION_OUTDATE_INTERVAL;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}

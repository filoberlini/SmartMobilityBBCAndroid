package bbc.unibo.it.smartmoblitybbc.utils.gps;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Message;

import bbc.unibo.it.smartmoblitybbc.MainActivity;

/**
 * Created by brando on 04/06/2015.
 */
public class GPSListener implements LocationListener {
    private MainActivity.GPSServiceHandler gpsServiceHandler;
    public GPSListener(MainActivity.GPSServiceHandler gpsServiceHandler){
        this.gpsServiceHandler = gpsServiceHandler;
    }
    public void onLocationChanged(Location location) {
        // Called when a new location is found by the network location provider.
        this.gpsServiceHandler.obtainMessage(0, location).sendToTarget();
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
}

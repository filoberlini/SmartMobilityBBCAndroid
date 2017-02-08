package bbc.unibo.it.smartmoblitybbc.utils.gps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

/**
 * Created by brando on 04/06/2015.
 */
public class GPSUtils {
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 3;
    private static final long MIN_TIME_BW_UPDATES = 500;

    public static void initGPS(GPSListener listener, Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, listener);
    }
}

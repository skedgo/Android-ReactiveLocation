package pl.charmas.android.reactivelocation.observables.location;

import android.content.Context;
import android.location.Location;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import pl.charmas.android.reactivelocation.observables.BaseLocationObservable;
import rx.Observable;
import rx.Observer;

public class LocationUpdatesObservable extends BaseLocationObservable<Location> {

    private static final String TAG = LocationUpdatesObservable.class.getSimpleName();

    public static Observable<Location> createObservable(Context ctx, LocationRequest locationRequest) {
        return Observable.create(new LocationUpdatesObservable(ctx, locationRequest));
    }

    private final LocationRequest locationRequest;
    @Nullable private UnsubscribableLocationListener listener;

    private LocationUpdatesObservable(Context ctx, LocationRequest locationRequest) {
        super(ctx);
        this.locationRequest = locationRequest;
    }

    @Override
    protected void onGoogleApiClientReady(GoogleApiClient apiClient, final Observer<? super Location> observer) {
        listener = new UnsubscribableLocationListener(observer);
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, listener);
    }

    @Override
    protected void onUnsubscribed(GoogleApiClient locationClient) {
        if (locationClient.isConnected() && listener != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(locationClient, listener);
            listener.unsubscribe();
        }
    }

    static class UnsubscribableLocationListener implements LocationListener {
        private Observer<? super Location> observer;

        public UnsubscribableLocationListener(Observer<? super Location> observer) {
            this.observer = observer;
        }

        @Override
        public void onLocationChanged(Location location) {
            if (observer != null) {
                observer.onNext(location);
            }
        }

        void unsubscribe() {
            this.observer = null;
        }
    }
}

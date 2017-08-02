package pl.charmas.android.reactivelocation.observables;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;


public abstract class BaseObservable<T> implements Observable.OnSubscribe<T> {
    private final Context ctx;
    private final List<Api<? extends Api.ApiOptions.NotRequiredOptions>> services;

    @SafeVarargs
    protected BaseObservable(Context ctx, Api<? extends Api.ApiOptions.NotRequiredOptions>... services) {
        this.ctx = ctx;
        this.services = Arrays.asList(services);
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {

        final ApiClientConnectionCallbacks apiClientConnectionCallbacks = new ApiClientConnectionCallbacks(subscriber);

        GoogleApiClient.Builder apiClientBuilder = new GoogleApiClient.Builder(ctx);

        for (Api<? extends Api.ApiOptions.NotRequiredOptions> service : services) {
            apiClientBuilder.addApi(service);
        }

        apiClientBuilder.addConnectionCallbacks(apiClientConnectionCallbacks);
        apiClientBuilder.addOnConnectionFailedListener(apiClientConnectionCallbacks);

        final GoogleApiClient apiClient = apiClientBuilder.build();
        apiClientConnectionCallbacks.setClient(apiClient);

        try {
            apiClient.connect();
        } catch (Throwable ex) {
            subscriber.onError(ex);
        }

        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (apiClient.isConnected() || apiClient.isConnecting()) {
                    onUnsubscribed(apiClient);
                    apiClient.disconnect();
                    apiClient.unregisterConnectionFailedListener(apiClientConnectionCallbacks);
                    apiClient.unregisterConnectionCallbacks(apiClientConnectionCallbacks);
                }
            }
        }));
    }


    protected void onUnsubscribed(GoogleApiClient locationClient) {
    }

    protected abstract void onGoogleApiClientReady(GoogleApiClient apiClient, Observer<? super T> observer);

    private class ApiClientConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        final private Subscriber<? super T> subscriber;

        private GoogleApiClient apiClient;

        private ApiClientConnectionCallbacks(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onConnected(Bundle bundle) {
            if (subscriber.isUnsubscribed()) {
                return;
            }
            try {
                onGoogleApiClientReady(apiClient, subscriber);
            } catch (Throwable ex) {
                subscriber.onError(ex);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            subscriber.onError(new GoogleAPIConnectionSuspendedException(cause));
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            subscriber.onError(new GoogleAPIConnectionException("Error connecting to GoogleApiClient.", connectionResult));
        }

        public void setClient(GoogleApiClient client) {
            this.apiClient = client;
        }
    }

}

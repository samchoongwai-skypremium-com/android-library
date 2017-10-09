/* Copyright 2017 Urban Airship and Contributors */

package com.urbanairship.automation;

import com.urbanairship.ActivityMonitor;
import com.urbanairship.Cancelable;
import com.urbanairship.json.JsonSerializable;
import com.urbanairship.json.JsonValue;
import com.urbanairship.reactive.Function;
import com.urbanairship.reactive.Observable;
import com.urbanairship.reactive.Observer;
import com.urbanairship.reactive.Subscription;

/**
 * Factory methods for creating compound trigger observables
 */
public class TriggerObservables {
    /**
     * Creates a state observable that sends onNext if the app is currently foregrounded,
     * and completes.
     *
     * @param monitor An instance of ActivityMonitor.
     * @return An Observable of JsonSerializable.
     */
    public static Observable<JsonSerializable> foregrounded(final ActivityMonitor monitor) {
        return Observable.create(new Function<Observer<JsonSerializable>, Subscription>() {
            @Override
            public Subscription apply(Observer<JsonSerializable> observer) {
                if (monitor.isAppForegrounded()) {
                    observer.onNext(JsonValue.NULL);
                }
                observer.onCompleted();
                return Subscription.empty();
            }
        });
    }

    /**
     * Creates an event observable that sends onNext when a new session begins.
     *
     * @param monitor An instance of ActivityMonitor.
     * @return An Observable of JsonSerializable.
     */
    public static Observable<JsonSerializable> newSession(final ActivityMonitor monitor) {
        return Observable.create(new Function<Observer<JsonSerializable>, Subscription>() {
            @Override
            public Subscription apply(final Observer<JsonSerializable> observer) {
                final ActivityMonitor.SimpleListener listener = new ActivityMonitor.SimpleListener() {
                    @Override
                    public void onForeground(long time) {
                        observer.onNext(JsonValue.NULL);
                    }
                };

                monitor.addListener(listener);

                return Subscription.create(new Runnable() {
                    @Override
                    public void run() {
                        monitor.removeListener(listener);
                    }
                });
            }
        });
    }
}

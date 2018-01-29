/* Copyright 2018 Urban Airship and Contributors */

package com.urbanairship.reactive;

import android.support.annotation.RestrictTo;

import java.util.ArrayList;

/**
 * Subject implementation. A Subject is both an Observer and an Observable.
 *
 * @param <T> The type under observation.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Subject<T> extends Observable<T> implements Observer<T> {

    private ArrayList<Observer<T>> observers;
    private boolean completed = false;
    private Exception error;

    /**
     * Subject constructor.
     */
    Subject() {
        this.observers = new ArrayList<>();
    }

    /**
     * Creates a new Subject.
     *
     * @param <T> The type under observation
     * @return A Subject of the underlying type.
     */
    public static <T> Subject<T> create() {
        return new Subject<>();
    }

    /**
     * {@code true} if an error has been observed, {@code false} otherwise.
     *
     * @return {@code true} if an error has been observed, {@code false} otherwise.
     */
    synchronized boolean hasError() {
        return error != null;
    }

    /**
     * {@code true} if the subject is completed, {@code false} otherwise.
     *
     * @return {@code true} if the subject is completed, {@code false} otherwise.
     */
    synchronized boolean isCompleted() {
        return completed;
    }

    /**
     * {@code true} if the subject has any subscribed observers, {@code false} otherwise.
     *
     * @return {@code true} if the subject has any subscribed observers, {@code false} otherwise.
     */
    synchronized boolean hasObservers() {
        return this.observers.size() > 0;
    }

    @Override
    synchronized
    public void onNext(T value) {
        for (Observer<T> observer : new ArrayList<>(observers)) {
            observer.onNext(value);
        }
    }

    @Override
    synchronized
    public void onCompleted() {
        completed = true;
        for (Observer<T> observer : new ArrayList<>(observers)) {
            observer.onCompleted();
        }
    }

    @Override
    synchronized
    public void onError(Exception e) {
        error = e;
        for (Observer<T> observer : new ArrayList<>(observers)) {
            observer.onError(e);
        }
    }

    @Override
    synchronized
    public Subscription subscribe(final Observer<T> observer) {
        if (!isCompleted() && !hasError()) {
            observers.add(observer);
        }
        return Subscription.create(new Runnable() {
            @Override
            public void run() {
                if (hasObservers()) {
                    observers.remove(observer);
                }
            }
        });
    }
}

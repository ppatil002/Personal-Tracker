package com.example.pp.core.event;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * A simple centralized event bus using LiveData for the observer/publisher pattern.
 */
public class EventBus {

    private static EventBus sInstance;
    private final MutableLiveData<AppEvent> eventPublisher = new MutableLiveData<>();

    private EventBus() {}

    public static EventBus getInstance() {
        if (sInstance == null) {
            sInstance = new EventBus();
        }
        return sInstance;
    }

    public void publish(AppEvent event) {
        // Use postValue if calling from background thread
        eventPublisher.postValue(event);
    }

    public LiveData<AppEvent> observe() {
        return eventPublisher;
    }

    public enum AppEvent {
        CATEGORIES_LOADED_FROM_DB,
        TAGS_LOADED_FROM_DB,
        DATABASE_SEEDING_COMPLETED,
        SYNC_COMPLETED,
        CACHE_REFRESHED
    }
}
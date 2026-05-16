package com.example.pp.core.cache;

import com.example.pp.features.category.model.Tag;

import java.util.ArrayList;
import java.util.List;

public class TagCacheManager {
    private static volatile TagCacheManager instance;
    private List<Tag> cachedTags;
    private boolean isDirty = true;

    private TagCacheManager() {
        this.cachedTags = new ArrayList<>();
    }

    public static TagCacheManager getInstance() {
        if (instance == null) {
            synchronized (TagCacheManager.class) {
                if (instance == null) {
                    instance = new TagCacheManager();
                }
            }
        }
        return instance;
    }

    public List<Tag> getCachedTags() {
        return cachedTags;
    }

    public void updateCache(List<Tag> newTags) {
        this.cachedTags = newTags;
        this.isDirty = false;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
}
package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.core.cache.TagCacheManager;
import com.example.pp.core.enums.AppEvent;
import com.example.pp.core.event.EventBus;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.TagDao;
import com.example.pp.data.local.entity.TagEntity;
import com.example.pp.features.category.model.Tag;

import java.util.ArrayList;
import java.util.List;

public class TagRepository {
    private final TagDao tagDao;
    private final TagCacheManager cacheManager;
    private final AppExecutors executors;
    private final MutableLiveData<List<Tag>> tagsLiveData = new MutableLiveData<>();

    public TagRepository(Context context) {
        this.tagDao = AppDatabase.getInstance(context).tagDao();
        this.cacheManager = TagCacheManager.getInstance();
        this.executors = AppExecutors.getInstance();
    }

    public LiveData<List<Tag>> getTags() {
        refreshTags();
        return tagsLiveData;
    }

    private void refreshTags() {
        executors.diskIO().execute(() -> {
            if (cacheManager.isDirty() || tagsLiveData.getValue() == null) {
                List<TagEntity> entities = tagDao.getAllTags();
                List<Tag> tags = new ArrayList<>();
                for (TagEntity entity : entities) {
                    tags.add(new Tag(entity.getTagId(), entity.getName()));
                }
                cacheManager.updateCache(tags);
                tagsLiveData.postValue(tags);
                
                EventBus.getInstance().publish(AppEvent.TAGS_LOADED_FROM_DB);
            }
        });
    }

    public void addTag(String name, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            Integer maxId = tagDao.getMaxTagId();
            String tagId = String.valueOf(maxId == null ? 1 : maxId + 1);
            TagEntity tag = new TagEntity(tagId, name, System.currentTimeMillis());
            tagDao.insert(tag);
            cacheManager.setDirty(true);
            refreshTags();
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void updateTag(TagEntity tag, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            tag.setLastUpdatedTimestamp(System.currentTimeMillis());
            tagDao.update(tag);
            cacheManager.setDirty(true);
            refreshTags();
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void deleteTag(TagEntity tag, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            tagDao.delete(tag);
            cacheManager.setDirty(true);
            refreshTags();
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void seedTagsIfEmpty() {
        executors.diskIO().execute(() -> {
            List<TagEntity> existing = tagDao.getAllTags();
            if (existing.isEmpty()) {
                long now = System.currentTimeMillis();
                String[] seedNames = {"Office", "Outside", "Fruits", "Vegetables", "Eggs", "Gym", "Subscriptions", "Dreams", "Grooming", "Auto", "Bus", "Cab", "Train", "Flight"};
                for (int i = 0; i < seedNames.length; i++) {
                    tagDao.insert(new TagEntity(String.valueOf(i + 1), seedNames[i], now));
                }
                cacheManager.setDirty(true);
                refreshTags();
            }
        });
    }
}
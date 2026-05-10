package com.example.pp.core.cache;

import com.example.pp.domain.model.CategoryTreeNode;
import java.util.ArrayList;
import java.util.List;

public class CategoryCacheManager {
    private static volatile CategoryCacheManager instance;
    private List<CategoryTreeNode> cachedTree;
    private boolean isDirty = true;

    private CategoryCacheManager() {
        this.cachedTree = new ArrayList<>();
    }

    public static CategoryCacheManager getInstance() {
        if (instance == null) {
            synchronized (CategoryCacheManager.class) {
                if (instance == null) {
                    instance = new CategoryCacheManager();
                }
            }
        }
        return instance;
    }

    public List<CategoryTreeNode> getCachedTree() {
        return cachedTree;
    }

    public void updateCache(List<CategoryTreeNode> newTree) {
        this.cachedTree = newTree;
        this.isDirty = false;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
}
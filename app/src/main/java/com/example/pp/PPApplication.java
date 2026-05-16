package com.example.pp;

import android.app.Application;

import com.example.pp.data.repository.CategoryRepository;
import com.example.pp.data.repository.GenericTableRepository;
import com.example.pp.data.repository.TagRepository;
import com.example.pp.data.repository.TransactionRepository;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class PPApplication extends Application {
    private CategoryRepository categoryRepository;
    private TagRepository tagRepository;
    private TransactionRepository transactionRepository;
    private GenericTableRepository genericTableRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        categoryRepository = new CategoryRepository(this);
        tagRepository = new TagRepository(this);
        transactionRepository = new TransactionRepository(this);
        genericTableRepository = new GenericTableRepository(this);
    }

    public CategoryRepository getCategoryRepository() {
        return categoryRepository;
    }

    public TagRepository getTagRepository() {
        return tagRepository;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public GenericTableRepository getGenericTableRepository() {
        return genericTableRepository;
    }
}
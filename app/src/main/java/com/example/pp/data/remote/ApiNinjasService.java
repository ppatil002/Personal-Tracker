package com.example.pp.data.remote;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ApiNinjasService {

    @GET("v1/dayinhistory")
    Call<List<HistoryResponse>> getDayInHistory(
            @Header("X-Api-Key") String apiKey
    );

    @GET("v2/quoteoftheday")
    Call<List<QuoteResponse>> getQuoteOfTheDay(
            @Header("X-Api-Key") String apiKey
    );

    @GET("v2/randomquotes")
    Call<List<QuoteResponse>> getRandomQuotes(
            @Header("X-Api-Key") String apiKey,
            @Query("exclude_categories") String excludeCategories
    );

    class HistoryResponse {
        public int year;
        public int month;
        public int day;
        public String event;
    }

    class QuoteResponse {
        public String quote;
        public String author;
        public String work;
        public List<String> categories;
    }
}

package com.example.pp.core.preferences;

import android.content.Context;
import com.example.pp.data.remote.ApiNinjasService;

public class QuoteHistoryPrefs extends BasePreferenceHelper {
    private static final String KEY_LATEST_QUOTE = "latest_quote";
    private static final String KEY_LATEST_HISTORY = "latest_history";
    private static final String KEY_LAST_FETCH_DATE = "last_fetch_date";

    public QuoteHistoryPrefs(Context context) {
        super(context);
    }

    public void saveLatestQuote(ApiNinjasService.QuoteResponse quote) {
        putObject(KEY_LATEST_QUOTE, quote);
    }

    public ApiNinjasService.QuoteResponse getLatestQuote() {
        return getObject(KEY_LATEST_QUOTE, ApiNinjasService.QuoteResponse.class);
    }

    public void saveLatestHistory(ApiNinjasService.HistoryResponse history) {
        putObject(KEY_LATEST_HISTORY, history);
    }

    public ApiNinjasService.HistoryResponse getLatestHistory() {
        return getObject(KEY_LATEST_HISTORY, ApiNinjasService.HistoryResponse.class);
    }

    public void saveLastFetchDate(String date) {
        putString(KEY_LAST_FETCH_DATE, date);
    }

    public String getLastFetchDate() {
        return getString(KEY_LAST_FETCH_DATE, "");
    }
}

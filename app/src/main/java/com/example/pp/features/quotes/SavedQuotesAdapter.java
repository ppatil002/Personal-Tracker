package com.example.pp.features.quotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.entity.SavedQuote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SavedQuotesAdapter extends RecyclerView.Adapter<SavedQuotesAdapter.ViewHolder> {
    private List<SavedQuote> quotes = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
    private OnQuoteActionListener listener;

    public interface OnQuoteActionListener {
        void onEdit(SavedQuote quote);
        void onDelete(SavedQuote quote);
    }

    public void setListener(OnQuoteActionListener listener) {
        this.listener = listener;
    }

    public void setQuotes(List<SavedQuote> newQuotesList) {
        // Group by quote text to prevent duplicates if multiple categories exist
        Map<String, SavedQuote> grouped = new LinkedHashMap<>();
        for (SavedQuote q : newQuotesList) {
            if (!grouped.containsKey(q.quote)) {
                // Create a shallow copy to avoid modifying the original if it's cached
                SavedQuote copy = new SavedQuote(q.quote, q.author, q.category, q.notes, q.createdDate);
                copy.id = q.id;
                grouped.put(q.quote, copy);
            } else {
                SavedQuote existing = grouped.get(q.quote);
                if (existing != null && q.category != null && !q.category.isEmpty()) {
                    Set<String> categories = new LinkedHashSet<>();
                    if (existing.category != null && !existing.category.isEmpty()) {
                        for (String s : existing.category.split(",")) {
                            String t = s.trim();
                            if (!t.isEmpty()) categories.add(t);
                        }
                    }
                    for (String s : q.category.split(",")) {
                        String t = s.trim();
                        if (!t.isEmpty()) categories.add(t);
                    }
                    existing.category = String.join(", ", categories);
                }
            }
        }
        
        List<SavedQuote> newGroupedList = new ArrayList<>(grouped.values());
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return quotes.size();
            }

            @Override
            public int getNewListSize() {
                return newGroupedList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return quotes.get(oldItemPosition).quote.equals(newGroupedList.get(newItemPosition).quote);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                SavedQuote oldQ = quotes.get(oldItemPosition);
                SavedQuote newQ = newGroupedList.get(newItemPosition);
                return oldQ.quote.equals(newQ.quote) && 
                       oldQ.category.equals(newQ.category) &&
                       (oldQ.notes == null ? newQ.notes == null : oldQ.notes.equals(newQ.notes));
            }
        });

        this.quotes = newGroupedList;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_quote, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedQuote quote = quotes.get(position);
        holder.tvQuote.setText("“ " + quote.quote + " ”");
        holder.tvAuthor.setText(quote.author);
        holder.tvCategory.setText(quote.category.toUpperCase());
        holder.tvDate.setText("Saved on " + sdf.format(new Date(quote.createdDate)));

        if (quote.notes != null && !quote.notes.isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.notesDivider.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(quote.notes);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
            holder.notesDivider.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(quote);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(quote);
        });
    }

    @Override
    public int getItemCount() {
        return quotes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuote, tvAuthor, tvCategory, tvNotes, tvDate;
        View notesDivider;
        View btnEdit, btnDelete;

        ViewHolder(View view) {
            super(view);
            tvQuote = view.findViewById(R.id.tvSavedQuoteText);
            tvAuthor = view.findViewById(R.id.tvSavedQuoteAuthor);
            tvCategory = view.findViewById(R.id.tvSavedQuoteCategory);
            tvNotes = view.findViewById(R.id.tvSavedQuoteNotes);
            tvDate = view.findViewById(R.id.tvSavedDate);
            notesDivider = view.findViewById(R.id.notesDivider);
            btnEdit = view.findViewById(R.id.btnEditQuote);
            btnDelete = view.findViewById(R.id.btnDeleteQuote);
        }
    }
}

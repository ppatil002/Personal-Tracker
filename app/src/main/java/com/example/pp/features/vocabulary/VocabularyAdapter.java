package com.example.pp.features.vocabulary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.entity.Vocabulary;

import java.util.ArrayList;
import java.util.List;

public class VocabularyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_WORD = 1;

    public interface ListItem {}

    public interface OnItemClickListener {
        void onItemClick(Vocabulary vocabulary);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class HeaderItem implements ListItem {
        public String date;
        public HeaderItem(String date) { this.date = date; }
    }

    public static class WordItem implements ListItem {
        public Vocabulary vocabulary;
        public WordItem(Vocabulary vocabulary) { this.vocabulary = vocabulary; }
    }

    private final List<ListItem> items = new ArrayList<>();

    public void setItems(List<ListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addItems(List<ListItem> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof HeaderItem) ? TYPE_HEADER : TYPE_WORD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vocabulary_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vocabulary_word, parent, false);
            return new WordViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HeaderItem) items.get(position));
        } else {
            WordItem item = (WordItem) items.get(position);
            ((WordViewHolder) holder).bind(item);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item.vocabulary);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(View v) {
            super(v);
            tvHeader = v.findViewById(R.id.tvHeader);
        }
        void bind(HeaderItem item) {
            tvHeader.setText(item.date);
        }
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        TextView tvWord, tvPartOfSpeech, tvMeaning, tvExample;
        WordViewHolder(View v) {
            super(v);
            tvWord = v.findViewById(R.id.tvWord);
            tvPartOfSpeech = v.findViewById(R.id.tvPartOfSpeech);
            tvMeaning = v.findViewById(R.id.tvMeaning);
            tvExample = v.findViewById(R.id.tvExample);
        }
        void bind(WordItem item) {
            Vocabulary v = item.vocabulary;
            tvWord.setText(v.word);
            tvPartOfSpeech.setText(v.partOfSpeech);
            tvMeaning.setText(v.meaning);
            if (v.example != null && !v.example.isEmpty()) {
                tvExample.setText("Example: " + v.example);
                tvExample.setVisibility(View.VISIBLE);
            } else {
                tvExample.setVisibility(View.GONE);
            }
        }
    }
}

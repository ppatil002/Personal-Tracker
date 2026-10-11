package com.example.pp.domain.model;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VocabularyLlmResponse {

    @SerializedName("word")
    private String word;

    @SerializedName("partOfSpeech")
    private String partOfSpeech;

    @SerializedName("meaning")
    private String meaning;

    @SerializedName("example")
    private String example;

    public VocabularyLlmResponse() {
    }

    public VocabularyLlmResponse(String word, String partOfSpeech, String meaning, String example) {
        this.word = word;
        this.partOfSpeech = partOfSpeech;
        this.meaning = meaning;
        this.example = example;
    }
}

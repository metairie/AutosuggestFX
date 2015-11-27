package org.fxpart.common.util;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 *  -------------------------------------------
 *  This class is not used yet
 *  -------------------------------------------
 *
 * Word
 */
public class Word {
    /**
     * The value
     */
    private String value;
    /**
     * Indicator value is found
     */
    private boolean found;

    /**
     * Default constructor
     *
     * @param value word value
     * @param found is found
     */
    public Word(String value, boolean found) {

        this.value = value;
        this.found = found;
    }

    /**
     * Word value
     *
     * @return string
     */
    public String getValue() {
        return value;
    }

    /**
     * Indicate if the word is found
     *
     * @return boolean
     */
    public boolean isFound() {
        return found;
    }

    /**
     * Convert a fullText to word split by searchTern
     *
     * @param fullText   sentence
     * @param searchTerm search term
     * @return list of Word
     */
    public static List<Word> convertToWord(String fullText, String searchTerm) {
        // no more text
        if (fullText.isEmpty()) {
            return new ArrayList<>();
        }
        // search index of the searchTerm
        int index = fullText.toLowerCase().indexOf(searchTerm.toLowerCase());

        // no word found
        if (index == -1) {
            return Lists.newArrayList(new Word(fullText, false));
        }
        int indexWordEnd = index + searchTerm.length();
        List<Word> words = new ArrayList<>();

        if (index == 0) {
            // the word begin
            words.add(new Word(fullText.substring(index, indexWordEnd), true));
            words.addAll(convertToWord(fullText.substring(indexWordEnd, fullText.length()), searchTerm));
        } else if (indexWordEnd == fullText.length()) {
            // word at the end
            words.add(new Word(fullText.substring(index, indexWordEnd), false));
        } else {
            // word in the middle
            words.add(new Word(fullText.substring(0, index), false));
            words.add(new Word(fullText.substring(index, indexWordEnd), true));
            words.addAll(convertToWord(fullText.substring(indexWordEnd, fullText.length()), searchTerm));
        }

        return words;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Word)) {
            return false;
        }

        Word word = (Word) o;

        return isFound() == word.isFound() && getValue().equals(word.getValue());
    }

    @Override
    public int hashCode() {
        int result = getValue().hashCode();
        result = 31 * result + (isFound() ? 1 : 0);
        return result;
    }
}

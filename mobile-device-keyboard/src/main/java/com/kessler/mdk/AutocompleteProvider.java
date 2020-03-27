package com.kessler.mdk;

import java.util.List;

public interface AutocompleteProvider {
    List<Candidate> getWords(String fragment);
    void train(String passage);
}

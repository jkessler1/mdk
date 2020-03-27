package com.kessler.mdk.trie;

import com.kessler.mdk.AutocompleteProvider;
import com.kessler.mdk.Candidate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An implementation of the AutocompleteProvider interface that utilizes a Trie data structure. All methods within
 * this implementation are thread safe.
 */
public class TrieAutocompleteProvider implements AutocompleteProvider {
    private final String wordBoundaries;
    final ArrayTrieNode root = new ArrayTrieNode('0', false);
    private final ExecutorService passageTrainer = Executors.newCachedThreadPool();

    public TrieAutocompleteProvider(String wordBoundaries) {
        this.wordBoundaries = wordBoundaries;
    }

    /**
     * Traverses the trie structure based on the fragment to find the point from which suggestions
     * should be created. From there the List of Candidates is populated recursively.
     * @param fragment The fragment that requires autocompletion suggestions
     * @return A list of Candidates for autocompletion, ordered by confidence.
     */
    @Override
    public List<Candidate> getWords(String fragment) {
        Optional<ArrayTrieNode> currentNode = Optional.of(root);
        // Traverse to the lowest point in the trie based on the incoming fragment.
        char[] fragmentCharacters = fragment.toCharArray();
        for (char fragmentCharacter : fragmentCharacters) {
            if(currentNode.isPresent()) {
                currentNode = currentNode.get().getChild(fragmentCharacter);
            } else {
                // If we are unable to find a child node for the given character then we are unable to generate Candidates
                return Collections.emptyList();
            }
        }

        // Ensure a node existed for the last character in the fragment. If so, recursively populate our Candidate list.
        if(currentNode.isPresent()) {
            List<Candidate> candidateList = new ArrayList<>();
            currentNode.get().getCandidates(candidateList, fragment.substring(0, fragment.length() - 1).toCharArray());
            candidateList.sort(Comparator.naturalOrder());
            return candidateList;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Submits a new passage to our executor to be "trained".
     * @param passage The new passage to train into our trie.
     */
    @Override
    public void train(String passage) {
        passageTrainer.execute(() -> performTrain(passage));
    }

    /**
     * Splits the provided passage into individual words to train into our trie.
     * @param passage The new passage to train into our trie
     */
    void performTrain(String passage) {
        String[] wordsToTrain = passage.toLowerCase().split(wordBoundaries);
        Arrays.stream(wordsToTrain).forEach(word -> {
            char[] wordCharacters = word.toCharArray();
            ArrayTrieNode arrayTrieNode = root;
            for (int i = 0; i < wordCharacters.length; i++) {
                arrayTrieNode = arrayTrieNode.addValue(wordCharacters[i], i == wordCharacters.length - 1);
            }
        });
    }
}

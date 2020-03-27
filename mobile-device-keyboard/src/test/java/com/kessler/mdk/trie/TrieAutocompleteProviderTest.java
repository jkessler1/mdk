package com.kessler.mdk.trie;

import com.kessler.mdk.Candidate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrieAutocompleteProviderTest {
    @Test
    public void performTrain() {
        TrieAutocompleteProvider trieAutocompleteProvider = new TrieAutocompleteProvider("[ \\.]+");
        trieAutocompleteProvider.performTrain("The third thing that I need to tell you is that this thing does not think thoroughly.");
        assertEquals(40, trieAutocompleteProvider.root.size());
    }

    @Test
    public void getWords() {
        TrieAutocompleteProvider trieAutocompleteProvider = new TrieAutocompleteProvider("[ \\.]+");
        trieAutocompleteProvider.performTrain("The third thing that I need to tell you is that this thing does not think thoroughly.");

        validateCandidateList(trieAutocompleteProvider.getWords("th"),
                new Candidate("that", 2),
                new Candidate("thing", 2),
                new Candidate("think", 1),
                new Candidate("this", 1),
                new Candidate("third", 1),
                new Candidate("the", 1),
                new Candidate("thoroughly", 1));

        validateCandidateList(trieAutocompleteProvider.getWords("thi"),
                new Candidate("thing", 2),
                new Candidate("think", 1),
                new Candidate("this", 1),
                new Candidate("third", 1));

        validateCandidateList(trieAutocompleteProvider.getWords("nee"),
                new Candidate("need", 1));
    }

    private void validateCandidateList(List<Candidate> candidates, Candidate... expectedCandidates) {
        List<Candidate> expectedCandidatesList = Arrays.asList(expectedCandidates);
        assert(candidates.containsAll(expectedCandidatesList));
        // The reason I'm doing a containsAll instead of equals is that our Candidate comparator only considers confidence.
        // Different JVMs may sort Candidates of equal confidence differently resulting in a different albeit equally valid
        // order.
        Iterator<Candidate> candidateIterator = candidates.iterator();
        Candidate candidate = null;
        do {
            Candidate previousCandidate = candidate;
            candidate = candidateIterator.next();
            // Ensure the order is correct
            assert previousCandidate == null || (previousCandidate.getConfidence() >= candidate.getConfidence());
        } while(candidateIterator.hasNext());
    }
}
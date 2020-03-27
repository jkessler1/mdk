package com.kessler.mdk.trie;

import com.kessler.mdk.Candidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrayTrieNodeTest {
    @Test
    public void addValue() {
        // Ensure our original node has no children.
        ArrayTrieNode arrayTrieNode = new ArrayTrieNode('a', false);
        assertNull(arrayTrieNode.childrenReference.get());

        // Add a single child to our original node and ensure its children object is a single ArrayTrieNode
        ArrayTrieNode arrayTrieNodeB = arrayTrieNode.addValue('b', false);
        assertNotNull(arrayTrieNode.childrenReference.get());
        assert(arrayTrieNode.childrenReference.get() instanceof ArrayTrieNode);
        assertEquals(0, arrayTrieNodeB.getWordEndCount());

        // Purposefully using == operator to ensure this is the same object that we created above.
        assert(arrayTrieNode.addValue('b', true) == arrayTrieNodeB);
        assert(arrayTrieNode.childrenReference.get() instanceof ArrayTrieNode);
        // Ensure that the wordEndCount increments appropriately after we added the value again, with isWordEnd set to true
        assertEquals(1, arrayTrieNodeB.getWordEndCount());

        // Add a second child and ensure that children is now an ArrayTrieNode[] array
        ArrayTrieNode arrayTrieNodeC = arrayTrieNode.addValue('c', false);
        assert(arrayTrieNode.childrenReference.get() instanceof ArrayTrieNode[]);
        assertEquals(2, ((ArrayTrieNode[]) arrayTrieNode.childrenReference.get()).length);

        // Now attempt to add 'c' again, but increment and verify the word end count.
        arrayTrieNode.addValue('c', true);
        assertEquals(2, ((ArrayTrieNode[]) arrayTrieNode.childrenReference.get()).length);
        assertEquals(1, arrayTrieNodeC.getWordEndCount());
    }

    @Test
    public void getChild() {
        ArrayTrieNode root = new ArrayTrieNode('0', false);
        assertTrue(root.getChild('a').isEmpty());
        ArrayTrieNode childA = root.addValue('a', true);
        assertEquals(childA, root.getChild('a').get());
    }

    @Test
    public void getCandidates() {
        ArrayTrieNode root = new ArrayTrieNode('0', false);
        addWordsToNode(root, "apple", "arrow", "anvil");
        addWordsToNode(root, "apple", "apple", "arrow");

        List<Candidate> candidateList = new ArrayList<>();
        root.getChild('a').get().getCandidates(candidateList, "".toCharArray());
        candidateList.sort(Comparator.naturalOrder());
        List<Candidate> expectedCandidates = Arrays.asList(new Candidate("apple", 3),
                new Candidate("arrow", 2),
                new Candidate("anvil", 1));

        assertEquals(expectedCandidates, candidateList);
    }

    private void addWordsToNode(ArrayTrieNode node, String... wordList) {
        Arrays.stream(wordList).forEach(word -> {
            char[] wordCharacters = word.toCharArray();
            ArrayTrieNode arrayTrieNode = node;
            for (int i = 0; i < wordCharacters.length; i++) {
                arrayTrieNode = arrayTrieNode.addValue(wordCharacters[i], i == wordCharacters.length - 1);
            }
        });
    }

    @Test
    public void size() {
        ArrayTrieNode arrayTrieNode = new ArrayTrieNode('0', false);
        addWordsToNode(arrayTrieNode, "abc");
        arrayTrieNode.getChild('a').get().addValue('a', true);
        assertEquals(5, arrayTrieNode.size());
    }
}
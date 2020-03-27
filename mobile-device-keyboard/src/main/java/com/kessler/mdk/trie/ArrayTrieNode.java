package com.kessler.mdk.trie;

import com.kessler.mdk.Candidate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A node in a Trie structure backed by an array when there are multiple children. Its "children" reference can also
 * contain a null value or a single ArrayTrieNode to save space when there are not multiple children.
 */
public class ArrayTrieNode implements Comparable<ArrayTrieNode> {
    // I'm sacrificing the memory of having an initially empty AtomicReference here, instead of null, to improve write
    // speeds. The alternative was to use some locking/synchronization within the addValue method which would decidedly
    // slow things down.
    final AtomicReference<Object> childrenReference = new AtomicReference<>(null);
    private AtomicInteger wordEndCount = null;
    private final char charValue;

    public ArrayTrieNode(char charValue, boolean isWordEnd) {
        this.charValue = charValue;
        if(isWordEnd) {
            // Don't allocate memory for this until we have to.
            wordEndCount = new AtomicInteger(1);
        }
    }

    /**
     * Increments the total number of times that this node was the end of a word.
     */
    public void incrementWordEndCount() {
        if(wordEndCount == null) {
            wordEndCount = new AtomicInteger(1);
        } else {
            wordEndCount.incrementAndGet();
        }
    }

    /**
     * @return The char value associated with this node.
     */
    public char getCharValue() {
        return charValue;
    }

    /**
     * @return The number of times that this node was the end of a word
     */
    public int getWordEndCount() {
        if(wordEndCount == null) {
            return 0;
        } else {
            return wordEndCount.get();
        }
    }

    /**
     * Returns an Optional containing a reference to the child node whose value is equal to that of the parameter
     * when it exists. Otherwise the Optional will be empty.
     * @param c The value of the child to be returned
     * @return An Optional containing a reference to the correct child node or null if none exists.
     */
    public Optional<ArrayTrieNode> getChild(char c) {
        Object children = childrenReference.get();
        if(children == null) {
            return Optional.empty();
        }

        if(children instanceof ArrayTrieNode) {
            return Optional.ofNullable(((ArrayTrieNode) children).getCharValue() == c ? (ArrayTrieNode)children : null);
        }

        ArrayTrieNode[] childrenArray = (ArrayTrieNode[])children;
        int insertionPoint = Arrays.binarySearch(childrenArray, new ArrayTrieNode(c, false));
        return Optional.ofNullable(insertionPoint >= 0 ? childrenArray[insertionPoint] : null);
    }

    /**
     * Populates the list of Candidates based on this node's wordEndCount value. It will then call the same method on
     * all of its children after adding its own value to the array of characters being passed to that method.
     * @param candidateList The list of Candidates being populated
     * @param fragment Will be prepended to this node's value if this node is a valid Candidate
     */
    public void getCandidates(List<Candidate> candidateList, char[] fragment) {
        char[] fragmentCopy = Arrays.copyOf(fragment, fragment.length + 1);
        fragmentCopy[fragmentCopy.length - 1] = charValue;
        int wordEndCount = this.getWordEndCount();
        if (wordEndCount > 0) {
            candidateList.add(new Candidate(new String(fragmentCopy), wordEndCount));
        }

        Object children = childrenReference.get();
        if(children != null) {
            if(children instanceof ArrayTrieNode) {
                ((ArrayTrieNode) children).getCandidates(candidateList, fragmentCopy);
            } else {
                Arrays.stream((ArrayTrieNode[])children).forEach(child -> child.getCandidates(candidateList, fragmentCopy));
            }
        }
    }

    /**
     * Traverses all descendants of this node to calculate a size.
     * @return The size of the trie structure starting at this node.
     */
    public int size() {
        int i = 1;
        Object children = childrenReference.get();
        if(children == null) {
            return i;
        } else if(children instanceof ArrayTrieNode) {
            return i + ((ArrayTrieNode) children).size();
        } else {
            return i + Arrays.stream((ArrayTrieNode[])children).mapToInt(ArrayTrieNode::size).sum();
        }
    }

    /**
     * Attempts to add the provided character as a new child to this node. If such a node already exists, its
     * wordEndCount is incremented appropriately.
     * @param c The value to insert into the new node if one is needed
     * @param isWordEnd Whether or not the new/target node's wordEndCount will need to be incremented.
     * @return The newly created ArrayTrieNode or the child that already existed that contained this value.
     */
    public ArrayTrieNode addValue(char c, boolean isWordEnd) {
        Object children = childrenReference.get();

        // If this is our first child, create the node and attempt to set the childrenReference
        if(children == null) {
            return addValueToNullChildren(c, isWordEnd);
        }

        // In an attempt at optimizing memory usage as much as possible, when we only have a single child we can just
        // refer to an ArrayTrieNode rather an array of nodes.
        if(children instanceof ArrayTrieNode) {
            return addValueToChild(children, c, isWordEnd);
        } else {
            return addValueToChildren(children, c, isWordEnd);
        }
    }

    /**
     * Used when the current reference to children is null, this will create and set a reference to a new
     * ArrayTrieNode based on the provided paremeters. If the childrenReference was updated before this method
     * is able to finish it will instead call the addValue method again.
     * @param value The value to use for the new child node.
     * @param isWordEnd Whether or not the new child node is the end of a word.
     * @return The newly created ArrayTrieNode child.
     */
    private ArrayTrieNode addValueToNullChildren(char value, boolean isWordEnd) {
        ArrayTrieNode children = new ArrayTrieNode(value, isWordEnd);
        // Only return this new child ArrayTrieNode if we successfully set the childrenReference.
        if(childrenReference.compareAndSet(null, children)) {
            return children;
        } else {
            // If someone else set childrenReference before we could, start over
            return addValue(value, isWordEnd);
        }
    }

    /**
     * Used to attempt to add a new value as a child when there is currently just one child node. It will
     * do this by determining whether or not the existing child contains the same value. If not, the childrenReference
     * will be changed to an array and a new ArrayTrieNode will be created and inserted. If the childReference was
     * updated before this method is finished it will instead call the addValue method again.
     * @param children What was referenced by the childrenReference before this method was called
     * @param value The value to attempt to add as a child
     * @param isWordEnd Whether or not the provided value is the end of a word.
     * @return Either the existing child if its value matched that of the one provided, or a newly created ArrayTrieNode
     */
    private ArrayTrieNode addValueToChild(Object children, char value, boolean isWordEnd) {
        ArrayTrieNode child = (ArrayTrieNode)children;
        // If our "children" is a single node, check to see if it's value is equal to char value. If so, increment
        // its word count appropriately. If not, we need to convert it to an array and add a new child.
        if(child.getCharValue() == value) {
            if(isWordEnd) {
                child.incrementWordEndCount();
            }

            return child;
        } else {
            // We have a single child and its value does not match what is being added. We need to
            // convert children to an array and add a new node.
            ArrayTrieNode newChild = new ArrayTrieNode(value, isWordEnd);
            ArrayTrieNode[] newChildrenArray = addChildToArray(children, newChild);
            // Sorting can be expensive. One last sanity check that childReference hasn't been set since
            // we started this method.
            if(childrenReference.get() == children) {
                Arrays.sort(newChildrenArray);
            }

            if(childrenReference.compareAndSet(children, newChildrenArray)) {
                return newChild;
            } else {
                return addValue(value, isWordEnd);
            }
        }
    }

    /** Used when it is known that we already have multiple children, this attempts to add a child with the provided value.
     * If a child already exists with the provided value, its word end count will be incremented appropriately. If not,
     * a new child will be created and added to a new array of children. If the childrenReference is updated before
     * this method completes, it will just call the addValue method to try again. The newly created array will be sorted
     * prior to being set as the reference to allow future binary searches.
     * @param children A reference to the existing children array
     * @param value The value to attempt to add
     * @param isWordEnd Whether or not the provided value is the end of a word.
     * @return The newly created ArrayTrieNode or the existing child with the equivalent value
     */
    private ArrayTrieNode addValueToChildren(Object children, char value, boolean isWordEnd) {
        // We already have more than one child. Determine if we have to add a new child node or if an equivalent
        // already exists.
        ArrayTrieNode[] childrenArray = (ArrayTrieNode[])children;
        ArrayTrieNode newChild = new ArrayTrieNode(value, isWordEnd);
        int insertionPoint = Arrays.binarySearch(childrenArray, newChild);
        if(insertionPoint < 0) {
            // An equivalent child does not already exist so we must add one
            ArrayTrieNode[] newChildrenArray = addChildToArray(children, newChild);
            // Sorting can be expensive. One last sanity check that childReference hasn't been set since
            // we started this method.
            if(childrenReference.get() == children) {
                Arrays.sort(newChildrenArray);
            }
            if(childrenReference.compareAndSet(children, newChildrenArray)) {
                return newChild;
            } else {
                return addValue(value, isWordEnd);
            }
        } else {
            // An equivalent child exists. Obtain a reference to it and increment its word end count if necessary.
            ArrayTrieNode childToUpdate = childrenArray[insertionPoint];
            if(isWordEnd) {
                childToUpdate.incrementWordEndCount();
            }
            return childToUpdate;
        }
    }

    /**
     * Called when it is already know that the childrenReference contains a reference to an array, this method
     * will add the provided ArrayTrieNode child to the existing Array after resizing it appropriately.
     * @param children The array that the childrenReference referenced prior to calling this method
     * @param child The node to be added.
     * @return A newly created array containing the children from before, plus this new child.
     */
    public ArrayTrieNode[] addChildToArray(Object children, ArrayTrieNode child) {
        ArrayTrieNode[] newChildrenArray;

        // If we only have a single child, convert children to an array and add the new child
        if(children instanceof ArrayTrieNode) {
            ArrayTrieNode originalChild = (ArrayTrieNode)children;
            newChildrenArray = new ArrayTrieNode[] {originalChild, child};
        } else {
            // Grow our children array by 1, inserting the new member at the end.
            newChildrenArray = Arrays.copyOf((ArrayTrieNode[])children, ((ArrayTrieNode[])children).length + 1);
            newChildrenArray[newChildrenArray.length - 1] = child;
        }

        return newChildrenArray;
    }

    @Override
    public int compareTo(ArrayTrieNode arrayTrieNode) {
        return Character.compare(this.charValue, arrayTrieNode.charValue);
    }

    /**
     * To make searching children easier, I wanted ArrayTrieNode's to be considered equal if their values were equal.
     * The wordEndCount playing a role would have only complicated things.
     * @param o The Object to which this one will be compared.
     * @return Whether or not this Object is equal to the one provided.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayTrieNode that = (ArrayTrieNode) o;
        return charValue == that.charValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(charValue);
    }
}
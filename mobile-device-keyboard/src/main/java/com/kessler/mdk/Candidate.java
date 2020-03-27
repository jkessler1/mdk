package com.kessler.mdk;

import java.util.Objects;

/**
 * A simple Candidate implementation.
 */
public class Candidate implements Comparable<Candidate> {
    final String word;
    final Integer confidence;

    public Candidate(String word, Integer confidence) {
        this.word = word;
        this.confidence = confidence;
    }

    public String getWord() {
        return word;
    }

    public Integer getConfidence() {
        return confidence;
    }

    /**
     * @param candidate The Candidate to compare this object to
     * @return The results of comparing this Candidates confidence value to the target Candidate's confidence value
     */
    @Override
    public int compareTo(Candidate candidate) {
        return candidate.confidence.compareTo(confidence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Candidate candidate = (Candidate) o;
        return Objects.equals(word, candidate.word) &&
                Objects.equals(confidence, candidate.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, confidence);
    }

    @Override
    public String toString() {
        return "\"" + word + "\" (" + confidence + ")";
    }
}

package com.kessler.mdk;

import com.kessler.mdk.trie.TrieAutocompleteProvider;

import java.util.List;
import java.util.Scanner;

/**
 * A simple Driver class to allow a user to make use of the AutocompleteProvider implementation that I developed.
 */
public class Driver {
    public static void main(String[] args) {
        TrieAutocompleteProvider trieAutocompleteProvider = new TrieAutocompleteProvider("[ \\.]");
        printMenu();
        Scanner scanner = new Scanner(System.in);
        String command;
        do {
            command = getCommand(scanner);
            System.out.println("Command entered was: " + command);
            executeCommand(trieAutocompleteProvider, command);
        } while(!command.equalsIgnoreCase("exit"));
    }

    private static void printMenu() {
        System.out.println("Instructions:\n1) To train with a new passage, type \"train \" followed by the passage. Press enter to end passage.\n" +
                "2) To get a list of candidates based on a string fragment, type \"get \" followed by the fragment and press return.\n" +
                "3) To exit, type 'exit'");
    }

    private static String getCommand(Scanner scanner) {
        System.out.print("\n> ");
        return scanner.nextLine();
    }

    private static void executeCommand(TrieAutocompleteProvider trieAutocompleteProvider, String command) {
        try {
            if (command.startsWith("train ")) {
                String passage = command.substring(6);
                trieAutocompleteProvider.train(passage);
                System.out.println("Training passage.");
            } else if (command.startsWith("get ")) {
                String fragment = command.substring(4);
                List<Candidate> candidateList = trieAutocompleteProvider.getWords(fragment);
                String candidateListString = candidateList.toString();
                System.out.println("\"" + fragment + "\" --> " + candidateListString.substring(1, candidateListString.length() - 1));
            } else if (command.equalsIgnoreCase("exit")) {
                System.exit(0);
            } else {
                System.out.println("Unrecognized command \"" + command + "\".");
            }
        } catch (Exception e) {
            System.err.println("There was an error while executing your command: " + e.getMessage());
        }
    }
}

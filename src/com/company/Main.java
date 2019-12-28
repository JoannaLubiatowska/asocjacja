package com.company;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    private static final double DEFAULT_MIN_SUPPORT = 0.8;

    public static void main(String[] args) throws IOException {
        //private static String filePath = "com/company/file/association.txt";
        String filePath = getFilePath(args);
        double minSupport = getMinSupport(args);

        List<Receipt> receipts = readFromFile(filePath);
        System.out.println(receipts);
        Set<Set<String>> go = go(receipts, minSupport);
        System.out.println(go);
    }

    private static double getMinSupport(String[] args) {
        double minSupport = args.length >= 2 ? Double.valueOf(args[1]) : DEFAULT_MIN_SUPPORT;
        if (minSupport > 1 || minSupport < 0) throw new RuntimeException("minSupport: bad value");
        return minSupport;
    }

    private static String getFilePath(String[] args) {
        return args[0];
    }

    private static List<Receipt> readFromFile(String path) throws IOException {
        final Pattern pattern = Pattern.compile("^\\[(.*)]$");
        return Files.readAllLines(new File(path).toPath(), StandardCharsets.UTF_8).stream().map(line -> {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String listOfProducts = matcher.group(1);
                List<String> articles = Arrays.asList(listOfProducts.split(", "));
                return new Receipt(articles);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static Set<Set<String>> go(List<Receipt> receipts, double minSupport) {
        long numTransactions = receipts.size();
        Set<Set<String>> frequentItems = new HashSet<>();
        Set<Set<String>> bestFrequentItems = new HashSet<>();
        fillWithOneElementSet(frequentItems, receipts);

        int itemSetsElements = 1;

        while (!frequentItems.isEmpty()) {
            frequentItems = calculateFrequentElementsItemSets(receipts, frequentItems, numTransactions, minSupport);
            if (!frequentItems.isEmpty()) {
                bestFrequentItems = frequentItems;
                frequentItems = createNewItemSetsFromPreviousOnes(frequentItems, itemSetsElements);
                itemSetsElements++;
            }
        }

        return bestFrequentItems;
    }

    private static void fillWithOneElementSet(Set<Set<String>> itemSets, List<Receipt> receipts) {
        receipts.forEach(receipt -> receipt.getArticles().forEach(item -> itemSets.add(Set.of(item))));
    }

    private static Set<Set<String>> calculateFrequentElementsItemSets(List<Receipt> receipts, Set<Set<String>> itemSets, long numTransactions, double minSupport) {
        Map<Set<String>, Long> itemSetsCounter = itemSets.stream().collect(Collectors.toMap(it -> it, it -> 0L));

        receipts.stream()
                .map(receipt -> itemSets.stream().filter(set -> receipt.getArticles().containsAll(set)))
                .flatMap(setStream -> setStream)
                .forEach(set -> itemSetsCounter.put(set, itemSetsCounter.get(set) + 1));

        return itemSets.stream()
                .filter(set -> itemSetsCounter.get(set) / (double) numTransactions >= minSupport)
                .collect(Collectors.toSet());
    }

    private static Set<Set<String>> createNewItemSetsFromPreviousOnes(Set<Set<String>> itemSets, int itemSetsElements) {
        Set<Set<String>> newItemSets = new HashSet<>();

        for (Set<String> items : itemSets) {
            for (Set<String> otherItems : itemSets) {
                assert (items.size() == otherItems.size());
                HashSet<String> diffItems = new HashSet<>(otherItems);
                diffItems.removeAll(items);
                if (diffItems.size() == 1) {
                    Set<String> newItems = new HashSet<>(items);
                    newItems.addAll(diffItems);
                    newItemSets.add(newItems);
                }
            }
        }

        return newItemSets;
    }

    private static class Receipt {

        private List<String> articles;

        Receipt(List<String> articles) {
            this.articles = articles;
        }

        List<String> getArticles() {
            return articles;
        }

        @Override
        public String toString() {
            return this.articles.toString();
        }
    }
}

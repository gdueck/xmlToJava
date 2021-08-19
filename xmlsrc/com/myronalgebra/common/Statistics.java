package com.myronalgebra.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.function.BiFunction;

public class Statistics {
    public static double stdev(Collection<Double> stats) {
        if (stats.size() == 1)
            return 0;
        double avg = avg(stats);
        double sumsq = stats.stream().mapToDouble(t->(t - avg) * (t-avg)).sum();
        double stdev = Math.sqrt(sumsq/(stats.size() - 1));
        return stdev;
    }

    public static double stdevInt(Collection<Integer> stats) {
        if (stats.size() == 1)
            return 0;
        double avg = avgInt(stats);
        double sumsq = stats.stream().mapToDouble(t->(t - avg) * (t-avg)).sum();
        double stdev = Math.sqrt(sumsq/(stats.size() - 1));
        return stdev;
    }

    public static double avg(Collection<Double> stats) {
        if (stats.size() == 0)
            return 0;
        return stats.stream().mapToDouble(t -> t).summaryStatistics().getAverage();
    }

    public static double avgInt(Collection<Integer> stats) {
        if (stats.size() == 0)
            return 0;
        return stats.stream().mapToDouble(t -> t).summaryStatistics().getAverage();
    }

    public static double sum(Collection<Double> stats) {
        return stats.stream().mapToDouble(t -> t).summaryStatistics().getSum();
    }

    public static long sumInt(Collection<Integer> stats) {
        return stats.stream().mapToInt(t -> t).summaryStatistics().getSum();
    }

    public static <K, V extends Comparable<V>> K keyOfMin(Hashtable<K, V> detected) {
        K minKey = null;
        V minValue = null;
        for (K k: detected.keySet()) {
            if (minValue == null) {
                minValue = detected.get(k);
                minKey = k;
            } else if (detected.get(k).compareTo(minValue) < 0) {
                minValue = detected.get(k);
                minKey = k;
            }
        }
        return minKey;
    }

    public static <K, V extends Comparable<V>> K keyOf(Hashtable<K, V> detected, BiFunction<V, V, Boolean> comparison ) {
        K minKey = null;
        V minValue = null;
        for (K k: detected.keySet()) {
            if (minValue == null) {
                minValue = detected.get(k);
                minKey = k;
            } else if (comparison.apply(detected.get(k), minValue)) {
                minValue = detected.get(k);
                minKey = k;
            }
        }
        return minKey;
    }


    public static void main(String[] args) {
        Double[] data = new Double[]

                {
                        8.097831079,
                        8.088621533,
                        1.814603736,
                        2.679102501,
                        1.963725896,
                        6.251346735,
                        1.023922715,
                        8.130708008,
                        5.987287764,
                        3.103458717,


                };
        Double[] stats = new Double[]{81.42,
                72.98
        };
        System.out.println(String.format("sum: %.2f", sum(Arrays.asList(data))));
        System.out.println(String.format("avg: %.2f", avg(Arrays.asList(data))));
        System.out.println(String.format("std: %.2f", stdev(Arrays.asList(data))));
        System.out.println(String.format("std: %.2f", stdev(Arrays.asList(stats))));
    }}

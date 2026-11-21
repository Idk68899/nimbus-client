package dev.nimbus.util;

import java.util.concurrent.ThreadLocalRandom;

public final class Rand {
    public static double between(double a, double b) {
        return a + ThreadLocalRandom.current().nextDouble() * (b - a);
    }
}
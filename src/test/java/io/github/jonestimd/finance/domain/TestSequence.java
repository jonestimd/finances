package io.github.jonestimd.finance.domain;

public class TestSequence {
    private static long nextId = 1L;

    private TestSequence() {}

    public static long nextId() {
        return nextId++;
    }
}

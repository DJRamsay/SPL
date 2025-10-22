package codegen;

import java.util.concurrent.atomic.AtomicInteger;

public class LabelGenerator {
    private final AtomicInteger counter = new AtomicInteger(1);
    public String newLabel(String prefix) {
        return (prefix == null || prefix.isEmpty() ? "L" : prefix) + counter.getAndIncrement();
    }
}
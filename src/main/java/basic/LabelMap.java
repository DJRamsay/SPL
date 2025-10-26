package basic;

import java.util.HashMap;
import java.util.Map;

public class LabelMap {
    private final Map<String, Integer> map = new HashMap<>();

    /**
     * Records the final line number for a given symbolic label (e.g., L1 -> 180).
     * @param label The symbolic label (e.g., "L1").
     * @param lineNumber The generated BASIC line number (e.g., 180).
     */
    public void put(String label, int lineNumber) {
        if (map.containsKey(label)) {
            // Should not happen if the intermediate code is correct, but good practice to check.
            throw new CodeGenerationException("Duplicate definition for label: " + label);
        }
        map.put(label, lineNumber);
    }

    /**
     * Retrieves the line number for a given label.
     * @param label The symbolic label.
     * @return The corresponding BASIC line number.
     * @throws CodeGenerationException If the label is not found (a GOTO to an undefined label).
     */
    public int get(String label) {
        if (!map.containsKey(label)) {
            throw new CodeGenerationException("Undefined label reference: " + label);
        }
        return map.get(label);
    }
}
package codegen;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps SPL variable names to valid Quite BASIC variable names.
 * 
 * Quite BASIC rules:
 * - Variables must be a single letter (A-Z)
 * - Optionally followed by digits (0-9)
 * - Examples: A, B, X1, Y2, Z9
 * 
 * This mapper assigns BASIC names systematically.
 */
public class VariableMapper {
    private final Map<String, String> map = new HashMap<>();
    private int nextLetterIndex = 0;
    private int nextDigit = 0;
    
    // Available letters for variables (avoid some that might be used for special purposes)
    private static final char[] LETTERS = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 
        'U', 'V', 'W', 'X', 'Y'
        // Skip Z - we use it for return values
    };
    
    public VariableMapper() {
        // Reserve Z for function return values
        map.put("__RETURN__", "Z");
    }
    
    /**
     * Get the BASIC variable name for an SPL variable.
     * If not seen before, assigns a new BASIC name.
     */
    public String get(String splVariable) {
        if (map.containsKey(splVariable)) {
            return map.get(splVariable);
        }
        
        String basicName = generateNextName();
        map.put(splVariable, basicName);
        return basicName;
    }
    
    /**
     * Check if a variable has been mapped.
     */
    public boolean contains(String splVariable) {
        return map.containsKey(splVariable);
    }
    
    /**
     * Generate the next available BASIC variable name.
     */
    private String generateNextName() {
        // First use all single letters: A, B, C, ..., Y
        if (nextLetterIndex < LETTERS.length && nextDigit == 0) {
            char letter = LETTERS[nextLetterIndex];
            nextLetterIndex++;
            return String.valueOf(letter);
        }
        
        // Then use letters with digits: A1, A2, ... A9, B1, B2, ...
        if (nextLetterIndex >= LETTERS.length) {
            nextLetterIndex = 0;
            nextDigit++;
        }
        
        if (nextDigit > 9) {
            throw new CodeGenerationException(
                "Too many variables! BASIC only supports single-letter variables with single digits."
            );
        }
        
        char letter = LETTERS[nextLetterIndex];
        nextLetterIndex++;
        
        return String.valueOf(letter) + nextDigit;
    }
    
    /**
     * Get the reserved return value variable name.
     */
    public String getReturnVariable() {
        return "Z";
    }
}
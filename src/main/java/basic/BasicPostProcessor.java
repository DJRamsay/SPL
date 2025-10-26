package basic;

import java.util.ArrayList;
import java.util.List;

public class BasicPostProcessor {
    private static final int LINE_INCREMENT = 10;
    
    // Assuming LabelMap is correct and throws CodeGenerationException on missing label.

    /**
     * Performs line numbering and extracts label definitions into a LabelMap.
     */
    private List<String> pass1_NumberLinesAndMapLabels(
            List<String> intermediateCode, 
            LabelMap labelMap) {
        
        List<String> numberedCode = new ArrayList<>();
        int currentLineNumber = LINE_INCREMENT; // Start at 10

        for (String instruction : intermediateCode) {
            
            // FIX 1: Change condition to check for any REM instruction
            if (instruction.trim().startsWith("REM ")) {
                // Extract the label name (e.g., "START" from "REM START")
                // Use substring and trim to reliably get the label
                String label = instruction.trim().substring("REM ".length()).trim();
                if (!label.isEmpty()) {
                    labelMap.put(label, currentLineNumber);
                }
            }
            
            // 2. Add the line number and instruction to the new list
            numberedCode.add(currentLineNumber + " " + instruction);
            
            // 3. Increment the line number
            currentLineNumber += LINE_INCREMENT;
        }
        return numberedCode;
    }

    /**
     * Resolves all symbolic jumps (GOTO Lx, THEN Lx) using the LabelMap.
     */
    private List<String> pass2_ResolveJumps(
            List<String> numberedCode, 
            LabelMap labelMap) {
        
        List<String> finalCode = new ArrayList<>();
        
        for (String line : numberedCode) {
            String resolvedLine = line;
            String instructionPart = line.substring(line.indexOf(' ') + 1); // e.g., "GOTO START" or "IF (i>0) THEN L1"
            
            // Check for GOTO [label]
            if (instructionPart.startsWith("GOTO ")) {
                // Extract the label (e.g., "START" from "GOTO START")
                String label = instructionPart.substring("GOTO ".length()).trim();
                int targetLine = labelMap.get(label); // throws exception if missing
                
                // FIX 2: Reconstruct the entire instruction line with the number
                resolvedLine = line.replace("GOTO " + label, "GOTO " + targetLine);
            } 
            
            // Check for THEN [label] (part of an IF statement)
            else if (instructionPart.contains("THEN ")) {
                // We must find the label *after* the "THEN " keyword.
                int thenIndex = instructionPart.lastIndexOf("THEN ");
                String afterThen = instructionPart.substring(thenIndex + "THEN ".length()).trim();
                
                // The label is the first word after THEN (e.g., "L1" in "L1")
                String label = afterThen.split("\\s+")[0];
                int targetLine = labelMap.get(label); // throws exception if missing
                
                // FIX 3: Reconstruct the line correctly:
                resolvedLine = line.replace("THEN " + label, "THEN " + targetLine);
            }
            
            finalCode.add(resolvedLine);
        }
        return finalCode;
    }

    /**
     * Public method to run the entire post-processing chain.
     */
    public List<String> generateExecutableBasic(List<String> intermediateCode) {
        LabelMap labelMap = new LabelMap();
        
        // Pass 1: Number lines and create the map
        List<String> numberedCode = pass1_NumberLinesAndMapLabels(intermediateCode, labelMap);
        
        // Pass 2: Resolve jumps using the created map
        return pass2_ResolveJumps(numberedCode, labelMap);
    }
}
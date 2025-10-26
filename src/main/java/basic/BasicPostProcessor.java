package basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms intermediate code into executable BASIC.
 * 
 * Handles:
 * 1. Function/procedure definitions -> GOSUB subroutines
 * 2. CALL statements -> parameter passing + GOSUB + return value
 * 3. RETURN value -> LET Z = value; RETURN
 * 4. Line numbering
 * 5. Label resolution (GOTO/THEN)
 */
public class BasicPostProcessor {
    private static final int LINE_INCREMENT = 10;
    private static final int FUNCTION_START = 1000;  // Functions start at line 1000
    private static final int MAIN_START = 500;        // Main program starts at line 500
    private static final String RETURN_VAR = "Z";     // Use Z for return values (RESULT is reserved)
    
    /**
     * Main entry point - transforms intermediate code to executable BASIC.
     */
    public List<String> generateExecutableBasic(List<String> intermediateCode) {
        // Step 1: Parse and separate functions/procedures from main code
        CodeStructure structure = parseStructure(intermediateCode);
        
        // Step 2: Transform functions/procedures
        List<String> transformedFunctions = transformFunctions(structure);
        
        // Step 3: Transform main code
        List<String> transformedMain = transformMain(structure);
        
        // Step 4: Combine: skip to main, then functions, then main
        List<String> combined = new ArrayList<>();
        combined.add("GOTO MAIN");  // Will be resolved to line number later
        combined.addAll(transformedFunctions);
        combined.add("REM MAIN");
        combined.addAll(transformedMain);
        
        // Step 5: Number lines and map labels
        LabelMap labelMap = new LabelMap();
        List<String> numberedCode = numberLinesAndMapLabels(combined, labelMap);
        
        // Step 6: Resolve all jumps (GOTO/THEN)
        return resolveJumps(numberedCode, labelMap);
    }
    
    /**
     * Parse intermediate code into functions/procedures and main code.
     */
    private CodeStructure parseStructure(List<String> intermediateCode) {
        CodeStructure structure = new CodeStructure();
        FunctionDef currentFunction = null;
        boolean inFunction = false;
        
        for (String line : intermediateCode) {
            String trimmed = line.trim();
            
            // Check for function/procedure definition
            if (trimmed.startsWith("REM FUNC ") || trimmed.startsWith("REM PROC ")) {
                // Extract function name and parameters
                boolean isFunc = trimmed.startsWith("REM FUNC ");
                String rest = trimmed.substring(isFunc ? 9 : 9).trim();
                
                // Parse "funcname (param1, param2)" or "funcname ()"
                String funcName;
                List<String> params = new ArrayList<>();
                
                int parenIdx = rest.indexOf('(');
                if (parenIdx > 0) {
                    funcName = rest.substring(0, parenIdx).trim();
                    int closeIdx = rest.indexOf(')');
                    if (closeIdx > parenIdx) {
                        String paramStr = rest.substring(parenIdx + 1, closeIdx).trim();
                        if (!paramStr.isEmpty()) {
                            for (String p : paramStr.split(",")) {
                                params.add(p.trim());
                            }
                        }
                    }
                } else {
                    funcName = rest;
                }
                
                currentFunction = new FunctionDef(funcName, params, isFunc);
                structure.functions.add(currentFunction);
                inFunction = true;
                continue;
            }
            
            // Check for RETURN (marks end of function)
            if (trimmed.startsWith("RETURN")) {
                if (inFunction && currentFunction != null) {
                    currentFunction.body.add(trimmed);
                    inFunction = false;
                    currentFunction = null;
                } else {
                    structure.mainCode.add(trimmed);
                }
                continue;
            }
            
            // Add to current function or main
            if (inFunction && currentFunction != null) {
                currentFunction.body.add(trimmed);
            } else {
                structure.mainCode.add(trimmed);
            }
        }
        
        return structure;
    }
    
    /**
     * Transform function definitions into BASIC subroutines.
     */
    private List<String> transformFunctions(CodeStructure structure) {
        List<String> result = new ArrayList<>();
        
        for (FunctionDef func : structure.functions) {
            // Add function label
            result.add("REM " + func.name);
            
            // Transform the function body
            for (String line : func.body) {
                if (line.startsWith("RETURN ")) {
                    // Transform "RETURN value" to "LET Z = value" then "RETURN"
                    String returnValue = line.substring(7).trim();
                    result.add("LET " + RETURN_VAR + " = " + returnValue);
                    result.add("RETURN");
                } else if (line.equals("RETURN")) {
                    // Procedure with no return value
                    result.add("RETURN");
                } else {
                    result.add(line);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Transform main code, handling CALL statements.
     */
    private List<String> transformMain(CodeStructure structure) {
        List<String> result = new ArrayList<>();
        
        // Pattern to match: LET var = CALL funcname arg1 arg2 ...
        Pattern assignCallPattern = Pattern.compile("LET\\s+(\\w+)\\s*=\\s*CALL\\s+(\\w+)\\s*(.*)");
        // Pattern to match: CALL funcname arg1 arg2 ...
        Pattern callPattern = Pattern.compile("CALL\\s+(\\w+)\\s*(.*)");
        
        for (String line : structure.mainCode) {
            Matcher assignMatcher = assignCallPattern.matcher(line);
            Matcher callMatcher = callPattern.matcher(line);
            
            if (assignMatcher.matches()) {
                // LET result = CALL funcname args
                String resultVar = assignMatcher.group(1);
                String funcName = assignMatcher.group(2);
                String argsStr = assignMatcher.group(3).trim();
                
                // Find the function definition to get parameter names
                FunctionDef func = structure.findFunction(funcName);
                if (func != null) {
                    // Parse arguments
                    List<String> args = parseArguments(argsStr);
                    
                    // Assign arguments to parameters
                    for (int i = 0; i < Math.min(args.size(), func.parameters.size()); i++) {
                        result.add("LET " + func.parameters.get(i) + " = " + args.get(i));
                    }
                    
                    // Call the function
                    result.add("GOSUB " + funcName);
                    
                    // Get return value
                    result.add("LET " + resultVar + " = " + RETURN_VAR);
                } else {
                    // Unknown function, keep as-is (will likely error later)
                    result.add(line);
                }
            } else if (callMatcher.matches()) {
                // CALL procname args (procedure call without assignment)
                String procName = callMatcher.group(1);
                String argsStr = callMatcher.group(2).trim();
                
                FunctionDef proc = structure.findFunction(procName);
                if (proc != null) {
                    List<String> args = parseArguments(argsStr);
                    
                    // Assign arguments to parameters
                    for (int i = 0; i < Math.min(args.size(), proc.parameters.size()); i++) {
                        result.add("LET " + proc.parameters.get(i) + " = " + args.get(i));
                    }
                    
                    // Call the procedure
                    result.add("GOSUB " + procName);
                } else {
                    result.add(line);
                }
            } else {
                // Regular line, keep as-is
                result.add(line);
            }
        }
        
        return result;
    }
    
    /**
     * Parse space-separated arguments.
     */
    private List<String> parseArguments(String argsStr) {
        List<String> args = new ArrayList<>();
        if (argsStr.isEmpty()) {
            return args;
        }
        
        // Split by spaces, but handle parentheses
        String[] parts = argsStr.split("\\s+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                args.add(part);
            }
        }
        return args;
    }
    
    /**
     * Number lines and create label map.
     */
    private List<String> numberLinesAndMapLabels(List<String> code, LabelMap labelMap) {
        List<String> numberedCode = new ArrayList<>();
        int currentLineNumber = LINE_INCREMENT;
        
        for (String instruction : code) {
            String trimmed = instruction.trim();
            
            // Map labels (REM statements)
            if (trimmed.startsWith("REM ")) {
                String label = trimmed.substring(4).trim();
                if (!label.isEmpty()) {
                    labelMap.put(label, currentLineNumber);
                }
            }
            
            // Add numbered line
            numberedCode.add(currentLineNumber + " " + instruction);
            currentLineNumber += LINE_INCREMENT;
        }
        
        return numberedCode;
    }
    
    /**
     * Resolve all GOTO and THEN references to line numbers.
     */
    private List<String> resolveJumps(List<String> numberedCode, LabelMap labelMap) {
        List<String> finalCode = new ArrayList<>();
        
        for (String line : numberedCode) {
            String resolvedLine = line;
            
            // Extract the instruction part (after line number)
            int spaceIdx = line.indexOf(' ');
            if (spaceIdx < 0) {
                finalCode.add(line);
                continue;
            }
            
            String instructionPart = line.substring(spaceIdx + 1);
            
            // Handle GOTO label
            if (instructionPart.startsWith("GOTO ")) {
                String label = instructionPart.substring(5).trim();
                if (labelMap.containsLabel(label)) {
                    int targetLine = labelMap.get(label);
                    resolvedLine = line.replace("GOTO " + label, "GOTO " + targetLine);
                }
            }
            
            // Handle GOSUB label
            else if (instructionPart.startsWith("GOSUB ")) {
                String label = instructionPart.substring(6).trim();
                if (labelMap.containsLabel(label)) {
                    int targetLine = labelMap.get(label);
                    resolvedLine = line.replace("GOSUB " + label, "GOSUB " + targetLine);
                }
            }
            
            // Handle IF ... THEN label
            else if (instructionPart.contains("THEN ")) {
                int thenIdx = instructionPart.lastIndexOf("THEN ");
                String afterThen = instructionPart.substring(thenIdx + 5).trim();
                String label = afterThen.split("\\s+")[0];
                
                if (labelMap.containsLabel(label)) {
                    int targetLine = labelMap.get(label);
                    resolvedLine = line.replace("THEN " + label, "THEN " + targetLine);
                }
            }
            
            finalCode.add(resolvedLine);
        }
        
        return finalCode;
    }
    
    // ===== Helper Classes =====
    
    private static class CodeStructure {
        List<FunctionDef> functions = new ArrayList<>();
        List<String> mainCode = new ArrayList<>();
        
        FunctionDef findFunction(String name) {
            for (FunctionDef func : functions) {
                if (func.name.equals(name)) {
                    return func;
                }
            }
            return null;
        }
    }
    
    private static class FunctionDef {
        String name;
        List<String> parameters;
        List<String> body;
        boolean isFunction;  // true = function (returns value), false = procedure
        
        FunctionDef(String name, List<String> parameters, boolean isFunction) {
            this.name = name;
            this.parameters = parameters;
            this.body = new ArrayList<>();
            this.isFunction = isFunction;
        }
    }
}
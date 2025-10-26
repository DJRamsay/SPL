import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import basic.BasicPostProcessor; 
import codegen.CodeGenerator;
import lexer.Lexer;
import lexer.LexerException;
import lexer.Token;
import parser.Parser;
import parser.ParserException;

public class Main {

    private static final String INTERMEDIATE_CODE_PATH = "target/target_code.txt";

    public static void main(String args[]) throws IOException {
        String source;

        try {
            // === Read Input ===
            if (args.length < 1) {
                String defaultInput = "src/main/java/input2.txt";
                System.out.println("No arguments provided. Reading from " + defaultInput);
                source = readFile(defaultInput);
            } else {
                source = readFile(args[0]);
            }

            // === 1. Lexical Analysis ===
            System.out.println("====Lexical Analysis====");
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            System.out.println("Tokens generated: " + tokens.size());

            // Debug: print source with line numbers
            System.out.println("---- Source (numbered) ----");
            String[] lines = source.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                System.out.printf("%3d: %s%n", i + 1, lines[i]);
            }

            // // Debug: print tokens
            // System.out.println("---- Tokens ----");
            // for (Token t : tokens) {
            //     System.out.println(t);
            // }
            // System.out.println("---- End Tokens ----");
            
            // === 2. Syntax Analysis (Parsing) ===
            System.out.println("\n====Syntax Analysis (Parsing)====");
            try {
                Parser parser = new Parser(tokens);
                parser.parse();
                System.out.println("Syntax is correct");
            } catch (ParserException pe) {
                System.err.println("Parse error: " + pe.getMessage());
                System.exit(1);
            }

            // === 3. Semantic Analysis - Scope ===
            System.out.println("\n====Semantic Analysis: Scope====");
            try {
                semantic.ScopeAnalyzer scopeAnalyzer = new semantic.ScopeAnalyzer(tokens);
                semantic.ScopeAnalyzer.ScopeAnalysisResult scopeResult = scopeAnalyzer.analyze();
                
                if (scopeResult.isSuccess()) {
                    System.out.println("Scope analysis passed");
                } else {
                    System.err.println("Scope errors found:");
                    for (String error : scopeResult.getErrors()) {
                        System.err.println("  - " + error);
                    }
                    System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("Scope analysis error: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

            // === 4. Semantic Analysis - Type ===
            System.out.println("\n====Semantic Analysis: Type====");
            try {
                semantic.TypeAnalyzer typeAnalyzer = new semantic.TypeAnalyzer(tokens);
                semantic.TypeAnalyzer.TypeAnalysisResult typeResult = typeAnalyzer.analyze();
                
                if (typeResult.isSuccess()) {
                    System.out.println("Type analysis passed");
                } else {
                    System.err.println("Type errors found:");
                    for (String error : typeResult.getErrors()) {
                        System.err.println("  - " + error);
                    }
                    System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("Type analysis error: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

            // === 5. Code Generation (ONLY if all checks passed) ===
            System.out.println("\n====Code Generation====");
            try {
                CodeGenerator gen = new CodeGenerator(tokens);
                String targetCode = gen.generate();
                
                System.out.println("---- Target Code ----");
                System.out.println(targetCode);
                System.out.println("---- End Target Code ----");

                // Save target code to file
                String outPath = "target/target_code.txt";
                writeFile(outPath, targetCode);
                System.out.println("\nTarget code saved to: " + outPath);
                
            } catch (Exception e) {
                System.err.println("Code generation error: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

            // === Compilation Successful ===
            System.out.println("\n========================================");
            System.out.println("COMPILATION SUCCESSFUL");
            System.out.println("========================================");

        } catch (LexerException e) {
            System.err.println("Lexical error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("File error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(
            Paths.get(path)));
    }
    
    private static void writeFile(String path, String content) throws IOException {
        var p = java.nio.file.Paths.get(path);
        var parent = p.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(
            p,
            content.getBytes(StandardCharsets.US_ASCII));
    }
}
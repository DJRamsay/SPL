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
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            System.out.println("- Lexical analysis complete");

            // === 2. Syntax Analysis (Parsing) ===
            try {
                Parser parser = new Parser(tokens);
                parser.parse();
                System.out.println("- Syntax analysis complete");
            } catch (ParserException pe) {
                System.err.println("Parse error: " + pe.getMessage());
                System.exit(1);
            }

            // === 3. Semantic Analysis - Scope ===
            try {
                semantic.ScopeAnalyzer scopeAnalyzer = new semantic.ScopeAnalyzer(tokens);
                semantic.ScopeAnalyzer.ScopeAnalysisResult scopeResult = scopeAnalyzer.analyze();
                
                if (scopeResult.isSuccess()) {
                    System.out.println("- Scope analysis complete");
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
            try {
                semantic.TypeAnalyzer typeAnalyzer = new semantic.TypeAnalyzer(tokens);
                semantic.TypeAnalyzer.TypeAnalysisResult typeResult = typeAnalyzer.analyze();
                
                if (typeResult.isSuccess()) {
                    System.out.println("- Type analysis complete");
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

            // === 5. Code Generation ===
            String targetCode;
            try {
                CodeGenerator gen = new CodeGenerator(tokens);
                targetCode = gen.generate();
                writeFile(INTERMEDIATE_CODE_PATH, targetCode);
                System.out.println("- Code generation complete");
            } catch (Exception e) {
                System.err.println("Code generation error: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            
            // === 6. Post-Processing ===
            try {
                List<String> intermediateCodeLines = Files.readAllLines(
                    Paths.get(INTERMEDIATE_CODE_PATH), 
                    StandardCharsets.US_ASCII
                );
                
                BasicPostProcessor postProcessor = new BasicPostProcessor();
                List<String> finalBasicCodeLines = postProcessor.generateExecutableBasic(intermediateCodeLines);
                String finalExecutableCode = String.join(System.lineSeparator(), finalBasicCodeLines);

                writeFile(INTERMEDIATE_CODE_PATH, finalExecutableCode);
                System.out.println("- Post-processing complete");
                
                System.out.println("\n" + "=".repeat(60));
                System.out.println("COMPILATION SUCCESSFUL");
                System.out.println("=".repeat(60));
                System.out.println("\n---- BASIC OUTPUT ----");
                System.out.println(finalExecutableCode);
                System.out.println("----------------------");
                System.out.println("\nOutput saved to: " + INTERMEDIATE_CODE_PATH);
                System.out.println("\nRun your code online at: https://www.quitebasic.com/");
            } catch (Exception e) {
                System.err.println("Post-processing error: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import codegen.CodeGenerator;
import lexer.Lexer;
import lexer.LexerException;
import lexer.Token;
import parser.Parser;
import parser.ParserException;

public class Main {

    public static void main(String args[]) throws IOException{
        String source;

        try {
            if (args.length < 1){
                String defaultInput = "src/main/java/input.txt";
                System.out.println("No arguments provided. Reading from " + defaultInput);
                source = readFile(defaultInput);
            } else {
                source = readFile(args[0]);
            }

            System.out.println("====Lexical Analysis====");
            Lexer lexer =  new Lexer(source);
            List<Token> tokens =  lexer.tokenize();
            System.out.println("Tokens generated: " + tokens.size());

            // Debug: print source with line numbers
            System.out.println("---- Source (numbered) ----");
            String[] _lines = source.split("\\R");
            for (int i = 0; i < _lines.length; i++) {
                System.out.printf("%3d: %s%n", i + 1, _lines[i]);
            }

            // Debug: print tokens (uses Token.toString())
            System.out.println("---- Tokens ----");
            for (Token t : tokens) {
                System.out.println(t);
            }
            System.out.println("---- End Tokens ----");
            
            // === parsing step ===
            System.out.println("====Parsing====");
            try {
                Parser parser = new Parser(tokens);
                parser.parse();
                System.out.println("Parse successful");

                // === Code Generation ===
                System.out.println("====Target Code====");
                CodeGenerator gen = new CodeGenerator(tokens);
                String target = gen.generate();
                System.out.println(target);

                // Save target code (ASCII) as .txt file
                String outPath = "target/target_code.txt";
                writeFile(outPath, target);
                System.out.println("Saved target code to: " + outPath);

            } catch (ParserException pe) {
                System.err.println("Parse error: " + pe.getMessage());
                System.exit(1);
            }
            // === end added parsing step ===

            // === Scope Analysis ===
            System.out.println("====Scope Analysis====");
            try {
                semantic.ScopeAnalyzer scopeAnalyzer = new semantic.ScopeAnalyzer(tokens);
                semantic.ScopeAnalyzer.ScopeAnalysisResult scopeResult = scopeAnalyzer.analyze();
                
                if (scopeResult.isSuccess()) {
                    System.out.println("Variable Naming and Function Naming accepted");
                } else {
                    System.err.println("Naming errors found:");
                    for (String error : scopeResult.getErrors()) {
                        System.err.println("  - " + error);
                    }
                    System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("Scope analysis error: " + e.getMessage());
                System.exit(1);
            }

        } catch (LexerException e) {
            System.err.println("Compilation error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("File error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String readFile(String path) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get(path)));
    }
    
    private static void writeFile(String path, String content) 
            throws IOException {
        var p = java.nio.file.Paths.get(path);
        var parent = p.getParent();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent);
        }
        java.nio.file.Files.write(
            p,
            content.getBytes(StandardCharsets.US_ASCII));
    }
}
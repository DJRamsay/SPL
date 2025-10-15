import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import lexer.Lexer;
import lexer.LexerException;
import lexer.Token;
import parser.Parser;
import parser.ParserException;
import parser.SyntaxNode;
import parser.SyntaxTreeBuilder;
public class Main {
    //Input mechamism to feed into main???

    public static void main(String args[]) throws IOException{
        String source = null;
        BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));

        try {
            if (args.length < 1){
                System.out.println("No input file provided.");
                System.out.println("Choose an option:");
                System.out.println("  1) Paste source (end with a line containing only EOF)");
                System.out.println("  2) Enter file path");
                System.out.print("Enter 1 or 2: ");
                String choice = consoleIn.readLine();

                if ("2".equals(choice)) {
                    System.out.print("Enter file path: ");
                    String path = consoleIn.readLine();
                    source = readFile(path);
                } else {
                    System.out.println("Paste source now. End with a line containing only EOF:");
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = consoleIn.readLine()) != null) {
                        if (line.equals("EOF")) break;
                        sb.append(line).append(System.lineSeparator());
                    }
                    source = sb.toString();
                }
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
                Parser parser = new Parser(tokens); // adjust if your Parser uses a different ctor
                parser.parse(); // adjust if method name differs
                System.out.println("Parse successful");

                // Build and print a simple syntax tree from the token stream
                System.out.println("====Syntax Tree====");
                SyntaxNode tree = new SyntaxTreeBuilder(tokens).build();
                tree.print(0);

            } catch (ParserException pe) {
                System.err.println("Parse error: " + pe.getMessage());
                System.exit(1);
            }
            // === end added parsing step ===

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
        java.nio.file.Files.write(
            java.nio.file.Paths.get(path), 
            content.getBytes());
    }
}
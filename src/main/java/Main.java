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
            if (args.length < 1) {
                String defaultInput = "input.txt";
                System.out.println("No arguments provided. Reading from " + defaultInput);
                source = readFile(defaultInput);
            } else {
                source = readFile(args[0]);
            }

            System.out.println("====Lexical Analysis====");
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            System.out.println("Tokens generated: " + tokens.size());

            // ... (Debug printing of source and tokens remains the same) ...
            
            // === parsing step ===
            System.out.println("====Parsing====");
            try {
                Parser parser = new Parser(tokens);
                parser.parse();
                System.out.println("Parse successful");

                // === 1. Code Generation (UN-NUMBERED INTERMEDIATE CODE) ===
                System.out.println("====Intermediate Code (Un-numbered)====");
                CodeGenerator gen = new CodeGenerator(tokens);
                String intermediateCodeString = gen.generate(); // Original output as single string
                System.out.println(intermediateCodeString);

                // Save the un-numbered code to the file (as the Code Generator currently does)
                writeFile(INTERMEDIATE_CODE_PATH, intermediateCodeString);
                System.out.println("Saved intermediate code to: " + INTERMEDIATE_CODE_PATH);

                // ----------------------------------------------------------------------
                // === 2. POST-PROCESSING STEP (Reads File -> Transforms -> Writes File) ===
                // ----------------------------------------------------------------------
                
                System.out.println("====Post-Processing (Line Numbering & Jumps)====");
                
                // A. READ: Read the un-numbered code back from the file, splitting it into lines
                List<String> intermediateCodeLines = Files.readAllLines(
                    Paths.get(INTERMEDIATE_CODE_PATH), 
                    StandardCharsets.US_ASCII
                );
                
                // B. PROCESS: Use your BasicPostProcessor to perform the transformation
                BasicPostProcessor postProcessor = new BasicPostProcessor();
                List<String> finalBasicCodeLines = postProcessor.generateExecutableBasic(intermediateCodeLines);
                
                // Convert the list of final BASIC lines back into a single string
                String finalExecutableCode = String.join(System.lineSeparator(), finalBasicCodeLines);

                // C. WRITE: Overwrite the file with the final, executable BASIC code
                writeFile(INTERMEDIATE_CODE_PATH, finalExecutableCode);
                
                System.out.println("---- Final Executable BASIC Code ----");
                System.out.println(finalExecutableCode);
                System.out.println("Final executable code saved to: " + INTERMEDIATE_CODE_PATH);


            } catch (ParserException pe) {
                System.err.println("Parse error: " + pe.getMessage());
                System.exit(1);
            }
            // ... (Error handling for Lexer and IO remains the same) ...

        } catch (LexerException e) {
            System.err.println("Compilation error: " + e.getMessage());
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
    
    private static void writeFile(String path, String content) 
            throws IOException {
        var p = Paths.get(path);
        var parent = p.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(
            p,
            content.getBytes(StandardCharsets.US_ASCII));
    }
}
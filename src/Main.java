import lexer.*;
import java.util.List;
import java.io.*;
public class Main {
    //Input mechamism to feed into main???

    public static void main(String args[]){
        if (args.length < 1){
            System.err.println("Empty Input");
            System.exit(1);
        }

        try {
            String source =  readFile(args[0]);

            System.out.println("====Lexical Analysis====");
            Lexer lexer =  new Lexer(source);
            List<Token> tokens =  lexer.tokenize();
            System.out.println("Token generated: " + tokens.size());

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

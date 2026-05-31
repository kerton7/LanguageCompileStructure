package PLProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String filePath = "ornek1.txt";
        if (args.length > 0) {
            filePath = args[0];
        }

        String sourceCode;
        try {
            sourceCode = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            System.out.println("--- Kaynak Kod ---");
            System.out.println(sourceCode);
            System.out.println("------------------");
        } catch (IOException e) {
            System.err.println("Dosya okuma hatası '" + filePath + "': " + e.getMessage());
            return;
        }

        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens;
        try {
            tokens = lexer.lex();
            System.out.println("\n--- Belirteçler (Tokenlar) ---");
            for (Token token : tokens) {
                System.out.println(token);
            }
            System.out.println("-----------------------------");
        } catch (RuntimeException e) {
            System.err.println("\nLeksik Analiz Hatası: " + e.getMessage());
            return;
        }

        Parser parser = new Parser(tokens);
        try {
            System.out.println("\n--- Ayrıştırma Çıktısı ---");
            parser.parse();
            System.out.println("-------------------------");
            System.out.println("\nAyrıştırma ve çalıştırma başarıyla tamamlandı.");
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }
}
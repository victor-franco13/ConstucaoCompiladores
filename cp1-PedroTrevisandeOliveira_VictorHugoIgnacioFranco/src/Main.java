import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import lexer.LexicalError;
import lexer.Scanner;
import lexer.Token;
import lexer.TokenType;

//Uso:  java -cp out Main exemplos/programa.mini

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Uso: java -cp out Main <arquivo.mini>");
            System.exit(2);
        }

        String fonte = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(fonte);

        System.out.printf("%-8s %-16s %s%n", "POSIÇÃO", "TOKEN", "LEXEMA");
        for (Token t : scanner.tokenize()) {
            String pos = t.line + ":" + t.column;
            if (t.type == TokenType.ERROR) {
                System.out.printf("%-8s %-16s %s   <-- %s%n", pos, t.type, t.lexeme, t.value);
            } else {
                System.out.printf("%-8s %-16s %s%n", pos, t.type, t.lexeme);
            }
        }

        System.out.println();
        if (scanner.getErrors().isEmpty()) {
            System.out.println("Nenhum erro léxico.");
        } else {
            System.out.println(scanner.getErrors().size() + " erro(s) léxico(s):");
            for (LexicalError e : scanner.getErrors()) {
                System.out.println("  " + e);
            }
        }
    }
}

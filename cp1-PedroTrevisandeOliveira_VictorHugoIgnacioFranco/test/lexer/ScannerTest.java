package lexer;

import java.util.ArrayList;
import java.util.List;


public class ScannerTest {

    private static int passou = 0;
    private static int falhou = 0;

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Tokeniza e devolve "TIPO(lexema) TIPO(lexema) ..." sem o EOF. */
    private static String tokens(String fonte) {
        List<String> partes = new ArrayList<>();
        for (Token t : new Scanner(fonte).tokenize()) {
            if (t.type != TokenType.EOF) {
                partes.add(t.type + "(" + t.lexeme + ")");
            }
        }
        return String.join(" ", partes);
    }

    private static void verifica(String nome, boolean condicao, String detalhe) {
        if (condicao) {
            passou++;
            System.out.println("  [OK]     " + nome);
        } else {
            falhou++;
            System.out.println("  [FALHOU] " + nome);
            System.out.println("           " + detalhe);
        }
    }

    /** Compara os tokens gerados para {@code fonte} com {@code esperado}. */
    private static void testa(String nome, String fonte, String esperado) {
        String obtido = tokens(fonte);
        verifica(nome, obtido.equals(esperado),
                "esperado: " + esperado + "\n           obtido:   " + obtido);
    }

    /** Verifica que o scanner reportou exatamente estes erros ("linha:coluna mensagem"). */
    private static void testaErros(String nome, String fonte, String... esperados) {
        Scanner sc = new Scanner(fonte);
        sc.tokenize();
        List<String> obtidos = new ArrayList<>();
        for (LexicalError e : sc.getErrors()) {
            obtidos.add(e.line + ":" + e.column + " " + e.message);
        }
        boolean ok = obtidos.size() == esperados.length;
        for (int i = 0; ok && i < esperados.length; i++) {
            ok = obtidos.get(i).startsWith(esperados[i]);
        }
        verifica(nome, ok,
                "esperado: " + String.join(" | ", esperados)
                        + "\n           obtido:   " + String.join(" | ", obtidos));
    }

    /** Verifica a posição (linha:coluna) do n-ésimo token. */
    private static void testaPosicao(String nome, String fonte, int indice, int linha, int coluna) {
        Token t = new Scanner(fonte).tokenize().get(indice);
        verifica(nome, t.line == linha && t.column == coluna,
                "esperado " + linha + ":" + coluna + ", obtido " + t.line + ":" + t.column
                        + " (token " + t + ")");
    }

    private static void secao(String titulo) {
        System.out.println();
        System.out.println(titulo);
    }

    // ------------------------------------------------------------------
    // Testes
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== Suíte de testes do scanner da linguagem Mini ===");

        // ---------------- Parte A: cada categoria isoladamente ----------------
        secao("Parte A — uma categoria de token por vez (casos válidos)");

        // 1. Identificador: letra (letra | digito | "_")*
        testa("A1 identificador simples", "total", "IDENTIFIER(total)");
        testa("A2 identificadores com dígito, camelCase e '_'",
                "x1 contaItens taxa_juros",
                "IDENTIFIER(x1) IDENTIFIER(contaItens) IDENTIFIER(taxa_juros)");
        testa("A3 identificador com exatamente 31 caracteres (limite)",
                "abcdefghijabcdefghijabcdefghija",
                "IDENTIFIER(abcdefghijabcdefghijabcdefghija)");

        // 2. Palavras reservadas: mesmo AFD do identificador + busca em tabela
        testa("A4 as 15 palavras reservadas",
                "int double bool char string void if else while for break continue return true false",
                "INT(int) DOUBLE(double) BOOL(bool) CHAR(char) STRING(string) VOID(void) "
                        + "IF(if) ELSE(else) WHILE(while) FOR(for) BREAK(break) CONTINUE(continue) "
                        + "RETURN(return) TRUE(true) FALSE(false)");
        testa("A5 case-sensitive: 'If' e 'WHILE' são identificadores",
                "If WHILE", "IDENTIFIER(If) IDENTIFIER(WHILE)");

        // 3. String: " ( [^"\\\n\r] | escape )* "
        testa("A6 strings simples", "\"ok\" \"linha 1\"",
                "STRING_LITERAL(\"ok\") STRING_LITERAL(\"linha 1\")");
        testa("A7 string vazia", "\"\"", "STRING_LITERAL(\"\")");
        testa("A8 string com escapes \\\" e \\n", "\"diz \\\"oi\\\"\\n\"",
                "STRING_LITERAL(\"diz \\\"oi\\\"\\n\")");
        Token s = new Scanner("\"a\\\"b\"").nextToken();
        verifica("A9 valor da string com escape decodificado (a\"b)",
                "a\"b".equals(s.value), "obtido: " + s.value);

        // 4. Operadores: simples e compostos
        testa("A10 operadores de um caractere", "+ - * / % = < > !",
                "PLUS(+) MINUS(-) STAR(*) SLASH(/) PERCENT(%) ASSIGN(=) LT(<) GT(>) NOT(!)");
        testa("A11 operadores compostos", "== != <= >= && ||",
                "EQ(==) NEQ(!=) LE(<=) GE(>=) AND(&&) OR(||)");

        // 5. Literal numérico: digito+ ( "." digito+ )?
        testa("A12 literais inteiros", "10 0 007",
                "INT_LITERAL(10) INT_LITERAL(0) INT_LITERAL(007)");
        testa("A13 literal real", "3.14", "DOUBLE_LITERAL(3.14)");

        // Tokens auxiliares
        testa("A14 literais de char", "'a' '\\n'", "CHAR_LITERAL('a') CHAR_LITERAL('\\n')");
        testa("A15 delimitadores", "( ) { } ; ,",
                "LPAREN(() RPAREN()) LBRACE({) RBRACE(}) SEMICOLON(;) COMMA(,)");

        // ---------------- Parte B: maximal munch ----------------
        secao("Parte B — maximal munch (prefixo válido mais longo)");

        testa("B1 'total==total+1;' sem espaços (Quiz 5, Aula 2)", "total==total+1;",
                "IDENTIFIER(total) EQ(==) IDENTIFIER(total) PLUS(+) INT_LITERAL(1) SEMICOLON(;)");
        testa("B2 'while1' é um único identificador (slide 15)", "while1",
                "IDENTIFIER(while1)");
        testa("B3 'x===y' vira '==' seguido de '='", "x===y",
                "IDENTIFIER(x) EQ(==) ASSIGN(=) IDENTIFIER(y)");
        testa("B4 'a<=b' vs 'a< =b'", "a<=b a< =b",
                "IDENTIFIER(a) LE(<=) IDENTIFIER(b) IDENTIFIER(a) LT(<) ASSIGN(=) IDENTIFIER(b)");
        testa("B5 '!x!=y'", "!x!=y", "NOT(!) IDENTIFIER(x) NEQ(!=) IDENTIFIER(y)");
        testa("B6 número seguido de identificador: '12abc'", "12abc",
                "INT_LITERAL(12) IDENTIFIER(abc)");

        // ---------------- Parte C: espaços e comentários ----------------
        secao("Parte C — espaços em branco e comentários (não geram token)");

        testa("C1 espaços, tabs e quebras de linha", "  a\t\r\n  b  ",
                "IDENTIFIER(a) IDENTIFIER(b)");
        testa("C2 comentário de linha", "x = 1; // comentário\ny",
                "IDENTIFIER(x) ASSIGN(=) INT_LITERAL(1) SEMICOLON(;) IDENTIFIER(y)");
        testa("C3 comentário de bloco multilinha", "a /* linha 1\n linha 2 */ b",
                "IDENTIFIER(a) IDENTIFIER(b)");
        testa("C4 comentário de bloco com '*' e '**/'", "a /* x*y **/ b",
                "IDENTIFIER(a) IDENTIFIER(b)");
        testa("C5 comentário de bloco NÃO aninha: sobra 'c */'", "/* a /* b */ c */",
                "IDENTIFIER(c) STAR(*) SLASH(/)");
        testa("C6 '/' sozinho é divisão", "a / b",
                "IDENTIFIER(a) SLASH(/) IDENTIFIER(b)");
        testa("C7 '//' dentro de string não é comentário", "\"http://x\"",
                "STRING_LITERAL(\"http://x\")");

        // ---------------- Parte D: erros léxicos ----------------
        secao("Parte D — erros léxicos (linha:coluna + recuperação)");

        // Os três obrigatórios do enunciado
        testaErros("D1 string não fechada até o EOF", "x = \"abc",
                "1:5 string não fechada (fim de arquivo)");
        testa("D1 ...e o scanner não trava: devolve o erro e depois EOF", "x = \"abc",
                "IDENTIFIER(x) ASSIGN(=) ERROR(\"abc)");

        testaErros("D2 string não fechada até o fim da linha", "x = \"abc\ny = 1;",
                "1:5 string não fechada (fim de linha)");
        testa("D2 ...e continua tokenizando a linha seguinte", "x = \"abc\ny = 1;",
                "IDENTIFIER(x) ASSIGN(=) ERROR(\"abc) IDENTIFIER(y) ASSIGN(=) INT_LITERAL(1) SEMICOLON(;)");

        testaErros("D3 caractere fora do alfabeto ('@')", "a @ b",
                "1:3 caractere inválido '@'");
        testa("D3 ...e segue para o próximo token", "a @ b",
                "IDENTIFIER(a) ERROR(@) IDENTIFIER(b)");

        // Outros erros previstos na especificação
        testaErros("D4 comentário de bloco não fechado até o EOF", "a\n/* sem fim",
                "2:1 comentário de bloco não fechado");
        testaErros("D5 literal real malformado '3.'", "3.",
                "1:1 literal numérico malformado");
        testaErros("D6 '&' e '|' sozinhos", "a & b | c",
                "1:3 operador inválido '&'", "1:7 operador inválido '|'");
        testaErros("D7 escape inválido em string", "\"a\\qb\"",
                "1:1 string com sequência de escape inválida");
        testaErros("D8 identificador com 32 caracteres (acima do limite)",
                "abcdefghijabcdefghijabcdefghijab", "1:1 identificador com mais de 31");
        testaErros("D9 '_' no início de identificador", "_x", "1:1 caractere inválido '_'");
        testaErros("D10 vários erros no mesmo arquivo, com linha e coluna",
                "int a = 1 # 2;\nstring s = \"ok\" $;\n",
                "1:11 caractere inválido '#'", "2:17 caractere inválido '$'");

        // ---------------- Parte E: código realista ----------------
        secao("Parte E — código realista, do início ao fim");

        testa("E1 'total = total + 1;' (slide 34, Aula 2)", "total = total + 1;",
                "IDENTIFIER(total) ASSIGN(=) IDENTIFIER(total) PLUS(+) INT_LITERAL(1) SEMICOLON(;)");

        testa("E2 'if (x >= 10) { print(\"ok\"); }' (slide 4, Aula 2)",
                "if (x >= 10) {\n  print(\"ok\");\n}",
                "IF(if) LPAREN(() IDENTIFIER(x) GE(>=) INT_LITERAL(10) RPAREN()) LBRACE({) "
                        + "IDENTIFIER(print) LPAREN(() STRING_LITERAL(\"ok\") RPAREN()) SEMICOLON(;) RBRACE(})");

        testa("E3 uma linha com várias declarações, espaços e comentários misturados",
                "int x = 10; double media = x / 2.5; /* calcula */ bool ok = media >= 3.0 && x != 0; // fim",
                "INT(int) IDENTIFIER(x) ASSIGN(=) INT_LITERAL(10) SEMICOLON(;) "
                        + "DOUBLE(double) IDENTIFIER(media) ASSIGN(=) IDENTIFIER(x) SLASH(/) DOUBLE_LITERAL(2.5) SEMICOLON(;) "
                        + "BOOL(bool) IDENTIFIER(ok) ASSIGN(=) IDENTIFIER(media) GE(>=) DOUBLE_LITERAL(3.0) "
                        + "AND(&&) IDENTIFIER(x) NEQ(!=) INT_LITERAL(0) SEMICOLON(;)");

        String programa =
                "// soma os números de 1 a n\n"
                + "int soma(int n) {\n"
                + "    int total = 0;\n"
                + "    while (n > 0) { total = total + n; n = n - 1; }\n"
                + "    return total;\n"
                + "}\n"
                + "/* procedimento: sem valor de retorno */\n"
                + "void main() {\n"
                + "    char c = 'A'; string msg = \"fim\\n\";\n"
                + "    if (soma(3) == 6 || !false) { return; } else { c = 'B'; }\n"
                + "}\n";
        testa("E4 programa com função, procedimento, while, if/else e os 5 tipos", programa,
                "INT(int) IDENTIFIER(soma) LPAREN(() INT(int) IDENTIFIER(n) RPAREN()) LBRACE({) "
                        + "INT(int) IDENTIFIER(total) ASSIGN(=) INT_LITERAL(0) SEMICOLON(;) "
                        + "WHILE(while) LPAREN(() IDENTIFIER(n) GT(>) INT_LITERAL(0) RPAREN()) LBRACE({) "
                        + "IDENTIFIER(total) ASSIGN(=) IDENTIFIER(total) PLUS(+) IDENTIFIER(n) SEMICOLON(;) "
                        + "IDENTIFIER(n) ASSIGN(=) IDENTIFIER(n) MINUS(-) INT_LITERAL(1) SEMICOLON(;) RBRACE(}) "
                        + "RETURN(return) IDENTIFIER(total) SEMICOLON(;) "
                        + "RBRACE(}) "
                        + "VOID(void) IDENTIFIER(main) LPAREN(() RPAREN()) LBRACE({) "
                        + "CHAR(char) IDENTIFIER(c) ASSIGN(=) CHAR_LITERAL('A') SEMICOLON(;) "
                        + "STRING(string) IDENTIFIER(msg) ASSIGN(=) STRING_LITERAL(\"fim\\n\") SEMICOLON(;) "
                        + "IF(if) LPAREN(() IDENTIFIER(soma) LPAREN(() INT_LITERAL(3) RPAREN()) EQ(==) INT_LITERAL(6) "
                        + "OR(||) NOT(!) FALSE(false) RPAREN()) LBRACE({) RETURN(return) SEMICOLON(;) RBRACE(}) "
                        + "ELSE(else) LBRACE({) IDENTIFIER(c) ASSIGN(=) CHAR_LITERAL('B') SEMICOLON(;) RBRACE(}) "
                        + "RBRACE(})");
        testaErros("E4 ...sem nenhum erro léxico", programa);
        testaPosicao("E5 posição de 'soma' na linha 2 do programa", programa, 1, 2, 5);
        testaPosicao("E6 posição de 'while' (linha 4, coluna 5)", programa, 12, 4, 5);

        // ---------------- Resumo ----------------
        System.out.println();
        System.out.println("=== Resultado: " + passou + " passaram, " + falhou + " falharam ("
                + (passou + falhou) + " testes) ===");
        if (falhou > 0) {
            System.exit(1);
        }
    }
}

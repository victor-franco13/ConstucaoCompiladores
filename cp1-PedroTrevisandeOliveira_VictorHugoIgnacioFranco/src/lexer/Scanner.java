package lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Scanner {

    public static final int MAX_ID_LENGTH = 31;

    private static final Map<String, TokenType> RESERVED = new HashMap<>();

    static {
        RESERVED.put("int", TokenType.INT);
        RESERVED.put("double", TokenType.DOUBLE);
        RESERVED.put("bool", TokenType.BOOL);
        RESERVED.put("char", TokenType.CHAR);
        RESERVED.put("string", TokenType.STRING);
        RESERVED.put("void", TokenType.VOID);
        RESERVED.put("if", TokenType.IF);
        RESERVED.put("else", TokenType.ELSE);
        RESERVED.put("while", TokenType.WHILE);
        RESERVED.put("for", TokenType.FOR);
        RESERVED.put("return", TokenType.RETURN);
        RESERVED.put("break", TokenType.BREAK);
        RESERVED.put("continue", TokenType.CONTINUE);
        RESERVED.put("true", TokenType.TRUE);
        RESERVED.put("false", TokenType.FALSE);
    }

    enum State {
        S0,                                 
        ID,                                 
        NUM_INT, NUM_DOT, NUM_FRAC,
        STR, STR_ESC,
        CHR, CHR_ESC, CHR_END,
        OP_ASSIGN, OP_LT, OP_GT, OP_NOT,
        OP_AND, OP_OR, 
        SLASH,
        LINE_COMMENT,
        BLOCK_COMMENT, BLOCK_STAR
    }

    private static final char EOF_CHAR = '\0';

    private final String src;
    private int pos = 0;
    private int line = 1;
    private int col = 1;
    private final List<LexicalError> errors = new ArrayList<>();

    public Scanner(String source) {
        this.src = source;
    }



    /** true enquanto ainda há caracteres a ler. */
    public boolean hasNext() {
        return pos < src.length();
    }

    /** Caractere atual, sem consumir ('\0' no fim do arquivo). */
    public char peek() {
        return hasNext() ? src.charAt(pos) : EOF_CHAR;
    }

    /** Consome o caractere atual e avança linha/coluna. */
    public char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    public List<LexicalError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** Tokeniza o arquivo inteiro (inclui o token EOF no final). */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token t;
        do {
            t = nextToken();
            tokens.add(t);
        } while (t.type != TokenType.EOF);
        return tokens;
    }

    // ------------------------------------------------------------------
    // Classes de caracteres do alfabeto
    // ------------------------------------------------------------------

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean isNewline(char c) {
        return c == '\n' || c == '\r';
    }

    /** Escapes válidos em string/char: \n \t \" \' \\ */
    private static char decodeEscape(char c) {
        switch (c) {
            case 'n': return '\n';
            case 't': return '\t';
            case '"': return '"';
            case '\'': return '\'';
            case '\\': return '\\';
            default: return EOF_CHAR;
        }
    }

    // ------------------------------------------------------------------
    // AFD
    // ------------------------------------------------------------------


    public Token nextToken() {
        State state = State.S0;
        StringBuilder lexeme = new StringBuilder(); // texto original do token
        StringBuilder value = new StringBuilder();  // conteúdo decodificado (string/char)
        boolean badEscape = false;
        int startLine = line;
        int startCol = col;

        while (true) {
            char c = peek();

            switch (state) {

                // ---------------- S0: estado inicial ----------------
                case S0: {
                    if (!hasNext()) {
                        return new Token(TokenType.EOF, "", line, col);
                    }
                    startLine = line;
                    startCol = col;
                    lexeme.setLength(0);
                    value.setLength(0);

                    if (isWhitespace(c)) {          // S0 --espaço--> S0
                        advance();
                    } else if (isLetter(c)) {       // S0 --letra--> ID
                        lexeme.append(advance());
                        state = State.ID;
                    } else if (isDigit(c)) {        // S0 --dígito--> NUM_INT
                        lexeme.append(advance());
                        state = State.NUM_INT;
                    } else if (c == '"') {          // S0 --"--> STR
                        lexeme.append(advance());
                        state = State.STR;
                    } else if (c == '\'') {         // S0 --'--> CHR
                        lexeme.append(advance());
                        state = State.CHR;
                    } else if (c == '=') {          // S0 --=--> OP_ASSIGN
                        lexeme.append(advance());
                        state = State.OP_ASSIGN;
                    } else if (c == '<') {
                        lexeme.append(advance());
                        state = State.OP_LT;
                    } else if (c == '>') {
                        lexeme.append(advance());
                        state = State.OP_GT;
                    } else if (c == '!') {
                        lexeme.append(advance());
                        state = State.OP_NOT;
                    } else if (c == '&') {
                        lexeme.append(advance());
                        state = State.OP_AND;
                    } else if (c == '|') {
                        lexeme.append(advance());
                        state = State.OP_OR;
                    } else if (c == '/') {
                        lexeme.append(advance());
                        state = State.SLASH;
                    } else {
                        // Tokens de um único caractere: aceitam direto a partir de S0
                        TokenType single = singleCharToken(c);
                        advance();
                        if (single != null) {
                            return token(single, String.valueOf(c), startLine, startCol);
                        }
                        // Caractere fora do alfabeto: erro e segue para o próximo token
                        return error(String.valueOf(c), startLine, startCol,
                                "caractere inválido '" + printable(c) + "'");
                    }
                    break;
                }

                // ---------------- Identificador / palavra reservada ----------------
                case ID: {
                    if (hasNext() && (isLetter(c) || isDigit(c) || c == '_')) {
                        lexeme.append(advance());       // ID --letra|dígito|_--> ID
                        break;
                    }
                    // aceita: consulta a tabela de palavras reservadas
                    String text = lexeme.toString();
                    if (text.length() > MAX_ID_LENGTH) {
                        return error(text, startLine, startCol,
                                "identificador com mais de " + MAX_ID_LENGTH + " caracteres");
                    }
                    TokenType kw = RESERVED.get(text);
                    return token(kw != null ? kw : TokenType.IDENTIFIER, text, startLine, startCol);
                }

                // ---------------- Literal numérico ----------------
                case NUM_INT: {
                    if (hasNext() && isDigit(c)) {      // NUM_INT --dígito--> NUM_INT
                        lexeme.append(advance());
                    } else if (c == '.' && hasNext()) { // NUM_INT --.--> NUM_DOT
                        lexeme.append(advance());
                        state = State.NUM_DOT;
                    } else {                            // aceita inteiro
                        return token(TokenType.INT_LITERAL, lexeme.toString(), startLine, startCol);
                    }
                    break;
                }

                case NUM_DOT: {
                    if (hasNext() && isDigit(c)) {      // NUM_DOT --dígito--> NUM_FRAC
                        lexeme.append(advance());
                        state = State.NUM_FRAC;
                        break;
                    }
                    // NUM_DOT não é de aceitação: "3." sem dígito depois é erro
                    return error(lexeme.toString(), startLine, startCol,
                            "literal numérico malformado '" + lexeme + "' (esperado dígito após '.')");
                }

                case NUM_FRAC: {
                    if (hasNext() && isDigit(c)) {      // NUM_FRAC --dígito--> NUM_FRAC
                        lexeme.append(advance());
                        break;
                    }
                    return token(TokenType.DOUBLE_LITERAL, lexeme.toString(), startLine, startCol);
                }

                // ---------------- String ----------------
                case STR: {
                    if (!hasNext()) {                   // EOF dentro da string
                        return error(lexeme.toString(), startLine, startCol,
                                "string não fechada (fim de arquivo)");
                    }
                    if (isNewline(c)) {                 // strings não podem ser multilinha
                        return error(lexeme.toString(), startLine, startCol,
                                "string não fechada (fim de linha)");
                    }
                    if (c == '"') {                     // STR --"--> aceita
                        lexeme.append(advance());
                        if (badEscape) {
                            return error(lexeme.toString(), startLine, startCol,
                                    "string com sequência de escape inválida");
                        }
                        return new Token(TokenType.STRING_LITERAL, lexeme.toString(),
                                startLine, startCol, value.toString());
                    }
                    if (c == '\\') {                    // STR --\--> STR_ESC
                        lexeme.append(advance());
                        state = State.STR_ESC;
                        break;
                    }
                    value.append(c);                    // STR --outro--> STR
                    lexeme.append(advance());
                    break;
                }

                case STR_ESC: {
                    if (!hasNext()) {
                        return error(lexeme.toString(), startLine, startCol,
                                "string não fechada (fim de arquivo)");
                    }
                    if (isNewline(c)) {
                        return error(lexeme.toString(), startLine, startCol,
                                "string não fechada (fim de linha)");
                    }
                    char decoded = decodeEscape(c);
                    if (decoded == EOF_CHAR) {
                        // escape inválido: registra e continua lendo a string (recuperação)
                        badEscape = true;
                    } else {
                        value.append(decoded);
                    }
                    lexeme.append(advance());           // STR_ESC --n|t|"|'|\--> STR
                    state = State.STR;
                    break;
                }

                // ---------------- Literal de char ----------------
                case CHR: {
                    if (!hasNext() || isNewline(c)) {
                        return error(lexeme.toString(), startLine, startCol,
                                "literal de char não fechado");
                    }
                    if (c == '\'') {                    // '' vazio
                        lexeme.append(advance());
                        return error(lexeme.toString(), startLine, startCol, "literal de char vazio");
                    }
                    if (c == '\\') {                    // CHR --\--> CHR_ESC
                        lexeme.append(advance());
                        state = State.CHR_ESC;
                        break;
                    }
                    value.append(c);                    // CHR --outro--> CHR_END
                    lexeme.append(advance());
                    state = State.CHR_END;
                    break;
                }

                case CHR_ESC: {
                    if (!hasNext() || isNewline(c)) {
                        return error(lexeme.toString(), startLine, startCol,
                                "literal de char não fechado");
                    }
                    char decoded = decodeEscape(c);
                    if (decoded == EOF_CHAR) {
                        badEscape = true;
                    } else {
                        value.append(decoded);
                    }
                    lexeme.append(advance());           // CHR_ESC --n|t|"|'|\--> CHR_END
                    state = State.CHR_END;
                    break;
                }

                case CHR_END: {
                    if (c == '\'' && hasNext()) {       // CHR_END --'--> aceita
                        lexeme.append(advance());
                        if (badEscape) {
                            return error(lexeme.toString(), startLine, startCol,
                                    "literal de char com sequência de escape inválida");
                        }
                        return new Token(TokenType.CHAR_LITERAL, lexeme.toString(),
                                startLine, startCol, value.toString());
                    }
                    // Recuperação: se houver um ' mais adiante na mesma linha,
                    // consome até ele (ex.: 'ab'); senão, para aqui.
                    if (closingQuoteOnSameLine()) {
                        while (peek() != '\'') {
                            lexeme.append(advance());
                        }
                        lexeme.append(advance());
                        return error(lexeme.toString(), startLine, startCol,
                                "literal de char com mais de um caractere");
                    }
                    return error(lexeme.toString(), startLine, startCol,
                            "literal de char não fechado");
                }

                // ---------------- Operadores (maximal munch) ----------------
                case OP_ASSIGN: {                       // '=' ou '=='
                    if (c == '=' && hasNext()) {
                        lexeme.append(advance());
                        return token(TokenType.EQ, "==", startLine, startCol);
                    }
                    return token(TokenType.ASSIGN, "=", startLine, startCol);
                }

                case OP_LT: {                           // '<' ou '<='
                    if (c == '=' && hasNext()) {
                        lexeme.append(advance());
                        return token(TokenType.LE, "<=", startLine, startCol);
                    }
                    return token(TokenType.LT, "<", startLine, startCol);
                }

                case OP_GT: {                           // '>' ou '>='
                    if (c == '=' && hasNext()) {
                        lexeme.append(advance());
                        return token(TokenType.GE, ">=", startLine, startCol);
                    }
                    return token(TokenType.GT, ">", startLine, startCol);
                }

                case OP_NOT: {                          // '!' ou '!='
                    if (c == '=' && hasNext()) {
                        lexeme.append(advance());
                        return token(TokenType.NEQ, "!=", startLine, startCol);
                    }
                    return token(TokenType.NOT, "!", startLine, startCol);
                }

                case OP_AND: {                          // só '&&' existe
                    if (c == '&' && hasNext()) {
                        lexeme.append(advance());
                        return token(TokenType.AND, "&&", startLine, startCol);
                    }
                    return error("&", startLine, startCol, "operador inválido '&' (use '&&')");
                }

                case OP_OR: {                           // só '||' existe
                    if (c == '|' && hasNext()) {
                        lexeme.append(advance());
                        return token(TokenType.OR, "||", startLine, startCol);
                    }
                    return error("|", startLine, startCol, "operador inválido '|' (use '||')");
                }

                // ---------------- '/' e comentários ----------------
                case SLASH: {
                    if (c == '/' && hasNext()) {        // SLASH --/--> LINE_COMMENT
                        advance();
                        state = State.LINE_COMMENT;
                    } else if (c == '*' && hasNext()) { // SLASH --*--> BLOCK_COMMENT
                        advance();
                        state = State.BLOCK_COMMENT;
                    } else {                            // aceita divisão
                        return token(TokenType.SLASH, "/", startLine, startCol);
                    }
                    break;
                }

                case LINE_COMMENT: {
                    if (!hasNext() || c == '\n') {      // fim do comentário: volta a S0
                        state = State.S0;               // (o '\n' é consumido em S0)
                    } else {
                        advance();
                    }
                    break;
                }

                case BLOCK_COMMENT: {
                    if (!hasNext()) {
                        return error("/*", startLine, startCol,
                                "comentário de bloco não fechado (fim de arquivo)");
                    }
                    if (c == '*') {                     // BLOCK_COMMENT --*--> BLOCK_STAR
                        state = State.BLOCK_STAR;
                    }
                    advance();
                    break;
                }

                case BLOCK_STAR: {
                    if (!hasNext()) {
                        return error("/*", startLine, startCol,
                                "comentário de bloco não fechado (fim de arquivo)");
                    }
                    if (c == '/') {                     // BLOCK_STAR --/--> S0
                        state = State.S0;
                    } else if (c != '*') {              // BLOCK_STAR --outro--> BLOCK_COMMENT
                        state = State.BLOCK_COMMENT;
                    }                                   // BLOCK_STAR --*--> BLOCK_STAR
                    advance();
                    break;
                }

                default:
                    throw new IllegalStateException("estado desconhecido: " + state);
            }
        }
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private static TokenType singleCharToken(char c) {
        switch (c) {
            case '+': return TokenType.PLUS;
            case '-': return TokenType.MINUS;
            case '*': return TokenType.STAR;
            case '%': return TokenType.PERCENT;
            case '(': return TokenType.LPAREN;
            case ')': return TokenType.RPAREN;
            case '{': return TokenType.LBRACE;
            case '}': return TokenType.RBRACE;
            case ';': return TokenType.SEMICOLON;
            case ',': return TokenType.COMMA;
            default: return null;
        }
    }

    private boolean closingQuoteOnSameLine() {
        for (int i = pos; i < src.length(); i++) {
            char ch = src.charAt(i);
            if (ch == '\'') return true;
            if (isNewline(ch)) return false;
        }
        return false;
    }

    private static String printable(char c) {
        if (c == '\t') return "\\t";
        if (c < 32 || c == 127) return String.format("\\u%04x", (int) c);
        return String.valueOf(c);
    }

    private Token token(TokenType type, String lexeme, int l, int c) {
        return new Token(type, lexeme, l, c);
    }

    private Token error(String lexeme, int l, int c, String message) {
        errors.add(new LexicalError(l, c, message));
        return new Token(TokenType.ERROR, lexeme, l, c, message);
    }
}

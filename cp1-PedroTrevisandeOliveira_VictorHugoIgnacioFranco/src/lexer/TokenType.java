package lexer;


public enum TokenType {
    // ---- Identificador ----
    IDENTIFIER(Category.IDENTIFICADOR),

    // ---- Palavras reservadas (lista fechada, ver especificacao-lexica.md) ----
    INT(Category.PALAVRA_RESERVADA),
    DOUBLE(Category.PALAVRA_RESERVADA),
    BOOL(Category.PALAVRA_RESERVADA),
    CHAR(Category.PALAVRA_RESERVADA),
    STRING(Category.PALAVRA_RESERVADA),
    VOID(Category.PALAVRA_RESERVADA),
    IF(Category.PALAVRA_RESERVADA),
    ELSE(Category.PALAVRA_RESERVADA),
    WHILE(Category.PALAVRA_RESERVADA),
    FOR(Category.PALAVRA_RESERVADA),
    RETURN(Category.PALAVRA_RESERVADA),
    BREAK(Category.PALAVRA_RESERVADA),
    CONTINUE(Category.PALAVRA_RESERVADA),
    TRUE(Category.PALAVRA_RESERVADA),
    FALSE(Category.PALAVRA_RESERVADA),

    // ---- Literais ----
    INT_LITERAL(Category.LITERAL_NUMERICO),
    DOUBLE_LITERAL(Category.LITERAL_NUMERICO),
    STRING_LITERAL(Category.STRING),
    CHAR_LITERAL(Category.CHAR),

    // ---- Operadores ----
    PLUS(Category.OPERADOR),      // +
    MINUS(Category.OPERADOR),     // -
    STAR(Category.OPERADOR),      // *
    SLASH(Category.OPERADOR),     // /
    PERCENT(Category.OPERADOR),   // %
    ASSIGN(Category.OPERADOR),    // =
    EQ(Category.OPERADOR),        // ==
    NEQ(Category.OPERADOR),       // !=
    LT(Category.OPERADOR),        // <
    LE(Category.OPERADOR),        // <=
    GT(Category.OPERADOR),        // >
    GE(Category.OPERADOR),        // >=
    AND(Category.OPERADOR),       // &&
    OR(Category.OPERADOR),        // ||
    NOT(Category.OPERADOR),       // !

    // ---- Delimitadores ----
    LPAREN(Category.DELIMITADOR),    // (
    RPAREN(Category.DELIMITADOR),    // )
    LBRACE(Category.DELIMITADOR),    // {
    RBRACE(Category.DELIMITADOR),    // }
    SEMICOLON(Category.DELIMITADOR), // ;
    COMMA(Category.DELIMITADOR),     // ,

    // ---- Especiais ----
    ERROR(Category.ERRO),
    EOF(Category.FIM);

    public enum Category {
        IDENTIFICADOR, PALAVRA_RESERVADA, STRING, CHAR, OPERADOR,
        LITERAL_NUMERICO, DELIMITADOR, ERRO, FIM
    }

    private final Category category;

    TokenType(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }
}

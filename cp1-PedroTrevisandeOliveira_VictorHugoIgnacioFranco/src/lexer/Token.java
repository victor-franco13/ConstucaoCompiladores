package lexer;

public final class Token {
    public final TokenType type;
    public final String lexeme;
    public final int line;
    public final int column;
    public final String value;

    public Token(TokenType type, String lexeme, int line, int column) {
        this(type, lexeme, line, column, null);
    }

    public Token(TokenType type, String lexeme, int line, int column, String value) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%d:%d %s '%s'", line, column, type, lexeme);
    }
}

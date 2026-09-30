package lexer;

public final class LexicalError {
    public final int line;
    public final int column;
    public final String message;

    public LexicalError(int line, int column, String message) {
        this.line = line;
        this.column = column;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("Erro léxico [%d:%d]: %s", line, column, message);
    }
}

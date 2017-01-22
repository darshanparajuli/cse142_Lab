package crux;

public class Token {

    public enum Kind {

        AND("and"),
        OR("or"),
        NOT("not"),

        LET("let"),
        VAR("var"),
        ARRAY("array"),
        FUNC("func"),
        IF("if"),
        ELSE("else"),
        WHILE("while"),
        TRUE("true"),
        FALSE("false"),
        RETURN("return"),

        ADD("+"),
        SUB("-"),
        MUL("*"),
        DIV("/"),

        GREATER_EQUAL(">="),
        LESSER_EQUAL("<="),
        NOT_EQUAL("!="),
        EQUAL("=="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        ASSIGN("="),
        COMMA(","),
        SEMICOLON(";"),
        COLON(":"),
        CALL("::"),

        OPEN_PAREN("("),
        CLOSE_PAREN(")"),
        OPEN_BRACE("{"),
        CLOSE_BRACE("}"),
        OPEN_BRACKET("["),
        CLOSE_BRACKET("]"),

        IDENTIFIER(),
        INTEGER(),
        FLOAT(),
        ERROR(),
        EOF();

        private final String defaultLexeme;

        Kind() {
            defaultLexeme = "";
        }

        Kind(String lexeme) {
            defaultLexeme = lexeme;
        }

        @Override
        public String toString() {
            return defaultLexeme;
        }
    }

    private Kind kind;

    private int lineNum;
    private int charPos;
    private String lexeme = "";

    public static Token EOF(int lineNum, int charPos) {
        final Token token = new Token(lineNum, charPos);
        token.kind = Kind.EOF;
        return token;
    }

    public static Token IDENTIFIER(String lexeme, int lineNum, int charPos) {
        final Token token = new Token(lineNum, charPos);
        token.kind = Kind.IDENTIFIER;
        token.lexeme = lexeme;
        return token;
    }

    public static Token INTEGER(String lexeme, int lineNum, int charPos) {
        final Token token = new Token(lineNum, charPos);
        token.kind = Kind.INTEGER;
        token.lexeme = lexeme;
        return token;
    }

    public static Token FLOAT(String lexeme, int lineNum, int charPos) {
        final Token token = new Token(lineNum, charPos);
        token.kind = Kind.FLOAT;
        token.lexeme = lexeme;
        return token;
    }

    public static Token ERROR(String lexeme, int lineNum, int charPos) {
        final Token token = new Token(lineNum, charPos);
        token.kind = Kind.ERROR;
        token.lexeme = "Unexpected character: " + lexeme;
        return token;
    }

    private Token(int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;

        // if we don't match anything, signal error
        this.kind = Kind.ERROR;
        this.lexeme = "No Lexeme Given";
    }

    public Token(String lexeme, int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;

        for (Kind k : Kind.values()) {
            if (lexeme.equals(k.defaultLexeme)) {
                this.kind = k;
                this.lexeme = k.defaultLexeme;
                return;
            }
        }

        // if we don't match anything, signal error
        this.kind = Kind.ERROR;
        this.lexeme = "Unrecognized lexeme: " + lexeme;
    }

    public Kind kind() {
        return kind;
    }

    public int lineNumber() {
        return lineNum;
    }

    public int charPosition() {
        return charPos;
    }

    public String lexeme() {
        return lexeme;
    }

    public boolean is(Kind kind) {
        return this.kind.equals(kind);
    }

    public String toString() {
        switch (kind) {
            case IDENTIFIER:
            case INTEGER:
            case FLOAT:
            case ERROR: {
                return String.format("%s(%s)(lineNum:%d, charPos:%d)", kind.name(),
                        lexeme, lineNum, charPos);
            }
            default: {
                return String.format("%s(lineNum:%d, charPos:%d)", kind.name(), lineNum, charPos);
            }
        }
    }
}

package crux;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class Scanner implements Iterable<Token> {

    public static String studentName = "Darshan Parajuli";
    public static String studentID = "16602518";
    public static String uciNetID = "dparajul";

    private int lineNum;  // current line count
    private int charPos;  // character offset for current line
    private int nextChar; // contains the next char (-1 == EOF)
    private Reader input;

    private State mNextState;

    public Scanner(Reader reader) {
        lineNum = 1;
        charPos = 0;
        input = reader;
        mNextState = State.START;
        readNextChar();
    }

    private void readNextChar() {
        try {
            nextChar = input.read();
            charPos++;
        } catch (IOException e) {
            nextChar = -1;
        }
    }

    /* Invariants:
     *  1. call assumes that nextChar is already holding an unread character
     *  2. return leaves nextChar containing an untokenized character
     */
    public Token next() {
        final StringBuilder lexeme = new StringBuilder();
        while (mNextState != State.FINISH) {
            switch (mNextState) {
                case START: {
                    lexeme.setLength(0);
                    switch (nextChar) {
                        case -1: {
                            mNextState = State.FINISH;
                            return Token.EOF(lineNum, charPos);
                        }
                        case '\n': {
                            lineNum++;
                            charPos = 0;
                            readNextChar();
                        }
                        break;
                        case ' ': {
                            do {
                                readNextChar();
                            } while (nextChar == ' ');
                        }
                        break;
                        case '=': {
                            mNextState = State.EQUAL;
                            readNextChar();
                        }
                        break;
                        case '+': {
                            readNextChar();
                            return new Token("+", lineNum, charPos - 1);
                        }
                        case '-': {
                            readNextChar();
                            return new Token("-", lineNum, charPos - 1);
                        }
                        case '*': {
                            readNextChar();
                            return new Token("*", lineNum, charPos - 1);
                        }
                        case '/': {
                            mNextState = State.COMMENT;
                            readNextChar();
                        }
                        break;
                        case ',': {
                            readNextChar();
                            return new Token(",", lineNum, charPos - 1);
                        }
                        case ';': {
                            readNextChar();
                            return new Token(";", lineNum, charPos - 1);
                        }
                        case ':': {
                            mNextState = State.CALL;
                            readNextChar();
                        }
                        break;
                        case '(': {
                            readNextChar();
                            return new Token("(", lineNum, charPos - 1);
                        }
                        case ')': {
                            readNextChar();
                            return new Token(")", lineNum, charPos - 1);
                        }
                        case '{': {
                            readNextChar();
                            return new Token("{", lineNum, charPos - 1);
                        }
                        case '}': {
                            readNextChar();
                            return new Token("}", lineNum, charPos - 1);
                        }
                        case '[': {
                            readNextChar();
                            return new Token("[", lineNum, charPos - 1);
                        }
                        case ']': {
                            readNextChar();
                            return new Token("]", lineNum, charPos - 1);
                        }
                        case '<': {
                            mNextState = State.LESSER_EQUAL;
                            readNextChar();
                        }
                        break;
                        case '>': {
                            mNextState = State.GREATER_EQUAL;
                            readNextChar();
                        }
                        break;
                        case '!': {
                            mNextState = State.NOT_EQUAL;
                            readNextChar();
                        }
                        break;
                        case '_': {
                            mNextState = State.IDENTIFIER;
                        }
                        break;
                        default: {
                            if (Character.isLetter(nextChar)) {
                                if (Character.isUpperCase(nextChar)) {
                                    mNextState = State.IDENTIFIER;
                                } else {
                                    switch (nextChar) {
                                        case 'a': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            switch (nextChar) {
                                                case 'n': {
                                                    lexeme.append(Character.toChars(nextChar));
                                                    readNextChar();
                                                    final Token t = matchKeywordAndSetNextState(Keyword.AND,
                                                            2, lexeme);
                                                    if (t != null) {
                                                        return t;
                                                    }
                                                }
                                                break;
                                                case 'r': {
                                                    lexeme.append(Character.toChars(nextChar));
                                                    readNextChar();
                                                    final Token t = matchKeywordAndSetNextState(Keyword.ARRAY,
                                                            2, lexeme);
                                                    if (t != null) {
                                                        return t;
                                                    }
                                                }
                                                break;
                                                default: {
                                                    mNextState = State.IDENTIFIER;
                                                }
                                            }
                                        }
                                        break;
                                        case 'e': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.ELSE, 1,
                                                    lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 'f': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            switch (nextChar) {
                                                case 'a': {
                                                    lexeme.append(Character.toChars(nextChar));
                                                    readNextChar();
                                                    final Token t = matchKeywordAndSetNextState(Keyword.FALSE,
                                                            2, lexeme);
                                                    if (t != null) {
                                                        return t;
                                                    }
                                                }
                                                break;
                                                case 'u': {
                                                    lexeme.append(Character.toChars(nextChar));
                                                    readNextChar();
                                                    final Token t = matchKeywordAndSetNextState(Keyword.FUNC,
                                                            2, lexeme);
                                                    if (t != null) {
                                                        return t;
                                                    }
                                                }
                                                break;
                                                default: {
                                                    mNextState = State.IDENTIFIER;
                                                }
                                            }
                                        }
                                        break;
                                        case 'i': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.IF,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 'l': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.LET,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 'n': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.NOT,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 'o': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.OR,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 'r': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.RETURN,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 't': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.TRUE,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 'v': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.VAR,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        case 'w': {
                                            lexeme.append(Character.toChars(nextChar));
                                            readNextChar();
                                            final Token t = matchKeywordAndSetNextState(Keyword.WHILE,
                                                    1, lexeme);
                                            if (t != null) {
                                                return t;
                                            }
                                        }
                                        break;
                                        default: {
                                            mNextState = State.IDENTIFIER;
                                        }
                                    }
                                }
                            } else if (Character.isDigit(nextChar)) {
                                mNextState = State.INTEGER;
                                lexeme.append(Character.toChars(nextChar));
                                readNextChar();
                            } else {
                                final Token t = Token.ERROR(String.valueOf((char) nextChar), lineNum, charPos);
                                readNextChar();
                                return t;
                            }
                        }
                    }
                }
                break;
                case CALL: {
                    mNextState = State.START;
                    switch (nextChar) {
                        case ':': {
                            readNextChar();
                            return new Token("::", lineNum, charPos - 2);
                        }
                        default: {
                            return new Token(":", lineNum, charPos - 1);
                        }
                    }
                }
                case EQUAL: {
                    mNextState = State.START;
                    switch (nextChar) {
                        case '=': {
                            readNextChar();
                            return new Token("==", lineNum, charPos - 2);
                        }
                        default: {
                            return new Token("=", lineNum, charPos - 1);
                        }
                    }
                }
                case LESSER_EQUAL: {
                    mNextState = State.START;
                    switch (nextChar) {
                        case '=': {
                            readNextChar();
                            return new Token("<=", lineNum, charPos - 2);
                        }
                        default: {
                            return new Token("<", lineNum, charPos - 1);
                        }
                    }
                }
                case GREATER_EQUAL: {
                    mNextState = State.START;
                    switch (nextChar) {
                        case '=': {
                            readNextChar();
                            return new Token(">=", lineNum, charPos - 2);
                        }
                        default: {
                            return new Token(">", lineNum, charPos - 1);
                        }
                    }
                }
                case COMMENT: {
                    mNextState = State.START;
                    switch (nextChar) {
                        case '/': {
                            // eat until end of line
                            do {
                                readNextChar();
                            } while (nextChar != '\n' && nextChar != -1);
                        }
                        break;
                        default: {
                            return new Token("/", lineNum, charPos - 1);
                        }
                    }
                }
                break;
                case NOT_EQUAL: {
                    mNextState = State.START;
                    switch (nextChar) {
                        case '=': {
                            readNextChar();
                            return new Token("!=", lineNum, charPos - 2);
                        }
                        default: {
                            return Token.ERROR("!", lineNum, charPos - 1);
                        }
                    }
                }
                case IDENTIFIER: {
                    mNextState = State.START;
                    final int startPos = charPos - lexeme.length();

                    while (isValidIdentifier(nextChar)) {
                        lexeme.append(Character.toChars(nextChar));
                        readNextChar();
                    }

                    return Token.IDENTIFIER(lexeme.toString(), lineNum, startPos);
                }
                case INTEGER: {
                    mNextState = State.START;
                    final int startPos = charPos - 1;

                    while (Character.isDigit(nextChar)) {
                        lexeme.append(Character.toChars(nextChar));
                        readNextChar();
                    }

                    switch (nextChar) {
                        case '.': {
                            mNextState = State.FLOAT;
                            lexeme.append(Character.toChars(nextChar));
                            readNextChar();
                        }
                        break;
                        default: {
                            return Token.INTEGER(lexeme.toString(), lineNum, startPos);
                        }
                    }
                }
                break;
                case FLOAT: {
                    mNextState = State.START;
                    final int startPos = charPos - lexeme.length();
                    while (Character.isDigit(nextChar)) {
                        lexeme.append(Character.toChars(nextChar));
                        readNextChar();
                    }

                    return Token.FLOAT(lexeme.toString(), lineNum, startPos);
                }
            }
        }

        return null;
    }

    private Token matchKeywordAndSetNextState(String keyword, int startIndex, StringBuilder lexeme) {
        final int N = keyword.length();
        boolean matches = true;
        for (int i = startIndex; i < N; i++) {
            if (keyword.charAt(i) == (char) nextChar) {
                lexeme.append(Character.toChars(nextChar));
                readNextChar();
            } else {
                matches = false;
                break;
            }
        }

        if (matches && !isValidIdentifier(nextChar)) {
            mNextState = State.START;
            return new Token(keyword, lineNum, charPos - keyword.length());
        } else {
            mNextState = State.IDENTIFIER;
        }
        return null;
    }

    private boolean isValidIdentifier(int c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    @Override
    public Iterator<Token> iterator() {
        return new MyIterator();
    }

    private class MyIterator implements Iterator<Token> {

        @Override
        public boolean hasNext() {
            return mNextState != State.FINISH;
        }

        @Override
        public Token next() {
            return Scanner.this.next();
        }
    }

    private interface Keyword {

        String AND = Token.Kind.AND.toString();
        String OR = Token.Kind.OR.toString();
        String NOT = Token.Kind.NOT.toString();
        String LET = Token.Kind.LET.toString();
        String VAR = Token.Kind.VAR.toString();
        String ARRAY = Token.Kind.ARRAY.toString();
        String FUNC = Token.Kind.FUNC.toString();
        String IF = Token.Kind.IF.toString();
        String ELSE = Token.Kind.ELSE.toString();
        String WHILE = Token.Kind.WHILE.toString();
        String TRUE = Token.Kind.TRUE.toString();
        String FALSE = Token.Kind.FALSE.toString();
        String RETURN = Token.Kind.RETURN.toString();
    }

    private enum State {
        START,

        GREATER_EQUAL,
        LESSER_EQUAL,
        NOT_EQUAL,
        EQUAL,
        CALL,

        IDENTIFIER,

        INTEGER,
        FLOAT,

        COMMENT,

        FINISH
    }
}

package crux;

public class Parser {

    public static String studentName = "Darshan Parajuli";
    public static String studentID = "16602518";
    public static String uciNetID = "dparajul";

    // SymbolTable Management ==========================
    private SymbolTable symbolTable;
    private StringBuilder errorBuffer;

    private Scanner scanner;
    private Token currentToken;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
        errorBuffer = new StringBuilder();
        currentToken = scanner.next();
    }

    public boolean hasError() {
        return errorBuffer.length() != 0;
    }

    public String errorReport() {
        return errorBuffer.toString();
    }

    private void initSymbolTable() {
        symbolTable = SymbolTable.pushSymbolTable(symbolTable);
    }

    private void enterScope() {
        symbolTable = SymbolTable.pushSymbolTable(symbolTable);
    }

    private void exitScope() {
        symbolTable = SymbolTable.popSymbolTable(symbolTable);
    }

    private Symbol tryResolveSymbol(Token ident) {
        assert (ident.is(Token.Kind.IDENTIFIER));
        String name = ident.lexeme();
        try {
            return symbolTable.lookup(name);
        } catch (SymbolNotFoundError e) {
            String message = reportResolveSymbolError(name, ident.lineNumber(), ident.charPosition());
            return new ErrorSymbol(message);
        }
    }

    private String reportResolveSymbolError(String name, int lineNum, int charPos) {
        String message = "ResolveSymbolError(" + lineNum + "," + charPos + ")[Could not find " + name + ".]";
        errorBuffer.append(message)
                .append("\n");
        errorBuffer.append(symbolTable.toString())
                .append("\n");
        return message;
    }

    private Symbol tryDeclareSymbol(Token ident) {
        assert (ident.is(Token.Kind.IDENTIFIER));
        String name = ident.lexeme();
        try {
            return symbolTable.insert(name);
        } catch (RedeclarationError re) {
            String message = reportDeclareSymbolError(name, ident.lineNumber(), ident.charPosition());
            return new ErrorSymbol(message);
        }
    }

    private String reportDeclareSymbolError(String name, int lineNum, int charPos) {
        String message = "DeclareSymbolError(" + lineNum + "," + charPos + ")[" + name + " already exists.]";
        errorBuffer.append(message)
                .append("\n");
        errorBuffer.append(symbolTable.toString())
                .append("\n");
        return message;
    }

// Helper Methods ==========================================

    private String reportSyntaxError(NonTerminal nt) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected a token from " + nt.name()
                + " but got " + currentToken.kind().name() + ".]";
        errorBuffer.append(message)
                .append("\n");
        return message;
    }

    private String reportSyntaxError(Token.Kind kind) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected " + kind.name()
                + " but got " + currentToken.kind().name() + ".]";
        errorBuffer.append(message)
                .append("\n");
        return message;
    }

    private int lineNumber() {
        return currentToken.lineNumber();
    }

    private int charPosition() {
        return currentToken.charPosition();
    }

    private Token expectRetrieve(Token.Kind kind) {
        Token tok = currentToken;
        if (accept(kind))
            return tok;
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
        //return ErrorToken(errorMessage);
    }

    private Token expectRetrieve(NonTerminal nt) {
        Token tok = currentToken;
        if (accept(nt))
            return tok;
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
        //return ErrorToken(errorMessage);
    }

    public static class QuitParseException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public QuitParseException(String errorMessage) {
            super(errorMessage);
        }
    }

    private boolean have(Token.Kind kind) {
        return currentToken.is(kind);
    }

    private boolean have(NonTerminal nt) {
        return nt.firstSet().contains(currentToken.kind());
    }

    private boolean accept(Token.Kind kind) {
        if (have(kind)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }

    private boolean accept(NonTerminal nt) {
        if (have(nt)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }

    private boolean expect(Token.Kind kind) {
        if (accept(kind))
            return true;
        final String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private boolean expect(NonTerminal nt) {
        if (accept(nt))
            return true;
        final String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

// Parser ==========================================

    public void parse() {
        initSymbolTable();
        try {
            program();
        } catch (QuitParseException q) {
            errorBuffer.append("SyntaxError(")
                    .append(lineNumber())
                    .append(",")
                    .append(charPosition())
                    .append(")");
            errorBuffer.append("[Could not complete parsing.]");
        }
    }

    /********************************************** Grammar Rules ****************************************************/

    // literal := INTEGER | FLOAT | TRUE | FALSE .
    private void literal() {
        expect(NonTerminal.LITERAL);
    }

    // designator := IDENTIFIER { "[" expression0 "]" } .
    private void designator() {
        expect(Token.Kind.IDENTIFIER);
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expression0();
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }

    // type := IDENTIFIER .
    private void type() {
        expect(Token.Kind.IDENTIFIER);
    }

    // op0 := ">=" | "<=" | "!=" | "==" | ">" | "<" .
    private void op0() {
        expect(NonTerminal.OP0);
    }

    // op1 := "+" | "-" | "or" .
    private void op1() {
        expect(NonTerminal.OP1);
    }

    // op2 := "*" | "/" | "and" .
    private void op2() {
        expect(NonTerminal.OP2);
    }

    // expression0 := expression1 [ op0 expression1 ] .
    private void expression0() {
        expression1();
        if (have(NonTerminal.OP0)) {
            op0();
            expression1();
        }
    }

    // expression1 := expression2 { op1  expression2 } .
    private void expression1() {
        expression2();
        while (have(NonTerminal.OP1)) {
            op1();
            expression2();
        }
    }

    // expression2 := expression3 { op2 expression3 } .
    private void expression2() {
        expression3();
        while (have(NonTerminal.OP2)) {
            op2();
            expression3();
        }
    }

    // expression3 := "not" expression3
    //    | "(" expression0 ")"
    //    | designator
    //    | call-expression
    //    | literal .
    private void expression3() {
        if (accept(Token.Kind.NOT)) {
            expression3();
        } else if (accept(Token.Kind.OPEN_PAREN)) {
            expression0();
            expect(Token.Kind.CLOSE_PAREN);
        } else if (have(NonTerminal.DESIGNATOR)) {
            designator();
        } else if (have(NonTerminal.CALL_EXPRESSION)) {
            call_expression();
        } else if (have(NonTerminal.LITERAL)) {
            literal();
        }
    }

    // call-expression := "::" IDENTIFIER "(" expression-list ")" .
    private void call_expression() {
        expect(Token.Kind.CALL);
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.OPEN_PAREN);
        expression_list();
        expect(Token.Kind.CLOSE_PAREN);
    }

    // expression-list := [ expression0 { "," expression0 } ] .
    private void expression_list() {
        if (have(NonTerminal.EXPRESSION0)) {
            do {
                expression0();
            } while (accept(Token.Kind.COMMA));
        }
    }

    // parameter := IDENTIFIER ":" type .
    private void parameter() {
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.COLON);
        type();
    }

    // parameter-list := [ parameter { "," parameter } ] .
    private void parameter_list() {
        if (have(NonTerminal.PARAMETER)) {
            do {
                parameter();
            } while (accept(Token.Kind.COMMA));
        }
    }

    // variable-declaration := "var" IDENTIFIER ":" type ";" .
    private void variable_declaration() {
        expect(Token.Kind.VAR);
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.COLON);
        type();
        expect(Token.Kind.SEMICOLON);
    }

    // array-declaration := "array" IDENTIFIER ":" type "[" INTEGER "]" { "[" INTEGER "]" } ";" .
    private void array_declaration() {
        expect(NonTerminal.ARRAY_DECLARATION);
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.COLON);
        type();
        expect(Token.Kind.OPEN_BRACKET);
        expect(Token.Kind.INTEGER);
        expect(Token.Kind.CLOSE_BRACKET);

        while (accept(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.INTEGER);
            expect(Token.Kind.CLOSE_BRACKET);
        }
        expect(Token.Kind.SEMICOLON);
    }

    // function-definition := "func" IDENTIFIER "(" parameter-list ")" ":" type statement-block .
    private void function_definition() {
        expect(Token.Kind.FUNC);
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.OPEN_PAREN);
        parameter_list();
        expect(Token.Kind.CLOSE_PAREN);
        expect(Token.Kind.COLON);
        type();
        statement_block();
    }

    // declaration := variable-declaration | array-declaration | function-definition .
    private void declaration() {
        if (have(NonTerminal.VARIABLE_DECLARATION)) {
            variable_declaration();
        } else if (have(NonTerminal.ARRAY_DECLARATION)) {
            array_declaration();
        } else if (have(NonTerminal.FUNCTION_DEFINITION)) {
            function_definition();
        }
    }

    // declaration-list := { declaration } .
    private void declaration_list() {
        while (have(NonTerminal.DECLARATION)) {
            declaration();
        }
    }

    // assignment-statement := "let" designator "=" expression0 ";" .
    private void assignment_statement() {
        expect(Token.Kind.LET);
        designator();
        expect(Token.Kind.ASSIGN);
        expression0();
        expect(Token.Kind.SEMICOLON);
    }

    // call-statement := call-expression ";" .
    private void call_statement() {
        call_expression();
        expect(Token.Kind.SEMICOLON);
    }

    // if-statement := "if" expression0 statement-block [ "else" statement-block ] .
    private void if_statement() {
        expect(Token.Kind.IF);
        expression0();
        statement_block();
        if (accept(Token.Kind.ELSE)) {
            statement_block();
        }
    }

    // while-statement := "while" expression0 statement-block .
    private void while_statement() {
        expect(Token.Kind.WHILE);
        expression0();
        statement_block();
    }

    // return-statement := "return" expression0 ";" .
    private void return_statement() {
        expect(Token.Kind.RETURN);
        expression0();
        expect(Token.Kind.SEMICOLON);
    }

    // statement := variable-declaration
    //        | call-statement
    //        | assignment-statement
    //        | if-statement
    //        | while-statement
    //        | return-statement .
    private void statement() {
        if (have(NonTerminal.VARIABLE_DECLARATION)) {
            variable_declaration();
        } else if (have(NonTerminal.CALL_STATEMENT)) {
            call_statement();
        } else if (have(NonTerminal.ASSIGNMENT_STATEMENT)) {
            assignment_statement();
        } else if (have(NonTerminal.IF_STATEMENT)) {
            if_statement();
        } else if (have(NonTerminal.WHILE_STATEMENT)) {
            while_statement();
        } else if (have(NonTerminal.RETURN_STATEMENT)) {
            return_statement();
        }
    }

    // statement-list := { statement } .
    private void statement_list() {
        while (have(NonTerminal.STATEMENT)) {
            statement();
        }
    }

    // statement-block := "{" statement-list "}" .
    private void statement_block() {
        expect(Token.Kind.OPEN_BRACE);
        statement_list();
        expect(Token.Kind.CLOSE_BRACE);
    }

    // program := declaration-list EOF .
    private void program() {
        declaration_list();
        expect(Token.Kind.EOF);
    }
}

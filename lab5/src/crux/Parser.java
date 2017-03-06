package crux;

import ast.*;
import ast.Error;
import types.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    public static String studentName = "Darshan Parajuli";
    public static String studentID = "16602518";
    public static String uciNetID = "dparajul";

    // SymbolTable Management ==========================
    private SymbolTable currentSymbolTable;
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
        currentSymbolTable = new SymbolTable();
        insertSymbolReadInt();
        insertSymbolReadFloat();
        insertSymbolPrintBool();
        insertSymbolPrintInt();
        insertSymbolPrintFloat();
        insertSymbolPrintln();
    }

    private void insertSymbolReadInt() {
        final Symbol symbol = currentSymbolTable.insert("readInt");
        final TypeList args = new TypeList();
        symbol.setType(new FuncType(args, new IntType()));
    }

    private void insertSymbolReadFloat() {
        final Symbol symbol = currentSymbolTable.insert("readFloat");
        final TypeList args = new TypeList();
        symbol.setType(new FuncType(args, new FloatType()));
    }

    private void insertSymbolPrintBool() {
        final Symbol symbol = currentSymbolTable.insert("printBool");
        final TypeList args = new TypeList();
        args.append(new BoolType());
        symbol.setType(new FuncType(args, new VoidType()));
    }

    private void insertSymbolPrintInt() {
        final Symbol symbol = currentSymbolTable.insert("printInt");
        final TypeList args = new TypeList();
        args.append(new IntType());
        symbol.setType(new FuncType(args, new VoidType()));
    }

    private void insertSymbolPrintFloat() {
        final Symbol symbol = currentSymbolTable.insert("printFloat");
        final TypeList args = new TypeList();
        args.append(new FloatType());
        symbol.setType(new FuncType(args, new VoidType()));
    }

    private void insertSymbolPrintln() {
        final Symbol symbol = currentSymbolTable.insert("println");
        final TypeList args = new TypeList();
        symbol.setType(new FuncType(args, new VoidType()));
    }

    private void enterScope() {
        final SymbolTable newSymbolTable = new SymbolTable();
        if (currentSymbolTable != null) {
            newSymbolTable.setParent(currentSymbolTable);
            newSymbolTable.setDepth(currentSymbolTable.getDepth() + 1);
        }
        currentSymbolTable = newSymbolTable;
    }

    private void exitScope() {
        currentSymbolTable = currentSymbolTable.getParent();
    }

    private Symbol tryResolveSymbol(Token ident) {
        assert (ident.is(Token.Kind.IDENTIFIER));
        String name = ident.lexeme();
        try {
            return currentSymbolTable.lookup(name);
        } catch (SymbolNotFoundError e) {
            String message = reportResolveSymbolError(name, ident.lineNumber(), ident.charPosition());
            return new ErrorSymbol(message);
        }
    }

    private String reportResolveSymbolError(String name, int lineNum, int charPos) {
        String message = "ResolveSymbolError(" + lineNum + "," + charPos + ")[Could not find " + name + ".]";
        errorBuffer.append(message)
                .append("\n");
        errorBuffer.append(currentSymbolTable.toString())
                .append("\n");
        return message;
    }

    private Symbol tryDeclareSymbol(Token ident) {
        assert (ident.is(Token.Kind.IDENTIFIER));
        String name = ident.lexeme();
        try {
            return currentSymbolTable.insert(name);
        } catch (RedeclarationError re) {
            String message = reportDeclareSymbolError(name, ident.lineNumber(), ident.charPosition());
            return new ErrorSymbol(message);
        }
    }

    private String reportDeclareSymbolError(String name, int lineNum, int charPos) {
        String message = "DeclareSymbolError(" + lineNum + "," + charPos + ")[" + name + " already exists.]";
        errorBuffer.append(message)
                .append("\n");
        errorBuffer.append(currentSymbolTable.toString())
                .append("\n");
        return message;
    }

    // Typing System ===================================

    private Type tryResolveType(String typeStr) {
        return Type.getBaseType(typeStr);
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

    public Command parse() {
        initSymbolTable();
        try {
            return program();
        } catch (QuitParseException q) {
            return new Error(lineNumber(), charPosition(), "Could not complete parsing.");
        }
    }

    /********************************************** Grammar Rules ****************************************************/

    // literal :1= INTEGER | FLOAT | TRUE | FALSE .
    private Expression literal() {
        final Token token = expectRetrieve(NonTerminal.LITERAL);
        return Command.newLiteral(token);
    }

    // designator := IDENTIFIER { "[" expression0 "]" } .
    private Expression designator() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        final Symbol symbol = tryResolveSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        Expression base = new AddressOf(linNum, charPos, symbol);

        while (accept(Token.Kind.OPEN_BRACKET)) {
            final int ln = lineNumber();
            final int cp = charPosition();
            final Expression exp0 = expression0();
            base = new Index(ln, cp, base, exp0);
            expect(Token.Kind.CLOSE_BRACKET);
        }

        return base;
    }

    // type := IDENTIFIER .
    private Type type() {
        return tryResolveType(expectRetrieve(Token.Kind.IDENTIFIER).lexeme());
    }

    // op0 := ">=" | "<=" | "!=" | "==" | ">" | "<" .
    private Token op0() {
        return expectRetrieve(NonTerminal.OP0);
    }

    // op1 := "+" | "-" | "or" .
    private Token op1() {
        return expectRetrieve(NonTerminal.OP1);
    }

    // op2 := "*" | "/" | "and" .
    private Token op2() {
        return expectRetrieve(NonTerminal.OP2);
    }

    // expression0 := expression1 [ op0 expression1 ] .
    private Expression expression0() {
        Expression left = expression1();
        if (have(NonTerminal.OP0)) {
            final Token op = op0();
            final Expression right = expression1();
            left = Command.newExpression(left, op, right);
        }
        return left;
    }

    // expression1 := expression2 { op1  expression2 } .
    private Expression expression1() {
        Expression left = expression2();
        while (have(NonTerminal.OP1)) {
            final Token op = op1();
            final Expression right = expression2();
            left = Command.newExpression(left, op, right);
        }
        return left;
    }

    // expression2 := expression3 { op2 expression3 } .
    private Expression expression2() {
        Expression left = expression3();
        while (have(NonTerminal.OP2)) {
            final Token op = op2();
            final Expression right = expression3();
            left = Command.newExpression(left, op, right);
        }
        return left;
    }

    // expression3 := "not" expression3
    //    | "(" expression0 ")"
    //    | designator
    //    | call-expression
    //    | literal .
    private Expression expression3() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        final Token notToken = currentToken;
        if (accept(Token.Kind.NOT)) {
            return Command.newExpression(expression3(), notToken, null);
        } else if (accept(Token.Kind.OPEN_PAREN)) {
            final Expression exp0 = expression0();
            expect(Token.Kind.CLOSE_PAREN);
            return exp0;
        } else if (have(NonTerminal.DESIGNATOR)) {
            return new Dereference(linNum, charPos, designator());
        } else if (have(NonTerminal.CALL_EXPRESSION)) {
            return call_expression();
        } else if (have(NonTerminal.LITERAL)) {
            return literal();
        } else {
            return new Error(linNum, charPos, "invalid expression");
        }
    }

    // call-expression := "::" IDENTIFIER "(" expression-list ")" .
    private Call call_expression() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        expect(Token.Kind.CALL);
        final Symbol symbol = tryResolveSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.OPEN_PAREN);
        final ExpressionList expressionList = expression_list();
        expect(Token.Kind.CLOSE_PAREN);

        return new Call(linNum, charPos, symbol, expressionList);
    }

    // expression-list := [ expression0 { "," expression0 } ] .
    private ExpressionList expression_list() {
        final ExpressionList expressionList = new ExpressionList(lineNumber(), charPosition());
        if (have(NonTerminal.EXPRESSION0)) {
            do {
                expressionList.add(expression0());
            } while (accept(Token.Kind.COMMA));
        }
        return expressionList;
    }

    // parameter := IDENTIFIER ":" type .
    private Symbol parameter() {
        final Symbol symbol = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.COLON);
        symbol.setType(type());
        return symbol;
    }

    // parameter-list := [ parameter { "," parameter } ] .
    private List<Symbol> parameter_list() {
        final List<Symbol> symbols = new ArrayList<>();
        if (have(NonTerminal.PARAMETER)) {
            do {
                symbols.add(parameter());
            } while (accept(Token.Kind.COMMA));
        }
        return symbols;
    }

    // variable-declaration := "var" IDENTIFIER ":" type ";" .
    private VariableDeclaration variable_declaration() {
        final int lineNum = lineNumber();
        final int charPos = charPosition();

        expect(Token.Kind.VAR);

        final Symbol symbol = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));

        expect(Token.Kind.COLON);
        symbol.setType(type());
        expect(Token.Kind.SEMICOLON);

        return new VariableDeclaration(lineNum, charPos, symbol);
    }

    // array-declaration := "array" IDENTIFIER ":" type "[" INTEGER "]" { "[" INTEGER "]" } ";" .
    private ArrayDeclaration array_declaration() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        expect(NonTerminal.ARRAY_DECLARATION);
        final Symbol symbol = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.COLON);
        Type baseType = type();
        expect(Token.Kind.OPEN_BRACKET);
        final Token token = expectRetrieve(Token.Kind.INTEGER);
        baseType = new ArrayType(Integer.parseInt(token.lexeme()), baseType);
        expect(Token.Kind.CLOSE_BRACKET);

        for (int i = 0; accept(Token.Kind.OPEN_BRACKET); i++) {
            final Token t = expectRetrieve(Token.Kind.INTEGER);
            baseType = new ArrayType(Integer.parseInt(t.lexeme()), baseType);
            expect(Token.Kind.CLOSE_BRACKET);
        }
        expect(Token.Kind.SEMICOLON);

        symbol.setType(baseType);
        return new ArrayDeclaration(linNum, charPos, symbol);
    }

    // function-definition := "func" IDENTIFIER "(" parameter-list ")" ":" type statement-block .
    private FunctionDefinition function_definition() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        expect(Token.Kind.FUNC);
        final Symbol symbolFunc = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.OPEN_PAREN);
        enterScope();
        final List<Symbol> parameters = parameter_list();
        expect(Token.Kind.CLOSE_PAREN);
        expect(Token.Kind.COLON);

        final TypeList args = new TypeList();
        for (final Symbol s : parameters) {
            args.append(s.type());
        }
        symbolFunc.setType(new FuncType(args, type()));
        final StatementList statements = statement_block();
        exitScope();

        return new FunctionDefinition(linNum, charPos, symbolFunc, parameters, statements);
    }

    // declaration := variable-declaration | array-declaration | function-definition .
    private Declaration declaration() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        if (have(NonTerminal.VARIABLE_DECLARATION)) {
            return variable_declaration();
        } else if (have(NonTerminal.ARRAY_DECLARATION)) {
            return array_declaration();
        } else if (have(NonTerminal.FUNCTION_DEFINITION)) {
            return function_definition();
        }
        return new Error(linNum, charPos, "invalid declaration");
    }

    // declaration-list := { declaration } .
    private DeclarationList declaration_list() {
        final DeclarationList declarationList = new DeclarationList(lineNumber(), charPosition());
        while (have(NonTerminal.DECLARATION)) {
            declarationList.add(declaration());
        }
        return declarationList;
    }

    // assignment-statement := "let" designator "=" expression0 ";" .
    private Assignment assignment_statement() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        expect(Token.Kind.LET);
        final Expression dest = designator();
        expect(Token.Kind.ASSIGN);
        final Expression src = expression0();
        expect(Token.Kind.SEMICOLON);
        return new Assignment(linNum, charPos, dest, src);
    }

    // call-statement := call-expression ";" .
    private Call call_statement() {
        final Call callStatement = call_expression();
        expect(Token.Kind.SEMICOLON);
        return callStatement;
    }

    // if-statement := "if" expression0 statement-block [ "else" statement-block ] .
    private IfElseBranch if_statement() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        expect(Token.Kind.IF);
        final Expression cond = expression0();
        enterScope();
        final StatementList thenBlock = statement_block();
        final StatementList elseBlock;
        exitScope();
        if (accept(Token.Kind.ELSE)) {
            enterScope();
            elseBlock = statement_block();
            exitScope();
        } else {
            elseBlock = new StatementList(lineNumber(), charPosition());
        }
        return new IfElseBranch(linNum, charPos, cond, thenBlock, elseBlock);
    }

    // while-statement := "while" expression0 statement-block .
    private WhileLoop while_statement() {
        final int linNum = lineNumber();
        final int charPos = charPosition();

        expect(Token.Kind.WHILE);
        final Expression cond = expression0();
        enterScope();
        final StatementList body = statement_block();
        exitScope();

        return new WhileLoop(linNum, charPos, cond, body);
    }

    // return-statement := "return" expression0 ";" .
    private Return return_statement() {
        final int linNum = lineNumber();
        final int charPos = charPosition();
        expect(Token.Kind.RETURN);
        final Expression arg = expression0();
        expect(Token.Kind.SEMICOLON);
        return new Return(linNum, charPos, arg);
    }

    // statement := variable-declaration
    //        | array-declaration
    //        | call-statement
    //        | assignment-statement
    //        | if-statement
    //        | while-statement
    //        | return-statement .
    private Statement statement() {
        final int linNum = lineNumber();
        final int charPos = charPosition();
        if (have(NonTerminal.VARIABLE_DECLARATION)) {
            return variable_declaration();
        } else if (have(NonTerminal.ARRAY_DECLARATION)) {
            return array_declaration();
        } else if (have(NonTerminal.CALL_STATEMENT)) {
            return call_statement();
        } else if (have(NonTerminal.ASSIGNMENT_STATEMENT)) {
            return assignment_statement();
        } else if (have(NonTerminal.IF_STATEMENT)) {
            return if_statement();
        } else if (have(NonTerminal.WHILE_STATEMENT)) {
            return while_statement();
        } else if (have(NonTerminal.RETURN_STATEMENT)) {
            return return_statement();
        } else {
            return new Error(linNum, charPos, "invalid statement");
        }
    }

    // statement-list := { statement } .
    private StatementList statement_list() {
        final StatementList statements = new StatementList(lineNumber(), charPosition());
        while (have(NonTerminal.STATEMENT)) {
            statements.add(statement());
        }
        return statements;
    }

    // statement-block := "{" statement-list "}" .
    private StatementList statement_block() {
        expect(Token.Kind.OPEN_BRACE);
        final StatementList statements = statement_list();
        expect(Token.Kind.CLOSE_BRACE);
        return statements;
    }

    // program := declaration-list EOF .
    private DeclarationList program() {
        final DeclarationList declarationList = declaration_list();
        expect(Token.Kind.EOF);
        return declarationList;
    }

}

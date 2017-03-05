package types;

import ast.*;

import java.util.HashMap;

public class TypeChecker implements CommandVisitor {

    private HashMap<Visitable, Type> typeMap;
    private StringBuffer errorBuffer;

    /* Useful error strings:
     *
     * "Function " + func.name() + " has a void argument in position " + pos + "."
     * "Function " + func.name() + " has an error in argument in position " + pos + ": " + error.getMessage()
     *
     * "Function main has invalid signature."
     *
     * "Not all paths in function " + currentFunctionName + " have a return."
     *
     * "IfElseBranch requires bool condition not " + condType + "."
     * "WhileLoop requires bool condition not " + condType + "."
     *
     * "Function " + currentFunctionName + " returns " + currentReturnType + " not " + retType + "."
     *
     * "Variable " + varName + " has invalid type " + varType + "."
     * "Array " + arrayName + " has invalid base type " + baseType + "."
     */

    public TypeChecker() {
        typeMap = new HashMap<>();
        errorBuffer = new StringBuffer();
    }

    private void reportError(int lineNum, int charPos, String message) {
        errorBuffer.append("TypeError(")
                .append(lineNum)
                .append(",")
                .append(charPos)
                .append(")");
        errorBuffer.append("[")
                .append(message)
                .append("]")
                .append("\n");
    }

    private void put(Command node, Type type) {
        if (type instanceof ErrorType) {
            reportError(node.lineNumber(), node.charPosition(), ((ErrorType) type).getMessage());
        }
        typeMap.put(node, type);
    }

    private Type getType(Visitable node) {
        return typeMap.get(node);
    }

    public boolean check(Command ast) {
        ast.accept(this);
        return !hasError();
    }

    public boolean hasError() {
        return errorBuffer.length() != 0;
    }

    public String errorReport() {
        return errorBuffer.toString();
    }

    @Override
    public void visit(ExpressionList node) {
        final TypeList typeList = new TypeList();
        for (Expression expression : node) {
            expression.accept(this);
            typeList.add(getType(expression));
        }
        put(node, typeList);
    }

    @Override
    public void visit(DeclarationList node) {
        final TypeList typeList = new TypeList();
        for (Declaration declaration : node) {
            declaration.accept(this);
            typeList.add(getType(declaration));
        }
        put(node, typeList);
    }

    @Override
    public void visit(StatementList node) {
        for (Statement statement : node) {
            statement.accept(this);
        }
    }

    @Override
    public void visit(AddressOf node) {
        put(node, new AddressType(node.symbol().type()));
    }

    @Override
    public void visit(LiteralBool node) {
        put(node, new BoolType());
    }

    @Override
    public void visit(LiteralFloat node) {
        put(node, new FloatType());
    }

    @Override
    public void visit(LiteralInt node) {
        put(node, new IntType());
    }

    @Override
    public void visit(VariableDeclaration node) {
        put(node, node.symbol().type());
    }

    @Override
    public void visit(ArrayDeclaration node) {
        Type base = node.symbol().type();
        while (base.deref() != null) {
            base = base.deref();
        }

        final Type type = node.symbol().type();
        if (type instanceof ArrayType) {
            final ArrayType arrayType = (ArrayType) type;
            put(node, new ArrayType(arrayType.extent(), arrayType));
        } else {
            put(node, new ErrorType("Variable " + node.symbol().name() + " has invalid type " +
                    node.symbol().type() + "."));
        }
    }

    @Override
    public void visit(FunctionDefinition node) {
        final FuncType funcType = (FuncType) node.function().type();

        node.body().accept(this);
        final Type type = getType(node.body());
        if (!funcType.returnType().equivalent(type)) {
            put(node, new ErrorType("Function main has invalid signature."));
        }
    }

    @Override
    public void visit(Comparison node) {
        node.leftSide().accept(this);
        final Type left = getType(node.leftSide());
        node.rightSide().accept(this);
        final Type right = getType(node.rightSide());
        put(node, left.compare(right));
    }

    @Override
    public void visit(Addition node) {
        node.leftSide().accept(this);
        final Type left = getType(node.leftSide());
        node.rightSide().accept(this);
        final Type right = getType(node.rightSide());
        put(node, left.add(right));
    }

    @Override
    public void visit(Subtraction node) {
        node.leftSide().accept(this);
        final Type left = getType(node.leftSide());
        node.rightSide().accept(this);
        final Type right = getType(node.rightSide());
        put(node, left.sub(right));
    }

    @Override
    public void visit(Multiplication node) {
        node.leftSide().accept(this);
        final Type left = getType(node.leftSide());
        node.rightSide().accept(this);
        final Type right = getType(node.rightSide());
        put(node, left.mul(right));
    }

    @Override
    public void visit(Division node) {
        node.leftSide().accept(this);
        final Type left = getType(node.leftSide());
        node.rightSide().accept(this);
        final Type right = getType(node.rightSide());
        put(node, left.div(right));
    }

    @Override
    public void visit(LogicalAnd node) {
        node.leftSide().accept(this);
        final Type left = getType(node.leftSide());
        node.rightSide().accept(this);
        final Type right = getType(node.rightSide());
        put(node, left.and(right));
    }

    @Override
    public void visit(LogicalOr node) {
        node.leftSide().accept(this);
        final Type left = getType(node.leftSide());
        node.rightSide().accept(this);
        final Type right = getType(node.rightSide());
        put(node, left.or(right));
    }

    @Override
    public void visit(LogicalNot node) {
        node.expression().accept(this);
        put(node, getType(node.expression()).not());
    }

    @Override
    public void visit(Dereference node) {
        node.expression().accept(this);
        put(node, new AddressType(getType(node.expression())));
    }

    @Override
    public void visit(Index node) {
        node.base().accept(this);
        final Type baseType = getType(node.base());
        node.amount().accept(this);
        final Type amountType = getType(node.amount());

        put(node, amountType.index(baseType));
    }

    @Override
    public void visit(Assignment node) {
        node.destination().accept(this);
        final Type destType = getType(node.destination());
        node.source().accept(this);
        final Type srcType = getType(node.source());
        put(node, destType.assign(srcType));
    }

    @Override
    public void visit(Call node) {
        node.arguments().accept(this);
        put(node, getType(node.arguments()));
    }

    @Override
    public void visit(IfElseBranch node) {
        node.condition().accept(this);
        node.elseBlock().accept(this);
        node.thenBlock().accept(this);
    }

    @Override
    public void visit(WhileLoop node) {
        node.condition().accept(this);
        node.body().accept(this);
    }

    @Override
    public void visit(Return node) {
        node.argument().accept(this);
        put(node, getType(node.argument()));
    }

    @Override
    public void visit(ast.Error node) {
        put(node, new ErrorType(node.message()));
    }
}

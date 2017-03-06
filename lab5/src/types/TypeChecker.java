package types;

import ast.*;
import crux.Symbol;

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

    private Symbol lastFuncSymbol;

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
            typeList.append(getType(expression));
        }
        put(node, typeList);
    }

    @Override
    public void visit(DeclarationList node) {
        final TypeList typeList = new TypeList();
        for (Declaration declaration : node) {
            declaration.accept(this);
            typeList.append(getType(declaration));
        }
        put(node, typeList);
    }

    @Override
    public void visit(StatementList node) {
        final TypeList typeList = new TypeList();
        for (Statement statement : node) {
            statement.accept(this);
            typeList.append(getType(statement));
        }
        put(node, typeList);
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
        final Type type = node.symbol().type();
        if (type instanceof IntType || type instanceof FloatType || type instanceof BoolType) {
            put(node, node.symbol().type());
        } else {
            put(node, new ErrorType("Variable " + node.symbol().name() + " has invalid type " + type + "."));
        }
    }

    @Override
    public void visit(ArrayDeclaration node) {
        Type baseType = node.symbol().type();
        while (baseType instanceof ArrayType) {
            baseType = ((ArrayType) baseType).base();
        }
        if (!(baseType instanceof IntType || baseType instanceof FloatType || baseType instanceof BoolType)) {
            put(node, new ErrorType("Array " + node.symbol().name() + " has invalid base type " + baseType + "."));
        }
    }

    @Override
    public void visit(FunctionDefinition node) {
        lastFuncSymbol = node.function();

        final FuncType funcType = (FuncType) node.function().type();
        final Type returnType = funcType.returnType();
        final TypeList argsType = funcType.arguments();

        if (node.function().name().equals("main")) {
            if (!(returnType instanceof VoidType) || argsType.count() > 0) {
                put(node, new ErrorType("Function main has invalid signature."));
                return;
            }
        } else {
            for (Type t : argsType) {
                if (t instanceof VoidType) {
                    put(node, new ErrorType("Function " + node.function().name()
                            + " has a void argument in position " +
                            (node.charPosition() - 1) + "."));
                    return;
                } else if (t instanceof ErrorType) {
                    put(node, new ErrorType("Function " + node.function().name()
                            + " has an error in argument in position" + " " +
                            (node.charPosition() - 1) + ": " + ((ErrorType) t).getMessage()));
                    return;
                }
            }
        }

        node.body().accept(this);

        if (!(returnType instanceof VoidType)) {
            final TypeList paths = (TypeList) getType(node.body());
            boolean missingArgs = true;
            for (Type t : paths) {
                if (returnType.equivalent(t)) {
                    missingArgs = false;
                    break;
                } else if (t instanceof ErrorType) {
                    missingArgs = false;
                    break;
                }
            }
            if (missingArgs) {
                put(node, new ErrorType("Not all paths in function " + node.function().name() + " have a return."));
            }
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
        final Type type = getType(node.expression());
        put(node, type.deref());
    }

    @Override
    public void visit(Index node) {
        node.base().accept(this);
        final Type baseType = getType(node.base());
        node.amount().accept(this);
        final Type amountType = getType(node.amount());

        final Type type = baseType.index(amountType);
        put(node, type);
    }

    @Override
    public void visit(Assignment node) {
        node.destination().accept(this);
        final Type destType = getType(node.destination());
        node.source().accept(this);
        final Type srcType = getType(node.source());
        final Type assignType = destType.assign(srcType);
        put(node, assignType);
    }

    @Override
    public void visit(Call node) {
        node.arguments().accept(this);
        final FuncType funcType = (FuncType) node.function().type();
        final Type argType = getType(node.arguments());
        put(node, funcType.call(argType));
    }

    @Override
    public void visit(IfElseBranch node) {
        node.condition().accept(this);
        final Type condType = getType(node.condition());
        if (condType instanceof BoolType) {
            node.thenBlock().accept(this);
            node.elseBlock().accept(this);
        } else {
            put(node, new ErrorType("IfElseBranch requires bool condition not " + condType + "."));
        }
    }

    @Override
    public void visit(WhileLoop node) {
        node.condition().accept(this);
        final Type type = getType(node.condition());
        if (!(type instanceof BoolType)) {
            put(node, new ErrorType("WhileLoop requires bool condition not " + type + "."));
        }
        node.body().accept(this);
    }

    @Override
    public void visit(Return node) {
        node.argument().accept(this);
        final Type type = getType(node.argument());

        final FuncType funcType = (FuncType) lastFuncSymbol.type();
        if (!lastFuncSymbol.name().equals("main") && !type.equivalent(funcType.returnType())) {
            put(node, new ErrorType("Function " + lastFuncSymbol.name()
                    + " returns " + funcType.returnType()
                    + " not " + "" + type + "."));
        } else {
            put(node, type);
        }
    }

    @Override
    public void visit(ast.Error node) {
        put(node, new ErrorType(node.message()));
    }
}

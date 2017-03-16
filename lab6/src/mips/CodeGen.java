package mips;

import ast.*;
import ast.Error;
import types.TypeChecker;

public class CodeGen implements CommandVisitor {

    private StringBuffer errorBuffer = new StringBuffer();
    private TypeChecker tc;
    private Program program;
    private ActivationRecord currentFunction;

    public CodeGen(TypeChecker tc) {
        this.tc = tc;
        this.program = new Program();
    }

    public boolean hasError() {
        return errorBuffer.length() != 0;
    }

    public String errorReport() {
        return errorBuffer.toString();
    }

    private class CodeGenException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CodeGenException(String errorMessage) {
            super(errorMessage);
        }
    }

    public boolean generate(Command ast) {
        try {
            currentFunction = ActivationRecord.newGlobalFrame();
            ast.accept(this);
            return !hasError();
        } catch (CodeGenException e) {
            return false;
        }
    }

    public Program getProgram() {
        return program;
    }

    @Override
    public void visit(ExpressionList node) {
        System.out.println(node);
        for (Expression expression : node) {
            expression.accept(this);
        }
    }

    @Override
    public void visit(DeclarationList node) {
        System.out.println(node);
        for (Declaration declaration : node) {
            declaration.accept(this);
        }
    }

    @Override
    public void visit(StatementList node) {
        System.out.println(node);
        for (Statement statement : node) {
            statement.accept(this);
        }

    }

    @Override
    public void visit(AddressOf node) {
        System.out.println(node);
    }

    @Override
    public void visit(LiteralBool node) {
        System.out.println(node);
        final int val = node.value() == LiteralBool.Value.FALSE ? 0 : 1;
        program.appendInstruction("li $t0, " + val);
        program.pushInt("$t0");
    }

    @Override
    public void visit(LiteralFloat node) {
        System.out.println(node);
        program.appendInstruction("li $f0, " + node.value());
        program.pushFloat("$f0");
    }

    @Override
    public void visit(LiteralInt node) {
        System.out.println(node);
        program.appendInstruction("li $t0, " + node.value());
        program.pushInt("$t0");
    }

    @Override
    public void visit(VariableDeclaration node) {
        System.out.println(node);
        currentFunction.add(program, node);
    }

    @Override
    public void visit(ArrayDeclaration node) {
        System.out.println(node);
        currentFunction.add(program, node);
    }

    @Override
    public void visit(FunctionDefinition node) {
        System.out.println(node);
        final String name = node.function().name();
        currentFunction = new ActivationRecord(node, currentFunction);
        final int pos = program.appendInstruction(program.newFuncLabel(name) + ":");
        node.body().accept(this);
        program.insertPrologue(pos, currentFunction.stackSize());
        if (name.equals("main")) {
            program.appendExitSequence();
        } else {
            program.appendEpilogue(currentFunction.stackSize());
        }
        if (currentFunction.parent() != null) {
            currentFunction = currentFunction.parent();
        }
    }

    @Override
    public void visit(Addition node) {
        System.out.println(node);
        node.leftSide().accept(this);
        node.rightSide().accept(this);
    }

    @Override
    public void visit(Subtraction node) {
        System.out.println(node);
        node.leftSide().accept(this);
        node.rightSide().accept(this);
    }

    @Override
    public void visit(Multiplication node) {
        System.out.println(node);
        node.leftSide().accept(this);
        node.rightSide().accept(this);
    }

    @Override
    public void visit(Division node) {
        System.out.println(node);
        node.leftSide().accept(this);
        node.rightSide().accept(this);
    }

    @Override
    public void visit(LogicalAnd node) {
        System.out.println(node);
        node.leftSide().accept(this);
        node.rightSide().accept(this);
    }

    @Override
    public void visit(LogicalOr node) {
        System.out.println(node);
        node.leftSide().accept(this);
        node.rightSide().accept(this);
    }

    @Override
    public void visit(LogicalNot node) {
        System.out.println(node);
        node.expression().accept(this);
    }

    @Override
    public void visit(Comparison node) {
        System.out.println(node);
        node.leftSide().accept(this);
        node.rightSide().accept(this);
    }

    @Override
    public void visit(Dereference node) {
        System.out.println(node);
        node.expression().accept(this);
    }

    @Override
    public void visit(Index node) {
        System.out.println(node);
        node.base().accept(this);
        node.amount().accept(this);
    }

    @Override
    public void visit(Assignment node) {
        System.out.println(node);
        node.destination().accept(this);
        node.source().accept(this);
    }

    @Override
    public void visit(Call node) {
        System.out.println(node);
        node.arguments().accept(this);
        program.appendInstruction("jal " + program.newFuncLabel(node.function().name()));
    }

    @Override
    public void visit(IfElseBranch node) {
        System.out.println(node);
        node.condition().accept(this);
        node.thenBlock().accept(this);
        node.elseBlock().accept(this);
    }

    @Override
    public void visit(WhileLoop node) {
        System.out.println(node);
        node.condition().accept(this);
        node.body().accept(this);
    }

    @Override
    public void visit(Return node) {
        System.out.println(node);
        node.argument().accept(this);
    }

    @Override
    public void visit(Error node) {
        System.out.println(node);
        String message = "CodeGen cannot compile a " + node.toString();
        errorBuffer.append(message);
        throw new CodeGenException(message);
    }
}

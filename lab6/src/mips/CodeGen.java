package mips;

import ast.*;
import ast.Error;
import types.*;

public class CodeGen implements CommandVisitor {

    private StringBuffer errorBuffer = new StringBuffer();
    private TypeChecker tc;
    private Program program;
    private ActivationRecord currentActivationRecord;
    private String currentFunctionName;

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

    private String getFuncEpilogueLabel(String name) {
        return program.newFuncLabel(name) + ".epilogue";
    }

    public boolean generate(Command ast) {
        try {
            currentActivationRecord = ActivationRecord.newGlobalFrame();
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
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        for (Expression expression : node) {
            expression.accept(this);
        }
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(DeclarationList node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        for (Declaration declaration : node) {
            declaration.accept(this);
        }
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(StatementList node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        for (Statement statement : node) {
            statement.accept(this);
        }
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(AddressOf node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        currentActivationRecord.getAddress(program, "$t1", node.symbol());
        program.pushInt("$t1");
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LiteralBool node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        final int val = node.value() == LiteralBool.Value.FALSE ? 0 : 1;
        program.appendInstruction("li $t1, " + val);
        program.pushInt("$t1");
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LiteralFloat node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        program.appendInstruction("li.s $f0, " + node.value());
        program.pushFloat("$f0");
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LiteralInt node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        program.appendInstruction("li $t1, " + node.value());
        program.pushInt("$t1");
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(VariableDeclaration node) {
        currentActivationRecord.add(program, node);
    }

    @Override
    public void visit(ArrayDeclaration node) {
        currentActivationRecord.add(program, node);
    }

    @Override
    public void visit(FunctionDefinition node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        currentFunctionName = node.function().name();
        currentActivationRecord = new ActivationRecord(node, currentActivationRecord);
        final int pos = program.appendInstruction(program.newFuncLabel(currentFunctionName) + ":");
        node.body().accept(this);
        program.insertPrologue(pos + 1, currentActivationRecord.stackSize());
        program.appendInstruction(getFuncEpilogueLabel(currentFunctionName) + ":");
        program.appendEpilogue(currentActivationRecord.stackSize());
        currentActivationRecord = currentActivationRecord.parent();
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Addition node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Subtraction node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Multiplication node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Division node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LogicalAnd node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LogicalOr node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LogicalNot node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.expression().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Comparison node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Dereference node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.expression().accept(this);
        program.popInt("$t1");
        program.appendInstruction("lw $t1, 0($t1)");
        program.pushInt("$t1");
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Index node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.base().accept(this);
        node.amount().accept(this);

        final AddressType baseType = (AddressType) tc.getType(node);

        program.popInt("$t2"); //
        program.popInt("$t1");
        program.appendInstruction("li $t3, " + ActivationRecord.numBytes(baseType.base()));
        program.appendInstruction("mul $t2, $t2, $t3");
        program.appendInstruction("add $t1, $t1, $t2");
        program.pushInt("$t1");

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Assignment node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));

        node.destination().accept(this);
        node.source().accept(this);

        program.appendInstruction("lw $t1, 0($sp)");
        program.appendInstruction("addi $sp, $sp, 4");
        program.appendInstruction("lw $t2, 0($sp)");
        program.appendInstruction("addi $sp, $sp, 4");

        program.appendInstruction("sw $t1, 0($t2)");

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Call node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.arguments().accept(this);

        final FuncType funcType = (FuncType) node.function().type();
        final TypeList argType = (TypeList) tc.getType(node.arguments());

        program.appendInstruction("jal " + program.newFuncLabel(node.function().name()));
        program.appendInstruction("addi $sp, $sp, " + node.arguments().size() * 4);

        final Type ret = funcType.call(argType);
        if (!(ret instanceof VoidType)) {
            program.appendInstruction("subu $sp, $sp, 4");
            program.appendInstruction("sw $v0, 0($sp)");
        }

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(IfElseBranch node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.condition().accept(this);
        node.thenBlock().accept(this);
        node.elseBlock().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(WhileLoop node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.condition().accept(this);
        node.body().accept(this);
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Return node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.argument().accept(this);
        program.popInt("$v0");
        program.appendInstruction("j " + getFuncEpilogueLabel(currentFunctionName));
        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Error node) {
        String message = "CodeGen cannot compile a " + node.toString();
        errorBuffer.append(message);
        throw new CodeGenException(message);
    }
}

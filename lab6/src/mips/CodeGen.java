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
            if (statement instanceof Call) {
                final Type type = tc.getType(statement);
                if (!(type instanceof VoidType)) {
                    if (type instanceof FloatType) {
                        program.popFloat("$f0");
                    } else {
                        program.popInt("$t1");
                    }
                }
            }
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

        final Type type = tc.getType(node);
        if (type instanceof FloatType) {
            program.popFloat("$f1");
            program.popFloat("$f0");
            program.appendInstruction("add.s $f0, $f0, $f1");
            program.pushFloat("$f0");
        } else {
            program.popInt("$t1");
            program.popInt("$t0");
            program.appendInstruction("add $t0, $t0, $t1");
            program.pushInt("$t0");
        }

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Subtraction node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        final Type type = tc.getType(node);
        if (type instanceof FloatType) {
            program.popFloat("$f1");
            program.popFloat("$f0");
            program.appendInstruction("sub.s $f0, $f0, $f1");
            program.pushFloat("$f0");
        } else {
            program.popInt("$t1");
            program.popInt("$t0");
            program.appendInstruction("sub $t0, $t0, $t1");
            program.pushInt("$t0");
        }

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Multiplication node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        final Type type = tc.getType(node);
        if (type instanceof FloatType) {
            program.popFloat("$f1");
            program.popFloat("$f0");
            program.appendInstruction("mul.s $f0, $f0, $f1");
            program.pushFloat("$f0");
        } else {
            program.popInt("$t1");
            program.popInt("$t0");
            program.appendInstruction("mul $t0, $t0, $t1");
            program.pushInt("$t0");
        }

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Division node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        final Type type = tc.getType(node);
        if (type instanceof FloatType) {
            program.popFloat("$f1");
            program.popFloat("$f0");
            program.appendInstruction("div.s $f0, $f0, $f1");
            program.pushFloat("$f0");
        } else {
            program.popInt("$t1");
            program.popInt("$t0");
            program.appendInstruction("div $t0, $t0, $t1");
            program.pushInt("$t0");
        }

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LogicalAnd node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        program.popInt("$t1");
        program.popInt("$t0");
        program.appendInstruction("and $t0, $t0, $t1");
        program.pushInt("$t0");

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LogicalOr node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        program.popInt("$t1");
        program.popInt("$t0");
        program.appendInstruction("or $t0, $t0, $t1");
        program.pushInt("$t0");

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(LogicalNot node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.expression().accept(this);

        program.popInt("$t0");
        program.appendInstruction("nor $t0, $t0, $0");
        program.pushInt("$t0");

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Comparison node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        final Type type = tc.getType(node);
        if (type instanceof FloatType) {
            program.popFloat("$f1");
            program.popFloat("$f0");
        } else {
            program.popInt("$t1");
            program.popInt("$t0");
        }

        final String labelTrue = program.newLabel();
        final String labelFalse = program.newLabel();
        final String labelExit = program.newLabel();

        switch (node.operation()) {
            case EQ: {
                if (type instanceof FloatType) {
                    program.appendInstruction("c.eq.s, $f0, $f1");
                    program.appendInstruction("bc1t " + labelTrue);
                } else {
                    program.appendInstruction("seq, $t0, $t0, $t1");
                }
            }
            break;
            case GE: {
                if (type instanceof FloatType) {
                    program.appendInstruction("c.ge.s, $f0, $f1");
                    program.appendInstruction("bc1t " + labelTrue);
                } else {
                    program.appendInstruction("sge, $t0, $t0, $t1");
                }
            }
            break;
            case LE: {
                if (type instanceof FloatType) {
                    program.appendInstruction("c.le.s, $f0, $f1");
                    program.appendInstruction("bc1t " + labelTrue);
                } else {
                    program.appendInstruction("sle, $t0, $t0, $t1");
                }
            }
            break;
            case GT: {
                if (type instanceof FloatType) {
                    program.appendInstruction("c.gt.s, $f0, $f1");
                    program.appendInstruction("bc1t " + labelTrue);
                } else {
                    program.appendInstruction("sgt, $t0, $t0, $t1");
                }
            }
            break;
            case LT: {
                if (type instanceof FloatType) {
                    program.appendInstruction("c.lt.s, $f0, $f1");
                    program.appendInstruction("bc1t " + labelTrue);
                } else {
                    program.appendInstruction("slt, $t0, $t0, $t1");
                }
            }
            break;
        }

        if (type instanceof FloatType) {
            program.appendInstruction(labelTrue + ":");
            program.appendInstruction("addi $t0, $0, 1");
            program.appendInstruction("j " + labelExit);
            program.appendInstruction(labelFalse + ":");
            program.appendInstruction("addi $t0, $0, 0");
            program.appendInstruction(labelExit + ":");
        }
        program.pushInt("$t0");

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

        final Type type = tc.getType(node);
        if (type instanceof FloatType) {
            program.popFloat("$f1");
            program.popFloat("$f2");
            program.appendInstruction("s.s $f1, 0($f2)");
        } else {
            program.popInt("$t1");
            program.popInt("$t2");
            program.appendInstruction("sw $t1, 0($t2)");
        }

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
            if (ret instanceof FloatType) {
                program.pushFloat("$v0");
            } else {
                program.pushInt("$v0");
            }
        }

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(IfElseBranch node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));

        final String ifThenLabel = program.newLabel();
        final String elseLabel = program.newLabel();
        final String exitLabel = program.newLabel();

        node.condition().accept(this);

        program.appendInstruction(ifThenLabel + ":");
        program.popInt("$t0");
        program.appendInstruction("beqz $t0, " + elseLabel);

        node.thenBlock().accept(this);

        program.appendInstruction("j " + exitLabel);

        program.appendInstruction(elseLabel + ":");
        node.elseBlock().accept(this);

        program.appendInstruction(exitLabel + ":");

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(WhileLoop node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));

        final String loop = program.newLabel();
        final String exitLoop = program.newLabel();

        program.appendInstruction(loop + ":");
        node.condition().accept(this);
        program.popInt("$t0");
        program.appendInstruction("beqz $t0, " + exitLoop);

        node.body().accept(this);
        program.appendInstruction("j " + loop);
        program.appendInstruction(exitLoop + ":");

        program.appendInstruction(String.format("%24s %s", "#end", node));
    }

    @Override
    public void visit(Return node) {
        program.appendInstruction(String.format("%24s %s", "#begin", node));
        node.argument().accept(this);
        final Type type = tc.getType(node);
        if (type instanceof FloatType) {
            program.popFloat("$v0");
        } else {
            program.popInt("$v0");
        }
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

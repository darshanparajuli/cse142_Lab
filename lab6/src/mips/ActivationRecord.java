package mips;

import ast.ArrayDeclaration;
import ast.FunctionDefinition;
import ast.VariableDeclaration;
import crux.Symbol;
import types.*;

import java.util.HashMap;

public class ActivationRecord {

    private int stackSize;
    private ActivationRecord parent;
    private FunctionDefinition func;
    private HashMap<Symbol, Integer> locals;
    private HashMap<Symbol, Integer> arguments;

    public static ActivationRecord newGlobalFrame() {
        return new GlobalFrame();
    }

    protected static int numBytes(Type type) {
        if (type instanceof BoolType)
            return 4;
        if (type instanceof IntType)
            return 4;
        if (type instanceof FloatType)
            return 4;
        if (type instanceof ArrayType) {
            final ArrayType aType = (ArrayType) type;
            return aType.extent() * numBytes(aType.base());
        }
        throw new RuntimeException("No size known for " + type);
    }

    protected ActivationRecord() {
        this.func = null;
        this.parent = null;
        this.stackSize = 0;
        this.locals = null;
        this.arguments = null;
    }

    public ActivationRecord(FunctionDefinition fd, ActivationRecord parent) {
        this.func = fd;
        this.parent = parent;
        this.stackSize = 0;
        this.locals = new HashMap<>();

        // map this function's parameters
        this.arguments = new HashMap<>();
        int offset = 0;
        for (int i = fd.arguments().size() - 1; i >= 0; --i) {
            Symbol arg = fd.arguments().get(i);
            arguments.put(arg, offset);
            offset += numBytes(arg.type());
        }
    }

    public String name() {
        return func.symbol().name();
    }

    public ActivationRecord parent() {
        return parent;
    }

    public int stackSize() {
        return stackSize;
    }

    public void add(Program prog, VariableDeclaration var) {
        final Symbol sym = var.symbol();
        final int numBytes = numBytes(sym.type());
        stackSize += numBytes;
        locals.put(sym, stackSize + 8);
    }

    public void add(Program prog, ArrayDeclaration array) {
        final Symbol sym = array.symbol();
        final int numBytes = numBytes(sym.type());
        stackSize += numBytes;
        locals.put(sym, stackSize + 8);
    }

    public void getAddress(Program prog, String reg, Symbol sym) {
        if (arguments.containsKey(sym)) {
            prog.appendInstruction("addi " + reg + ", $fp, " + arguments.get(sym));
        } else if (locals.containsKey(sym)) {
            prog.appendInstruction("addi " + reg + ", $fp, " + (-locals.get(sym)));
        } else {
            parent.getAddress(prog, reg, sym);
        }
    }
}

class GlobalFrame extends ActivationRecord {

    public GlobalFrame() {
    }

    private String mangleDataName(String name) {
        return "cruxdata." + name;
    }

    @Override
    public void add(Program prog, VariableDeclaration var) {
        final Symbol symbol = var.symbol();
        prog.appendData(mangleDataName(symbol.name()) + ": .word " + numBytes(symbol.type()));
    }

    @Override
    public void add(Program prog, ArrayDeclaration array) {
        final Symbol symbol = array.symbol();
        prog.appendData(mangleDataName(symbol.name()) + ": .space " + numBytes(symbol.type()));
    }

    @Override
    public void getAddress(Program prog, String reg, Symbol sym) {
        prog.appendInstruction("la " + reg + ", " + mangleDataName(sym.name()));
    }
}

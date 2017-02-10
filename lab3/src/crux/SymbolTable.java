package crux;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {

    private SymbolTable parent;
    private int depth;

    private Map<String, Symbol> map;

    public SymbolTable() {
        parent = null;
        depth = 0;
        map = new LinkedHashMap<>();
    }

    public Symbol lookup(String name) throws SymbolNotFoundError {
        Symbol symbol = map.get(name);
        SymbolTable myParent = parent;
        while (symbol == null && myParent != null) {
            symbol = myParent.map.get(name);
            myParent = myParent.parent;
        }

        if (symbol == null) {
            throw new SymbolNotFoundError(name);
        }

        return symbol;
    }

    public Symbol insert(String name) throws RedeclarationError {
        Symbol symbol = map.get(name);
        if (symbol == null) {
            symbol = new Symbol(name);
            map.put(name, symbol);
        } else {
            throw new RedeclarationError(symbol);
        }
        return symbol;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parent != null)
            sb.append(parent.toString());

        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }

        for (Symbol s : map.values()) {
            sb.append(indent)
                    .append(s.toString())
                    .append("\n");
        }
        return sb.toString();
    }

    static SymbolTable pushSymbolTable(SymbolTable parent) {
        final SymbolTable symbolTable = new SymbolTable();
        if (parent != null) {
            symbolTable.parent = parent;
            symbolTable.depth = parent.depth + 1;
        }
        return symbolTable;
    }

    static SymbolTable popSymbolTable(SymbolTable current) {
        return current.parent;
    }
}

class SymbolNotFoundError extends Error {

    private static final long serialVersionUID = 1L;
    private String name;

    SymbolNotFoundError(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}

class RedeclarationError extends Error {

    private static final long serialVersionUID = 1L;

    public RedeclarationError(Symbol sym) {
        super("Symbol " + sym + " being redeclared.");
    }
}

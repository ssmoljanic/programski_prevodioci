package semantic;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private static class Scope {
        final Map<String, Symbol> symbols = new HashMap<>();
        final Scope parent;
        final String name;

        Scope(Scope parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }

    private Scope currentScope;
    private final Scope globalScope;

    public SymbolTable() {
        this.globalScope = new Scope(null, "global");
        this.currentScope = globalScope;
    }

    public void enterScope(String name) {
        Scope newScope = new Scope(currentScope, name);
        currentScope = newScope;
    }

    public void exitScope() {
        if (currentScope.parent != null) {
            currentScope = currentScope.parent;
        }
    }

    public boolean define(Symbol symbol) {
        if (currentScope.symbols.containsKey(symbol.name)) {
            return false;
        }
        currentScope.symbols.put(symbol.name, symbol);
        return true;
    }

    public boolean defineGlobal(Symbol symbol) {
        if (globalScope.symbols.containsKey(symbol.name)) {
            return false;
        }
        globalScope.symbols.put(symbol.name, symbol);
        return true;
    }

    public Symbol lookup(String name) {
        Scope scope = currentScope;

        while (scope != null) {
            Symbol symbol = scope.symbols.get(name);
            if (symbol != null) {
                return symbol;
            }
            scope = scope.parent;
        }

        return null;
    }

    public Symbol lookupLocal(String name) {
        return currentScope.symbols.get(name);
    }

    public Symbol lookupGlobal(String name) {
        return globalScope.symbols.get(name);
    }

    public boolean isGlobalScope() {
        return currentScope == globalScope;
    }

    public String getCurrentScopeName() {
        return currentScope.name;
    }

    public void dump() {
        System.out.println("=== SYMBOL TABLE DUMP ===");
        dumpScope(currentScope, 0);
        System.out.println("=========================");
    }

    private void dumpScope(Scope scope, int indent) {
        if (scope == null) return;

        dumpScope(scope.parent, indent);

        String prefix = "  ".repeat(indent);
        System.out.println(prefix + "┌─ Scope: " + scope.name);
        for (Symbol sym : scope.symbols.values()) {
            System.out.println(prefix + "│  " + sym);
        }
        System.out.println(prefix + "└─────────────────────");
    }
}

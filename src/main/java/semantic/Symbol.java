package semantic;

import parser.ast.Ast;
import java.util.List;

public class Symbol {

    public enum Kind {
        VARIABLE,
        FUNCTION
    }

    public final String name;
    public final Ast.Type type;
    public final Kind kind;
    public final List<Ast.Param> params;
    public final int line;
    public final int column;

    public Symbol(String name, Ast.Type type, int line, int column) {
        this.name = name;
        this.type = type;
        this.kind = Kind.VARIABLE;
        this.params = List.of();
        this.line = line;
        this.column = column;
    }

    public Symbol(String name, Ast.Type type, List<Ast.Param> params, int line, int column) {
        this.name = name;
        this.type = type;
        this.kind = Kind.FUNCTION;
        this.params = params;
        this.line = line;
        this.column = column;
    }

    public boolean isVariable() {
        return kind == Kind.VARIABLE;
    }

    public boolean isFunction() {
        return kind == Kind.FUNCTION;
    }

    @Override
    public String toString() {
        if (kind == Kind.VARIABLE) {
            return "Variable[" + name + " : " + typeToString(type) + "]";
        } else {
            return "Function[" + name + "(" + params.size() + " params) : " + typeToString(type) + "]";
        }
    }

    private String typeToString(Ast.Type t) {
        if (t == null || t.kind == Ast.Type.Kind.VOID) {
            return "void";
        }
        String base = t.baseType != null ? t.baseType.lexeme : "?";
        if (t.kind == Ast.Type.Kind.ARRAY) {
            return "listaCekanja{" + base + "}[" + t.rank + "]";
        }
        return base;
    }
}

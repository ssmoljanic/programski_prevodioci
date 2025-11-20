package parser.ast;

import lexer.token.Token;
import java.util.List;

public final class Ast {


    public static final class Program {

        public final List<TopItem> items;

        public Program(List<TopItem> items) {
            this.items = items;
        }
    }


    public interface TopItem {}

    public static final class TopVarDecl implements TopItem {
        public final Stmt.VarDecl decl;

        public TopVarDecl(Stmt.VarDecl decl) {
            this.decl = decl;
        }
    }


    public static final class TopStmt implements TopItem {
        public final Stmt stmt;

        public TopStmt(Stmt stmt) {
            this.stmt = stmt;
        }
    }


    public static final class FuncDef implements TopItem {
        public final Token name;
        public final List<Param> params;
        public final Type returnType;
        public final List<Stmt> body;

        public FuncDef(Token name, List<Param> params, Type returnType, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.body = body;
        }
    }


    public static final class MainDef implements TopItem {
        public final List<Stmt> body;

        public MainDef(List<Stmt> body) {
            this.body = body;
        }
    }


    public static final class Param {
        public final Token name;
        public final Type type;

        public Param(Token name, Type type) {
            this.name = name;
            this.type = type;
        }
    }


    public static final class Type {


        public enum Kind { SCALAR, ARRAY, VOID }

        public final Kind kind;


        public final Token baseType;

        public final int rank;

        public Type(Kind kind, Token baseType, int rank) {
            this.kind = kind;
            this.baseType = baseType;
            this.rank = rank;
        }



        public static Type scalar(Token baseType) {
            return new Type(Kind.SCALAR, baseType, 0);
        }

        public static Type array(Token baseType, int rank) {
            return new Type(Kind.ARRAY, baseType, rank);
        }

        public static Type voidType() {
            return new Type(Kind.VOID, null, 0);
        }
    }
}

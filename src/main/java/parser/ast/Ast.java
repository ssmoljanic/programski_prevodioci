package parser.ast;

import lexer.token.Token;
import java.util.List;

public final class Ast {

    // ===== Root čvor =====
    public static final class Program {
        // lista top_item iz gramatike: var_decl | func_def | main_def | stmt
        public final List<TopItem> items;

        public Program(List<TopItem> items) {
            this.items = items;
        }
    }

    // ===== Top-level stvari =====
    public interface TopItem {}

    // globalna deklaracija: IZVOLITE type ident ... ;
    public static final class TopVarDecl implements TopItem {
        public final Stmt.VarDecl decl;

        public TopVarDecl(Stmt.VarDecl decl) {
            this.decl = decl;
        }
    }

    // top-level naredba (ako dopustiš stmt na vrhu fajla)
    public static final class TopStmt implements TopItem {
        public final Stmt stmt;

        public TopStmt(Stmt stmt) {
            this.stmt = stmt;
        }
    }

    // definicija funkcije: RECEPT type IDENT ( [params] ) block
    public static final class FuncDef implements TopItem {
        public final Token name;         // IDENT
        public final List<Param> params; // param lista
        public final Type returnType;    // povratni tip (type iz gramatike)
        public final List<Stmt> body;    // block => lista Stmt-ova

        public FuncDef(Token name, List<Param> params, Type returnType, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.body = body;
        }
    }

    // definicija glavnog programa: GLAVNIOBROK() block
    public static final class MainDef implements TopItem {
        public final List<Stmt> body; // block => lista Stmt-ova

        public MainDef(List<Stmt> body) {
            this.body = body;
        }
    }

    // ===== Parametar funkcije =====
    // param = type IDENT ;
    public static final class Param {
        public final Token name; // IDENT
        public final Type type;

        public Param(Token name, Type type) {
            this.name = name;
            this.type = type;
        }
    }

    // ===== Tip =====
    // Pokriva base_type i listaCekanja{...} sa više dimenzija.
    public static final class Type {

        // Vrsta tipa: skalar, niz, ili void (npr. za funkcije bez povratne vrednosti, ako uvedeš)
        public enum Kind { SCALAR, ARRAY, VOID }

        public final Kind kind;

        // Token za bazni tip: PORUDZBINA, RACUN, PREDJELO, USLUZENNEUSLUZEN, JELOVNIK
        public final Token baseType;

        // rank = broj dimenzija niza:
        // 0 => nije niz (samo skalar)
        // 1 => listaCekanja{T}[...]
        // 2 => listaCekanja{T}[...][...]
        public final int rank;

        public Type(Kind kind, Token baseType, int rank) {
            this.kind = kind;
            this.baseType = baseType;
            this.rank = rank;
        }

        // Pomoćni konstruktori da ti parser bude čitljiviji:

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

package parser.ast;

import lexer.token.Token;
import java.util.List;

public abstract class Stmt {

    // ============================================================
    // VISITOR
    // ============================================================
    public interface Visitor<R> {
        R visitVarDecl(VarDecl s);
        R visitExprStmt(ExprStmt s);
        R visitPrint(Print s);
        R visitRead(Read s);
        R visitIf(If s);
        R visitWhile(While s);
        R visitDoWhile(DoWhile s);
        R visitFor(For s);
        R visitSwitch(Switch s);
        R visitReturn(Return s);
        R visitBlock(Block s);
    }

    public abstract <R> R accept(Visitor<R> v);

    // ============================================================
    // 1) VAR DECLARATION
    // ============================================================
    public static final class VarDecl extends Stmt {
        public final Ast.Type type;
        public final List<Token> names;
        public final List<Expr> inits;

        public VarDecl(Ast.Type type, List<Token> names, List<Expr> inits) {
            this.type = type;
            this.names = names;
            this.inits = inits;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitVarDecl(this); }
    }

    // ============================================================
    // 2) Expression as a statement:   expr ;
    // ============================================================
    public static final class ExprStmt extends Stmt {
        public final Expr expr;

        public ExprStmt(Expr expr) {
            this.expr = expr;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitExprStmt(this); }
    }

    // ============================================================
    // 3) Print:  konobar(expr1, expr2, ...)
    // ============================================================
    public static final class Print extends Stmt {
        public final List<Expr> args;

        public Print(List<Expr> args) {
            this.args = args;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitPrint(this); }
    }

    // ============================================================
    // 4) Read:  daceteMi(x);
    // ============================================================
    public static final class Read extends Stmt {
        public final Token name; // IDENT

        public Read(Token name) {
            this.name = name;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitRead(this); }
    }

    // ============================================================
    // 5) IF / ELSEIF / ELSE
    // ============================================================
    public static final class If extends Stmt {

        public static final class Arm {
            public final Expr cond;
            public final List<Stmt> block;

            public Arm(Expr cond, List<Stmt> block) {
                this.cond = cond;
                this.block = block;
            }
        }

        public final Arm ifArm;                   // rezervisanSto(cond) { ... }
        public final List<Arm> elseIfArms;        // slobodanSto(cond) { ... }
        public final List<Stmt> elseBlock;        // jescemoNegdeDrugo { ... }   (može null)

        public If(Arm ifArm, List<Arm> elseIfArms, List<Stmt> elseBlock) {
            this.ifArm = ifArm;
            this.elseIfArms = elseIfArms;
            this.elseBlock = elseBlock;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitIf(this); }
    }

    // ============================================================
    // 6) WHILE: dokNeDobijemObrok (cond) { ... }
    // ============================================================
    public static final class While extends Stmt {
        public final Expr cond;
        public final List<Stmt> body;

        public While(Expr cond, List<Stmt> body) {
            this.cond = cond;
            this.body = body;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitWhile(this); }
    }

    // ============================================================
    // 7) DO WHILE: radiNesto { ... } dokNeDobijemObrok(cond);
    // ============================================================
    public static final class DoWhile extends Stmt {
        public final List<Stmt> body;
        public final Expr cond;

        public DoWhile(List<Stmt> body, Expr cond) {
            this.body = body;
            this.cond = cond;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitDoWhile(this); }
    }

    // ============================================================
    // 8) FOR: novaTura(init ; cond ; updates) { ... }
    //
    // init = VarDecl | ExprStmt | null
    // cond = Expr | null
    // updates = List<Expr>
    // ============================================================
    public static final class For extends Stmt {
        public final Stmt init;          // inicijalizacija
        public final Expr cond;          // uslov
        public final List<Expr> update;  // ažuriranja
        public final List<Stmt> body;

        public For(Stmt init, Expr cond, List<Expr> update, List<Stmt> body) {
            this.init = init;
            this.cond = cond;
            this.update = update;
            this.body = body;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitFor(this); }
    }

    // ============================================================
    // 9) SWITCH: naplati(expr) { sto label: stmts... ; naplaceno: stmts... }
    // ============================================================
    public static final class Switch extends Stmt {

        public static final class CaseArm {
            public final Token label;       // literal ili IDENT
            public final List<Stmt> body;   // statements

            public CaseArm(Token label, List<Stmt> body) {
                this.label = label;
                this.body = body;
            }
        }

        public final Expr expr;
        public final List<CaseArm> cases;
        public final List<Stmt> defaultBlock; // može null

        public Switch(Expr expr, List<CaseArm> cases, List<Stmt> defaultBlock) {
            this.expr = expr;
            this.cases = cases;
            this.defaultBlock = defaultBlock;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitSwitch(this); }
    }

    // ============================================================
    // 10) RETURN
    // ============================================================
    public static final class Return extends Stmt {
        public final Expr expr; // može biti null

        public Return(Expr expr) {
            this.expr = expr;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitReturn(this); }
    }

    // ============================================================
    // 11) BLOCK: { stmt stmt stmt ... }
    // ============================================================
    public static final class Block extends Stmt {
        public final List<Stmt> stmts;

        public Block(List<Stmt> stmts) {
            this.stmts = stmts;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitBlock(this); }
    }
}

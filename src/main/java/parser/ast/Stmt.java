package parser.ast;

import lexer.token.Token;
import java.util.List;

public abstract class Stmt {


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


    public static final class ExprStmt extends Stmt {
        public final Expr expr;

        public ExprStmt(Expr expr) {
            this.expr = expr;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitExprStmt(this); }
    }


    public static final class Print extends Stmt {
        public final List<Expr> args;

        public Print(List<Expr> args) {
            this.args = args;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitPrint(this); }
    }


    public static final class Read extends Stmt {
        public final Token name; // IDENT

        public Read(Token name) {
            this.name = name;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitRead(this); }
    }


    public static final class If extends Stmt {

        public static final class Arm {
            public final Expr cond;
            public final List<Stmt> block;

            public Arm(Expr cond, List<Stmt> block) {
                this.cond = cond;
                this.block = block;
            }
        }

        public final Arm ifArm;
        public final List<Arm> elseIfArms;
        public final List<Stmt> elseBlock;

        public If(Arm ifArm, List<Arm> elseIfArms, List<Stmt> elseBlock) {
            this.ifArm = ifArm;
            this.elseIfArms = elseIfArms;
            this.elseBlock = elseBlock;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitIf(this); }
    }


    public static final class While extends Stmt {
        public final Expr cond;
        public final List<Stmt> body;

        public While(Expr cond, List<Stmt> body) {
            this.cond = cond;
            this.body = body;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitWhile(this); }
    }


    public static final class DoWhile extends Stmt {
        public final List<Stmt> body;
        public final Expr cond;

        public DoWhile(List<Stmt> body, Expr cond) {
            this.body = body;
            this.cond = cond;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitDoWhile(this); }
    }


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


    public static final class Return extends Stmt {
        public final Expr expr; // može biti null

        public Return(Expr expr) {
            this.expr = expr;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitReturn(this); }
    }


    public static final class Block extends Stmt {
        public final List<Stmt> stmts;

        public Block(List<Stmt> stmts) {
            this.stmts = stmts;
        }

        @Override public <R> R accept(Visitor<R> v) { return v.visitBlock(this); }
    }
}

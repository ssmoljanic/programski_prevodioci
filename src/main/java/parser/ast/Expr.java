package parser.ast;

import lexer.token.Token;
import java.util.List;

public abstract class Expr {


    public interface Visitor<R> {
        R visitLiteral(Literal e);
        R visitIdent(Ident e);
        R visitIndex(Index e);
        R visitGrouping(Grouping e);
        R visitCall(Call e);
        R visitBinary(Binary e);
        R visitUnary(Unary e);
        R visitAssign(Assign e);
    }

    public abstract <R> R accept(Visitor<R> v);


    public static final class Literal extends Expr {
        public final Token token;   // INT_LIT, DOUBLE_LIT, STRING_LIT, CHAR_LIT, USLUZEN, NEUSLUZEN
        public final Object value;  // npr. Integer, Double, String, Character, Boolean

        public Literal(Token token, Object value) {
            this.token = token;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitLiteral(this); }
    }


    public static final class Ident extends Expr {
        public final Token name; // IDENT

        public Ident(Token name) { this.name = name; }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitIdent(this); }
    }


    public static final class Index extends Expr {
        public final Token name;      // IDENT
        public final List<Expr> indices; // jedna ili više dimenzija

        public Index(Token name, List<Expr> indices) {
            this.name = name;
            this.indices = indices;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitIndex(this); }
    }


    public static final class Grouping extends Expr {
        public final Expr inner;

        public Grouping(Expr inner) { this.inner = inner; }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitGrouping(this); }
    }

    // poziv funk
    public static final class Call extends Expr {
        public final Token callee;  // IDENT – ime funkcije
        public final List<Expr> args;

        public Call(Token callee, List<Expr> args) {
            this.callee = callee;
            this.args = args;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitCall(this); }
    }

    //binarni operatori
    public static final class Binary extends Expr {
        public final Expr left;
        public final Token op;
        public final Expr right;

        public Binary(Expr left, Token op, Expr right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitBinary(this); }
    }

    //unarni
    public static final class Unary extends Expr {
        public final Token op;  // NOT ili MANJE (unary minus)
        public final Expr expr;

        public Unary(Token op, Expr expr) {
            this.op = op;
            this.expr = expr;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitUnary(this); }
    }


    public static final class Assign extends Expr {
        public final Expr target; // tipično Ident ili Index
        public final Token op;    // "=" ili JE
        public final Expr value;

        public Assign(Expr target, Token op, Expr value) {
            this.target = target;
            this.op = op;
            this.value = value;
        }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitAssign(this); }
    }
}

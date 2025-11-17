package parser.ast;

import lexer.token.Token;
import java.util.List;

public abstract class Expr {

    // ==== VISITOR ====
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

    // ==== LITERAL ====
    // Sada podržava INT, DOUBLE, STRING, CHAR, USLUZEN/NEUSLUZEN...
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

    // ==== IDENTIFIKATOR ====
    public static final class Ident extends Expr {
        public final Token name; // IDENT

        public Ident(Token name) { this.name = name; }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitIdent(this); }
    }

    // ==== INDEXIRANJE NIZA ====
    // IDENT [expr] [expr] ...
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

    // ==== ( expr ) ====
    public static final class Grouping extends Expr {
        public final Expr inner;

        public Grouping(Expr inner) { this.inner = inner; }

        @Override
        public <R> R accept(Visitor<R> v) { return v.visitGrouping(this); }
    }

    // ==== POZIV FUNKCIJE ====
    // U Restoranu: IDENT "(" [ arg_list ] ")"
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

    // ==== BINARNI OPERATORI ====
    // UKUPNO, MANJE, PUTA, DELJENO, KUSUR, OR, AND, LT, LE, GT, GE, EQ, NEQ ...
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

    // ==== UNARNI OPERATOR ====
    // unary = (NOT | MANJE) unary | postfix;
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

    // ==== DODELA ====
    // assign_expr = lvalue ( "=" | JE ) assign_expr | or_expr;
    // lvalue = IDENT { "[" expr "]" };
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

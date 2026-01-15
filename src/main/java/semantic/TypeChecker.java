package semantic;

import lexer.token.Token;
import lexer.token.TokenType;
import parser.ast.Ast;

public class TypeChecker {

    private static Token makeTypeToken(TokenType type, String lexeme) {
        return new Token(type, lexeme, null, 0, 0, 0);
    }

    public static final Ast.Type PORUDZBINA = Ast.Type.scalar(
        makeTypeToken(TokenType.PORUDZBINA, "porudzbina")
    );

    public static final Ast.Type RACUN = Ast.Type.scalar(
        makeTypeToken(TokenType.RACUN, "racun")
    );

    public static final Ast.Type PREDJELO = Ast.Type.scalar(
        makeTypeToken(TokenType.PREDJELO, "predjelo")
    );

    public static final Ast.Type JELOVNIK = Ast.Type.scalar(
        makeTypeToken(TokenType.JELOVNIK, "jelovnik")
    );

    public static final Ast.Type USLUZEN_NEUSLUZEN = Ast.Type.scalar(
        makeTypeToken(TokenType.USLUZENNEUSLUZEN, "usluzenNeusluzen")
    );

    public static final Ast.Type VOID = Ast.Type.voidType();

    public static boolean areEqual(Ast.Type a, Ast.Type b) {
        if (a == null || b == null) return false;
        if (a.kind != b.kind) return false;

        if (a.kind == Ast.Type.Kind.VOID) {
            return b.kind == Ast.Type.Kind.VOID;
        }

        String aBase = getBaseTypeName(a);
        String bBase = getBaseTypeName(b);

        if (!aBase.equals(bBase)) return false;

        if (a.kind == Ast.Type.Kind.ARRAY) {
            return a.rank == b.rank;
        }

        return true;
    }

    public static String getBaseTypeName(Ast.Type type) {
        if (type == null || type.baseType == null) {
            return "void";
        }
        return type.baseType.lexeme;
    }

    public static String getTypeName(Ast.Type type) {
        if (type == null || type.kind == Ast.Type.Kind.VOID) {
            return "void";
        }

        String base = getBaseTypeName(type);

        if (type.kind == Ast.Type.Kind.ARRAY) {
            return "listaCekanja{" + base + "}" + "[]".repeat(type.rank);
        }

        return base;
    }

    public static boolean isNumeric(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        String name = getBaseTypeName(type);
        return name.equals("porudzbina") || name.equals("racun");
    }

    public static boolean isInteger(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("porudzbina");
    }

    public static boolean isDouble(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("racun");
    }

    public static boolean isBoolean(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("usluzenNeusluzen");
    }

    public static boolean isString(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("predjelo");
    }

    public static boolean isChar(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("jelovnik");
    }

    public static boolean isArray(Ast.Type type) {
        return type != null && type.kind == Ast.Type.Kind.ARRAY;
    }

    public static Ast.Type getArithmeticResultType(Ast.Type left, Ast.Type right) {
        if (!isNumeric(left) || !isNumeric(right)) {
            return null;
        }

        if (!areEqual(left, right)) {
            return null;
        }

        return left;
    }

    public static Ast.Type getRelationalResultType(Ast.Type left, Ast.Type right) {
        if (!isNumeric(left) || !isNumeric(right)) {
            return null;
        }

        if (!areEqual(left, right)) {
            return null;
        }

        return USLUZEN_NEUSLUZEN;
    }

    public static Ast.Type getEqualityResultType(Ast.Type left, Ast.Type right) {
        if (!areEqual(left, right)) {
            return null;
        }

        return USLUZEN_NEUSLUZEN;
    }

    public static Ast.Type getLogicalResultType(Ast.Type left, Ast.Type right) {
        if (!isBoolean(left) || !isBoolean(right)) {
            return null;
        }

        return USLUZEN_NEUSLUZEN;
    }

    public static Ast.Type getNotResultType(Ast.Type operand) {
        if (!isBoolean(operand)) {
            return null;
        }
        return USLUZEN_NEUSLUZEN;
    }

    public static Ast.Type getUnaryMinusResultType(Ast.Type operand) {
        if (!isNumeric(operand)) {
            return null;
        }
        return operand;
    }

    public static Ast.Type getArrayElementType(Ast.Type arrayType, int indexCount) {
        if (arrayType == null || arrayType.kind != Ast.Type.Kind.ARRAY) {
            return null;
        }

        int newRank = arrayType.rank - indexCount;

        if (newRank < 0) {
            return null;
        }

        if (newRank == 0) {
            return Ast.Type.scalar(arrayType.baseType);
        }

        return Ast.Type.array(arrayType.baseType, newRank);
    }

    public static boolean isAssignable(Ast.Type target, Ast.Type value) {
        return areEqual(target, value);
    }

    public static boolean isCastAllowed(Ast.Type from, Ast.Type to) {
        if (areEqual(from, to)) {
            return true;
        }

        if (!isNumeric(from) || !isNumeric(to)) {
            return false;
        }

        return true;
    }

    public static Ast.Type getCastResultType(Ast.Type from, Ast.Type to) {
        if (isCastAllowed(from, to)) {
            return to;
        }
        return null;
    }
}

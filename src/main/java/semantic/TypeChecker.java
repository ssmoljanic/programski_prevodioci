package semantic;

import lexer.token.Token;
import lexer.token.TokenType;
import parser.ast.Ast;

/**
 * Pomoćna klasa za rad sa tipovima.
 *
 * Sadrži:
 * - Konstante za osnovne tipove
 * - Metode za poređenje tipova
 * - Metode za određivanje tipa rezultata operacija
 */
public class TypeChecker {

    // ===== KONSTANTE ZA OSNOVNE TIPOVE =====
    // Zašto konstante? Da ne kreiramo nove objekte svaki put i da lakše poredimo.

    // Kreiramo "lažne" tokene za tipove (samo nam treba lexeme za poređenje)
    private static Token makeTypeToken(TokenType type, String lexeme) {
        return new Token(type, lexeme, null, 0, 0, 0);
    }

    // Osnovni tipovi kao konstante
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

    // ===== POREĐENJE TIPOVA =====

    /**
     * Proverava da li su dva tipa jednaka.
     */
    public static boolean areEqual(Ast.Type a, Ast.Type b) {
        if (a == null || b == null) return false;
        if (a.kind != b.kind) return false;

        if (a.kind == Ast.Type.Kind.VOID) {
            return b.kind == Ast.Type.Kind.VOID;
        }

        // Za SCALAR i ARRAY, poredimo baseType
        String aBase = getBaseTypeName(a);
        String bBase = getBaseTypeName(b);

        if (!aBase.equals(bBase)) return false;

        // Za nizove, poredimo i rank (broj dimenzija)
        if (a.kind == Ast.Type.Kind.ARRAY) {
            return a.rank == b.rank;
        }

        return true;
    }

    /**
     * Vraća ime osnovnog tipa kao string.
     */
    public static String getBaseTypeName(Ast.Type type) {
        if (type == null || type.baseType == null) {
            return "void";
        }
        return type.baseType.lexeme;
    }

    /**
     * Vraća čitljivo ime tipa za poruke o greškama.
     */
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

    // ===== PROVERE KATEGORIJA TIPOVA =====

    /**
     * Da li je tip numerički (porudzbina ili racun)?
     */
    public static boolean isNumeric(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        String name = getBaseTypeName(type);
        return name.equals("porudzbina") || name.equals("racun");
    }

    /**
     * Da li je tip celobrojni (porudzbina)?
     */
    public static boolean isInteger(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("porudzbina");
    }

    /**
     * Da li je tip realan broj (racun)?
     */
    public static boolean isDouble(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("racun");
    }

    /**
     * Da li je tip logički (usluzenNeusluzen)?
     */
    public static boolean isBoolean(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("usluzenNeusluzen");
    }

    /**
     * Da li je tip string (predjelo)?
     */
    public static boolean isString(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("predjelo");
    }

    /**
     * Da li je tip karakter (jelovnik)?
     */
    public static boolean isChar(Ast.Type type) {
        if (type == null || type.kind != Ast.Type.Kind.SCALAR) return false;
        return getBaseTypeName(type).equals("jelovnik");
    }

    /**
     * Da li je tip niz?
     */
    public static boolean isArray(Ast.Type type) {
        return type != null && type.kind == Ast.Type.Kind.ARRAY;
    }

    // ===== ODREĐIVANJE TIPA REZULTATA =====

    /**
     * Vraća tip rezultata aritmetičke operacije.
     *
     * Pravila:
     * - porudzbina OP porudzbina → porudzbina
     * - racun OP racun → racun
     * - Različiti tipovi → null (greška)
     */
    public static Ast.Type getArithmeticResultType(Ast.Type left, Ast.Type right) {
        if (!isNumeric(left) || !isNumeric(right)) {
            return null;  // Oba moraju biti numerička
        }

        if (!areEqual(left, right)) {
            return null;  // Moraju biti istog tipa (bez implicitnog kastovanja)
        }

        return left;  // Rezultat je istog tipa kao operandi
    }

    /**
     * Vraća tip rezultata relacione operacije.
     *
     * Pravila:
     * - broj OP broj → usluzenNeusluzen
     * - Ostalo → null (greška)
     */
    public static Ast.Type getRelationalResultType(Ast.Type left, Ast.Type right) {
        // Za <, <=, >, >= - oba moraju biti numerička i istog tipa
        if (!isNumeric(left) || !isNumeric(right)) {
            return null;
        }

        if (!areEqual(left, right)) {
            return null;  // Moraju biti istog tipa
        }

        return USLUZEN_NEUSLUZEN;  // Rezultat je uvek boolean
    }

    /**
     * Vraća tip rezultata operacije jednakosti (==, !=).
     *
     * Pravila:
     * - Isti tipovi mogu se porediti
     * - Rezultat je uvek usluzenNeusluzen
     */
    public static Ast.Type getEqualityResultType(Ast.Type left, Ast.Type right) {
        // Za == i != - tipovi moraju biti isti
        if (!areEqual(left, right)) {
            return null;
        }

        return USLUZEN_NEUSLUZEN;
    }

    /**
     * Vraća tip rezultata logičke operacije.
     *
     * Pravila:
     * - usluzenNeusluzen OP usluzenNeusluzen → usluzenNeusluzen
     * - Ostalo → null (greška)
     */
    public static Ast.Type getLogicalResultType(Ast.Type left, Ast.Type right) {
        if (!isBoolean(left) || !isBoolean(right)) {
            return null;
        }

        return USLUZEN_NEUSLUZEN;
    }

    /**
     * Vraća tip rezultata unarne negacije (!).
     */
    public static Ast.Type getNotResultType(Ast.Type operand) {
        if (!isBoolean(operand)) {
            return null;
        }
        return USLUZEN_NEUSLUZEN;
    }

    /**
     * Vraća tip rezultata unarnog minusa (-).
     */
    public static Ast.Type getUnaryMinusResultType(Ast.Type operand) {
        if (!isNumeric(operand)) {
            return null;
        }
        return operand;  // Isti tip kao operand
    }

    /**
     * Vraća tip elementa niza nakon indeksiranja.
     *
     * Primer: listaCekanja{porudzbina}[] indeksirano jednom → porudzbina
     */
    public static Ast.Type getArrayElementType(Ast.Type arrayType, int indexCount) {
        if (arrayType == null || arrayType.kind != Ast.Type.Kind.ARRAY) {
            return null;
        }

        int newRank = arrayType.rank - indexCount;

        if (newRank < 0) {
            return null;  // Previše indeksa
        }

        if (newRank == 0) {
            // Vratili smo se na skalarni tip
            return Ast.Type.scalar(arrayType.baseType);
        }

        // Još uvek je niz, ali sa manje dimenzija
        return Ast.Type.array(arrayType.baseType, newRank);
    }

    /**
     * Proverava da li se tip može dodeliti drugom tipu.
     *
     * Za sada: tipovi moraju biti identični (nema implicitnog kastovanja).
     */
    public static boolean isAssignable(Ast.Type target, Ast.Type value) {
        return areEqual(target, value);
    }

    // ===== KASTOVANJE =====

    /**
     * Proverava da li je kastovanje dozvoljeno.
     *
     * Pravila iz specifikacije:
     * - porudzbina → racun: UVEK dozvoljeno (5 → 5.0)
     * - racun → porudzbina: Dozvoljeno samo ako su decimale 0 (provera u runtime)
     * - Ostalo: Nije dozvoljeno
     *
     * @param from Izvorni tip
     * @param to   Ciljni tip
     * @return true ako je kastovanje dozvoljeno
     */
    public static boolean isCastAllowed(Ast.Type from, Ast.Type to) {
        // Kastovanje u isti tip - uvek OK
        if (areEqual(from, to)) {
            return true;
        }

        // Samo numerički tipovi mogu se međusobno kastovati
        if (!isNumeric(from) || !isNumeric(to)) {
            return false;
        }

        // porudzbina → racun: UVEK dozvoljeno
        // racun → porudzbina: Dozvoljeno (provera vrednosti je u runtime)
        return true;
    }

    /**
     * Vraća tip rezultata kastovanja.
     * Ako kastovanje nije dozvoljeno, vraća null.
     */
    public static Ast.Type getCastResultType(Ast.Type from, Ast.Type to) {
        if (isCastAllowed(from, to)) {
            return to;
        }
        return null;
    }
}

package semantic;

/**
 * Predstavlja semantičku grešku pronađenu tokom analize.
 *
 * Sadrži:
 * - Tip greške (iz specifikacije)
 * - Poruku koja opisuje grešku
 * - Lokaciju u izvornom kodu (linija, kolona)
 */
public class SemanticError extends RuntimeException {

    // ===== TIPOVI GREŠAKA =====
    // Svi tipovi grešaka iz specifikacije zadatka.
    // Zašto enum? Da imamo strogo definisan skup mogućih grešaka
    // i da možemo lako dodati specifično rukovanje za svaku.
    public enum ErrorType {

        // --- Greške vezane za main funkciju ---
        MISSING_MAIN(
            "Nepostojanje main funkcije"
        ),
        DUPLICATE_MAIN(
            "Više od jedne deklaracije main funkcije"
        ),

        // --- Greške vezane za deklaracije ---
        UNDECLARED_VARIABLE(
            "Korišćenje promenljive koja prethodno nije deklarisana"
        ),
        UNDECLARED_FUNCTION(
            "Korišćenje funkcije koja prethodno nije deklarisana"
        ),
        DUPLICATE_VARIABLE(
            "Deklaracija promenljive koja već postoji u tom okruženju"
        ),
        DUPLICATE_FUNCTION(
            "Deklaracija funkcije koja već postoji"
        ),

        // --- Greške vezane za tipove ---
        INVALID_CAST(
            "Neispravno kastovanje"
        ),
        TYPE_MISMATCH_ARITHMETIC(
            "Nepodudaranje tipova podataka u aritmetičkim izrazima"
        ),
        TYPE_MISMATCH_RELATIONAL(
            "U izrazima sa relacionim operatorima operandi nisu brojevi"
        ),
        TYPE_MISMATCH_LOGICAL(
            "U logičkim izrazima operandi i/ili rezultat nisu logički izrazi"
        ),
        TYPE_MISMATCH_CONDITION(
            "Neispravno korišćenje grananja i petlji - uslov nije logička vrednost"
        ),
        TYPE_MISMATCH_ARRAY_ELEMENT(
            "Dodavanje vrednosti u niz pogrešnog tipa"
        ),
        TYPE_MISMATCH_ASSIGNMENT(
            "Nepodudaranje tipova pri dodeli vrednosti"
        ),

        // --- Greške vezane za funkcije ---
        RETURN_TYPE_MISMATCH(
            "Nepodudaranje povratnog tipa funkcije sa povratnom vrednošću"
        ),
        ARGUMENT_COUNT_MISMATCH(
            "Nepodudaranje broja parametara funkcije i argumenata pri pozivu"
        ),
        ARGUMENT_TYPE_MISMATCH(
            "Nepodudaranje tipova parametara funkcije i argumenata pri pozivu"
        ),
        FUNCTION_RETURN_TYPE_MISMATCH(
            "Nepodudaranje povratnog tipa funkcije sa tipom podataka u izrazu"
        ),

        // --- Greške vezane za indeksiranje i pozive ---
        INVALID_INDEX_TARGET(
            "Pristup preko indeksa u tipu podatka koji ne podržava indeksiranje"
        ),
        INVALID_CALL_TARGET(
            "Poziv objekta kao funkcije"
        ),
        INVALID_INDEX_TYPE(
            "Indeks mora biti celobrojnog tipa"
        );

        // Opis greške (za korisnika)
        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ===== ATRIBUTI =====

    private final ErrorType type;
    private final int line;
    private final int column;

    // ===== KONSTRUKTORI =====

    /**
     * Kreira semantičku grešku sa svim informacijama.
     *
     * @param type    Tip greške
     * @param message Detaljna poruka
     * @param line    Linija u izvornom kodu
     * @param column  Kolona u izvornom kodu
     */
    public SemanticError(ErrorType type, String message, int line, int column) {
        super(message);
        this.type = type;
        this.line = line;
        this.column = column;
    }

    /**
     * Kreira semantičku grešku bez tačne lokacije (npr. MISSING_MAIN).
     */
    public SemanticError(ErrorType type, String message) {
        this(type, message, 0, 0);
    }

    // ===== GETTERI =====

    public ErrorType getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    // ===== FORMATIRANA PORUKA =====

    /**
     * Vraća formatiranu poruku o grešci za ispis korisniku.
     *
     * Format:
     * SEMANTIČKA GREŠKA [linija X, kolona Y]: Opis
     * Detalji: poruka
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("SEMANTIČKA GREŠKA");

        if (line > 0) {
            sb.append(" [linija ").append(line);
            if (column > 0) {
                sb.append(", kolona ").append(column);
            }
            sb.append("]");
        }

        sb.append(": ").append(type.getDescription());
        sb.append("\n  Detalji: ").append(getMessage());

        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    // ===== FACTORY METODE =====
    // Zašto factory metode? Da olakšamo kreiranje čestih grešaka
    // i da osiguramo konzistentne poruke.

    public static SemanticError missingMain() {
        return new SemanticError(
            ErrorType.MISSING_MAIN,
            "Program mora imati tačno jednu 'glavniObrok' funkciju"
        );
    }

    public static SemanticError duplicateMain(int line, int column) {
        return new SemanticError(
            ErrorType.DUPLICATE_MAIN,
            "Funkcija 'glavniObrok' je već definisana",
            line, column
        );
    }

    public static SemanticError undeclaredVariable(String name, int line, int column) {
        return new SemanticError(
            ErrorType.UNDECLARED_VARIABLE,
            "Promenljiva '" + name + "' nije deklarisana",
            line, column
        );
    }

    public static SemanticError undeclaredFunction(String name, int line, int column) {
        return new SemanticError(
            ErrorType.UNDECLARED_FUNCTION,
            "Funkcija '" + name + "' nije deklarisana",
            line, column
        );
    }

    public static SemanticError duplicateVariable(String name, int line, int column) {
        return new SemanticError(
            ErrorType.DUPLICATE_VARIABLE,
            "Promenljiva '" + name + "' je već deklarisana u ovom bloku",
            line, column
        );
    }

    public static SemanticError duplicateFunction(String name, int line, int column) {
        return new SemanticError(
            ErrorType.DUPLICATE_FUNCTION,
            "Funkcija '" + name + "' je već definisana",
            line, column
        );
    }

    public static SemanticError typeMismatch(String expected, String actual, int line, int column) {
        return new SemanticError(
            ErrorType.TYPE_MISMATCH_ASSIGNMENT,
            "Očekivan tip '" + expected + "', dobijen '" + actual + "'",
            line, column
        );
    }

    public static SemanticError invalidCast(String from, String to, int line, int column) {
        return new SemanticError(
            ErrorType.INVALID_CAST,
            "Nije moguće kastovati iz '" + from + "' u '" + to + "'",
            line, column
        );
    }

    public static SemanticError conditionNotBoolean(int line, int column) {
        return new SemanticError(
            ErrorType.TYPE_MISMATCH_CONDITION,
            "Uslov mora biti tipa 'usluzenNeusluzen' (boolean)",
            line, column
        );
    }

    public static SemanticError argumentCountMismatch(String funcName, int expected, int actual, int line, int column) {
        return new SemanticError(
            ErrorType.ARGUMENT_COUNT_MISMATCH,
            "Funkcija '" + funcName + "' očekuje " + expected + " argument(a), dobijeno " + actual,
            line, column
        );
    }

    public static SemanticError argumentTypeMismatch(String funcName, int argIndex, String expected, String actual, int line, int column) {
        return new SemanticError(
            ErrorType.ARGUMENT_TYPE_MISMATCH,
            "Funkcija '" + funcName + "', argument " + (argIndex + 1) + ": očekivan tip '" + expected + "', dobijen '" + actual + "'",
            line, column
        );
    }

    public static SemanticError returnTypeMismatch(String expected, String actual, int line, int column) {
        return new SemanticError(
            ErrorType.RETURN_TYPE_MISMATCH,
            "Povratni tip funkcije je '" + expected + "', a vraća se '" + actual + "'",
            line, column
        );
    }

    public static SemanticError cannotIndex(String typeName, int line, int column) {
        return new SemanticError(
            ErrorType.INVALID_INDEX_TARGET,
            "Tip '" + typeName + "' ne podržava indeksiranje",
            line, column
        );
    }

    public static SemanticError cannotCall(String name, int line, int column) {
        return new SemanticError(
            ErrorType.INVALID_CALL_TARGET,
            "'" + name + "' nije funkcija i ne može se pozvati",
            line, column
        );
    }

    public static SemanticError indexNotInteger(int line, int column) {
        return new SemanticError(
            ErrorType.INVALID_INDEX_TYPE,
            "Indeks niza mora biti celobrojnog tipa (porudzbina)",
            line, column
        );
    }
}
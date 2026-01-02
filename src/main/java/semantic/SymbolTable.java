package semantic;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabela simbola sa podrškom za ugnježdene scope-ove.
 *
 * Svaki scope ima:
 * - Mapu simbola (ime -> Symbol)
 * - Referencu na roditeljski scope (ili null za globalni)
 *
 * Vizualizacija:
 *
 *   ┌─────────────────────────┐
 *   │ GLOBALNI SCOPE          │  parent = null
 *   │   mojRecept, glavniObrok│
 *   └───────────┬─────────────┘
 *               │ parent
 *   ┌───────────▼─────────────┐
 *   │ SCOPE funkcije          │
 *   │   x, y                  │
 *   └───────────┬─────────────┘
 *               │ parent
 *   ┌───────────▼─────────────┐
 *   │ SCOPE if bloka          │  ← currentScope
 *   │   z                     │
 *   └─────────────────────────┘
 */
public class SymbolTable {

    // ===== INNER KLASA: SCOPE =====
    // Zašto inner klasa? Jer Scope nema smisla van konteksta SymbolTable.
    // Enkapsuliramo implementacione detalje.
    private static class Scope {
        // Mapa: ime simbola -> Symbol objekat
        // Zašto HashMap? Brza pretraga O(1) po imenu.
        final Map<String, Symbol> symbols = new HashMap<>();

        // Referenca na roditeljski scope
        // Zašto? Da bismo mogli da pretražujemo "naviše" kroz lanac.
        final Scope parent;

        // Ime scope-a (za debagovanje): "global", "funkcija:mojRecept", "blok"
        final String name;

        Scope(Scope parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }

    // ===== ATRIBUTI =====

    // Trenutni scope u kom se nalazimo
    private Scope currentScope;

    // Globalni scope (čuvamo referencu za brz pristup)
    private final Scope globalScope;

    // ===== KONSTRUKTOR =====
    public SymbolTable() {
        // Na početku kreiramo globalni scope (nema roditelja)
        this.globalScope = new Scope(null, "global");
        this.currentScope = globalScope;
    }

    // ===== UPRAVLJANJE SCOPE-OVIMA =====

    /**
     * Ulazak u novi scope (npr. ulazak u funkciju, if blok, while petlju...)
     *
     * @param name Ime scope-a za debagovanje (npr. "funkcija:mojRecept")
     */
    public void enterScope(String name) {
        // Kreiramo novi scope čiji roditelj je trenutni scope
        Scope newScope = new Scope(currentScope, name);
        // Novi scope postaje trenutni
        currentScope = newScope;
    }

    /**
     * Izlazak iz trenutnog scope-a (vraćamo se u roditeljski).
     *
     * VAŽNO: Svi simboli deklarisani u ovom scope-u se "zaboravljaju"
     * jer više nemamo referencu na taj scope.
     */
    public void exitScope() {
        if (currentScope.parent != null) {
            // Vraćamo se na roditeljski scope
            currentScope = currentScope.parent;
        }
        // Ako smo u globalnom scope-u, ne radimo ništa
        // (ne možemo izaći iz globalnog scope-a)
    }

    // ===== DEFINISANJE SIMBOLA =====

    /**
     * Definiše novi simbol u TRENUTNOM scope-u.
     *
     * @param symbol Simbol koji dodajemo
     * @return true ako je uspešno dodato, false ako već postoji u OVOM scope-u
     *
     * NAPOMENA: Simbol MOŽE postojati u roditeljskom scope-u (shadowing je dozvoljen).
     * Greška je SAMO ako postoji u ISTOM scope-u.
     */
    public boolean define(Symbol symbol) {
        // Proveravamo da li već postoji u TRENUTNOM scope-u
        if (currentScope.symbols.containsKey(symbol.name)) {
            return false;  // Već postoji - greška!
        }
        // Dodajemo simbol u trenutni scope
        currentScope.symbols.put(symbol.name, symbol);
        return true;
    }

    /**
     * Definiše simbol u GLOBALNOM scope-u.
     * Koristi se za funkcije koje su uvek globalno vidljive.
     */
    public boolean defineGlobal(Symbol symbol) {
        if (globalScope.symbols.containsKey(symbol.name)) {
            return false;
        }
        globalScope.symbols.put(symbol.name, symbol);
        return true;
    }

    // ===== PRETRAGA SIMBOLA =====

    /**
     * Traži simbol po imenu, krećući od trenutnog scope-a naviše.
     *
     * Algoritam:
     * 1. Pogledaj u trenutnom scope-u
     * 2. Ako nema, pogledaj u roditeljskom
     * 3. Nastavi dok ne nađeš ili dok ne dođeš do kraja (null)
     *
     * @param name Ime simbola
     * @return Symbol ako postoji, null ako ne postoji
     */
    public Symbol lookup(String name) {
        Scope scope = currentScope;

        // Idemo kroz lanac scope-ova
        while (scope != null) {
            Symbol symbol = scope.symbols.get(name);
            if (symbol != null) {
                return symbol;  // Našli smo!
            }
            // Idemo na roditeljski scope
            scope = scope.parent;
        }

        // Nismo našli ni u jednom scope-u
        return null;
    }

    /**
     * Traži simbol SAMO u trenutnom scope-u (ne gleda roditelje).
     *
     * Korisno za proveru: "Da li je ovo ime već deklarisano u OVOM bloku?"
     */
    public Symbol lookupLocal(String name) {
        return currentScope.symbols.get(name);
    }

    /**
     * Traži simbol SAMO u globalnom scope-u.
     * Korisno za proveru funkcija.
     */
    public Symbol lookupGlobal(String name) {
        return globalScope.symbols.get(name);
    }

    // ===== POMOĆNE METODE =====

    /**
     * Da li smo trenutno u globalnom scope-u?
     */
    public boolean isGlobalScope() {
        return currentScope == globalScope;
    }

    /**
     * Vraća ime trenutnog scope-a (za debagovanje).
     */
    public String getCurrentScopeName() {
        return currentScope.name;
    }

    /**
     * Ispisuje sadržaj tabele simbola (za debagovanje).
     */
    public void dump() {
        System.out.println("=== SYMBOL TABLE DUMP ===");
        dumpScope(currentScope, 0);
        System.out.println("=========================");
    }

    private void dumpScope(Scope scope, int indent) {
        if (scope == null) return;

        // Prvo ispiši roditeljske scope-ove
        dumpScope(scope.parent, indent);

        String prefix = "  ".repeat(indent);
        System.out.println(prefix + "┌─ Scope: " + scope.name);
        for (Symbol sym : scope.symbols.values()) {
            System.out.println(prefix + "│  " + sym);
        }
        System.out.println(prefix + "└─────────────────────");
    }
}
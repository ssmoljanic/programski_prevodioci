package semantic;

import parser.ast.Ast;
import java.util.List;

/**
 * Predstavlja jedan simbol u tabeli simbola.
 * Simbol može biti PROMENLJIVA ili FUNKCIJA.
 */
public class Symbol {

    // ===== VRSTA SIMBOLA =====
    // Zašto enum? Jer simbol može biti SAMO jedno od ova dva.
    // Ovo nam pomaže da razlikujemo promenljive od funkcija.
    public enum Kind {
        VARIABLE,   // promenljiva: izvolite porudzbina x;
        FUNCTION    // funkcija: recept porudzbina mojRecept() { }
    }

    // ===== ATRIBUTI =====

    // Ime simbola (npr. "x", "mojRecept")
    public final String name;

    // Tip simbola (porudzbina, racun, listaCekanja{porudzbina}, itd.)
    // Za funkcije, ovo je POVRATNI tip
    public final Ast.Type type;

    // Da li je promenljiva ili funkcija
    public final Kind kind;

    // Parametri funkcije (prazna lista za promenljive)
    // Zašto čuvamo parametre? Da bismo proverili da li se funkcija
    // poziva sa ispravnim brojem i tipovima argumenata.
    public final List<Ast.Param> params;

    // Lokacija u izvornom kodu (za poruke o greškama)
    public final int line;
    public final int column;

    // ===== KONSTRUKTOR ZA PROMENLJIVU =====
    // Zašto poseban konstruktor? Jer promenljive nemaju parametre,
    // pa je jednostavnije imati konstruktor koji to ne zahteva.
    public Symbol(String name, Ast.Type type, int line, int column) {
        this.name = name;
        this.type = type;
        this.kind = Kind.VARIABLE;
        this.params = List.of();  // prazna lista
        this.line = line;
        this.column = column;
    }

    // ===== KONSTRUKTOR ZA FUNKCIJU =====
    // Funkcije imaju povratni tip I listu parametara.
    public Symbol(String name, Ast.Type type, List<Ast.Param> params, int line, int column) {
        this.name = name;
        this.type = type;
        this.kind = Kind.FUNCTION;
        this.params = params;
        this.line = line;
        this.column = column;
    }

    // ===== POMOĆNE METODE =====

    public boolean isVariable() {
        return kind == Kind.VARIABLE;
    }

    public boolean isFunction() {
        return kind == Kind.FUNCTION;
    }

    // Za lepši ispis prilikom debagovanja
    @Override
    public String toString() {
        if (kind == Kind.VARIABLE) {
            return "Variable[" + name + " : " + typeToString(type) + "]";
        } else {
            return "Function[" + name + "(" + params.size() + " params) : " + typeToString(type) + "]";
        }
    }

    // Pomoćna metoda za konverziju tipa u string
    private String typeToString(Ast.Type t) {
        if (t == null || t.kind == Ast.Type.Kind.VOID) {
            return "void";
        }
        String base = t.baseType != null ? t.baseType.lexeme : "?";
        if (t.kind == Ast.Type.Kind.ARRAY) {
            return "listaCekanja{" + base + "}[" + t.rank + "]";
        }
        return base;
    }
}
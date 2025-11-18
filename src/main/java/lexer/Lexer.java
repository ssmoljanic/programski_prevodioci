package lexer;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final ScannerCore sc;
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("izvolite", TokenType.IZVOLITE),
            Map.entry("porudzbina", TokenType.PORUDZBINA),
            Map.entry("racun", TokenType.RACUN),
            Map.entry("predjelo", TokenType.PREDJELO),
            Map.entry("usluzenNeusluzen", TokenType.USLUZENNEUSLUZEN),
            Map.entry("jelovnik", TokenType.JELOVNIK),
            Map.entry("listaCekanja", TokenType.LISTACEKANJA),

            Map.entry("daceteMi", TokenType.DACETEMI),
            Map.entry("konobar", TokenType.KONOBAR),

            Map.entry("rezervisanSto", TokenType.REZERVISANSTO),
            Map.entry("slobodanSto", TokenType.SLOBODANSTO),
            Map.entry("jescemoNegdeDrugo", TokenType.JESCEMONEGDEDRUGO),
            Map.entry("novaTura", TokenType.NOVATURA),
            Map.entry("dokNeDobijemObrok", TokenType.DOKNEDOBIJEMOBROK),
            Map.entry("radiNesto", TokenType.RADINESTO),

            Map.entry("naplati", TokenType.NAPLATI),
            Map.entry("sto", TokenType.STO),
            Map.entry("naplaceno", TokenType.NAPLACENO),

            Map.entry("recept", TokenType.RECEPT),
            Map.entry("return", TokenType.RETURN),
            Map.entry("glavniObrok", TokenType.GLAVNIOBROK),

            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE),
            Map.entry("usluzen", TokenType.USLUZEN),
            Map.entry("neusluzen", TokenType.NEUSLUZEN),

            Map.entry("je", TokenType.JE),
            Map.entry("ukupno", TokenType.UKUPNO),
            Map.entry("manje", TokenType.MANJE),
            Map.entry("puta", TokenType.PUTA),
            Map.entry("deljeno", TokenType.DELJENO),
            Map.entry("kusur", TokenType.KUSUR)


    );

    public Lexer(String source) {
        this.source = source;
        this.sc = new ScannerCore(source);
    }

    public List<Token> scanTokens() {
        while (!sc.isAtEnd()) {
            sc.beginToken();
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "\0", null, sc.getLine(), sc.getCol(), sc.getCol()));
        return tokens;
    }

    private void scanToken() {
        char c = sc.advance();

        switch (c) {
            case '(' -> add(TokenType.LPAREN);
            case ')' -> add(TokenType.RPAREN);
            case '{' -> add(TokenType.LBRACE);
            case '}' -> add(TokenType.RBRACE);
            case '[' -> add(TokenType.LBRACKET);
            case ']' -> add(TokenType.RBRACKET);
            case ',' -> add(TokenType.SEPARATOR_COMMA);
            case ':' -> add(TokenType.TYPE_COLON);
            case ';' -> add(TokenType.SEMICOLON);
            case '"'  -> stringLiteral();
            case '\'' -> charLiteral();
            case '/' -> {
                if (sc.match('/')) {
                    skipLineComment();
                } else if (sc.match('*')) {
                    skipBlockComment();
                } else {
                    throw error("Operator '/' nije dozvoljen. Koristi 'deljeno'.");
                }
            }
            case '#' -> { skipLineComment(); }

            case '<' -> add(sc.match('=') ? TokenType.LE : TokenType.LT);
            case '>' -> add(sc.match('=') ? TokenType.GE : TokenType.GT);
            case '=' -> add(sc.match('=') ? TokenType.EQ : TokenType.EQUAL);


            case '!' -> {
                if (sc.match('=')) add(TokenType.NEQ);
                else add(TokenType.NOT);
            }
            case '&' -> {
                if (sc.match('&')) add(TokenType.AND);
                else throw error("Očekivano &&");
            }
            case '|' -> {
                if (sc.match('|')) add(TokenType.OR);
                else throw error("Očekivano ||");
            }

            case '\n',' ', '\r', '\t' -> {}
            default -> {
                if (Character.isDigit(c)) number();
                else if (isIdentStart(c)) identifier();
                else throw error("Neocekivani karakter");
            }
        }
    }

    private void number() {
        boolean isDouble = false;

        while (Character.isDigit(sc.peek())) sc.advance();

        if (sc.peek() == '.') {
            isDouble = true;
            sc.advance();
            if (!Character.isDigit(sc.peek()))
                throw error("Očekivan broj posle decimalne tačke");
            while (Character.isDigit(sc.peek())) sc.advance();
        }

        String text = source.substring(sc.getStartIdx(), sc.getCur());

        char nextChar = sc.peek();
        if (Character.isAlphabetic(nextChar)) {
            throw error("Karakter u numeričkom literalu");
        }

        if (isDouble) {
            add(TokenType.DOUBLE_LIT, text);
        } else {
            addLiteralInt(text);
        }
    }


    private void identifier() {
        while (isIdentPart(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        add(type, text);
    }
    // Komentari
    private void skipLineComment() {
        while (!sc.isAtEnd() && sc.peek() != '\n') sc.advance();
    }

    private void skipBlockComment() {
        while (!sc.isAtEnd()) {
            if (sc.peek() == '*') {
                sc.advance();
                if (!sc.isAtEnd() && sc.peek() == '/') { sc.advance(); return; }
            } else {
                sc.advance();
            }
        }
        throw error("Nedovršen blok komentar /* ... */");
    }
    private void stringLiteral() {

        while (!sc.isAtEnd()) {
            char p = sc.peek();
            if (p == '"') {
                sc.advance();
                String lex = source.substring(sc.getStartIdx(), sc.getCur());
                add(TokenType.STRING_LIT, lex);
                return;
            }
            if (p == '\\') {
                sc.advance();
                if (sc.isAtEnd()) throw error("Nedovršen escape u stringu");
                char e = sc.peek();
                switch (e) {
                    case 'n', 't', '0', '"', '\\' -> sc.advance();
                    default -> throw error("Nepoznat escape u stringu: \\" + e);
                }
            } else {
                if (p == '\n') throw error("Novi red unutar stringa");
                sc.advance();
            }
        }
        throw error("Nedovršen string literal");
    }

    private void charLiteral() {

        if (sc.isAtEnd()) throw error("Nedovršen char literal");

        if (sc.peek() == '\\') {
            sc.advance();
            if (sc.isAtEnd()) throw error("Nedovršen escape u char literal-u");
            char e = sc.peek();
            switch (e) {
                case 'n', 't', '0', '\'', '\\' -> sc.advance();
                default -> throw error("Nepoznat escape u char literal-u: \\" + e);
            }
        } else {
            sc.advance();
        }

        if (sc.peek() != '\'') throw error("Očekivan zatvarajući ' u char literal-u");
        sc.advance();
        String lex = source.substring(sc.getStartIdx(), sc.getCur());
        add(TokenType.CHAR_LIT, lex);
    }


    private boolean isIdentStart(char c) { return Character.isLetter(c) || c == '_'; }
    private boolean isIdentPart(char c)  { return isIdentStart(c) || Character.isDigit(c); }

    private void add(TokenType type) {
        String lex = source.substring(sc.getStartIdx(), sc.getCur());
        tokens.add(new Token(type, lex, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void add(TokenType type, String text) {
        tokens.add(new Token(type, text, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralInt(String literal) {
        tokens.add(new Token(TokenType.INT_LIT, literal, Integer.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private RuntimeException error(String msg) {
        String near = source.substring(sc.getStartIdx(), Math.min(sc.getCur(), source.length()));
        return new RuntimeException("LEXER > " + msg + " at " + sc.getStartLine() + ":" + sc.getStartCol() + " near '" + near + "'");
    }
}

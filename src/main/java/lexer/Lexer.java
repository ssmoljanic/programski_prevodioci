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
            case '[' -> add(TokenType.LBRACKET);
            case ']' -> add(TokenType.RBRACKET);
            case ',' -> add(TokenType.SEPARATOR_COMMA);
            case ':' -> add(TokenType.TYPE_COLON);

            case '<' -> add(sc.match('=') ? TokenType.LE : TokenType.LT);
            case '>' -> add(sc.match('=') ? TokenType.GE : TokenType.GT);
            case '=' -> add(TokenType.EQ);
            case '!' -> {
                if (sc.match('=')) add(TokenType.NEQ);
                else throw error("Unexpected '!'");
            }
            case '\n' -> tokens.add(new Token(
                    TokenType.NEWLINE, "\n", null, sc.getStartLine(), sc.getStartCol(), sc.getStartCol()
            ));
            case ' ', '\r', '\t' -> {}
            default -> {
                if (Character.isDigit(c)) number();
                else if (isIdentStart(c)) identifier();
                else throw error("Unexpected character");
            }
        }
    }

    private void number() {
        while (Character.isDigit(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        char nextChar = sc.peek();
        if (Character.isAlphabetic(nextChar)) {
            throw error("Error: Character in int literal");
        }
        addLiteralInt(text);
    }

    private void identifier() {
        while (isIdentPart(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        add(type, text);
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

package parser;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.List;

import static lexer.token.TokenType.*;

public final class RecognizerParser {

    private final List<Token> tokens;
    private int current = 0;

    public RecognizerParser(List<Token> tokens) {
        this.tokens = tokens;
    }


    // program = { top_item } EOF ;
    public void parseProgram() {
        while (!isAtEnd()) {
            parseTopItem();
        }
        consume(EOF, "Ocekivan EOF na kraju programa.");
    }

    // top_item = var_decl | func_def | main_def | stmt ;
    private void parseTopItem() {
        if (check(IZVOLITE)) {
            parseVarDecl();
        } else if (check(RECEPT)) {
            parseFuncDef();
        } else if (check(GLAVNIOBROK)) {
            parseMainDef();
        } else {
            parseStmt();
        }
    }



    // type = base_type | LISTACEKANJA "{" type "}" dims ;
    private void parseType() {
        if (startsBaseType()) {
            parseBaseType();
        } else if (match(LISTACEKANJA)) {
            consume(LBRACE, "Ocekivana '{' posle LISTACEKANJA.");
            parseType();
            consume(RBRACE, "Ocekivana '}' posle tipa u LISTACEKANJA.");
            parseDims();
        } else {
            error(peek(), "Ocekivan tip podataka.");
        }
    }


    private void parseDims() {
        consume(LBRACKET, "Ocekivana '[' u definiciji niza.");
        consume(INT_LIT, "Ocekivan ceo broj kao velicina dimenzije.");
        consume(RBRACKET, "Ocekivana ']' u definiciji niza.");
        while (match(LBRACKET)) {
            consume(INT_LIT, "Ocekivan ceo broj kao velicina dimenzije.");
            consume(RBRACKET, "Ocekivana ']' u definiciji niza.");
        }
    }

    // base_type = PORUDZBINA | RACUN | PREDJELO | USLUZENNEUSLUZEN | JELOVNIK ;
    private void parseBaseType() {
        if (!match(PORUDZBINA, RACUN, PREDJELO, USLUZENNEUSLUZEN, JELOVNIK)) {
            error(peek(), "Ocekivan osnovni tip (porudzbina/racun/predjelo/usluzenNeusluzen/jelovnik).");
        }
    }

    private boolean startsBaseType() {
        return check(PORUDZBINA) || check(RACUN) || check(PREDJELO)
                || check(USLUZENNEUSLUZEN) || check(JELOVNIK);
    }



    // var_decl = IZVOLITE type ident init_opt { "," ident init_opt } ";" ;
    private void parseVarDecl() {
        consume(IZVOLITE, "Ocekivano 'izvolite'.");
        parseType();
        parseIdentInit();
        while (match(SEPARATOR_COMMA)) {
            parseIdentInit();
        }
        consume(SEMICOLON, "Ocekivana ';' na kraju deklaracije.");
    }

    // ident = IDENT ; init_opt = [ ( "=" | JE ) expr ] ;
    private void parseIdentInit() {
        consume(IDENT, "Ocekivan identifikator.");
        parseInitOpt();
    }

    private void parseInitOpt() {
        if (match(EQUAL, JE)) {
            parseExpr();
        }
    }

    // func_def = RECEPT type IDENT "(" [ params ] ")" block ;
    private void parseFuncDef() {
        consume(RECEPT, "Ocekivano 'recept'.");
        parseType();
        consume(IDENT, "Ocekivano ime funkcije.");
        consume(LPAREN, "Ocekivana '('.");
        if (!check(RPAREN)) {
            parseParams();
        }
        consume(RPAREN, "Ocekivana ')'.");
        parseBlock();
    }

    // params = param { "," param } ;
    // param  = type IDENT ;
    private void parseParams() {
        parseParam();
        while (match(SEPARATOR_COMMA)) parseParam();
    }

    private void parseParam() {
        parseType();
        consume(IDENT, "Ocekivano ime parametra.");
    }

    // main_def = GLAVNIOBROK "(" ")" block ;
    private void parseMainDef() {
        consume(GLAVNIOBROK, "Ocekivano 'glavniObrok'.");
        consume(LPAREN, "Ocekivana '('.");
        consume(RPAREN, "Ocekivana ')'.");
        parseBlock();
    }

    // block = "{" { stmt } "}" ;
    private void parseBlock() {
        consume(LBRACE, "Ocekivana '{'.");
        while (!check(RBRACE) && !isAtEnd()) {
            parseStmt();
        }
        consume(RBRACE, "Ocekivana '}' na kraju bloka.");
    }



    // stmt =
    //    var_decl
    //  | print_stmt
    //  | read_stmt
    //  | if_stmt
    //  | while_stmt
    //  | do_while_stmt
    //  | for_stmt
    //  | switch_stmt
    //  | return_stmt
    //  | expr ";" ;
    private void parseStmt() {
        if (check(IZVOLITE)) {
            parseVarDecl();
        } else if (check(KONOBAR)) {
            parsePrintStmt();
        } else if (check(DACETEMI)) {
            parseReadStmt();
        } else if (check(REZERVISANSTO)) {
            parseIfStmt();
        } else if (check(DOKNEDOBIJEMOBROK)) {
            parseWhileStmt();
        } else if (check(RADINESTO)) {
            parseDoWhileStmt();
        } else if (check(NOVATURA)) {
            parseForStmt();
        } else if (check(NAPLATI)) {
            parseSwitchStmt();
        } else if (check(RETURN)) {
            parseReturnStmt();
        } else {
            parseExpr();
            consume(SEMICOLON, "Ocekivana ';' posle izraza.");
        }
    }

    // return_stmt = RETURN [ expr ] ";" ;
    private void parseReturnStmt() {
        consume(RETURN, "Ocekivano 'return'.");
        if (!check(SEMICOLON)) {
            parseExpr();
        }
        consume(SEMICOLON, "Ocekivana ';' posle return-a.");
    }

    // print_stmt = KONOBAR "(" [ arg_list ] ")" ";" ;
    private void parsePrintStmt() {
        consume(KONOBAR, "Ocekivano 'konobar'.");
        consume(LPAREN, "Ocekivana '('.");
        if (!check(RPAREN)) {
            parseArgList();
        }
        consume(RPAREN, "Ocekivana ')'.");
        consume(SEMICOLON, "Ocekivana ';' posle konobar.");
    }

    // read_stmt = DACETEMI "(" IDENT ")" ";" ;
    private void parseReadStmt() {
        consume(DACETEMI, "Ocekivano 'daceteMi'.");
        consume(LPAREN, "Ocekivana '('.");
        consume(IDENT, "Ocekivan identifikator u daceteMi.");
        consume(RPAREN, "Ocekivana ')'.");
        consume(SEMICOLON, "Ocekivana ';' posle daceteMi.");
    }

    // arg_list = expr { "," expr } ;
    private void parseArgList() {
        parseExpr();
        while (match(SEPARATOR_COMMA)) parseExpr();
    }

    // if_stmt =
    //   REZERVISANSTO "(" expr ")" block
    //   { SLOBODANSTO "(" expr ")" block }
    //   [ JESCEMONEGDEDRUGO block ] ;
    private void parseIfStmt() {
        consume(REZERVISANSTO, "Ocekivano 'rezervisanSto'.");
        consume(LPAREN, "Ocekivana '('.");
        parseExpr();
        consume(RPAREN, "Ocekivana ')'.");
        parseBlock();

        while (match(SLOBODANSTO)) {
            consume(LPAREN, "Ocekivana '('.");
            parseExpr();
            consume(RPAREN, "Ocekivana ')'.");
            parseBlock();
        }

        if (match(JESCEMONEGDEDRUGO)) {
            parseBlock();
        }
    }

    // while_stmt = DOKNEDOBIJEMOBROK "(" expr ")" block ;
    private void parseWhileStmt() {
        consume(DOKNEDOBIJEMOBROK, "Ocekivano 'dokNeDobijemObrok'.");
        consume(LPAREN, "Ocekivana '('.");
        parseExpr();
        consume(RPAREN, "Ocekivana ')'.");
        parseBlock();
    }

    // do_while_stmt = RADINESTO block DOKNEDOBIJEMOBROK "(" expr ")" ";" ;
    private void parseDoWhileStmt() {
        consume(RADINESTO, "Ocekivano 'radiNesto'.");
        parseBlock();
        consume(DOKNEDOBIJEMOBROK, "Ocekivano 'dokNeDobijemObrok' posle radiNesto bloka.");
        consume(LPAREN, "Ocekivana '('.");
        parseExpr();
        consume(RPAREN, "Ocekivana ')'.");
        consume(SEMICOLON, "Ocekivana ';' posle do-while.");
    }

    // for_stmt =
    //   NOVATURA "(" for_init_opt ";" [ expr ] ";" for_update_opt ")" block ;
    private void parseForStmt() {
        consume(NOVATURA, "Ocekivano 'novaTura'.");
        consume(LPAREN, "Ocekivana '('.");
        // for_init_opt
        if (!check(SEMICOLON)) {
            parseForInitOpt();
        }
        consume(SEMICOLON, "Ocekivana ';' posle for inicijalizacije.");

        // [ expr ]
        if (!check(SEMICOLON)) {
            parseExpr();
        }
        consume(SEMICOLON, "Ocekivana ';' posle for uslova.");

        // for_update_opt
        if (!check(RPAREN)) {
            parseForUpdateOpt();
        }
        consume(RPAREN, "Ocekivana ')' na kraju zaglavlja for petlje.");
        parseBlock();
    }

    // for_init_opt = [ ( var_decl_in_for | for_expr_list ) ] ;
    private void parseForInitOpt() {
        if (check(IZVOLITE)) {
            parseVarDeclInFor();
        } else {
            parseForExprList();
        }
    }

    // var_decl_in_for = IZVOLITE type ident init_opt { "," ident init_opt }
    private void parseVarDeclInFor() {
        consume(IZVOLITE, "Ocekivano 'izvolite' u for inicijalizaciji.");
        parseType();
        parseIdentInit();
        while (match(SEPARATOR_COMMA)) {
            parseIdentInit();
        }
    }

    // for_update_opt = [ for_expr_list ]
    private void parseForUpdateOpt() {
        parseForExprList();
    }

    // for_expr_list = assign_expr { "," assign_expr }
    private void parseForExprList() {
        parseAssignExpr();
        while (match(SEPARATOR_COMMA)) parseAssignExpr();
    }

    // switch_stmt =
    //   NAPLATI "(" expr ")" "{"
    //       { STO case_label ":" { stmt } }
    //       [ NAPLACENO ":" { stmt } ]
    //   "}" ;
    private void parseSwitchStmt() {
        consume(NAPLATI, "Ocekivano 'naplati'.");
        consume(LPAREN, "Ocekivana '('.");
        parseExpr();
        consume(RPAREN, "Ocekivana ')'.");
        consume(LBRACE, "Ocekivana '{' u switch-u.");

        // { STO case_label ":" { stmt } }
        while (match(STO)) {
            parseCaseLabel();
            consume(TYPE_COLON, "Ocekivana ':' posle case label.");
            while (!check(RBRACE) && !check(STO) && !check(NAPLACENO) && !isAtEnd()) {
                parseStmt();
            }
        }

        // [ NAPLACENO ":" { stmt } ]
        if (match(NAPLACENO)) {
            consume(TYPE_COLON, "Ocekivana ':' posle naplaceno.");
            while (!check(RBRACE) && !isAtEnd()) {
                parseStmt();
            }
        }

        consume(RBRACE, "Ocekivana '}' na kraju switch-a.");
    }

    // case_label = INT_LIT | CHAR_LIT | STRING_LIT | IDENT ;
    private void parseCaseLabel() {
        if (!match(INT_LIT, CHAR_LIT, STRING_LIT, IDENT)) {
            error(peek(), "Ocekivana konstanta ili identifikator u case label.");
        }
    }



    // expr = assign_expr ;
    private void parseExpr() {
        parseAssignExpr();
    }

    // assign_expr = lvalue ( "=" | JE ) assign_expr | or_expr ;
    private void parseAssignExpr() {
        // pokusaj kao lvalue (=|JE) expr, inace or_expr
        if (check(IDENT)) {
            int save = current;
            try {
                parseLValue();
                if (match(EQUAL, JE)) {
                    parseAssignExpr();
                    return;
                } else {
                    current = save;
                }
            } catch (ParseError e) {
                current = save;
            }
        }
        parseOrExpr();
    }

    // lvalue = IDENT { "[" expr "]" } ;
    private void parseLValue() {
        consume(IDENT, "Ocekivan identifikator (lvalue).");
        while (match(LBRACKET)) {
            parseExpr();
            consume(RBRACKET, "Ocekivana ']' u indexiranju.");
        }
    }

    // or_expr = and_expr { OR and_expr } ;
    private void parseOrExpr() {
        parseAndExpr();
        while (match(OR)) parseAndExpr();
    }

    // and_expr = eq_expr { AND eq_expr } ;
    private void parseAndExpr() {
        parseEqExpr();
        while (match(AND)) parseEqExpr();
    }

    // eq_expr = rel_expr { (EQ | NEQ) rel_expr } ;
    private void parseEqExpr() {
        parseRelExpr();
        while (match(EQ, NEQ)) parseRelExpr();
    }

    // rel_expr = add_expr { (LT | LE | GT | GE) add_expr } ;
    private void parseRelExpr() {
        parseAddExpr();
        while (match(LT, LE, GT, GE)) parseAddExpr();
    }

    // add_expr = mul_expr { (UKUPNO | MANJE) mul_expr } ;
    private void parseAddExpr() {
        parseMulExpr();
        while (match(UKUPNO, MANJE)) parseMulExpr();
    }

    // mul_expr = unary { (PUTA | DELJENO | KUSUR) unary } ;
    private void parseMulExpr() {
        parseUnary();
        while (match(PUTA, DELJENO, KUSUR)) parseUnary();
    }

    // unary = (NOT | MANJE) unary | postfix ;
    private void parseUnary() {
        if (match(NOT, MANJE)) {
            parseUnary();
        } else {
            parsePostfix();
        }
    }

    // postfix = primary { "[" expr "]" | "(" [ arg_list ] ")" } ;
    private void parsePostfix() {
        parsePrimary();
        while (true) {
            if (match(LBRACKET)) {
                parseExpr();
                consume(RBRACKET, "Ocekivana ']' u indexiranju.");
            } else if (match(LPAREN)) {
                if (!check(RPAREN)) {
                    parseArgList();
                }
                consume(RPAREN, "Ocekivana ')' posle poziva funkcije.");
            } else {
                break;
            }
        }
    }

    // primary =
    //    INT_LIT | DOUBLE_LIT | STRING_LIT | CHAR_LIT
    //  | USLUZEN | NEUSLUZEN | TRUE | FALSE
    //  | IDENT
    //  | "(" type ")" unary   (kastovanje)
    //  | "(" expr ")" ;
    private void parsePrimary() {
        if (match(INT_LIT, DOUBLE_LIT, STRING_LIT, CHAR_LIT,
                USLUZEN, NEUSLUZEN, TRUE, FALSE, IDENT)) {
            return;
        }
        if (match(LPAREN)) {
            // Proveri da li je kastovanje: (tip) expr
            if (startsBaseType() || check(LISTACEKANJA)) {
                parseType();
                consume(RPAREN, "Ocekivana ')' posle tipa u kastovanju.");
                parseUnary();  // Kastovanje ima viši prioritet
                return;
            }
            // Inače je grupiranje: (expr)
            parseExpr();
            consume(RPAREN, "Ocekivana ')' u zagradi izraza.");
            return;
        }
        error(peek(), "Ocekivan literal, identifikator ili '(' u izrazu.");
    }



    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        error(peek(), message);
        throw new ParseError(message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return type == EOF;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return current == 0 ? tokens.get(0) : tokens.get(current - 1);
    }

    // poslednje ispravna leksema i leksema greske
    private void error(Token errorToken, String message) {
        Token lastOk = (current > 0) ? previous() : null;

        StringBuilder sb = new StringBuilder();
        sb.append("PARSER ERROR > ").append(message).append(" ");

        if (lastOk != null) {
            sb.append("Poslednja ispravna leksema: '")
                    .append(lastOk.lexeme).append("' (tip=")
                    .append(lastOk.type).append(", linija=")
                    .append(lastOk.line).append(", kolona=")
                    .append(lastOk.colStart).append("). ");
        } else {
            sb.append("Poslednja ispravna leksema: nema (greska na samom pocetku) ");
        }

        sb.append("Leksema na kojoj je nastala greska: '")
                .append(errorToken.type == EOF ? "EOF" : errorToken.lexeme)
                .append("' (tip=").append(errorToken.type)
                .append(", linija=").append(errorToken.line)
                .append(", kolona=").append(errorToken.colStart)
                .append(")");

        throw new ParseError(sb.toString());
    }


    public static final class ParseError extends RuntimeException {
        public ParseError(String s) { super(s); }
    }

}

package parser.ast;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;

import static lexer.token.TokenType.*;

public final class ParserAst {

    private final List<Token> tokens;
    private int current = 0;

    public ParserAst(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program = { top_item } EOF ;
    public Ast.Program parseProgram() {
        List<Ast.TopItem> items = new ArrayList<>();
        while (!check(EOF)) {
            items.add(parseTopItem());
        }
        consume(EOF);
        return new Ast.Program(items);
    }

    // top_item = var_decl | func_def | main_def | stmt ;
    private Ast.TopItem parseTopItem() {
        if (check(IZVOLITE)) {
            Stmt.VarDecl vd = parseVarDecl();
            return new Ast.TopVarDecl(vd);
        }
        if (check(RECEPT)) {
            Ast.FuncDef f = parseFuncDef();
            return f;
        }
        if (check(GLAVNIOBROK)) {
            Ast.MainDef m = parseMainDef();
            return m;
        }
        Stmt s = parseStmt();
        return new Ast.TopStmt(s);
    }

    // func_def = RECEPT type IDENT "(" [ params ] ")" block ;
    private Ast.FuncDef parseFuncDef() {
        consume(RECEPT);
        Ast.Type returnType = parseType();
        Token name = consume(IDENT);
        consume(LPAREN);

        List<Ast.Param> params = new ArrayList<>();
        if (!check(RPAREN)) {
            params = parseParams();
        }

        consume(RPAREN);
        Stmt.Block bodyBlock = parseBlock();
        return new Ast.FuncDef(name, params, returnType, bodyBlock.stmts);
    }

    // params = param { "," param } ;
    private List<Ast.Param> parseParams() {
        List<Ast.Param> ps = new ArrayList<>();
        ps.add(parseParam());
        while (match(SEPARATOR_COMMA)) {
            ps.add(parseParam());
        }
        return ps;
    }

    // param = type IDENT ;
    private Ast.Param parseParam() {
        Ast.Type t = parseType();
        Token name = consume(IDENT);
        return new Ast.Param(name, t);
    }

    // main_def = GLAVNIOBROK "(" ")" block ;
    private Ast.MainDef parseMainDef() {
        consume(GLAVNIOBROK);
        consume(LPAREN);
        consume(RPAREN);
        Stmt.Block body = parseBlock();
        return new Ast.MainDef(body.stmts);
    }

    /*
      type =
         base_type
        | LISTACEKANJA "{" type "}" dims ;

      base_type = PORUDZBINA | RACUN | PREDJELO | USLUZENNEUSLUZEN | JELOVNIK ;
    */
    private Ast.Type parseType() {
        if (match(LISTACEKANJA)) {
            consume(LBRACE);
            Ast.Type inner = parseType();
            consume(RBRACE);
            int extraRank = parseDims();
            int totalRank = inner.rank + extraRank;
            return Ast.Type.array(inner.baseType, totalRank);
        } else {
            Token base;
            if (match(PORUDZBINA, RACUN, PREDJELO, USLUZENNEUSLUZEN, JELOVNIK)) {
                base = previous();
            } else {
                throw new IllegalStateException(
                        "ParserAst: expected base type, got " + peek().type
                );
            }
            return Ast.Type.scalar(base);
        }
    }

    // dims = "[" INT_LIT "]" { "[" INT_LIT "]" } ;
    private int parseDims() {
        int rank = 0;
        consume(LBRACKET);
        consume(INT_LIT);
        consume(RBRACKET);
        rank++;
        while (match(LBRACKET)) {
            consume(INT_LIT);
            consume(RBRACKET);
            rank++;
        }
        return rank;
    }

    /*
      var_decl =
        IZVOLITE type ident init_opt
        { "," ident init_opt } ";" ;

      init_opt = [ ( "=" | JE ) expr ] ;
    */
    private Stmt.VarDecl parseVarDecl() {
        consume(IZVOLITE);
        Ast.Type type = parseType();

        List<Token> names = new ArrayList<>();
        List<Expr> inits = new ArrayList<>();

        Token name = consume(IDENT);
        names.add(name);
        inits.add(parseInitOpt());
        while (match(SEPARATOR_COMMA)) {
            Token n = consume(IDENT);
            names.add(n);
            inits.add(parseInitOpt());
        }

        consume(SEMICOLON);
        return new Stmt.VarDecl(type, names, inits);
    }

    private Expr parseInitOpt() {

        if (match(EQUAL, JE)) {
            return parseExpr();
        }
        return null;
    }

    // block = "{" { stmt } "}" ;
    private Stmt.Block parseBlock() {
        consume(LBRACE);
        List<Stmt> stmts = new ArrayList<>();
        while (!check(RBRACE) && !check(EOF)) {
            stmts.add(parseStmt());
        }
        consume(RBRACE);
        return new Stmt.Block(stmts);
    }

    /*
      stmt =
          var_decl
        | print_stmt
        | read_stmt
        | if_stmt
        | while_stmt
        | do_while_stmt
        | for_stmt
        | switch_stmt
        | return_stmt
        | expr ";" ;
    */
    private Stmt parseStmt() {
        if (check(IZVOLITE))          return parseVarDecl();
        if (check(KONOBAR))           return parsePrintStmt();
        if (check(DACETEMI))          return parseReadStmt();
        if (check(REZERVISANSTO))     return parseIfStmt();
        if (check(DOKNEDOBIJEMOBROK)) return parseWhileStmt();
        if (check(RADINESTO))         return parseDoWhileStmt();
        if (check(NOVATURA))          return parseForStmt();
        if (check(NAPLATI))           return parseSwitchStmt();
        if (check(RETURN))            return parseReturnStmt();


        if (check(SLOBODANSTO) || check(JESCEMONEGDEDRUGO)) {
            throw new IllegalStateException(
                    "ParserAst: unexpected " + peek().type
            );
        }

        Expr e = parseExpr();
        consume(SEMICOLON);
        return new Stmt.ExprStmt(e);
    }

    // print_stmt = KONOBAR "(" [ arg_list ] ")" ";" ;
    // arg_list = expr { "," expr } ;
    private Stmt.Print parsePrintStmt() {
        consume(KONOBAR);
        consume(LPAREN);
        List<Expr> args = new ArrayList<>();
        if (!check(RPAREN)) {
            args.add(parseExpr());
            while (match(SEPARATOR_COMMA)) {
                args.add(parseExpr());
            }
        }
        consume(RPAREN);
        consume(SEMICOLON);
        return new Stmt.Print(args);
    }

    // read_stmt = DACETEMI "(" IDENT ")" ";" ;
    private Stmt.Read parseReadStmt() {
        consume(DACETEMI);
        consume(LPAREN);
        Token name = consume(IDENT);
        consume(RPAREN);
        consume(SEMICOLON);
        return new Stmt.Read(name);
    }

    // return_stmt = RETURN [ expr ] ";"
    private Stmt.Return parseReturnStmt() {
        consume(RETURN);
        Expr expr = null;
        if (!check(SEMICOLON)) {
            expr = parseExpr();
        }
        consume(SEMICOLON);
        return new Stmt.Return(expr);
    }

    /*
      if_stmt =
        REZERVISANSTO "(" expr ")" block
          { SLOBODANSTO "(" expr ")" block }
          [ JESCEMONEGDEDRUGO block ] ;
    */
    private Stmt.If parseIfStmt() {
        consume(REZERVISANSTO);
        consume(LPAREN);
        Expr cond = parseExpr();
        consume(RPAREN);
        Stmt.Block firstBlock = parseBlock();
        Stmt.If.Arm ifArm = new Stmt.If.Arm(cond, firstBlock.stmts);

        List<Stmt.If.Arm> elseIfArms = new ArrayList<>();
        while (match(SLOBODANSTO)) {
            consume(LPAREN);
            Expr c = parseExpr();
            consume(RPAREN);
            Stmt.Block b = parseBlock();
            elseIfArms.add(new Stmt.If.Arm(c, b.stmts));
        }

        List<Stmt> elseBlock = null;
        if (match(JESCEMONEGDEDRUGO)) {
            Stmt.Block eb = parseBlock();
            elseBlock = eb.stmts;
        }

        return new Stmt.If(ifArm, elseIfArms, elseBlock);
    }

    // while_stmt = DOKNEDOBIJEMOBROK "(" expr ")" block ;
    private Stmt.While parseWhileStmt() {
        consume(DOKNEDOBIJEMOBROK);
        consume(LPAREN);
        Expr cond = parseExpr();
        consume(RPAREN);
        Stmt.Block body = parseBlock();
        return new Stmt.While(cond, body.stmts);
    }

    // do_while_stmt =
    //    RADINESTO block
    //    DOKNEDOBIJEMOBROK "(" expr ")" ";" ;
    private Stmt.DoWhile parseDoWhileStmt() {
        consume(RADINESTO);
        Stmt.Block body = parseBlock();
        consume(DOKNEDOBIJEMOBROK);
        consume(LPAREN);
        Expr cond = parseExpr();
        consume(RPAREN);
        consume(SEMICOLON);
        return new Stmt.DoWhile(body.stmts, cond);
    }

    /*
      for_stmt =
        NOVATURA "(" for_init_opt ";" [ expr ] ";" for_update_opt ")" block ;

      init: Stmt (VarDecl ili ExprStmt ili null)
      cond: Expr
      update: List<Expr>
    */
    private Stmt.For parseForStmt() {
        consume(NOVATURA);
        consume(LPAREN);


        Stmt init = null;
        if (!check(SEMICOLON)) {
            if (check(IZVOLITE)) {
                init = parseVarDeclInFor();
            } else {
                Expr e = parseExpr();
                init = new Stmt.ExprStmt(e);
            }
        }
        consume(SEMICOLON);


        Expr cond = null;
        if (!check(SEMICOLON)) {
            cond = parseExpr();
        }
        consume(SEMICOLON);


        List<Expr> update = new ArrayList<>();
        if (!check(RPAREN)) {
            update = parseForExprList();
        }

        consume(RPAREN);
        Stmt.Block body = parseBlock();
        return new Stmt.For(init, cond, update, body.stmts);
    }

    // var_decl_in_for = IZVOLITE type ident init_opt { "," ident init_opt } ;
    private Stmt.VarDecl parseVarDeclInFor() {
        consume(IZVOLITE);
        Ast.Type type = parseType();

        List<Token> names = new ArrayList<>();
        List<Expr> inits = new ArrayList<>();

        Token name = consume(IDENT);
        names.add(name);
        inits.add(parseInitOpt());
        while (match(SEPARATOR_COMMA)) {
            Token n = consume(IDENT);
            names.add(n);
            inits.add(parseInitOpt());
        }

        return new Stmt.VarDecl(type, names, inits);
    }

    // for_expr_list = assign_expr { "," assign_expr } ;
    private List<Expr> parseForExprList() {
        List<Expr> list = new ArrayList<>();
        list.add(parseAssignExpr());
        while (match(SEPARATOR_COMMA)) {
            list.add(parseAssignExpr());
        }
        return list;
    }

    /*
      switch_stmt =
        NAPLATI "(" expr ")" "{"
            { STO case_label ":" { stmt } }
            [ NAPLACENO ":" { stmt } ]
        "}" ;
    */
    private Stmt.Switch parseSwitchStmt() {
        consume(NAPLATI);
        consume(LPAREN);
        Expr expr = parseExpr();
        consume(RPAREN);
        consume(LBRACE);

        List<Stmt.Switch.CaseArm> cases = new ArrayList<>();
        while (match(STO)) {
            Token labelTok = parseCaseLabel();
            consume(TYPE_COLON);

            List<Stmt> body = new ArrayList<>();
            while (!check(STO) && !check(NAPLACENO) && !check(RBRACE)) {
                body.add(parseStmt());
            }
            cases.add(new Stmt.Switch.CaseArm(labelTok, body));
        }

        List<Stmt> defaultBlock = null;
        if (match(NAPLACENO)) {
            consume(TYPE_COLON);
            defaultBlock = new ArrayList<>();
            while (!check(RBRACE)) {
                defaultBlock.add(parseStmt());
            }
        }

        consume(RBRACE);
        return new Stmt.Switch(expr, cases, defaultBlock);
    }

    // case_label = INT_LIT | CHAR_LIT | STRING_LIT | IDENT ;
    private Token parseCaseLabel() {
        if (match(INT_LIT, CHAR_LIT, STRING_LIT, IDENT)) {
            return previous();
        }
        throw new IllegalStateException(
                "ParserAst: unexpected token in case label: " + peek().type
        );
    }

    // expr = assign_expr ;
    private Expr parseExpr() {
        return parseAssignExpr();
    }

    /*
      assign_expr =
          lvalue ( "=" | JE ) assign_expr
        | or_expr ;
    */
    private Expr parseAssignExpr() {
        Expr left = parseOrExpr();

        if (match(EQUAL, JE)) {
            Token op = previous();
            Expr value = parseAssignExpr();
            return new Expr.Assign(left, op, value);
        }
        return left;
    }

    // or_expr = and_expr { OR and_expr } ;
    private Expr parseOrExpr() {
        Expr expr = parseAndExpr();
        while (match(OR)) {
            Token op = previous();
            Expr right = parseAndExpr();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // and_expr = eq_expr { AND eq_expr } ;
    private Expr parseAndExpr() {
        Expr expr = parseEqExpr();
        while (match(AND)) {
            Token op = previous();
            Expr right = parseEqExpr();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // eq_expr = rel_expr { (EQ | NEQ) rel_expr } ;
    private Expr parseEqExpr() {
        Expr expr = parseRelExpr();
        while (match(EQ, NEQ)) {
            Token op = previous();
            Expr right = parseRelExpr();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // rel_expr = add_expr { (LT | LE | GT | GE) add_expr } ;
    private Expr parseRelExpr() {
        Expr expr = parseAddExpr();
        while (match(LT, LE, GT, GE)) {
            Token op = previous();
            Expr right = parseAddExpr();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // add_expr = mul_expr { (UKUPNO | MANJE) mul_expr } ;
    private Expr parseAddExpr() {
        Expr expr = parseMulExpr();
        while (match(UKUPNO, MANJE)) {
            Token op = previous();
            Expr right = parseMulExpr();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // mul_expr = unary { (PUTA | DELJENO | KUSUR) unary } ;
    private Expr parseMulExpr() {
        Expr expr = parseUnary();
        while (match(PUTA, DELJENO, KUSUR)) {
            Token op = previous();
            Expr right = parseUnary();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // unary = (NOT | MANJE) unary | postfix ;
    private Expr parseUnary() {
        if (match(NOT, MANJE)) {
            Token op = previous();
            Expr right = parseUnary();
            return new Expr.Unary(op, right);
        }
        return parsePostfix();
    }

    // postfix = primary { "[" expr "]" | "(" [ arg_list ] ")" } ;
    private Expr parsePostfix() {
        Expr expr = parsePrimary();
        while (true) {
            if (match(LBRACKET)) {
                Expr indexExpr = parseExpr();
                consume(RBRACKET);
                if (expr instanceof Expr.Ident id) {
                    List<Expr> indices = new ArrayList<>();
                    indices.add(indexExpr);
                    while (match(LBRACKET)) {
                        Expr idx = parseExpr();
                        consume(RBRACKET);
                        indices.add(idx);
                    }
                    expr = new Expr.Index(id.name, indices);
                } else if (expr instanceof Expr.Index idxExpr) {
                    List<Expr> newIdx = new ArrayList<>(idxExpr.indices);
                    newIdx.add(indexExpr);
                    expr = new Expr.Index(idxExpr.name, newIdx);
                } else {
                    throw new IllegalStateException(
                            "ParserAst: indexing allowed only on identifiers/arrays"
                    );
                }
            } else if (match(LPAREN)) {
                List<Expr> args = new ArrayList<>();
                if (!check(RPAREN)) {
                    args.add(parseExpr());
                    while (match(SEPARATOR_COMMA)) {
                        args.add(parseExpr());
                    }
                }
                consume(RPAREN);
                if (expr instanceof Expr.Ident id) {
                    expr = new Expr.Call(id.name, args);
                } else {
                    throw new IllegalStateException(
                            "ParserAst: function call allowed only on IDENT"
                    );
                }
            } else {
                break;
            }
        }
        return expr;
    }

    // primary =
    //      INT_LIT | DOUBLE_LIT | STRING_LIT | CHAR_LIT
    //    | USLUZEN | NEUSLUZEN
    //    | IDENT
    //    | "(" type ")" unary    (kastovanje)
    //    | "(" expr ")" ;
    private Expr parsePrimary() {
        if (match(INT_LIT)) {
            Token t = previous();
            return new Expr.Literal(t, t.literal);
        }
        if (match(DOUBLE_LIT)) {
            Token t = previous();
            return new Expr.Literal(t, t.literal);
        }
        if (match(STRING_LIT)) {
            Token t = previous();
            return new Expr.Literal(t, t.literal);
        }
        if (match(CHAR_LIT)) {
            Token t = previous();
            return new Expr.Literal(t, t.literal);
        }
        if (match(USLUZEN)) {
            Token t = previous();
            return new Expr.Literal(t, Boolean.TRUE);
        }
        if (match(NEUSLUZEN)) {
            Token t = previous();
            return new Expr.Literal(t, Boolean.FALSE);
        }
        if (match(IDENT)) {
            Token id = previous();
            return new Expr.Ident(id);
        }
        if (match(LPAREN)) {
            Token parenToken = previous();

            // Proveri da li je kastovanje: (tip) expr
            // Kastovanje počinje sa tipom (PORUDZBINA, RACUN, PREDJELO, USLUZENNEUSLUZEN, JELOVNIK, LISTACEKANJA)
            if (check(PORUDZBINA) || check(RACUN) || check(PREDJELO) ||
                check(USLUZENNEUSLUZEN) || check(JELOVNIK) || check(LISTACEKANJA)) {

                Ast.Type targetType = parseType();
                consume(RPAREN);
                Expr expr = parseUnary();  // Kastovanje ima viši prioritet
                return new Expr.Cast(targetType, expr, parenToken);
            }

            // Inače je grupiranje: (expr)
            Expr inner = parseExpr();
            consume(RPAREN);
            return new Expr.Grouping(inner);
        }


        throw new IllegalStateException(
                "ParserAst: unexpected token in primary: " + peek().type
        );
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

    private Token consume(TokenType type) {
        if (check(type)) return advance();
        throw new IllegalStateException(
                "ParserAst: expected " + type + " but found " + peek().type
        );
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
}

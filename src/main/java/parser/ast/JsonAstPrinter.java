package parser.ast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

public final class JsonAstPrinter implements
        Expr.Visitor<JsonNode>,
        Stmt.Visitor<JsonNode> {

    private static final ObjectMapper M = new ObjectMapper();

    // ===== PROGRAM =====

    public String print(Ast.Program p) {
        try {
            ObjectNode root = M.createObjectNode();
            root.put("type", "program");

            ArrayNode items = M.createArrayNode();
            for (Ast.TopItem it : p.items) {
                items.add(printTopItem(it));
            }
            root.set("items", items);

            return M.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode printTopItem(Ast.TopItem it) {
        if (it instanceof Ast.TopVarDecl v) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "topVarDecl");
            o.set("decl", v.decl.accept(this));
            return o;
        }
        if (it instanceof Ast.FuncDef f) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "funcDef");
            o.put("name", f.name.lexeme);

            // return type
            ObjectNode rt = M.createObjectNode();
            if (f.returnType.kind == Ast.Type.Kind.VOID) {
                rt.put("base", "void");
            } else {
                rt.put("base", f.returnType.baseType.lexeme);
            }
            rt.put("rank", f.returnType.rank);
            o.set("returnType", rt);

            // params
            ArrayNode params = M.createArrayNode();
            for (Ast.Param p : f.params) {
                ObjectNode po = M.createObjectNode();
                po.put("name", p.name.lexeme);
                ObjectNode t = M.createObjectNode();
                t.put("base", p.type.baseType.lexeme);
                t.put("rank", p.type.rank);
                po.set("type", t);
                params.add(po);
            }
            o.set("params", params);

            // body
            ArrayNode body = M.createArrayNode();
            for (Stmt s : f.body) {
                body.add(s.accept(this));
            }
            o.set("body", body);
            return o;
        }
        if (it instanceof Ast.MainDef m) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "mainDef");
            ArrayNode body = M.createArrayNode();
            for (Stmt s : m.body) {
                body.add(s.accept(this));
            }
            o.set("body", body);
            return o;
        }
        if (it instanceof Ast.TopStmt ts) {
            ObjectNode o = M.createObjectNode();
            o.put("kind", "topStmt");
            o.set("stmt", ts.stmt.accept(this));
            return o;
        }

        ObjectNode u = M.createObjectNode();
        u.put("kind", "unknownTopItem");
        return u;
    }

    // ===== Expr.Visitor =====

    @Override
    public JsonNode visitLiteral(Expr.Literal e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "literal");

        // Ako lexer već popuni value (npr. za INT_LIT)
        if (e.value != null) {
            if (e.value instanceof Integer i) {
                o.put("int", i);
            } else if (e.value instanceof Double d) {
                o.put("double", d);
            } else if (e.value instanceof Boolean b) {
                o.put("bool", b);
            } else if (e.value instanceof Character c) {
                o.put("char", c.toString());
            } else if (e.value instanceof String s) {
                o.put("string", s);
            } else {
                o.put("raw", String.valueOf(e.value));
            }
            return o;
        }

        // Ako je value == null, oslonimo se na token.type i token.lexeme
        switch (e.token.type) {
            case STRING_LIT -> {
                // skini navodnike ako želiš "čist" tekst
                String lex = e.token.lexeme;
                String inner = lex;
                if (lex.length() >= 2 && lex.charAt(0) == '"' && lex.charAt(lex.length() - 1) == '"') {
                    inner = lex.substring(1, lex.length() - 1);
                }
                o.put("string", inner);
            }
            case CHAR_LIT -> {
                String lex = e.token.lexeme; // npr. 'a' ili '\n'
                o.put("char", lex);
            }
            case INT_LIT -> {
                // fallback, ako nekad ne napuniš literal u lexeru
                try {
                    int v = Integer.parseInt(e.token.lexeme);
                    o.put("int", v);
                } catch (NumberFormatException ex) {
                    o.put("raw", e.token.lexeme);
                }
            }
            default -> {
                // bilo šta drugo, samo izbaci kako piše u kodu
                o.put("raw", e.token.lexeme);
            }
        }

        return o;
    }


    @Override
    public JsonNode visitIdent(Expr.Ident e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "ident");
        o.put("name", e.name.lexeme);
        return o;
    }

    @Override
    public JsonNode visitIndex(Expr.Index e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "index");
        o.put("name", e.name.lexeme);
        ArrayNode idx = M.createArrayNode();
        for (Expr ex : e.indices) {
            idx.add(ex.accept(this));
        }
        o.set("indices", idx);
        return o;
    }

    @Override
    public JsonNode visitGrouping(Expr.Grouping e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "group");
        o.set("expr", e.inner.accept(this));
        return o;
    }

    @Override
    public JsonNode visitCall(Expr.Call e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "call");
        o.put("name", e.callee.lexeme);
        ArrayNode args = M.createArrayNode();
        for (Expr a : e.args) {
            args.add(a.accept(this));
        }
        o.set("args", args);
        return o;
    }

    @Override
    public JsonNode visitBinary(Expr.Binary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "binary");
        o.put("op", e.op.lexeme);
        o.set("left", e.left.accept(this));
        o.set("right", e.right.accept(this));
        return o;
    }

    @Override
    public JsonNode visitUnary(Expr.Unary e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "unary");
        o.put("op", e.op.lexeme);
        o.set("expr", e.expr.accept(this));
        return o;
    }

    @Override
    public JsonNode visitAssign(Expr.Assign e) {
        ObjectNode o = M.createObjectNode();
        o.put("type", "assignExpr");
        o.set("target", e.target.accept(this));
        o.put("op", e.op.lexeme);
        o.set("value", e.value.accept(this));
        return o;
    }

    // ===== Stmt.Visitor =====

    @Override
    public JsonNode visitVarDecl(Stmt.VarDecl s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "varDecl");

        // tip
        ObjectNode t = M.createObjectNode();
        if (s.type.kind == Ast.Type.Kind.VOID) {
            t.put("base", "void");
        } else {
            t.put("base", s.type.baseType.lexeme);
        }
        t.put("rank", s.type.rank);
        o.set("type", t);

        // imena + inicijalizacije
        ArrayNode vars = M.createArrayNode();
        for (int i = 0; i < s.names.size(); i++) {
            ObjectNode v = M.createObjectNode();
            v.put("name", s.names.get(i).lexeme);
            Expr init = s.inits.get(i);
            if (init != null) {
                v.set("init", init.accept(this));
            } else {
                v.set("init", NullNode.getInstance());
            }
            vars.add(v);
        }
        o.set("vars", vars);

        return o;
    }

    @Override
    public JsonNode visitExprStmt(Stmt.ExprStmt s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "exprStmt");
        o.set("expr", s.expr.accept(this));
        return o;
    }

    @Override
    public JsonNode visitPrint(Stmt.Print s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "print"); // konobar(...)
        ArrayNode args = M.createArrayNode();
        for (Expr e : s.args) {
            args.add(e.accept(this));
        }
        o.set("args", args);
        return o;
    }

    @Override
    public JsonNode visitRead(Stmt.Read s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "read"); // daceteMi(...)
        o.put("name", s.name.lexeme);
        return o;
    }

    @Override
    public JsonNode visitIf(Stmt.If s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "if");

        // if grana
        ObjectNode first = M.createObjectNode();
        first.set("cond", s.ifArm.cond.accept(this));
        ArrayNode ifBody = M.createArrayNode();
        for (Stmt st : s.ifArm.block) {
            ifBody.add(st.accept(this));
        }
        first.set("block", ifBody);
        o.set("ifArm", first);

        // elseif grane (slobodanSto)
        ArrayNode elseIfs = M.createArrayNode();
        for (Stmt.If.Arm arm : s.elseIfArms) {
            ObjectNode ar = M.createObjectNode();
            ar.set("cond", arm.cond.accept(this));
            ArrayNode bb = M.createArrayNode();
            for (Stmt st : arm.block) {
                bb.add(st.accept(this));
            }
            ar.set("block", bb);
            elseIfs.add(ar);
        }
        o.set("elseIfArms", elseIfs);

        // else (jescemoNegdeDrugo)
        if (s.elseBlock != null) {
            ArrayNode eb = M.createArrayNode();
            for (Stmt st : s.elseBlock) {
                eb.add(st.accept(this));
            }
            o.set("elseBlock", eb);
        }

        return o;
    }

    @Override
    public JsonNode visitWhile(Stmt.While s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "while");
        o.set("cond", s.cond.accept(this));
        ArrayNode body = M.createArrayNode();
        for (Stmt st : s.body) {
            body.add(st.accept(this));
        }
        o.set("body", body);
        return o;
    }

    @Override
    public JsonNode visitDoWhile(Stmt.DoWhile s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "doWhile");
        ArrayNode body = M.createArrayNode();
        for (Stmt st : s.body) {
            body.add(st.accept(this));
        }
        o.set("body", body);
        o.set("cond", s.cond.accept(this));
        return o;
    }

    @Override
    public JsonNode visitFor(Stmt.For s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "for");

        if (s.init != null) {
            o.set("init", s.init.accept(this));
        } else {
            o.set("init", NullNode.getInstance());
        }

        if (s.cond != null) {
            o.set("cond", s.cond.accept(this));
        } else {
            o.set("cond", NullNode.getInstance());
        }

        ArrayNode upd = M.createArrayNode();
        for (Expr e : s.update) {
            upd.add(e.accept(this));
        }
        o.set("update", upd);

        ArrayNode body = M.createArrayNode();
        for (Stmt st : s.body) {
            body.add(st.accept(this));
        }
        o.set("body", body);

        return o;
    }

    @Override
    public JsonNode visitSwitch(Stmt.Switch s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "switch");
        o.set("expr", s.expr.accept(this));

        ArrayNode cases = M.createArrayNode();
        for (Stmt.Switch.CaseArm c : s.cases) {
            ObjectNode ca = M.createObjectNode();
            ca.put("label", c.label.lexeme);
            ArrayNode body = M.createArrayNode();
            for (Stmt st : c.body) {
                body.add(st.accept(this));
            }
            ca.set("body", body);
            cases.add(ca);
        }
        o.set("cases", cases);

        if (s.defaultBlock != null) {
            ArrayNode def = M.createArrayNode();
            for (Stmt st : s.defaultBlock) {
                def.add(st.accept(this));
            }
            o.set("default", def);
        }

        return o;
    }

    @Override
    public JsonNode visitReturn(Stmt.Return s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "return");
        if (s.expr != null) {
            o.set("expr", s.expr.accept(this));
        } else {
            o.set("expr", NullNode.getInstance());
        }
        return o;
    }

    @Override
    public JsonNode visitBlock(Stmt.Block s) {
        ObjectNode o = M.createObjectNode();
        o.put("stmt", "block");
        ArrayNode body = M.createArrayNode();
        for (Stmt st : s.stmts) {
            body.add(st.accept(this));
        }
        o.set("stmts", body);
        return o;
    }
}


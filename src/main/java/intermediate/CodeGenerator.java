package intermediate;

import parser.ast.*;
import semantic.SemanticAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generator međukoda.
 *
 * Obilazi AST stablo i generiše instrukcije za stek mašinu.
 * Koristi mapu tipova iz semantičke analize za kastovanje.
 */
public class CodeGenerator implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<Expr, Ast.Type> exprTypes;

    private int labelCounter = 0;

    public CodeGenerator(Map<Expr, Ast.Type> exprTypes) {
        this.exprTypes = exprTypes;
    }

    /**
     * Generiše kod za ceo program.
     */
    public List<Instruction> generate(Ast.Program program) {
        instructions.clear();

        // Prvo generišemo kod za sve funkcije
        for (Ast.TopItem item : program.items) {
            if (item instanceof Ast.FuncDef func) {
                generateFunction(func);
            }
        }

        // Zatim generišemo kod za main (glavniObrok)
        for (Ast.TopItem item : program.items) {
            if (item instanceof Ast.MainDef main) {
                generateMain(main);
            }
        }

        // Dodajemo HALT na kraj
        emit(Instruction.OpCode.HALT);

        return new ArrayList<>(instructions);
    }

    /**
     * Generiše kod za funkciju.
     */
    private void generateFunction(Ast.FuncDef func) {
        // Labela za početak funkcije
        emit(Instruction.OpCode.LABEL, func.name.lexeme);

        // Generišemo kod za telo funkcije
        for (Stmt stmt : func.body) {
            stmt.accept(this);
        }

        // Ako funkcija nema eksplicitan return, dodajemo jedan
        // (semantička analiza proverava da void funkcije nemaju return sa vrednošću)
        if (func.returnType.kind == Ast.Type.Kind.VOID) {
            emit(Instruction.OpCode.RET);
        }
    }

    /**
     * Generiše kod za main funkciju (glavniObrok).
     */
    private void generateMain(Ast.MainDef main) {
        emit(Instruction.OpCode.LABEL, "glavniObrok");

        for (Stmt stmt : main.body) {
            stmt.accept(this);
        }
    }

    // ==================== POMOĆNE METODE ====================

    private void emit(Instruction.OpCode opCode) {
        instructions.add(new Instruction(opCode));
    }

    private void emit(Instruction.OpCode opCode, Object operand) {
        instructions.add(new Instruction(opCode, operand));
    }

    private void emit(Instruction.OpCode opCode, Object operand, Object operand2) {
        instructions.add(new Instruction(opCode, operand, operand2));
    }

    private String newLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    /**
     * Vraća tip izraza iz mape tipova.
     */
    private Ast.Type getType(Expr expr) {
        return exprTypes != null ? exprTypes.get(expr) : null;
    }

    // ==================== VISITOR ZA IZRAZE ====================

    @Override
    public Void visitLiteral(Expr.Literal e) {
        // Push literal vrednost na stek
        if (e.value != null) {
            emit(Instruction.OpCode.PUSH, e.value);
        } else {
            // Parsiramo iz tokena
            switch (e.token.type) {
                case INT_LIT -> emit(Instruction.OpCode.PUSH, Integer.parseInt(e.token.lexeme));
                case DOUBLE_LIT -> emit(Instruction.OpCode.PUSH, Double.parseDouble(e.token.lexeme));
                case STRING_LIT -> {
                    String s = e.token.lexeme;
                    if (s.startsWith("\"") && s.endsWith("\"")) {
                        s = s.substring(1, s.length() - 1);
                    }
                    emit(Instruction.OpCode.PUSH, s);
                }
                case CHAR_LIT -> {
                    String c = e.token.lexeme;
                    if (c.startsWith("'") && c.endsWith("'") && c.length() >= 3) {
                        emit(Instruction.OpCode.PUSH, c.charAt(1));
                    } else {
                        emit(Instruction.OpCode.PUSH, c);
                    }
                }
                case USLUZEN -> emit(Instruction.OpCode.PUSH, true);
                case NEUSLUZEN -> emit(Instruction.OpCode.PUSH, false);
                default -> emit(Instruction.OpCode.PUSH, e.token.lexeme);
            }
        }
        return null;
    }

    @Override
    public Void visitIdent(Expr.Ident e) {
        // Učitaj vrednost promenljive na stek
        emit(Instruction.OpCode.LOAD, e.name.lexeme);
        return null;
    }

    @Override
    public Void visitIndex(Expr.Index e) {
        // Učitaj indekse na stek (od poslednjeg ka prvom)
        for (Expr idx : e.indices) {
            idx.accept(this);
        }
        // Učitaj element niza
        emit(Instruction.OpCode.ALOAD, e.name.lexeme, e.indices.size());
        return null;
    }

    @Override
    public Void visitGrouping(Expr.Grouping e) {
        // Samo evaluiraj unutrašnji izraz
        e.inner.accept(this);
        return null;
    }

    @Override
    public Void visitCall(Expr.Call e) {
        // Push argumente na stek
        for (Expr arg : e.args) {
            arg.accept(this);
        }
        // Pozovi funkciju
        emit(Instruction.OpCode.CALL, e.callee.lexeme, e.args.size());
        return null;
    }

    @Override
    public Void visitBinary(Expr.Binary e) {
        // Evaluiraj oba operanda
        e.left.accept(this);
        e.right.accept(this);

        // Emituj odgovarajuću operaciju
        String op = e.op.lexeme;
        switch (op) {
            case "ukupno" -> emit(Instruction.OpCode.ADD);
            case "manje" -> emit(Instruction.OpCode.SUB);
            case "puta" -> emit(Instruction.OpCode.MUL);
            case "deljeno" -> emit(Instruction.OpCode.DIV);
            case "kusur" -> emit(Instruction.OpCode.MOD);
            case "==", "jednakoJe" -> emit(Instruction.OpCode.EQ);
            case "!=", "nijeJednako" -> emit(Instruction.OpCode.NEQ);
            case "<", "manjeOd" -> emit(Instruction.OpCode.LT);
            case "<=", "manjeIliJednako" -> emit(Instruction.OpCode.LE);
            case ">", "veceOd" -> emit(Instruction.OpCode.GT);
            case ">=", "veceIliJednako" -> emit(Instruction.OpCode.GE);
            case "i" -> emit(Instruction.OpCode.AND);
            case "ili" -> emit(Instruction.OpCode.OR);
            default -> throw new RuntimeException("Nepoznat operator: " + op);
        }
        return null;
    }

    @Override
    public Void visitUnary(Expr.Unary e) {
        // Evaluiraj operand
        e.expr.accept(this);

        // Emituj operaciju
        String op = e.op.lexeme;
        switch (op) {
            case "manje" -> emit(Instruction.OpCode.NEG);  // unarni minus je "manje"
            case "!" -> emit(Instruction.OpCode.NOT);      // logička negacija
            default -> throw new RuntimeException("Nepoznat unarni operator: " + op);
        }
        return null;
    }

    @Override
    public Void visitAssign(Expr.Assign e) {
        // Evaluiraj vrednost
        e.value.accept(this);

        // Sačuvaj u promenljivu
        if (e.target instanceof Expr.Ident ident) {
            emit(Instruction.OpCode.STORE, ident.name.lexeme);
        } else if (e.target instanceof Expr.Index idx) {
            // Za niz: prvo indeksi, pa vrednost, pa ASTORE
            for (Expr i : idx.indices) {
                i.accept(this);
            }
            emit(Instruction.OpCode.ASTORE, idx.name.lexeme, idx.indices.size());
        }

        // Assign je izraz koji vraća vrednost, pa ponovo učitamo
        if (e.target instanceof Expr.Ident ident) {
            emit(Instruction.OpCode.LOAD, ident.name.lexeme);
        }

        return null;
    }

    @Override
    public Void visitCast(Expr.Cast e) {
        // Evaluiraj izraz
        e.expr.accept(this);

        // Emituj konverziju
        Ast.Type targetType = e.targetType;
        if (targetType.baseType != null) {
            String base = targetType.baseType.lexeme;
            if (base.equals("porudzbina")) {
                emit(Instruction.OpCode.CAST_TO_INT);
            } else if (base.equals("racun")) {
                emit(Instruction.OpCode.CAST_TO_DOUBLE);
            }
        }

        return null;
    }

    // ==================== VISITOR ZA NAREDBE ====================

    @Override
    public Void visitVarDecl(Stmt.VarDecl s) {
        // Za svaku deklarisanu promenljivu
        for (int i = 0; i < s.names.size(); i++) {
            String name = s.names.get(i).lexeme;
            Expr init = s.inits.get(i);

            if (init != null) {
                // Ima inicijalizacija
                init.accept(this);
                emit(Instruction.OpCode.STORE, name);
            } else {
                // Bez inicijalizacije - postavi default vrednost
                pushDefaultValue(s.type);
                emit(Instruction.OpCode.STORE, name);
            }
        }
        return null;
    }

    private void pushDefaultValue(Ast.Type type) {
        if (type.baseType == null || type.kind == Ast.Type.Kind.VOID) {
            emit(Instruction.OpCode.PUSH, 0);
            return;
        }

        String base = type.baseType.lexeme;
        switch (base) {
            case "porudzbina" -> emit(Instruction.OpCode.PUSH, 0);
            case "racun" -> emit(Instruction.OpCode.PUSH, 0.0);
            case "predjelo" -> emit(Instruction.OpCode.PUSH, "");
            case "jelovnik" -> emit(Instruction.OpCode.PUSH, '\0');
            case "usluzenNeusluzen" -> emit(Instruction.OpCode.PUSH, false);
            default -> emit(Instruction.OpCode.PUSH, 0);
        }
    }

    @Override
    public Void visitExprStmt(Stmt.ExprStmt s) {
        s.expr.accept(this);
        // Ako izraz ostavlja vrednost na steku, odbacujemo je
        emit(Instruction.OpCode.POP);
        return null;
    }

    @Override
    public Void visitPrint(Stmt.Print s) {
        for (Expr arg : s.args) {
            arg.accept(this);
            emit(Instruction.OpCode.PRINT);
        }
        return null;
    }

    @Override
    public Void visitRead(Stmt.Read s) {
        emit(Instruction.OpCode.READ, s.name.lexeme);
        return null;
    }

    @Override
    public Void visitIf(Stmt.If s) {
        String elseLabel = newLabel("else");
        String endLabel = newLabel("endif");

        // if uslov
        s.ifArm.cond.accept(this);
        emit(Instruction.OpCode.JZ, elseLabel);

        // if blok
        for (Stmt stmt : s.ifArm.block) {
            stmt.accept(this);
        }
        emit(Instruction.OpCode.JMP, endLabel);

        // else-if grane
        emit(Instruction.OpCode.LABEL, elseLabel);

        if (!s.elseIfArms.isEmpty()) {
            for (int i = 0; i < s.elseIfArms.size(); i++) {
                Stmt.If.Arm arm = s.elseIfArms.get(i);
                String nextLabel = newLabel("elseif");

                arm.cond.accept(this);
                emit(Instruction.OpCode.JZ, nextLabel);

                for (Stmt stmt : arm.block) {
                    stmt.accept(this);
                }
                emit(Instruction.OpCode.JMP, endLabel);

                emit(Instruction.OpCode.LABEL, nextLabel);
            }
        }

        // else blok
        if (s.elseBlock != null) {
            for (Stmt stmt : s.elseBlock) {
                stmt.accept(this);
            }
        }

        emit(Instruction.OpCode.LABEL, endLabel);
        return null;
    }

    @Override
    public Void visitWhile(Stmt.While s) {
        String startLabel = newLabel("while_start");
        String endLabel = newLabel("while_end");

        emit(Instruction.OpCode.LABEL, startLabel);

        // Uslov
        s.cond.accept(this);
        emit(Instruction.OpCode.JZ, endLabel);

        // Telo
        for (Stmt stmt : s.body) {
            stmt.accept(this);
        }

        emit(Instruction.OpCode.JMP, startLabel);
        emit(Instruction.OpCode.LABEL, endLabel);
        return null;
    }

    @Override
    public Void visitDoWhile(Stmt.DoWhile s) {
        String startLabel = newLabel("dowhile_start");

        emit(Instruction.OpCode.LABEL, startLabel);

        // Telo
        for (Stmt stmt : s.body) {
            stmt.accept(this);
        }

        // Uslov
        s.cond.accept(this);
        emit(Instruction.OpCode.JNZ, startLabel);

        return null;
    }

    @Override
    public Void visitFor(Stmt.For s) {
        String startLabel = newLabel("for_start");
        String endLabel = newLabel("for_end");

        // Inicijalizacija
        if (s.init != null) {
            s.init.accept(this);
        }

        emit(Instruction.OpCode.LABEL, startLabel);

        // Uslov
        if (s.cond != null) {
            s.cond.accept(this);
            emit(Instruction.OpCode.JZ, endLabel);
        }

        // Telo
        for (Stmt stmt : s.body) {
            stmt.accept(this);
        }

        // Inkrement
        for (Expr upd : s.update) {
            upd.accept(this);
            emit(Instruction.OpCode.POP); // Odbaci rezultat
        }

        emit(Instruction.OpCode.JMP, startLabel);
        emit(Instruction.OpCode.LABEL, endLabel);
        return null;
    }

    @Override
    public Void visitSwitch(Stmt.Switch s) {
        String endLabel = newLabel("switch_end");

        // Evaluiraj switch izraz
        s.expr.accept(this);

        // Za svaki case
        List<String> caseLabels = new ArrayList<>();
        for (int i = 0; i < s.cases.size(); i++) {
            caseLabels.add(newLabel("case"));
        }
        String defaultLabel = s.defaultBlock != null ? newLabel("default") : endLabel;

        // Jump tabela
        for (int i = 0; i < s.cases.size(); i++) {
            Stmt.Switch.CaseArm c = s.cases.get(i);
            // Dupliraj switch vrednost
            emit(Instruction.OpCode.PUSH, Integer.parseInt(c.label.lexeme));
            emit(Instruction.OpCode.EQ);
            emit(Instruction.OpCode.JNZ, caseLabels.get(i));
        }

        // Ako ništa ne odgovara, idi na default
        emit(Instruction.OpCode.POP); // Odbaci switch vrednost
        emit(Instruction.OpCode.JMP, defaultLabel);

        // Case blokovi
        for (int i = 0; i < s.cases.size(); i++) {
            emit(Instruction.OpCode.LABEL, caseLabels.get(i));
            emit(Instruction.OpCode.POP); // Odbaci switch vrednost

            for (Stmt stmt : s.cases.get(i).body) {
                stmt.accept(this);
            }
            // Napomena: ovde bi trebalo da ide break logika
            emit(Instruction.OpCode.JMP, endLabel);
        }

        // Default blok
        if (s.defaultBlock != null) {
            emit(Instruction.OpCode.LABEL, defaultLabel);
            for (Stmt stmt : s.defaultBlock) {
                stmt.accept(this);
            }
        }

        emit(Instruction.OpCode.LABEL, endLabel);
        return null;
    }

    @Override
    public Void visitReturn(Stmt.Return s) {
        if (s.expr != null) {
            s.expr.accept(this);
        }
        emit(Instruction.OpCode.RET);
        return null;
    }

    @Override
    public Void visitBlock(Stmt.Block s) {
        for (Stmt stmt : s.stmts) {
            stmt.accept(this);
        }
        return null;
    }
}

package intermediate;

import parser.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeGenerator implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<Expr, Ast.Type> exprTypes;

    private int labelCounter = 0;

    public CodeGenerator(Map<Expr, Ast.Type> exprTypes) {
        this.exprTypes = exprTypes;
    }

    public List<Instruction> generate(Ast.Program program) {
        instructions.clear();

        for (Ast.TopItem item : program.items) {
            if (item instanceof Ast.FuncDef func) {
                generateFunction(func);
            }
        }

        for (Ast.TopItem item : program.items) {
            if (item instanceof Ast.MainDef main) {
                generateMain(main);
            }
        }

        emit(Instruction.OpCode.HALT);

        return new ArrayList<>(instructions);
    }

    private void generateFunction(Ast.FuncDef func) {
        emit(Instruction.OpCode.LABEL, func.name.lexeme);

        for (Stmt stmt : func.body) {
            stmt.accept(this);
        }

        if (func.returnType.kind == Ast.Type.Kind.VOID) {
            emit(Instruction.OpCode.RET);
        }
    }

    private void generateMain(Ast.MainDef main) {
        emit(Instruction.OpCode.LABEL, "glavniObrok");

        for (Stmt stmt : main.body) {
            stmt.accept(this);
        }
    }

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

    private Ast.Type getType(Expr expr) {
        return exprTypes != null ? exprTypes.get(expr) : null;
    }

    @Override
    public Void visitLiteral(Expr.Literal e) {
        if (e.value != null) {
            emit(Instruction.OpCode.PUSH, e.value);
        } else {
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
        emit(Instruction.OpCode.LOAD, e.name.lexeme);
        return null;
    }

    @Override
    public Void visitIndex(Expr.Index e) {
        for (Expr idx : e.indices) {
            idx.accept(this);
        }
        emit(Instruction.OpCode.ALOAD, e.name.lexeme, e.indices.size());
        return null;
    }

    @Override
    public Void visitGrouping(Expr.Grouping e) {
        e.inner.accept(this);
        return null;
    }

    @Override
    public Void visitCall(Expr.Call e) {
        for (Expr arg : e.args) {
            arg.accept(this);
        }
        emit(Instruction.OpCode.CALL, e.callee.lexeme, e.args.size());
        return null;
    }

    @Override
    public Void visitBinary(Expr.Binary e) {
        e.left.accept(this);
        e.right.accept(this);

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
        e.expr.accept(this);

        String op = e.op.lexeme;
        switch (op) {
            case "manje" -> emit(Instruction.OpCode.NEG);
            case "!" -> emit(Instruction.OpCode.NOT);
            default -> throw new RuntimeException("Nepoznat unarni operator: " + op);
        }
        return null;
    }

    @Override
    public Void visitAssign(Expr.Assign e) {
        e.value.accept(this);

        if (e.target instanceof Expr.Ident ident) {
            emit(Instruction.OpCode.STORE, ident.name.lexeme);
        } else if (e.target instanceof Expr.Index idx) {
            for (Expr i : idx.indices) {
                i.accept(this);
            }
            emit(Instruction.OpCode.ASTORE, idx.name.lexeme, idx.indices.size());
        }

        if (e.target instanceof Expr.Ident ident) {
            emit(Instruction.OpCode.LOAD, ident.name.lexeme);
        }

        return null;
    }

    @Override
    public Void visitCast(Expr.Cast e) {
        e.expr.accept(this);

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

    @Override
    public Void visitVarDecl(Stmt.VarDecl s) {
        for (int i = 0; i < s.names.size(); i++) {
            String name = s.names.get(i).lexeme;
            Expr init = s.inits.get(i);

            if (init != null) {
                init.accept(this);
                emit(Instruction.OpCode.STORE, name);
            } else {
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

        s.ifArm.cond.accept(this);
        emit(Instruction.OpCode.JZ, elseLabel);

        for (Stmt stmt : s.ifArm.block) {
            stmt.accept(this);
        }
        emit(Instruction.OpCode.JMP, endLabel);

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

        s.cond.accept(this);
        emit(Instruction.OpCode.JZ, endLabel);

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

        for (Stmt stmt : s.body) {
            stmt.accept(this);
        }

        s.cond.accept(this);
        emit(Instruction.OpCode.JNZ, startLabel);

        return null;
    }

    @Override
    public Void visitFor(Stmt.For s) {
        String startLabel = newLabel("for_start");
        String endLabel = newLabel("for_end");

        if (s.init != null) {
            s.init.accept(this);
        }

        emit(Instruction.OpCode.LABEL, startLabel);

        if (s.cond != null) {
            s.cond.accept(this);
            emit(Instruction.OpCode.JZ, endLabel);
        }

        for (Stmt stmt : s.body) {
            stmt.accept(this);
        }

        for (Expr upd : s.update) {
            upd.accept(this);
            emit(Instruction.OpCode.POP);
        }

        emit(Instruction.OpCode.JMP, startLabel);
        emit(Instruction.OpCode.LABEL, endLabel);
        return null;
    }

    @Override
    public Void visitSwitch(Stmt.Switch s) {
        String endLabel = newLabel("switch_end");

        s.expr.accept(this);

        List<String> caseLabels = new ArrayList<>();
        for (int i = 0; i < s.cases.size(); i++) {
            caseLabels.add(newLabel("case"));
        }
        String defaultLabel = s.defaultBlock != null ? newLabel("default") : endLabel;

        for (int i = 0; i < s.cases.size(); i++) {
            Stmt.Switch.CaseArm c = s.cases.get(i);
            emit(Instruction.OpCode.PUSH, Integer.parseInt(c.label.lexeme));
            emit(Instruction.OpCode.EQ);
            emit(Instruction.OpCode.JNZ, caseLabels.get(i));
        }

        emit(Instruction.OpCode.POP);
        emit(Instruction.OpCode.JMP, defaultLabel);

        for (int i = 0; i < s.cases.size(); i++) {
            emit(Instruction.OpCode.LABEL, caseLabels.get(i));
            emit(Instruction.OpCode.POP);

            for (Stmt stmt : s.cases.get(i).body) {
                stmt.accept(this);
            }
            emit(Instruction.OpCode.JMP, endLabel);
        }

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

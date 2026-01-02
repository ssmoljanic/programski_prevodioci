package semantic;

import lexer.token.Token;
import lexer.token.TokenType;
import parser.ast.Ast;
import parser.ast.Expr;
import parser.ast.Stmt;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantički analizator - prolazi kroz AST i proverava semantičku ispravnost.
 *
 * FAZA 1: Provera okruženja (scope)
 * - Da li postoji glavniObrok?
 * - Da li su sve promenljive/funkcije deklarisane pre korišćenja?
 * - Da li postoje duplikati u istom scope-u?
 *
 * FAZA 2: Provera tipova (type checking)
 * - Da li se tipovi slažu u izrazima?
 * - Da li su uslovi logičkog tipa?
 * - Da li se tipovi argumenata slažu sa parametrima?
 *
 * Koristi Visitor pattern za obilazak AST-a.
 */
public class SemanticAnalyzer implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    // ===== ATRIBUTI =====

    private final SymbolTable symbolTable;
    private boolean hasMain = false;
    private Symbol currentFunction = null;

    // FAZA 4: Mapa koja čuva tipove za sve izraze
    // Koristi IdentityHashMap jer poredimo reference objekata, ne sadržaj
    private final Map<Expr, Ast.Type> exprTypes = new IdentityHashMap<>();

    // ===== KONSTRUKTOR =====

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
    }

    // ===== GLAVNA METODA =====

    public void analyze(Ast.Program program) {
        // PRVI PROLAZ: Registruj sve funkcije
        for (Ast.TopItem item : program.items) {
            if (item instanceof Ast.FuncDef funcDef) {
                registerFunction(funcDef);
            } else if (item instanceof Ast.MainDef) {
                registerMain((Ast.MainDef) item);
            }
        }

        if (!hasMain) {
            throw SemanticError.missingMain();
        }

        // DRUGI PROLAZ: Analiziraj tela funkcija i globalne promenljive
        for (Ast.TopItem item : program.items) {
            analyzeTopItem(item);
        }
    }

    // ===== REGISTRACIJA FUNKCIJA (PRVI PROLAZ) =====

    private void registerFunction(Ast.FuncDef funcDef) {
        String name = funcDef.name.lexeme;
        int line = funcDef.name.line;
        int column = funcDef.name.colStart;

        if (symbolTable.lookupGlobal(name) != null) {
            throw SemanticError.duplicateFunction(name, line, column);
        }

        Symbol funcSymbol = new Symbol(name, funcDef.returnType, funcDef.params, line, column);
        symbolTable.defineGlobal(funcSymbol);
    }

    private void registerMain(Ast.MainDef mainDef) {
        if (hasMain) {
            throw SemanticError.duplicateMain(0, 0);
        }

        hasMain = true;

        Symbol mainSymbol = new Symbol(
            "glavniObrok",
            Ast.Type.voidType(),
            List.of(),
            0, 0
        );
        symbolTable.defineGlobal(mainSymbol);
    }

    // ===== ANALIZA TOP-LEVEL ELEMENATA (DRUGI PROLAZ) =====

    private void analyzeTopItem(Ast.TopItem item) {
        if (item instanceof Ast.TopVarDecl topVarDecl) {
            analyzeVarDecl(topVarDecl.decl);
        } else if (item instanceof Ast.FuncDef funcDef) {
            analyzeFunctionBody(funcDef);
        } else if (item instanceof Ast.MainDef mainDef) {
            analyzeMainBody(mainDef);
        } else if (item instanceof Ast.TopStmt topStmt) {
            analyzeStmt(topStmt.stmt);
        }
    }

    private void analyzeFunctionBody(Ast.FuncDef funcDef) {
        currentFunction = symbolTable.lookupGlobal(funcDef.name.lexeme);
        symbolTable.enterScope("funkcija:" + funcDef.name.lexeme);

        // Registruj parametre
        for (Ast.Param param : funcDef.params) {
            String paramName = param.name.lexeme;
            int line = param.name.line;
            int column = param.name.colStart;

            if (symbolTable.lookupLocal(paramName) != null) {
                throw SemanticError.duplicateVariable(paramName, line, column);
            }

            Symbol paramSymbol = new Symbol(paramName, param.type, line, column);
            symbolTable.define(paramSymbol);
        }

        for (Stmt stmt : funcDef.body) {
            analyzeStmt(stmt);
        }

        symbolTable.exitScope();
        currentFunction = null;
    }

    private void analyzeMainBody(Ast.MainDef mainDef) {
        currentFunction = symbolTable.lookupGlobal("glavniObrok");
        symbolTable.enterScope("glavniObrok");

        for (Stmt stmt : mainDef.body) {
            analyzeStmt(stmt);
        }

        symbolTable.exitScope();
        currentFunction = null;
    }

    // ===== ANALIZA STATEMENT-A =====

    private void analyzeStmt(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitVarDecl(Stmt.VarDecl stmt) {
        analyzeVarDecl(stmt);
        return null;
    }

    private void analyzeVarDecl(Stmt.VarDecl decl) {
        for (int i = 0; i < decl.names.size(); i++) {
            Token nameToken = decl.names.get(i);
            String name = nameToken.lexeme;
            int line = nameToken.line;
            int column = nameToken.colStart;

            if (symbolTable.lookupLocal(name) != null) {
                throw SemanticError.duplicateVariable(name, line, column);
            }

            Symbol varSymbol = new Symbol(name, decl.type, line, column);
            symbolTable.define(varSymbol);

            // FAZA 2: Proveri tip inicijalizatora
            Expr init = decl.inits.get(i);
            if (init != null) {
                Ast.Type initType = getExprType(init);
                if (!TypeChecker.isAssignable(decl.type, initType)) {
                    throw SemanticError.typeMismatch(
                        TypeChecker.getTypeName(decl.type),
                        TypeChecker.getTypeName(initType),
                        line, column
                    );
                }
            }
        }
    }

    @Override
    public Void visitExprStmt(Stmt.ExprStmt stmt) {
        getExprType(stmt.expr);  // Analiziraj i proveri tipove
        return null;
    }

    @Override
    public Void visitPrint(Stmt.Print stmt) {
        for (Expr arg : stmt.args) {
            getExprType(arg);
        }
        return null;
    }

    @Override
    public Void visitRead(Stmt.Read stmt) {
        String name = stmt.name.lexeme;
        int line = stmt.name.line;
        int column = stmt.name.colStart;

        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            throw SemanticError.undeclaredVariable(name, line, column);
        }

        if (symbol.isFunction()) {
            throw SemanticError.undeclaredVariable(name, line, column);
        }

        return null;
    }

    @Override
    public Void visitIf(Stmt.If stmt) {
        // FAZA 2: Proveri da li je uslov boolean
        checkCondition(stmt.ifArm.cond);

        symbolTable.enterScope("if");
        for (Stmt s : stmt.ifArm.block) {
            analyzeStmt(s);
        }
        symbolTable.exitScope();

        for (Stmt.If.Arm arm : stmt.elseIfArms) {
            checkCondition(arm.cond);

            symbolTable.enterScope("else-if");
            for (Stmt s : arm.block) {
                analyzeStmt(s);
            }
            symbolTable.exitScope();
        }

        if (stmt.elseBlock != null && !stmt.elseBlock.isEmpty()) {
            symbolTable.enterScope("else");
            for (Stmt s : stmt.elseBlock) {
                analyzeStmt(s);
            }
            symbolTable.exitScope();
        }

        return null;
    }

    @Override
    public Void visitWhile(Stmt.While stmt) {
        checkCondition(stmt.cond);

        symbolTable.enterScope("while");
        for (Stmt s : stmt.body) {
            analyzeStmt(s);
        }
        symbolTable.exitScope();

        return null;
    }

    @Override
    public Void visitDoWhile(Stmt.DoWhile stmt) {
        symbolTable.enterScope("do-while");
        for (Stmt s : stmt.body) {
            analyzeStmt(s);
        }
        symbolTable.exitScope();

        checkCondition(stmt.cond);

        return null;
    }

    @Override
    public Void visitFor(Stmt.For stmt) {
        symbolTable.enterScope("for");

        if (stmt.init != null) {
            analyzeStmt(stmt.init);
        }

        if (stmt.cond != null) {
            checkCondition(stmt.cond);
        }

        for (Expr update : stmt.update) {
            getExprType(update);
        }

        for (Stmt s : stmt.body) {
            analyzeStmt(s);
        }

        symbolTable.exitScope();

        return null;
    }

    @Override
    public Void visitSwitch(Stmt.Switch stmt) {
        Ast.Type switchType = getExprType(stmt.expr);

        for (Stmt.Switch.CaseArm caseArm : stmt.cases) {
            symbolTable.enterScope("case");
            for (Stmt s : caseArm.body) {
                analyzeStmt(s);
            }
            symbolTable.exitScope();
        }

        if (stmt.defaultBlock != null) {
            symbolTable.enterScope("default");
            for (Stmt s : stmt.defaultBlock) {
                analyzeStmt(s);
            }
            symbolTable.exitScope();
        }

        return null;
    }

    @Override
    public Void visitReturn(Stmt.Return stmt) {
        if (currentFunction == null) {
            return null;
        }

        Ast.Type expectedReturn = currentFunction.type;

        if (stmt.expr != null) {
            Ast.Type actualReturn = getExprType(stmt.expr);

            // FAZA 2: Proveri povratni tip
            if (!TypeChecker.isAssignable(expectedReturn, actualReturn)) {
                throw SemanticError.returnTypeMismatch(
                    TypeChecker.getTypeName(expectedReturn),
                    TypeChecker.getTypeName(actualReturn),
                    0, 0  // TODO: Dodati lokaciju return-a
                );
            }
        } else {
            // return bez vrednosti - funkcija mora biti void
            if (expectedReturn.kind != Ast.Type.Kind.VOID) {
                throw SemanticError.returnTypeMismatch(
                    TypeChecker.getTypeName(expectedReturn),
                    "void",
                    0, 0
                );
            }
        }

        return null;
    }

    @Override
    public Void visitBlock(Stmt.Block stmt) {
        symbolTable.enterScope("block");
        for (Stmt s : stmt.stmts) {
            analyzeStmt(s);
        }
        symbolTable.exitScope();

        return null;
    }

    // ===== POMOĆNA METODA: PROVERA USLOVA =====

    /**
     * Proverava da li je izraz tipa usluzenNeusluzen (boolean).
     * Koristi se za if, while, for, do-while uslove.
     */
    private void checkCondition(Expr cond) {
        Ast.Type condType = getExprType(cond);
        if (!TypeChecker.isBoolean(condType)) {
            // Pokušaj da dobijemo lokaciju iz izraza
            int line = 0, column = 0;
            if (cond instanceof Expr.Ident ident) {
                line = ident.name.line;
                column = ident.name.colStart;
            } else if (cond instanceof Expr.Binary binary) {
                line = binary.op.line;
                column = binary.op.colStart;
            } else if (cond instanceof Expr.Literal lit) {
                line = lit.token.line;
                column = lit.token.colStart;
            }
            throw SemanticError.conditionNotBoolean(line, column);
        }
    }

    // ===== ANALIZA IZRAZA - VRAĆANJE TIPOVA =====

    /**
     * Vraća tip izraza i proverava semantičku ispravnost.
     * Ovo je ključna metoda za FAZU 2.
     *
     * FAZA 4: Takođe čuva tip u mapu exprTypes za tipizirano stablo.
     */
    private Ast.Type getExprType(Expr expr) {
        Ast.Type type;

        if (expr instanceof Expr.Literal lit) {
            type = getLiteralType(lit);

        } else if (expr instanceof Expr.Ident ident) {
            type = getIdentType(ident);

        } else if (expr instanceof Expr.Index index) {
            type = getIndexType(index);

        } else if (expr instanceof Expr.Grouping grouping) {
            type = getExprType(grouping.inner);

        } else if (expr instanceof Expr.Call call) {
            type = getCallType(call);

        } else if (expr instanceof Expr.Binary binary) {
            type = getBinaryType(binary);

        } else if (expr instanceof Expr.Unary unary) {
            type = getUnaryType(unary);

        } else if (expr instanceof Expr.Assign assign) {
            type = getAssignType(assign);

        } else if (expr instanceof Expr.Cast cast) {
            type = getCastType(cast);

        } else {
            type = TypeChecker.VOID;
        }

        // FAZA 4: Sačuvaj tip u mapu
        exprTypes.put(expr, type);

        return type;
    }

    /**
     * Vraća tip literala.
     */
    private Ast.Type getLiteralType(Expr.Literal lit) {
        TokenType tokenType = lit.token.type;

        return switch (tokenType) {
            case INT_LIT -> TypeChecker.PORUDZBINA;
            case DOUBLE_LIT -> TypeChecker.RACUN;
            case STRING_LIT -> TypeChecker.PREDJELO;
            case CHAR_LIT -> TypeChecker.JELOVNIK;
            case USLUZEN, NEUSLUZEN -> TypeChecker.USLUZEN_NEUSLUZEN;
            default -> TypeChecker.VOID;
        };
    }

    /**
     * Vraća tip identifikatora (promenljive).
     */
    private Ast.Type getIdentType(Expr.Ident ident) {
        String name = ident.name.lexeme;
        int line = ident.name.line;
        int column = ident.name.colStart;

        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            throw SemanticError.undeclaredVariable(name, line, column);
        }

        return symbol.type;
    }

    /**
     * Vraća tip pristupa nizu (indeksiranje).
     */
    private Ast.Type getIndexType(Expr.Index index) {
        String name = index.name.lexeme;
        int line = index.name.line;
        int column = index.name.colStart;

        Symbol symbol = symbolTable.lookup(name);
        if (symbol == null) {
            throw SemanticError.undeclaredVariable(name, line, column);
        }

        // Proveri da li je niz
        if (!TypeChecker.isArray(symbol.type)) {
            throw SemanticError.cannotIndex(TypeChecker.getTypeName(symbol.type), line, column);
        }

        // Proveri da li su svi indeksi celobrojni
        for (Expr idx : index.indices) {
            Ast.Type idxType = getExprType(idx);
            if (!TypeChecker.isInteger(idxType)) {
                throw SemanticError.indexNotInteger(line, column);
            }
        }

        // Izračunaj tip rezultata
        Ast.Type resultType = TypeChecker.getArrayElementType(symbol.type, index.indices.size());
        if (resultType == null) {
            throw SemanticError.cannotIndex(TypeChecker.getTypeName(symbol.type), line, column);
        }

        return resultType;
    }

    /**
     * Vraća tip poziva funkcije.
     */
    private Ast.Type getCallType(Expr.Call call) {
        String name = call.callee.lexeme;
        int line = call.callee.line;
        int column = call.callee.colStart;

        Symbol symbol = symbolTable.lookup(name);

        if (symbol == null) {
            throw SemanticError.undeclaredFunction(name, line, column);
        }

        if (!symbol.isFunction()) {
            throw SemanticError.cannotCall(name, line, column);
        }

        // Proveri broj argumenata
        int expected = symbol.params.size();
        int actual = call.args.size();
        if (expected != actual) {
            throw SemanticError.argumentCountMismatch(name, expected, actual, line, column);
        }

        // FAZA 2: Proveri tipove argumenata
        for (int i = 0; i < call.args.size(); i++) {
            Ast.Type argType = getExprType(call.args.get(i));
            Ast.Type paramType = symbol.params.get(i).type;

            if (!TypeChecker.isAssignable(paramType, argType)) {
                throw SemanticError.argumentTypeMismatch(
                    name, i,
                    TypeChecker.getTypeName(paramType),
                    TypeChecker.getTypeName(argType),
                    line, column
                );
            }
        }

        return symbol.type;  // Povratni tip funkcije
    }

    /**
     * Vraća tip binarnog izraza.
     */
    private Ast.Type getBinaryType(Expr.Binary binary) {
        Ast.Type leftType = getExprType(binary.left);
        Ast.Type rightType = getExprType(binary.right);

        TokenType op = binary.op.type;
        int line = binary.op.line;
        int column = binary.op.colStart;

        Ast.Type resultType = null;

        // Aritmetički operatori: ukupno, manje, puta, deljeno, kusur
        if (op == TokenType.UKUPNO || op == TokenType.MANJE ||
            op == TokenType.PUTA || op == TokenType.DELJENO || op == TokenType.KUSUR) {

            resultType = TypeChecker.getArithmeticResultType(leftType, rightType);
            if (resultType == null) {
                throw new SemanticError(
                    SemanticError.ErrorType.TYPE_MISMATCH_ARITHMETIC,
                    "Aritmetički operatori zahtevaju operande istog numeričkog tipa. " +
                    "Dobijeno: " + TypeChecker.getTypeName(leftType) + " i " + TypeChecker.getTypeName(rightType),
                    line, column
                );
            }
        }
        // Relacioni operatori: <, <=, >, >=
        else if (op == TokenType.LT || op == TokenType.LE ||
                 op == TokenType.GT || op == TokenType.GE) {

            resultType = TypeChecker.getRelationalResultType(leftType, rightType);
            if (resultType == null) {
                throw new SemanticError(
                    SemanticError.ErrorType.TYPE_MISMATCH_RELATIONAL,
                    "Relacioni operatori zahtevaju operande istog numeričkog tipa. " +
                    "Dobijeno: " + TypeChecker.getTypeName(leftType) + " i " + TypeChecker.getTypeName(rightType),
                    line, column
                );
            }
        }
        // Operatori jednakosti: ==, !=
        else if (op == TokenType.EQ || op == TokenType.NEQ) {
            resultType = TypeChecker.getEqualityResultType(leftType, rightType);
            if (resultType == null) {
                throw new SemanticError(
                    SemanticError.ErrorType.TYPE_MISMATCH_RELATIONAL,
                    "Operatori jednakosti zahtevaju operande istog tipa. " +
                    "Dobijeno: " + TypeChecker.getTypeName(leftType) + " i " + TypeChecker.getTypeName(rightType),
                    line, column
                );
            }
        }
        // Logički operatori: &&, ||
        else if (op == TokenType.AND || op == TokenType.OR) {
            resultType = TypeChecker.getLogicalResultType(leftType, rightType);
            if (resultType == null) {
                throw new SemanticError(
                    SemanticError.ErrorType.TYPE_MISMATCH_LOGICAL,
                    "Logički operatori zahtevaju operande tipa usluzenNeusluzen. " +
                    "Dobijeno: " + TypeChecker.getTypeName(leftType) + " i " + TypeChecker.getTypeName(rightType),
                    line, column
                );
            }
        }

        return resultType != null ? resultType : TypeChecker.VOID;
    }

    /**
     * Vraća tip unarnog izraza.
     */
    private Ast.Type getUnaryType(Expr.Unary unary) {
        Ast.Type operandType = getExprType(unary.expr);
        TokenType op = unary.op.type;
        int line = unary.op.line;
        int column = unary.op.colStart;

        // Logička negacija: !
        if (op == TokenType.NOT) {
            Ast.Type resultType = TypeChecker.getNotResultType(operandType);
            if (resultType == null) {
                throw new SemanticError(
                    SemanticError.ErrorType.TYPE_MISMATCH_LOGICAL,
                    "Operator '!' zahteva operand tipa usluzenNeusluzen. " +
                    "Dobijeno: " + TypeChecker.getTypeName(operandType),
                    line, column
                );
            }
            return resultType;
        }
        // Unarni minus: -
        else if (op == TokenType.MANJE) {
            Ast.Type resultType = TypeChecker.getUnaryMinusResultType(operandType);
            if (resultType == null) {
                throw new SemanticError(
                    SemanticError.ErrorType.TYPE_MISMATCH_ARITHMETIC,
                    "Unarni minus zahteva numerički operand. " +
                    "Dobijeno: " + TypeChecker.getTypeName(operandType),
                    line, column
                );
            }
            return resultType;
        }

        return operandType;
    }

    /**
     * Vraća tip dodele vrednosti.
     */
    private Ast.Type getAssignType(Expr.Assign assign) {
        Ast.Type targetType = getExprType(assign.target);
        Ast.Type valueType = getExprType(assign.value);

        // FAZA 2: Proveri da li se tipovi slažu
        if (!TypeChecker.isAssignable(targetType, valueType)) {
            int line = 0, column = 0;
            if (assign.target instanceof Expr.Ident ident) {
                line = ident.name.line;
                column = ident.name.colStart;
            }
            throw SemanticError.typeMismatch(
                TypeChecker.getTypeName(targetType),
                TypeChecker.getTypeName(valueType),
                line, column
            );
        }

        return targetType;
    }

    /**
     * Vraća tip kastovanja.
     *
     * FAZA 3: Eksplicitno kastovanje
     * Primer: (racun) x
     *
     * Pravila:
     * - porudzbina → racun: UVEK dozvoljeno
     * - racun → porudzbina: Dozvoljeno (provera vrednosti u runtime)
     * - Ostalo: GREŠKA
     */
    private Ast.Type getCastType(Expr.Cast cast) {
        Ast.Type exprType = getExprType(cast.expr);
        Ast.Type targetType = cast.targetType;

        int line = cast.parenToken.line;
        int column = cast.parenToken.colStart;

        // Proveri da li je kastovanje dozvoljeno
        if (!TypeChecker.isCastAllowed(exprType, targetType)) {
            throw SemanticError.invalidCast(
                TypeChecker.getTypeName(exprType),
                TypeChecker.getTypeName(targetType),
                line, column
            );
        }

        return targetType;
    }

    // ===== VISITOR METODE ZA IZRAZE (delegiraju na getExprType) =====

    @Override
    public Void visitLiteral(Expr.Literal expr) {
        getLiteralType(expr);
        return null;
    }

    @Override
    public Void visitIdent(Expr.Ident expr) {
        getIdentType(expr);
        return null;
    }

    @Override
    public Void visitIndex(Expr.Index expr) {
        getIndexType(expr);
        return null;
    }

    @Override
    public Void visitGrouping(Expr.Grouping expr) {
        getExprType(expr.inner);
        return null;
    }

    @Override
    public Void visitCall(Expr.Call expr) {
        getCallType(expr);
        return null;
    }

    @Override
    public Void visitBinary(Expr.Binary expr) {
        getBinaryType(expr);
        return null;
    }

    @Override
    public Void visitUnary(Expr.Unary expr) {
        getUnaryType(expr);
        return null;
    }

    @Override
    public Void visitAssign(Expr.Assign expr) {
        getAssignType(expr);
        return null;
    }

    @Override
    public Void visitCast(Expr.Cast expr) {
        getCastType(expr);
        return null;
    }

    // ===== POMOĆNE METODE =====

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * FAZA 4: Vraća mapu tipova za sve izraze.
     * Koristi se u JsonAstPrinter za tipizirano stablo.
     */
    public Map<Expr, Ast.Type> getExprTypes() {
        return exprTypes;
    }
}

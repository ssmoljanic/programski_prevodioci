package application;

import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;
import parser.RecognizerParser;
import parser.ast.JsonAstPrinter;
import parser.ast.ParserAst;
import parser.ast.Ast;
import semantic.SemanticAnalyzer;
import semantic.SemanticError;
import intermediate.CodeGenerator;
import intermediate.CodeFormatter;
import intermediate.Instruction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class Application {



    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java main.Application <source-file>");
            System.exit(64);
        }
        Path inputFile = null;
        try {
            inputFile = Paths.get(args[0]);
            String code = Files.readString(inputFile);
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.scanTokens();

            System.out.println(TokenFormatter.formatList(tokens));

            RecognizerParser recognizerParser = new RecognizerParser(tokens);
            recognizerParser.parseProgram();
            System.out.println("Parsing finished successfully");


            ParserAst parser = new ParserAst(tokens);
            Ast.Program prog = parser.parseProgram();

            // ===== SEMANTIČKA ANALIZA =====
            // Proverava: deklaracije, scope, duplikate, glavniObrok...
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(prog);
            System.out.println("Semantic analysis finished successfully");

            // FAZA 4: Tipizirano AST stablo
            JsonAstPrinter astPrinter = new JsonAstPrinter();
            astPrinter.setExprTypes(semanticAnalyzer.getExprTypes());
            String json = astPrinter.print(prog);
            Path out = Path.of("program.json");
            Files.writeString(out, json);
            System.out.println("AST written to: " + out);

            // ===== FAZA 5-6: GENERISANJE MEĐUKODA =====
            CodeGenerator codeGen = new CodeGenerator(semanticAnalyzer.getExprTypes());
            List<Instruction> instructions = codeGen.generate(prog);

            // Formatiranje sa IP adresama
            CodeFormatter formatter = new CodeFormatter(instructions);

            // Ispis na konzolu
            System.out.println(formatter.format());
            System.out.println(formatter.formatLabelTable());

            // Zapisivanje međukoda u fajl (jednostavan format)
            Path codeOut = Path.of("program.icode");
            Files.writeString(codeOut, formatter.formatSimple());
            System.out.println("Intermediate code written to: " + codeOut);
        }
        catch (FileNotFoundException e) {
            System.err.println("File not found: " + inputFile);
            System.exit(65);

        } catch (IOException e) {
            System.err.println("I/O error while reading " + inputFile + ": " + e.getMessage());
            System.exit(66);
        }
        catch (SemanticError e) {
            // Semantička greška - ispiši formatiranu poruku
            System.err.println(e.getFormattedMessage());
            System.exit(67);  // Poseban exit code za semantičke greške
        }
        catch (Exception e) {
            System.err.println("Error: " + escapeVisible(e.getMessage()));
            System.exit(1);
        }
    }

    private static String escapeVisible(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

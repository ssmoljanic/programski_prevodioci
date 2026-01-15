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

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(prog);
            System.out.println("Semantic analysis finished successfully");


            JsonAstPrinter astPrinter = new JsonAstPrinter();
            astPrinter.setExprTypes(semanticAnalyzer.getExprTypes());
            String json = astPrinter.print(prog);
            Path out = Path.of("program.json");
            Files.writeString(out, json);
            System.out.println("AST written to: " + out);


            CodeGenerator codeGen = new CodeGenerator(semanticAnalyzer.getExprTypes());
            List<Instruction> instructions = codeGen.generate(prog);


            CodeFormatter formatter = new CodeFormatter(instructions);


            System.out.println(formatter.format());
            System.out.println(formatter.formatLabelTable());


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

            System.err.println(e.getFormattedMessage());
            System.exit(67);
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

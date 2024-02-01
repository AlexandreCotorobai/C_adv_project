import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.stringtemplate.v4.*;


public class advMain {
   public static void main(String[] args) {

      try {
         if(args.length < 0 || args.length > 3) {
            System.err.println("Usage: java advMain <input file> {optional -o outputfilename}");
            System.exit(1);
         }
         String[] outArr = args[0].split("/");
         String outputFileNameWithoutExtension = outArr[outArr.length-1].split("\\.")[0];
         String outputFilePath;

         // create a CharStream that reads from standard input:
         CharStream input = CharStreams.fromFileName(args[0]);
         // create a lexer that feeds off of input CharStream:
         advLexer lexer = new advLexer(input);
         // create a buffer of tokens pulled from the lexer:
         CommonTokenStream tokens = new CommonTokenStream(lexer);
         // create a parser that feeds off the tokens buffer:
         advParser parser = new advParser(tokens);
         // replace error listener:
         //parser.removeErrorListeners(); // remove ConsoleErrorListener
         //parser.addErrorListener(new ErrorHandlingListener());
         // begin parsing at program rule:
         ParseTree tree = parser.program();
         if (parser.getNumberOfSyntaxErrors() == 0) {
            // print LISP-style tree:
            // System.out.println(tree.toStringTree(parser));
            SemanticVis visitor0 = new SemanticVis();
            visitor0.visit(tree);
            AutomataVis visitor1 = new AutomataVis();
            visitor1.visit(tree);
            PropertiesVis visitor2 = new PropertiesVis();
            visitor2.visit(tree);
            CompilerVis compiler = new CompilerVis();
            ST result = compiler.visit(tree);
            if (args.length == 3 && args[1].equals("-o")) {
               // Output file to specified directory
               outputFilePath = args[2];
            } else {
               // Output file to current working directory
               outputFilePath = System.getProperty("user.dir") + "/" + outputFileNameWithoutExtension + ".py";
            }
            System.out.println(outputFilePath);
            FileWriter output = new FileWriter(outputFilePath);
            output.write(result.render());
            output.close();
                  
            System.out.println("Code Generation Complete");

         }
      }
      catch(IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
      catch(RecognitionException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
}

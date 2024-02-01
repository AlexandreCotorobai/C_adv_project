import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("CheckReturnValue")
public class AutomataVis extends advBaseVisitor<String> {
   private List<String> alphabet;
   private Automata currentAutomata;
   private List<Automata> automataList = new ArrayList<Automata>(); 
   private State currentState;
   private boolean existInitial;

   @Override public String visitDecl(advParser.DeclContext ctx) {
      if (ctx.automata_decl() != null)
      {
         visit(ctx.automata_decl());
      }
      return null;
   }

   @Override public String visitAlphabet_decl(advParser.Alphabet_declContext ctx) {
      List<String> res = new ArrayList<String>();
      for (int i = 0; i < ctx.ALPHABET_LETTER().size(); i++) {
         res.add(ctx.ALPHABET_LETTER(i).getText());
      }
      alphabet = res;
      return null;
   }

   @Override public String visitAutomata_decl(advParser.Automata_declContext ctx) {
      String mod = "";
      if (ctx.automata_mod() != null) {
         mod = ctx.automata_mod().getText();
      }
      String type = ctx.automata_type().getText();
      currentAutomata = new Automata(alphabet, type, mod);

      visitChildren(ctx);

      automataList.add(currentAutomata);
      if (!existInitial) {
         System.out.println("Error: No initial state in automata");
         System.exit(1);
      }
      currentAutomata.verify();

      return null;   
   }

   @Override public String visitAutomata_state_decl_statement(advParser.Automata_state_decl_statementContext ctx) {
      for (int i = 0; i < ctx.ID().size(); i++) {
         String ID = ctx.ID(i).getText();
         currentAutomata.addState(ID);
      }
      return null;
   }

   @Override public String visitAutomata_state_prop_statement(advParser.Automata_state_prop_statementContext ctx) {
      String stateName = ctx.ID().getText();
      currentState = currentAutomata.getState(stateName);
      visitChildren(ctx);
      currentState = null;
      return null;
   }

   @Override public String visitAutomata_transition_alphabet(advParser.Automata_transition_alphabetContext ctx) {
      String ID1 = ctx.ID(0).getText();
      String ID2 = ctx.ID(1).getText();
      for (int i = 0; i < ctx.ALPHABET_LETTER().size(); i++) {
         String letter = ctx.ALPHABET_LETTER(i).getText();
         currentAutomata.getState(ID1).addTransitionOut(letter, ID2);
         currentAutomata.getState(ID2).addTransitionIn(letter, ID1);
      }
      return null;
   }

   @Override public String visitAutomata_transition_epsilon(advParser.Automata_transition_epsilonContext ctx) {
      
      String ID1 = ctx.ID(0).getText();
      String ID2 = ctx.ID(1).getText();

      currentAutomata.getState(ID1).addTransitionOut("epsilon", ID2);
      currentAutomata.getState(ID2).addTransitionIn("epsilon", ID1);
      return null;
   }

   @Override public String visitExpr_bool(advParser.Expr_boolContext ctx) {
      return ctx.getText();
   }

   @Override public String visitKey_value(advParser.Key_valueContext ctx) {
      String ID = ctx.ID().getText();
      String value = visit(ctx.expr(0));
      switch (ID) {
         case "initial":
            if (value.equals("true")) {
               existInitial = true;
            }
            break;
         default:
            break;
      }
      return null;
   }
}

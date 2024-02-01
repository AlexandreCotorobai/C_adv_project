import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("CheckReturnValue")
public class PropertiesVis extends advBaseVisitor<String> {
   Map<String,String> states = new HashMap<String, String>();
   String highlightedState;
   boolean existsHighlighted = false;
   String currentStatebeingTested;
   boolean existsInitial = false;
   String initialState;

   @Override public String visitAutomata_state_decl_statement(advParser.Automata_state_decl_statementContext ctx) {
      for (int i = 0; i < ctx.ID().size(); i++) {
         states.put(ctx.ID(i).getText(), ctx.ID(i).getText());
      }
      return null;
   }

   @Override public String visitAutomata_state_prop_statement(advParser.Automata_state_prop_statementContext ctx) {
      currentStatebeingTested = ctx.ID().getText();
      visitChildren(ctx);
      return null;
      //return res;
   }

   @Override public String visitExpr_bool(advParser.Expr_boolContext ctx) {
      return ctx.getText();
      //return res;
   }

   @Override public String visitKey_value(advParser.Key_valueContext ctx) {
      String value = visit(ctx.expr(0));
      String key = ctx.ID().getText();

      switch (key) {
         case "highlighted":
            if (existsHighlighted) {
               if (value.equals("true")){
                  if (states.get(currentStatebeingTested).equals(highlightedState)){
                     System.out.println("Warning: Redundant code, State was already highlighted. Line: " + ctx.getStart().getLine());
                  } else {
                     System.out.println("Error: Can't have more than one state highlighted, current highlighted state " + highlightedState + " and trying to highlight " + currentStatebeingTested + ". Line: " + ctx.getStart().getLine());
                     System.exit(1);
                  }
               } else {
                  if (states.get(currentStatebeingTested).equals(highlightedState)){
                     highlightedState = "";
                     existsHighlighted = false;
                  } else {
                     System.out.println("Error: Can't have remove highlight because it wasn't highlighted. Line: " + ctx.getStart().getLine());
                     System.exit(1);
               }
            }
         } else {
            if (value.equals("true")){
               highlightedState = states.get(currentStatebeingTested);
               existsHighlighted = true;
            } else {
               System.out.println("Error: There's no highlighted State currently. Line: " + ctx.getStart().getLine());
               System.exit(1);
            }
         }
            break;
      
         case "initial":
            if(existsInitial){
               if (value.equals("true")){
                  if (states.get(currentStatebeingTested).equals(initialState))
                  {
                     System.out.println("Warning: Redundant code, State was already initial. Line: " + ctx.getStart().getLine());
                  } else {
                     System.out.println("Error: Can't have more than one initial state. Line: " + ctx.getStart().getLine());
                     System.exit(1);
                  }
               } else {
                  if (states.get(currentStatebeingTested).equals(initialState)){
                     initialState = "";
                     existsInitial = false;
                  } else {
                     System.out.println("Error: Can't have remove initial because it wasn't initial. Line: " + ctx.getStart().getLine());
                     System.exit(1);
                  }
               }
            } else {
               if (value.equals("true")){
                  initialState = states.get(currentStatebeingTested);
                  existsInitial = true;
               } else {
                  System.out.println("Error: There's no initial State currently. Line: " + ctx.getStart().getLine());
                  System.exit(1);
               }
            }
            break;
         default:
            break;
      }
      return null;
   }

   @Override public String visitViewport_show_var(advParser.Viewport_show_varContext ctx) {
      if (ctx.ID() != null){
         currentStatebeingTested = ctx.ID().getText();
         visitChildren(ctx);
         currentStatebeingTested = "";
      }
      return null;
   }

   @Override public String visitViewport_state_assign(advParser.Viewport_state_assignContext ctx) {
      String ID = ctx.ID().getText();
      String state = visit(ctx.expr());

      states.put(ID, state);

      return null;
   }
}

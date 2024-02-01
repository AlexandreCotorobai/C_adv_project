import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.TokenStream;
import org.antlr.v4.parse.ANTLRParser.parserRule_return;

@SuppressWarnings("CheckReturnValue")
public class SemanticVis extends advBaseVisitor<String> {
   private List<String> alphabet;
   private Map<Integer, HashMap<String, Boolean>> varsMap = new HashMap<Integer, HashMap<String, Boolean>>();
   private Map<Integer, HashMap<String, String>> varsType = new HashMap<Integer, HashMap<String, String>>();
   private Integer forDepth = 0;
   private Boolean temporaryPointDeclared = false;
   private String temporaryLabelVar = "";
   private boolean testingWindowSizeNotCoords = false;
   private List<String> colors = new ArrayList<String>(Arrays.asList("gray", "red", "green", "blue", "yellow", "purple", "orange", "brown", "black", "white", "pink"));
   private List<String> lineTypes = new ArrayList<String>(Arrays.asList("solid", " dashed", "dotted"));
   private Boolean temporaryWordDeclared = false;
   private Boolean temporaryStateDeclared = false;
   private boolean onePropmtperRead = false;
   @Override public String visitProgram(advParser.ProgramContext ctx) {
      varsMap.put(0, new HashMap<String, Boolean>());
      varsType.put(0, new HashMap<String, String>());
      return visitChildren(ctx);
   }

   @Override public String visitAlphabet_decl(advParser.Alphabet_declContext ctx) {
      List<String> res = new ArrayList<String>();
      for (int i = 0; i < ctx.ALPHABET_LETTER().size(); i++) {
         if (res.contains(ctx.ALPHABET_LETTER(i).getText())) {
            System.out.println("Error: Alphabet letter " + ctx.ALPHABET_LETTER(i).getText() + " already exists. Line: " + ctx.start.getLine());
            System.exit(1);
         }
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
      if(ctx.automata_type().getText().equals("NFA")){
         if (mod.equals("Complete")) {
            System.out.println("Error: NFA cannot be complete. Line: " + ctx.start.getLine());
            System.exit(1);
         }
      }
      
      String ID = ctx.ID().getText();

      if(getAllVars().containsKey(ID)){
         System.out.println("Error: Variable " + ID + " already exists. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      varsMap.get(forDepth).put(ID, true);
      varsType.get(forDepth).put(ID, "automata");
      visit(ctx.automata_statement());
      return null;
   }


   @Override public String visitAutomata_state_decl_statement(advParser.Automata_state_decl_statementContext ctx) {
      for (int i = 0; i < ctx.ID().size(); i++) {
         String ID = ctx.ID(i).getText();
         if(getAllVars().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " already exists. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         varsMap.get(forDepth).put(ID, true);
         varsType.get(forDepth).put(ID, "state");
      }
      return null;

   }

   @Override public String visitAutomata_state_prop_statement(advParser.Automata_state_prop_statementContext ctx) {
      String ID = ctx.ID().getText();
      if(!getAllVars().containsKey(ID)){
         System.out.println("Error: Variable " + ID + " does not exist. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if(!varsType.get(forDepth).get(ID).equals("state")){
         System.out.println("Error: Variable " + ID + " is not a state. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      
      if(visit(ctx.list_key_value()) != "state")
      {
         System.out.println("Error: Invalid properties for state " + ID + ". Line: " + ctx.start.getLine());
         System.exit(1);
      }  

      return null;

   }


   @Override public String visitAutomata_transition_alphabet(advParser.Automata_transition_alphabetContext ctx) {
      String ID1 = ctx.ID(0).getText();
      String ID2 = ctx.ID(1).getText();

      if(!getAllVars().containsKey(ID1)){
         System.out.println("Error: State " + ID1 + " does not exist. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!varsType.get(forDepth).get(ID1).equals("state")){
         System.out.println("Error: Variable " + ID1 + " is not a state. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if(!getAllVars().containsKey(ID2)){
         System.out.println("Error: State " + ID2 + " does not exist. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!varsType.get(forDepth).get(ID2).equals("state")){
         System.out.println("Error: Variable " + ID2 + " is not a state. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      for (int i = 0; i < ctx.ALPHABET_LETTER().size(); i++){
         if (!alphabet.contains(ctx.ALPHABET_LETTER(i).getText())){
            System.out.println("Error: Alphabet letter " + ctx.ALPHABET_LETTER(i).getText() + " does not exist. Line: " + ctx.start.getLine());
            System.exit(1);
         }
      }

      String stateCombo = "<" + ID1 + "," + ID2 + ">";
      if (getAllVars().containsKey(stateCombo)){
         System.out.println("Error: Transition " + ID1 + " -> " + ID2 + " already exists. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      varsMap.get(forDepth).put(stateCombo, false);
      varsType.get(forDepth).put(stateCombo, "transitionline");
      return null;
   }


   @Override public String visitAutomata_for_statement(advParser.Automata_for_statementContext ctx) {
      forDepth++;
      varsMap.put(forDepth, new HashMap<String, Boolean>());
      varsType.put(forDepth, new HashMap<String, String>());
      visit(ctx.for_statement());
      visit(ctx.automata_statement());
      varsMap.remove(forDepth);
      varsType.remove(forDepth);
      forDepth--;
      return null;

   }

   @Override public String visitView_decl(advParser.View_declContext ctx) {
      String viewID = ctx.ID(0).getText();
      String automataID = ctx.ID(1).getText();

      if(getAllVars().containsKey(viewID)){
         System.out.println("Error: Variable " + viewID + " already exists. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if(!getAllVars().containsKey(automataID)){
         System.out.println("Error: Variable " + automataID + " does not exist. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      
      if (!varsType.get(forDepth).get(automataID).equals("automata")){
         System.out.println("Error: Variable " + automataID + " is not an automata. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      varsMap.get(forDepth).put(viewID, true);
      varsType.get(forDepth).put(viewID, "view");
      
      visit(ctx.view_statement());
      return null;
   }

   @Override public String visitView_place_expr(advParser.View_place_exprContext ctx) {
      if (ctx.view_place_expr().size() != 0){
         for (int i = 0; i < ctx.view_place_expr().size(); i++){
            visit(ctx.view_place_expr(i));
         }
         return null;
      }
      
      if (ctx.view_label_statement() != null){
         visit(ctx.view_label_statement());
         String atID = ctx.ID().getText();
         if (!getAllVars().containsKey(atID)){
            System.out.println("Error: Variable " + atID + " does not exist. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         return null;
         
      } else {
         String ID = ctx.ID().getText();
         
         if (!getAllVars().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " does not exist. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         String type = visit(ctx.expr());
         if (!type.equals("coord")){
            System.out.println("Error: Invalid type for view place. Line: " + ctx.start.getLine());
            System.exit(1);
         }
      }

      return null;
   }

   @Override public String visitView_label_statement(advParser.View_label_statementContext ctx) {
      String ID = visit(ctx.state_combo());
      if (!getAllVars().containsKey(ID)){
         System.out.println("Error: Variable " + ID + " does not exist. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!getAllVarsTypes().get(ID).equals("transitionline")){
         System.out.println("Error: Variable " + ID + " is not a state or transitionline. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      temporaryLabelVar = ID;
      visit(ctx.label());
      temporaryLabelVar = "";
      return null;
   }

   // @Override public String visitView_point_decl_statement(advParser.View_point_decl_statementContext ctx) {
   //    String res = null;
   //    return visitChildren(ctx);
   
   // }

   @Override public String visitView_point_decl(advParser.View_point_declContext ctx) {
      if (ctx.view_point_decl().size() != 0){
         for (int i = 0; i < ctx.view_point_decl().size(); i++){
            visit(ctx.view_point_decl(i));
         }
         return null;
      }

      if (ctx.view_point_assign() != null){
         temporaryPointDeclared = true;
         visit(ctx.view_point_assign());
         temporaryPointDeclared = false;
         return null;
      } else {
         String ID = ctx.ID().getText();
         if (getAllVars().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " already exists. Line: " + ctx.start.getLine());
            System.exit(1);
         }

         varsMap.get(forDepth).put(ID, false);
         varsType.get(forDepth).put(ID, "coord");
         return null;
      }
   }


   @Override public String visitView_point_assign(advParser.View_point_assignContext ctx) {
      String ID = ctx.ID().getText();

      if (temporaryPointDeclared){
         if (getAllVars().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " already exists. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         varsMap.get(forDepth).put(ID, true);
         varsType.get(forDepth).put(ID, "coord");
      } else {
         if (!getAllVars().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " does not exist. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         if (!varsType.get(forDepth).get(ID).equals("coord")){
            System.out.println("Error: Variable " + ID + " is not a coord. Line: " + ctx.start.getLine());
            System.exit(1);
         }

         varsMap.get(forDepth).put(ID, true);
         varsType.get(forDepth).put(ID, "coord");
      }

      String type = visit(ctx.expr());
      if (!type.equals("coord") && !type.equals("state")){
         System.out.println("Error: Invalid value for view point. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      return null;
   }

   @Override public String visitView_line_statement(advParser.View_line_statementContext ctx) {
      String ID = ctx.state_combo().getText();

      if (!getAllVars().containsKey(ID)){
         System.out.println("Error: Transition " + ID + " doesn't exist. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      varsMap.get(forDepth).put(ID, true);

      visit(ctx.view_line());
      return null;
   }

   @Override public String visitView_line(advParser.View_lineContext ctx) {
      if (ctx.view_line().size() != 0){
         for (int i = 0; i < ctx.view_line().size(); i++){
            visit(ctx.view_line(i));
         }
         return null;
      }

      String ID = ctx.ID().getText();

      if (!getAllVars().containsKey(ID)){
         System.out.println("Error: Variable " + ID + " does not exist. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!getAllVarsTypes().get(ID).equals("coord")){
         System.out.println("Error: Variable " + ID + " is not declared point. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      return null;
   }

   @Override public String visitView_grid_decl_statement(advParser.View_grid_decl_statementContext ctx) {
      String ID = ctx.ID().getText();

      if (getAllVars().containsKey(ID)){
         System.out.println("Error: Variable " + ID + " already exists. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      varsMap.get(forDepth).put(ID, true);
      varsType.get(forDepth).put(ID, "grid");

      testingWindowSizeNotCoords = true;
      String sizeType = visit(ctx.expr());
      testingWindowSizeNotCoords = false;
      if (!sizeType.equals("intCoord"))
      {
         System.out.println("Error: Invalid type for grid, must be ints. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (ctx.list_key_value() != null){
         String type = visit(ctx.list_key_value());
         if (!type.equals("grid")){
            System.out.println("Error: Invalid properties " + type + "for grid. Line: " + ctx.start.getLine());
            System.exit(1);
         }
      }

      return null;
   }

   @Override public String visitExpr_add_sub(advParser.Expr_add_subContext ctx) {
      return CalculateValidOperations(visit(ctx.expr(0)), visit(ctx.expr(1)), ctx.op.getText());
   }


   @Override public String visitExpr_number(advParser.Expr_numberContext ctx) {
      if (ctx.INT() != null){
         return "int";
      } else {
         return "float";
      }
   }

   @Override public String visitExpr_string(advParser.Expr_stringContext ctx) {
      return "string";
   }

   @Override public String visitExpr_radials(advParser.Expr_radialsContext ctx) {
      return "coord";
   }

   @Override public String visitExpr_paren(advParser.Expr_parenContext ctx) {
      return visit(ctx.expr());
   }

   @Override public String visitExpr_set(advParser.Expr_setContext ctx) {
      String[] types = new String[ctx.expr().size()];
      for (int i = 0; i < ctx.expr().size(); i++){
         types[i] = visit(ctx.expr(i));
      }

      String type = types[0];

      for (int i = 1; i < types.length; i++){
         if (!types[i].equals(type)){
            System.out.println("Error: Set contains multiple types. Line: " + ctx.start.getLine());
            System.exit(1);
         }
      }

      return type;
   }

   @Override public String visitExpr_alphabetletter(advParser.Expr_alphabetletterContext ctx) {
      return "letter";
   }

   @Override public String visitExpr_unary(advParser.Expr_unaryContext ctx) {
      String exprType = visit(ctx.expr());

      switch (exprType) {
         case "coord":
         case "int":
         case "float":
            return exprType;   
         default:
            System.out.println("Error: Invalid type for unary operation. Line: " + ctx.start.getLine());
            System.exit(1);
            break;
      }

      return null;
   }

   @Override public String visitExpr_cartesian(advParser.Expr_cartesianContext ctx) {
      if (testingWindowSizeNotCoords)
      {
         String firstType = visit(ctx.expr(0));
         String secondType = visit(ctx.expr(1));
         if (firstType.equals("int") && secondType.equals("int"))
         {
            return "intCoord"; // this used for window sizes
         }
         else
         {
            return "coord";
         }
      }
      else 
      {
         String firstType = visit(ctx.expr(0));
         String secondType = visit(ctx.expr(1));
         if ((!firstType.equals("int") && !firstType.equals("float")) || (!secondType.equals("int") && !secondType.equals("float")))
         {
            System.out.println("Error: Invalid type for cartesian operation. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         return "coord";
      }
   }

   @Override public String visitExpr_mul_div(advParser.Expr_mul_divContext ctx) {
      return CalculateValidOperations(visit(ctx.expr(0)), visit(ctx.expr(1)), ctx.op.getText());
   }

   @Override public String visitExpr_id(advParser.Expr_idContext ctx) {
      String ID = ctx.ID().getText();

      if (lineTypes.contains(ID))
         return "line";

      if (colors.contains(ID))
         return "color";

      if (ID.equals("left")){
         return "left";
      }

      if (ID.equals("right")){
         return "right";
      }

      if (ID.equals("above")){
         return "above";
      }

      if (ID.equals("below")){
         return "below";
      }

      if (!getAllVars().containsKey(ID)){
         System.out.println("Error: Variable " + ID + " not declared at Line " + ctx.getStart().getLine() + ", did you mean one of these colors or line types? " + colors + "; " + lineTypes);
         System.exit(1);
      }

      if (!getAllVars().get(ID))
      {
         System.out.println("Error: Variable " + ID + " not initialized. Line: " + ctx.start.getLine());
         System.exit(1);
      }
   
      if (getAllVarsTypes().get(ID).equals("state"))
      {
         if (ctx.getParent() instanceof advParser.Expr_parenContext)
         {
            return "coord";
         }
         else
         {
            return "state";
         }
      }
      return getAllVarsTypes().get(ID);
   }

   @Override public String visitExpr_read(advParser.Expr_readContext ctx) {
      if (ctx.list_key_value() != null){
         String type = visit(ctx.list_key_value());
         if (!type.equals("read")){
            System.out.println("Error: Invalid properties " + type + "for read. Line: " + ctx.start.getLine());
            System.exit(1);
         }
      }
      return "read";
   }

   @Override public String visitExpr_bool(advParser.Expr_boolContext ctx) {
      return "bool";
   }

   @Override public String visitList_key_value(advParser.List_key_valueContext ctx) {
      onePropmtperRead = false;
      String[] listTypes = new String[ctx.key_value().size()];
      for (int i = 0; i < ctx.key_value().size(); i++) {
         listTypes[i] = visit(ctx.key_value(i));
      }

      String type = listTypes[0];

      for (int i = 1; i < listTypes.length; i++) {
         if (!listTypes[i].equals(type)) {
            System.out.println("Error: List contains properties of multiple types. Line " + ctx.getStart().getLine() + " Type" + type + " and " + listTypes[i]);
            System.exit(0);
         }
      }

      return type;

   }

   @Override public String visitKey_value(advParser.Key_valueContext ctx) {
      String key = ctx.ID().getText();
      String[] values = new String[ctx.expr().size()];
      for (int i = 0; i < ctx.expr().size(); i++) {
         values[i] = visit(ctx.expr(i));
      }
      Boolean leftRight = false;
      Boolean topBottom = false;
      switch (key) {
         case "initial":
         case "accepting":
            if (values.length == 1 && values[0].equals("bool")) {
               return "state"; // properties only for states
            } else {
               System.out.println("Error: " + key + " must be a boolean. Line: " + ctx.start.getLine());
               System.exit(1);
            }
            break;
         case "align":
            if (values.length > 2){
               System.out.println("Error: " + key + " must have 2 or less arguments. Line: " + ctx.start.getLine());
               System.exit(1);
            }

            if (values.length == 1){
               if (!values[0].equals("left") && !values[0].equals("right") && !values[0].equals("above") && !values[0].equals("below")){
                  System.out.println("Error: " + key + " must have values left, right, above or below. Line: " + ctx.start.getLine());
                  System.exit(1);
               }
            } else {
               for(int i = 0; i < values.length; i++)
               {
                  if (values[i].equals("left") || values[i].equals("right")){ 
                     if (leftRight){
                        System.out.println("Error: " + key + " must have only one left or right value. Line: " + ctx.start.getLine());
                        System.exit(1);
                     }
                     leftRight = true;
                  }

                  if (values[i].equals("above") || values[i].equals("below")){ 
                     if (topBottom){
                        System.out.println("Error: " + key + " must have only one above or below value. Line: " + ctx.start.getLine());
                        System.exit(1);
                     }
                     topBottom = true;
                  }
               }
            }
            return "label";
         case "step":
         case "margin":
               if (values.length == 1 && (values[0].equals("int") || values[0].equals("float"))) {
                  return "grid"; 
               } else {
                  System.out.println("Error: " + key + " must be an integer or float. Line: " + ctx.start.getLine());
                  System.exit(1);
               }
            break;
         case "color":
            if (values.length == 1 && values[0].equals("color")) {
               return "grid"; 
            } else {
               System.out.println("Error: " + key + " must be a color. Line: " + ctx.start.getLine());
               System.exit(1);
            }
            break;
         case "line":
            if (values.length == 1 && values[0].equals("line")) {
               return "grid"; 
            } else {
               System.out.println("Error: " + key + " must be a line. Line: " + ctx.start.getLine());
               System.exit(1);
            }
            break;
         case "prompt":
            if (values.length == 1 && values[0].equals("string")) {
               if (onePropmtperRead){
                  System.out.println("Error: There can only be on prompt. Line: " + ctx.start.getLine());
                  System.exit(1);
               }
               onePropmtperRead = true;
               return "read";
            } else {
               System.out.println("Error: " + key + " must be a string. Line: " + ctx.start.getLine());
               System.exit(1);
            }
            break;
         case "highlighted":
            if (values.length == 1 && values[0].equals("bool")) {
               return "statetransitionline"; 
            } else {
               System.out.println("Error: " + key + " must be a boolean. Line: " + ctx.start.getLine());
               System.exit(1);
            }
            break;
         default:
            System.out.println("Error: " + key + " is not a valid property. Line: " + ctx.start.getLine());
            System.exit(1);
      }
      return null;
   }

   @Override public String visitLabel(advParser.LabelContext ctx) {

      if(!visit(ctx.list_key_value()).equals("label")){
         System.out.println("Error: Invalid properties of Label for" + temporaryLabelVar + ". Line: " + ctx.start.getLine());
         System.exit(1);
      }
      return null;
   }

   @Override public String visitAnimation_decl(advParser.Animation_declContext ctx) {
      String ID = ctx.ID().getText();

      if (getAllVarsTypes().containsKey(ID)) {
         System.out.println("Error: Variable " + ID + " already declared. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      varsMap.get(forDepth).put(ID, true);
      varsType.get(forDepth).put(ID, "animation");
      
      visitChildren(ctx);
      
      return null;
   }

   @Override public String visitAnimation_viewport_decl_statement(advParser.Animation_viewport_decl_statementContext ctx) {
      String ID = ctx.ID(0).getText();
      String view = ctx.ID(1).getText();
      testingWindowSizeNotCoords = true;
      String initalCoordsType = visit(ctx.expr());
      testingWindowSizeNotCoords = false;
      if (getAllVarsTypes().containsKey(ID)) {
         System.out.println("Error: Variable " + ID + " already declared. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!getAllVars().containsKey(view)){
         System.out.println("Error: Variable " + view + " not declared. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!getAllVarsTypes().get(view).equals("view")){
         System.out.println("Error: Variable " + view + " is not a view. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!initalCoordsType.equals("intCoord")){
         System.out.println("Error: Variable " + ID + " must be initialized with integers. Line" + ctx.start.getLine());
         System.exit(1);
      }

      visit(ctx.viewport_size_expr());

      varsMap.get(forDepth).put(ID, true);
      varsType.get(forDepth).put(ID, "animation_viewport");
      return null;
   }

   @Override public String visitViewport_size_expr(advParser.Viewport_size_exprContext ctx) {
      testingWindowSizeNotCoords = true;
      String sizeType = visit(ctx.expr());
      testingWindowSizeNotCoords = false;

      if (!sizeType.equals("intCoord")){
         System.out.println("Error: Viewport size must be initialized with integers. Line" + ctx.start.getLine());
         System.exit(1);
      }

      return null;
   }
   
   @Override public String visitViewport_next_state(advParser.Viewport_next_stateContext ctx){
      String type = visit(ctx.expr());
      if (!type.equals("string")){
         System.out.println("Error: Next State can only have String arguments.");
         System.exit(1);
      }
      return null;
   }


   @Override public String visitAnimation_on_viewport_statement(advParser.Animation_on_viewport_statementContext ctx) {
      String ID = ctx.ID().getText();

      if (!getAllVarsTypes().containsKey(ID)) {
         System.out.println("Error: Variable " + ID + " not declared. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      visit(ctx.viewport_statement());
      return null;
   }

   @Override public String visitViewport_for_statement(advParser.Viewport_for_statementContext ctx) {
      forDepth++;
      varsMap.put(forDepth, new HashMap<String, Boolean>());
      varsType.put(forDepth, new HashMap<String, String>());
      visit(ctx.for_statement());
      visit(ctx.viewport_statement());
      varsMap.remove(forDepth);
      varsType.remove(forDepth);
      forDepth--;
      return null;
   }


   @Override public String visitViewport_show_var(advParser.Viewport_show_varContext ctx) {
     String ID;
      if (ctx.ID() != null){
         ID = ctx.ID().getText();
   
         if (!getAllVarsTypes().containsKey(ID)) {
            System.out.println("Error: Variable " + ID + " not declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }

      } else {
         ID = visit(ctx.state_combo());
         if (!getAllVarsTypes().containsKey(ID)) {
            System.out.println("Error: Variable " + ID + " not declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }
      }

      if (ID == null){
         System.out.println("Something catastrophic happened.");
         System.exit(1);
      }

      String type = getAllVarsTypes().get(ID);

      if (!type.equals("state") && !type.equals("transitionline") && !type.equals("grid")){
         System.out.println("Error: Variable " + ID + " is not a state or transition. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      
      if (ctx.list_key_value() != null){
         String listtype = visit(ctx.list_key_value());
         if (type.equals("state")){
            if (!listtype.equals("state") && !listtype.equals("statetransitionline")){
               System.out.println("Error: Invalid properties for show of State, must be accpeting, initial or highlighted. Line: " + ctx.start.getLine());
               System.exit(1);
            }
         } else if (type.equals("transitionline")) {
            if (!listtype.equals("statetransitionline")){
               System.out.println("Error: Invalid properties for show of Transition, must be highlighted. Line: " + ctx.start.getLine());
               System.exit(1);
            }
         }
      }

      return null;
   }

   @Override public String visitViewport_word_decl(advParser.Viewport_word_declContext ctx) {
      if (ctx.viewport_word_decl().size() != 0){
         for (int i = 0; i < ctx.viewport_word_decl().size(); i++){
            visit(ctx.viewport_word_decl(i));
         }
         return null;
      }

      if (ctx.viewport_word_assign() != null){
         temporaryWordDeclared = true;
         visit(ctx.viewport_word_assign());
         temporaryWordDeclared = false;
         return null;
      } else {
         String ID = ctx.ID().getText();
         if (getAllVarsTypes().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " already declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         varsMap.get(forDepth).put(ID, false);
         varsType.get(forDepth).put(ID, "string");
         return null;
      }
   }

   @Override public String visitViewport_word_assign(advParser.Viewport_word_assignContext ctx)
   {
      String ID = ctx.ID().getText();
      
      if (temporaryWordDeclared){
         if (getAllVarsTypes().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " already declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }

         varsMap.get(forDepth).put(ID, true);
         varsType.get(forDepth).put(ID, "string");
      } else {
         if (!getAllVarsTypes().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " not declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         if (!getAllVarsTypes().get(ID).equals("string")){
            System.out.println("Error: Variable " + ID + " is not a string. Line: " + ctx.start.getLine());
            System.exit(1);
         }

         varsMap.get(forDepth).put(ID, true);
         varsType.get(forDepth).put(ID, "string");
      }

      String type = visit(ctx.expr());
      if (!type.equals("string") && !type.equals("read")){
         System.out.println("Error: Invalid value for string. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      return null;
   }

   @Override public String visitViewport_state_decl(advParser.Viewport_state_declContext ctx) {
      if (ctx.viewport_state_decl().size() != 0){
         for (int i = 0; i < ctx.viewport_state_decl().size(); i++){
            visit(ctx.viewport_state_decl(i));
         }
         return null;
      }

      if (ctx.viewport_state_assign() != null){
         temporaryStateDeclared = true;
         visit(ctx.viewport_state_assign());
         temporaryStateDeclared = false;
         return null;
      } else {
         String ID = ctx.ID().getText();
         if (getAllVarsTypes().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " already declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         varsMap.get(forDepth).put(ID, false);
         varsType.get(forDepth).put(ID, "state");
         return null;
      }
      
   }

   @Override public String visitViewport_state_assign(advParser.Viewport_state_assignContext ctx) {
      String ID = ctx.ID().getText();
      
      if (temporaryStateDeclared)
      {  
         if (getAllVarsTypes().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " already declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         varsMap.get(forDepth).put(ID, true);
         varsType.get(forDepth).put(ID, "state");
      } else {
         if (!getAllVarsTypes().containsKey(ID)){
            System.out.println("Error: Variable " + ID + " not declared. Line: " + ctx.start.getLine());
            System.exit(1);
         }
         if (!getAllVarsTypes().get(ID).equals("state")){
            System.out.println("Error: Variable " + ID + " is not a state. Line: " + ctx.start.getLine());
            System.exit(1);
         }

         varsMap.get(forDepth).put(ID, true);
         varsType.get(forDepth).put(ID, "state");
      }

      String type = visit(ctx.expr());
      if (!type.equals("state")){
         System.out.println("Error: Invalid value for state assignment. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      
      return null;
   }

   @Override public String visitPlay_stat(advParser.Play_statContext ctx) {
      String ID = ctx.ID().getText();

      if (!getAllVarsTypes().containsKey(ID)) {
         System.out.println("Error: Variable " + ID + " not declared. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      if (!getAllVarsTypes().get(ID).equals("animation")){
         System.out.println("Error: Variable " + ID + " is not a animation. Line: " + ctx.start.getLine());
         System.exit(1);
      }
      return null;
   }

   @Override public String visitState_combo(advParser.State_comboContext ctx) {
      return "<" + ctx.ID(0).getText() + "," + ctx.ID(1).getText() + ">";

   }

   @Override public String visitFor_statement(advParser.For_statementContext ctx) {
      String ID = ctx.ID().getText();

      if(getAllVars().containsKey(ID)){
         System.out.println("Error: Variable " + ID + " already declared. Line: " + ctx.start.getLine());
         System.exit(1);
      }

      String type = visit(ctx.expr());
      
      varsMap.get(forDepth).put(ID, true);
      varsType.get(forDepth).put(ID, type);
      return null;
   }

   private HashMap<String, Boolean> getAllVars(){
      HashMap<String, Boolean> fullVarMap = new HashMap<String, Boolean>();
      for (HashMap<String, Boolean> vars : varsMap.values()){
         for (String var : vars.keySet()){
            fullVarMap.put(var, vars.get(var));
         }
      }
      return fullVarMap;
   }

   private HashMap<String,String> getAllVarsTypes(){
      HashMap<String, String> fullVarMap = new HashMap<String, String>();
      for (HashMap<String, String> vars : varsType.values()){
         for (String var : vars.keySet()){
            fullVarMap.put(var, vars.get(var));
         }
      }
      return fullVarMap;
   }

   private String CalculateValidOperations(String op1, String op2, String operator)
   {
      switch (operator) {
         case "+":
         case "-":
            return CalculateValidPlusSubtract(op1, op2);
      
         case "*":
         case "/":
            return CalculateValidMultiplyDivide(op1, op2);

         default:
            System.out.println("Error: Invalid operator " + operator);
            System.exit(1);
            break;
      }

      return null;
   }

   private String CalculateValidPlusSubtract(String op1, String op2)
   {
      String ops = op1 + op2;
      if (op1.equals("read") || op2.equals("read"))
         return op1.equals("read") ? op2 : op1;

      switch (ops) {
         case "coordcoord":
            return "coord";

         case "intint":
            return "int";

         case "floatfloat":
         case "intfloat":
         case "floatint":
            return "float";
      
         default:
            System.out.println("Error: Invalid operation " + op1 + " + " + op2);
            System.exit(1);
            break;
      }

      return null;
   }

   private String CalculateValidMultiplyDivide(String op1, String op2)
   {
      String ops = op1 + op2;

      if (op1.equals("read") || op2.equals("read"))
         return op1.equals("read") ? op2 : op1;

      switch (ops) {
         case "coordcoord":
         case "coordint":
         case "coordfloat":
         case "intcoord":
         case "floatcoord":
            return "coord";

         case "intint":
            return "int";
         
         case "floatfloat":
         case "intfloat":
         case "floatint":
            return "float";
      
         default:
            System.out.println("Error: Invalid operation " + op1 + " * " + op2);
            System.exit(1);
            break;
      }

      return null;
   }
}
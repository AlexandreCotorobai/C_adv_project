import org.antlr.v4.parse.ANTLRParser.elementEntry_return;
import org.antlr.v4.parse.GrammarTreeVisitor.element_return;
import org.stringtemplate.v4.*;
import java.util.*;

import javax.swing.plaf.synth.SynthScrollPaneUI;

@SuppressWarnings("CheckReturnValue")
public class CompilerVis extends advBaseVisitor<ST> {

   private STGroup stg = new STGroupFile("Template.stg");
   private HashMap<String, Boolean> automatas = new HashMap<>();
   private HashMap<String, Boolean> views = new HashMap<>();
   private HashMap<String, Boolean> animations = new HashMap<>();
   private Map<Integer, HashMap<String, String>> varTypes = new HashMap<Integer, HashMap<String, String>>();
   private List<String> states = new ArrayList<String>();   
   private Integer forDepth = 0;
   private boolean testingWindowSizeNotCoords = false;
   private boolean placingLabel = false;
   private HashMap<Integer, Boolean> testingWindow = new HashMap<Integer, Boolean>();
   private String highlightedState = "";
   private String currentState = "";
   @Override
   public ST visitProgram(advParser.ProgramContext ctx) {
      ST out = stg.getInstanceOf("main");
      ST res = stg.getInstanceOf("stats");
      varTypes.put(forDepth, new HashMap<String, String>());

      res.add("stat", visit(ctx.alphabet_decl()).render());

      Iterator<advParser.DeclContext> decl = ctx.decl().iterator();

      while (decl.hasNext()) {
         res.add("stat", visit(decl.next()).render());
      }

      Iterator<advParser.StatContext> stat = ctx.stat().iterator();

      while (stat.hasNext()) {
         ST statST = visit(stat.next());
         res.add("stat", statST.render());
      }

      out.add("stats", res.render());
      return out;
      // return res;
   }

   @Override
   public ST visitDecl(advParser.DeclContext ctx) {
      ST res = visitChildren(ctx);
      return res;
   }

   @Override
   public ST visitAlphabet_decl(advParser.Alphabet_declContext ctx) {
      ST res = stg.getInstanceOf("alphabet");

      String alphabetLetters = "";
      for (int i = 0; i < ctx.ALPHABET_LETTER().size(); i++) {
         alphabetLetters += ctx.ALPHABET_LETTER(i).getText() + ",";
      }

      alphabetLetters = alphabetLetters.substring(0, alphabetLetters.length() - 1);

      res.add("alphabetic_expr", alphabetLetters);

      return res;
   }

   @Override
   public ST visitAutomata_decl(advParser.Automata_declContext ctx) {
      ST res = stg.getInstanceOf("Automaton");
      ST stat = stg.getInstanceOf("stats");
      String automataType = ctx.automata_type().getText();
      String automataName = ctx.ID().getText();

      automatas.put(automataName, true);

      res.add("varName", automataName);
      res.add("type", automataType);
      stat.add("stat", res.render());
      // String automataMod = ctx.automata_mod() != null ?
      // ctx.automata_mod().getText() : "";

      stat.add("stat", visit(ctx.automata_statement()));

      automatas.put(automataName, false);
      return stat;
      // return res;
   }

   @Override
   public ST visitAutomata_mod(advParser.Automata_modContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      // return res;
   }

   @Override
   public ST visitAutomata_statement(advParser.Automata_statementContext ctx) {
      // ST res = stg.getInstanceOf("stats");

      if (ctx.getChildCount() == 2) {
         return visit(ctx.getChild(0));

      } else {
         return visitChildren(ctx);
      }

   }

   @Override
   public ST visitAutomata_compound_statement(advParser.Automata_compound_statementContext ctx) {

      ST res = stg.getInstanceOf("stats");

      Iterator<advParser.Automata_statementContext> automata_statement = ctx.automata_statement().iterator();

      while (automata_statement.hasNext()) {
         res.add("stat", visit(automata_statement.next()).render());
      }

      return res; 
   }

   @Override
   public ST visitAutomata_state_decl_statement(advParser.Automata_state_decl_statementContext ctx) {
      ST stat = stg.getInstanceOf("stats");

      for (int i = 0; i < ctx.ID().size(); i++) {
         ST res = stg.getInstanceOf("addState");
         res.add("varName", checkCurrentAutomata());
         res.add("stateName", ctx.ID(i).getText());
         stat.add("stat", res.render());
         varTypes.get(forDepth).put(ctx.ID(i).getText(), "state");
      }

      return stat;
   }

   @Override
   public ST visitAutomata_state_prop_statement(advParser.Automata_state_prop_statementContext ctx) {

      String stateName = ctx.ID().getText();
      String stateProp = visit(ctx.list_key_value()).render(); // get key-values

      ST stat = stg.getInstanceOf("stats");

      String[] statePropArray = stateProp.split(";");

      for (String prop : statePropArray) {

         ST res = stg.getInstanceOf("stateLabel");
         res.add("varName", checkCurrentAutomata());

         if (testingWindow.get(forDepth) != null && testingWindow.get(forDepth) == true) {
            res.add("stateName", stateName);
         } else {
            res.add("stateName", "'" + stateName + "'");
         }

         String[] propArray = prop.split(":");
         res.add("label", propArray[0]);
         res.add("labelValue", propArray[1]);

         stat.add("stat", res.render());
      }

      return stat;
      // return res;
   }

   @Override
   public ST visitAutomata_transition_statement(advParser.Automata_transition_statementContext ctx) {
      ST res = stg.getInstanceOf("stats");

      Iterator<advParser.Automata_transitionContext> transition = ctx.automata_transition().iterator();

      while (transition.hasNext()) {
         res.add("stat", visit(transition.next()).render());
      }

      return res;
   }

   @Override
   public ST visitAutomata_transition_alphabet(advParser.Automata_transition_alphabetContext ctx) {
      ST res = stg.getInstanceOf("addTransition");
      String stateFrom = ctx.ID(0).getText();
      String stateTo = ctx.ID(1).getText();

      String alphabetLetters = "";
      for (int i = 0; i < ctx.ALPHABET_LETTER().size(); i++) {
         alphabetLetters += ctx.ALPHABET_LETTER(i).getText() + ",";
      }
      alphabetLetters = alphabetLetters.substring(0, alphabetLetters.length() - 1);

      res.add("varName", checkCurrentAutomata());
      res.add("fromState", stateFrom);
      res.add("toState", stateTo);
      res.add("alphabetic_expr", alphabetLetters);

      return res;
   }


   @Override
   public ST visitAutomata_for_statement(advParser.Automata_for_statementContext ctx) {
      ST forLoop = stg.getInstanceOf("forLoop");
      String loopHeader = visit(ctx.for_statement()).render();
      

      // nobody knows this.. xD
      forDepth++;
      testingWindow.put(forDepth, true);
      String loopBody = visit(ctx.automata_statement()).render();
      testingWindow.put(forDepth, false);
      forDepth--;
      forLoop.add("header", loopHeader);
      forLoop.add("body", loopBody);
      return forLoop;
   }

   @Override
   public ST visitView_decl(advParser.View_declContext ctx) {
      ST stat = stg.getInstanceOf("stats");
      ST res = stg.getInstanceOf("view");
      String viewName = ctx.ID(0).getText();
      String automataName = ctx.ID(1).getText();
      views.put(viewName, true);
      automatas.put(automataName, true);

      res.add("viewVarName", viewName);
      res.add("automatonVarName", automataName);
      stat.add("stat", res.render());
      stat.add("stat", visit(ctx.view_statement()).render());

      views.put(viewName, false);
      automatas.put(automataName, false);
      return stat;
   }

   @Override
   public ST visitView_statement(advParser.View_statementContext ctx) {

      if (ctx.getChildCount() == 2) {
         return visit(ctx.getChild(0));
      } else {
         return visitChildren(ctx);
      }
   }

   @Override
   public ST visitView_compound_statement(advParser.View_compound_statementContext ctx) {
      ST res = stg.getInstanceOf("stats");
      Iterator<advParser.View_statementContext> view_statement = ctx.view_statement().iterator();

      while (view_statement.hasNext()) {
         res.add("stat", visit(view_statement.next()).render());
      }

      return res;
   }

   @Override
   public ST visitView_place_statement(advParser.View_place_statementContext ctx) {

      return visit(ctx.view_place_expr());
   }

   @Override
   public ST visitView_place_expr(advParser.View_place_exprContext ctx) {

      if (ctx.view_place_expr(0) != null) {
         ST stat = stg.getInstanceOf("stats");

         for (int i = 0; i < ctx.view_place_expr().size(); i++) {
            stat.add("stat", visit(ctx.view_place_expr(i)).render());
         }

         return stat;
      } else if (ctx.expr() != null) {
         ST res2 = stg.getInstanceOf("placeState");

         String id = ctx.ID().getText();
         String expr = ctx.expr().getText();

         res2.add("viewVarName", checkCurrentView());
         res2.add("stateName", id);
         res2.add("point", expr);

         varTypes.get(forDepth).put(id, "state");

         return res2;

      } else if (ctx.view_label_statement() != null) {
         placingLabel = true;
         String labelstatement = visit(ctx.view_label_statement()).render();
         placingLabel = false;
         String id = ctx.ID().getText();

         ST point = stg.getInstanceOf("getPoint");
         point.add("viewVarName", checkCurrentView());
         point.add("pointName", id);

         ST res = stg.getInstanceOf("placeTransitionLabel");
         res.add("viewVarName", checkCurrentView());
         res.add("labelstatement", labelstatement);
         res.add("point", point.render());
         return res;
      }

      return visitChildren(ctx);
      // return res;
   }

   @Override
   public ST visitView_label_statement(advParser.View_label_statementContext ctx) {
      ST align = stg.getInstanceOf("element");
      String combo = visit(ctx.state_combo()).render();
      String label = visit(ctx.label()).render();
      String[] labels = label.split(";");

      align.add("elem", combo);
      align.add("elem", ", ");

      String resultFinal = "[";
      for (String l : labels) {
         String[] labelValue = l.split("\\:");
         labelValue[1] = labelValue[1].toUpperCase();
         labelValue[0] = labelValue[0].toLowerCase();
         labelValue[0] = Character.toUpperCase(labelValue[0].charAt(0)) + labelValue[0].substring(1);
         resultFinal += labelValue[0] + "." + labelValue[1] + ",";

      }
      resultFinal = resultFinal.substring(0, resultFinal.length() - 1);

      align.add("elem", resultFinal + "]");

      if (placingLabel) {
         return align;
      }else{
         ST res = stg.getInstanceOf("changeLabelAlignment");
         res.add("viewVarName", checkCurrentView());
         res.add("body", align.render());
         return res;
      }
      
      // return res;
   }

   @Override
   public ST visitView_point_decl_statement(advParser.View_point_decl_statementContext ctx) {
      ST res = null;
      return visit(ctx.view_point_decl());
      // return res;
   }

   @Override
   public ST visitView_point_decl(advParser.View_point_declContext ctx) {
      if (ctx.view_point_decl(0) != null) {
         ST stats = stg.getInstanceOf("stats");
         String point1 = visit(ctx.view_point_decl(0)).render();
         String point2 = visit(ctx.view_point_decl(1)).render();

         stats.add("stat", point1);
         stats.add("stat", point2);

         return stats;

      } else if (ctx.view_point_assign() != null) { // TENHO QUASE A CERTEZA QUE ISTO TÁ MAL
         // System.out.println("TEST " + ctx.view_point_assign().ID().getText());
         ST stats = stg.getInstanceOf("stats");
         ST res = stg.getInstanceOf("addPoint");
         String viewName = checkCurrentView();
         String pointName = ctx.view_point_assign().ID().getText();
         res.add("viewVarName", viewName);
         res.add("pointName", pointName);
         stats.add("stat", res.render());

         varTypes.get(forDepth).put(pointName, "coordName");

         stats.add("stat", visit(ctx.view_point_assign()).render());

         return stats;

      } else if (ctx.ID() != null) {
         ST res = stg.getInstanceOf("addPoint");
         String viewName = checkCurrentView();
         String pointName = ctx.ID().getText();
         res.add("viewVarName", viewName);
         res.add("pointName", pointName);

         varTypes.get(forDepth).put(pointName, "coordName");
         return res;
      }
      return visitChildren(ctx);
      // return res;
   }


   @Override
   public ST visitView_point_assign(advParser.View_point_assignContext ctx) {

      String id = ctx.ID().getText(); // p1
      if (!getAllVarsTypes().containsKey(id)) {
         varTypes.get(forDepth).put(id, "coordName");
      }

      ST assignPoint = stg.getInstanceOf("getPoint");

      assignPoint.add("viewVarName", checkCurrentView());
      assignPoint.add("pointName", id);

      ST GeneralAssignment = stg.getInstanceOf("GeneralAssignment");
      GeneralAssignment.add("var", assignPoint.render());

      GeneralAssignment.add("body", visit(ctx.expr()).render());

      return GeneralAssignment;
   }

   @Override
   public ST visitView_line_statement(advParser.View_line_statementContext ctx) {
      ST transArrows = stg.getInstanceOf("redefineTransitionArrows");
      String stateCombo = visit(ctx.state_combo()).render();
      String viewLine = visit(ctx.view_line()).render();

      transArrows.add("viewVarName", checkCurrentView());
      transArrows.add("transition", stateCombo);
      transArrows.add("body", viewLine);
      return transArrows;
      // return res;
   }

   @Override
   public ST visitView_line(advParser.View_lineContext ctx) {
      ST elem = stg.getInstanceOf("element");
      String body = "";

      if (ctx.getChildCount() == 1) {
         String id = ctx.ID().getText();
         if (getAllVarsTypes().get(id).equals("coordName")) {
            ST res = stg.getInstanceOf("getPoint");
            res.add("pointName", id);
            res.add("viewVarName", checkCurrentView());
            body += res.render() + ",";
         } else {
            body += id + ",";
         }

      } else if(ctx.list_key_value() != null){
         String id = ctx.ID().getText();
         if (getAllVarsTypes().get(id).equals("coordName")) {
            ST res = stg.getInstanceOf("getPoint");
            res.add("pointName", id);
            res.add("viewVarName", checkCurrentView());
            body += "(" + res.render() + ",";
         } else {
            body += id + ",";
         }

         String key_value = visit(ctx.list_key_value()).render();
         String[] list = key_value.split(";");

         for (int i = 0; i < list.length; i++) {
            String value = list[i].split(":")[1];
            body += value + "),";
         }
      }
      else {
         for (int i = 0; i < ctx.view_line().size(); i++) {
            String view_line1 = visit(ctx.view_line(i)).render();
            body += view_line1 + ",";
         }
      }

      body = body.substring(0, body.length() - 1);
      elem.add("elem", body);
      return elem;

   }

   @Override
   public ST visitView_grid_decl_statement(advParser.View_grid_decl_statementContext ctx) {
      ST stat = stg.getInstanceOf("stats");
      String id = ctx.ID().getText();
      testingWindowSizeNotCoords = true;
      String expr = visit(ctx.expr()).render();
      testingWindowSizeNotCoords = false;
      String key_value = visit(ctx.list_key_value()).render();
      String[] list = key_value.split(";");

      ST addG = stg.getInstanceOf("addGrid");
      addG.add("viewVarName", checkCurrentView());
      addG.add("gridName", id);
      addG.add("window", expr);
      stat.add("stat", addG.render());

      for (int i = 0; i < list.length; i++) {
         String[] keyValue = list[i].split(":");
         String key = keyValue[0];
         String value = keyValue[1];

         ST res = stg.getInstanceOf("gridAttributes");
         res.add("viewVarName", checkCurrentView());
         res.add("gridName", id);
         res.add("attribute", key);
         try {
            Double.parseDouble(value);
            res.add("value", value);
         } catch (Exception e) {
            res.add("value", "'" + value + "'");
         }

         stat.add("stat", res.render());
      }

      varTypes.get(forDepth).put(id, "grid");

      return stat;
      // return res;
   }

   @Override
   public ST visitExpr_number(advParser.Expr_numberContext ctx) {
      ST res = stg.getInstanceOf("element");

      varTypes.get(forDepth).put(ctx.getText(), "int");
      return res.add("elem", ctx.getText());
      // return res;
   }

   @Override
   public ST visitExpr_string(advParser.Expr_stringContext ctx) {
      ST res = stg.getInstanceOf("element");

      return res.add("elem", ctx.STRING().getText());
      // return res;
   }

   @Override
   public ST visitExpr_radials(advParser.Expr_radialsContext ctx) {
      ST res = stg.getInstanceOf("PointFromPolar");
      String expr1 = visit(ctx.expr(0)).render();
      String expr2 = visit(ctx.expr(1)).render();

      res.add("angle", expr1);
      res.add("norm", expr2);

      varTypes.get(forDepth).put(res.render(), "coord");

      return res;
      // return res;
   }

   @Override
   public ST visitExpr_add_sub(advParser.Expr_add_subContext ctx) {
      ST elem = stg.getInstanceOf("element");
      String expr1 = visit(ctx.expr(0)).render();
      String expr2 = visit(ctx.expr(1)).render();

      // AQUI FAZEMOS TRIAGEM DO PRIMEIRO VALOR
      String type1 = getAllVarsTypes().get(expr1);
      String type2 = getAllVarsTypes().get(expr2);

      if (type1.equals("coordName")) {
         ST getPoint1 = stg.getInstanceOf("getPoint");

         getPoint1.add("viewVarName", checkCurrentView());
         getPoint1.add("pointName", expr1);
         elem.add("elem", getPoint1.render());
      } else if (type1.equals("coord")) {
         elem.add("elem", expr1);
      }

      String op = ctx.op.getText();
      // AQUI ADICIONAMOS O OPERATOR
      elem.add("elem", " " + op + " ");

      // AQUI FAZEMOS TRIAGEM DO SEGUNDO VALOR
      if (type2.equals("coordName")) {
         ST getPoint2 = stg.getInstanceOf("getPoint");

         getPoint2.add("viewVarName", checkCurrentView());
         getPoint2.add("pointName", expr2);
         elem.add("elem", getPoint2.render());
      } else if (type2.equals("coord")) {
         elem.add("elem", expr2);
      }

      String result = CalculateValidOperations(type1, type2, op);
      varTypes.get(forDepth).put(elem.render(), result);

      return elem;

   }

   @Override
   public ST visitExpr_paren(advParser.Expr_parenContext ctx) {
      ST elem = stg.getInstanceOf("element");
      ST result = visit(ctx.expr());
      String expr = result.render();
      if (getAllVarsTypes().get(expr).equals("state")) {
         ST res = stg.getInstanceOf("assignPointStateRef2");

         String viewName = checkCurrentView();

         res.add("viewVarName", viewName);
         res.add("stateName", expr);
         varTypes.get(forDepth).put(res.render(), "coord");
         return res;
      } else {
         elem.add("elem", "(");
         elem.add("elem", expr);
         elem.add("elem", ")");
         varTypes.get(forDepth).put(elem.render(), getAllVarsTypes().get(expr));
         return elem;
      }
   }

   @Override
   public ST visitExpr_set(advParser.Expr_setContext ctx) {
      ST elem = stg.getInstanceOf("element");

      String expr = "";
      for (int i = 0; i < ctx.expr().size(); i++) {
         expr += "'" + visit(ctx.expr(i)).render() + "',";

      }
      expr = expr.substring(0, expr.length() - 1);

      elem.add("elem", "{" + expr + "}");
      return elem;
   }

   @Override
   public ST visitExpr_bool(advParser.Expr_boolContext ctx) {
      ST res = stg.getInstanceOf("element");

      return res.add("elem", ctx.getText());
   }

   @Override
   public ST visitExpr_unary(advParser.Expr_unaryContext ctx) {
      ST elem = stg.getInstanceOf("element");
      String op = ctx.op.getText();
      String expr = visit(ctx.expr()).render();

      elem.add("elem", op);
      elem.add("elem", expr);
      return elem;
   }

   @Override
   public ST visitExpr_cartesian(advParser.Expr_cartesianContext ctx) {
      ST res;
      String expr1 = visit(ctx.expr(0)).render();
      String expr2 = visit(ctx.expr(1)).render();
      // res.add("x", visit(ctx.expr(0)).render());
      // res.add("y", visit(ctx.expr(1)).render());

      // varTypes.get(forDepth).put(res.render(), "coord");

      if (testingWindowSizeNotCoords) {
         res = stg.getInstanceOf("element");
         res.add("elem", "(" + expr1 + "," + expr2 + ")");
         return res;
      } else {
         res = stg.getInstanceOf("Point");
         res.add("x", expr1);
         res.add("y", expr2);
         varTypes.get(forDepth).put(res.render(), "coord");
         return res;
      }
   }

   @Override
   public ST visitExpr_mul_div(advParser.Expr_mul_divContext ctx) {
      ST elem = stg.getInstanceOf("element");
      String expr1 = visit(ctx.expr(0)).render();
      String expr2 = visit(ctx.expr(1)).render();
      String op = ctx.op.getText();

      elem.add("elem", expr1);
      elem.add("elem", " " + op + " ");
      elem.add("elem", expr2);

      String type1 = getAllVarsTypes().get(expr1);
      String type2 = getAllVarsTypes().get(expr2);

      varTypes.get(forDepth).put(elem.render(), CalculateValidOperations(type1, type2, op));

      return elem;
      // return res;
   }

   @Override
   public ST visitExpr_id(advParser.Expr_idContext ctx) {
      ST res = stg.getInstanceOf("element");

      return res.add("elem", ctx.ID().getText());
   }



   @Override
   public ST visitList_key_value(advParser.List_key_valueContext ctx) {
      // this is gonna return a strin of the form initial:true;accepting:true
      ST res = stg.getInstanceOf("element");

      Iterator<advParser.Key_valueContext> iter = ctx.key_value().iterator();

      String listOfStrings = "";
      while (iter.hasNext()) {

         listOfStrings += visit(iter.next()).render() + ";";
      }

      listOfStrings = listOfStrings.substring(0, listOfStrings.length() - 1);

      res.add("elem", listOfStrings);

      return res;
   }

   @Override
   public ST visitKey_value(advParser.Key_valueContext ctx) {
      // this is gonna return a string of the form "key:value,value,value"
      ST res = stg.getInstanceOf("element");
      String key = ctx.ID().getText();
      String values = "";

      for (int i = 0; i < ctx.expr().size(); i++) {
         if (key.equals("highlighted")){
            highlightedState = currentState;
         }
         values += key + ":" + visit(ctx.expr(i)).render() + ";";
      }
      values = values.substring(0, values.length() - 1);

      res.add("elem", values);

      return res;
   }

   @Override
   public ST visitLabel(advParser.LabelContext ctx) {
      ST res = visit(ctx.list_key_value());
      return res;
   }

   @Override
   public ST visitAnimation_decl(advParser.Animation_declContext ctx) {
      ST stat = stg.getInstanceOf("stats");
      ST anim = stg.getInstanceOf("animationDef");
      ST function = stg.getInstanceOf("functionDef");

      String animation = ctx.ID().getText();

      animations.put(animation, true);

      anim.add("animationName", animation);
      function.add("functionName", animation);

      stat.add("stat", anim.render());
      stat.add("stat", visit(ctx.animation_statement()).render());

      function.add("functionBody", stat.render());
      animations.put(animation, false);
      return function;
   }

   @Override
   public ST visitAnimation_statement(advParser.Animation_statementContext ctx) {
      if (ctx.getChildCount() == 2) {
         return visit(ctx.getChild(0));
      } else {
         return visitChildren(ctx);
      }
   }

   @Override
   public ST visitAnimation_viewport_decl_statement(advParser.Animation_viewport_decl_statementContext ctx) {
      ST res = stg.getInstanceOf("addViewport");
      String viewport = ctx.ID(0).getText();
      String view = ctx.ID(1).getText();
      testingWindowSizeNotCoords = true;
      String coords = visit(ctx.expr()).render();
      testingWindowSizeNotCoords = false;
      String vp_size = visit(ctx.viewport_size_expr()).render();

      res.add("animationName", checkCurrentAnimation());
      res.add("viewportName", viewport);
      res.add("viewVarName", view);
      res.add("startPosition", coords);
      res.add("size", vp_size);

      return res;
   }

   @Override
   public ST visitViewport_size_expr(advParser.Viewport_size_exprContext ctx) {
      ST res = stg.getInstanceOf("element");
      String expr = ctx.expr().getText();

      res.add("elem", expr);
      return res;
   }

   @Override
   public ST visitAnimation_compound_statement(advParser.Animation_compound_statementContext ctx) {
      ST stat = stg.getInstanceOf("stats");

      Iterator<advParser.Animation_statementContext> iter = ctx.animation_statement().iterator();

      while (iter.hasNext()) {
         stat.add("stat", visit(iter.next()).render());
      }

      return stat;
      // return res;
   }

   @Override
   public ST visitViewport_statement(advParser.Viewport_statementContext ctx) {
      if (ctx.getChildCount() == 2) {
         return visit(ctx.getChild(0));
      } else {
         return visitChildren(ctx);
      }
   }

   @Override
   public ST visitViewport_compound_statement(advParser.Viewport_compound_statementContext ctx) {
      ST stat = stg.getInstanceOf("stats");

      Iterator<advParser.Viewport_statementContext> iter = ctx.viewport_statement().iterator();

      while (iter.hasNext()) {
         stat.add("stat", visit(iter.next()).render());
      }

      return stat;
   }

   @Override
   public ST visitViewport_for_statement(advParser.Viewport_for_statementContext ctx) {
      ST forLoop = stg.getInstanceOf("forLoop");
      String header = visit(ctx.for_statement()).render();

      forDepth++;
      testingWindow.put(forDepth, true);
      String loopBody = visit(ctx.viewport_statement()).render();
      testingWindow.put(forDepth, false);
      forDepth--;

      forLoop.add("header", header);
      forLoop.add("body", loopBody);
      return forLoop;

   }

   @Override
   public ST visitViewport_show_selected(advParser.Viewport_show_selectedContext ctx) {
      ST stat = stg.getInstanceOf("stats");

      Iterator<advParser.Viewport_show_varContext> iter = ctx.viewport_show_var().iterator();

      while (iter.hasNext()) {
         stat.add("stat", visit(iter.next()).render());
      }

      return stat;
   }

   @Override
   public ST visitViewport_show_all(advParser.Viewport_show_allContext ctx) {
      ST res = stg.getInstanceOf("showAll");
      res.add("animationName", checkCurrentAnimation());
      return res;
   }

   @Override
   public ST visitViewport_show_var(advParser.Viewport_show_varContext ctx) {
      ST stat = stg.getInstanceOf("stats");
      if (ctx.getChildCount() == 1) {
         if (ctx.state_combo() == null && getAllVarsTypes().get(ctx.ID().getText()).equals("state")) {
            ST res = stg.getInstanceOf("showState");
            if (!states.contains(ctx.ID().getText())){
               res.add("animationName", checkCurrentAnimation());
               if (testingWindow.get(forDepth) != null && testingWindow.get(forDepth) == true) {
                  res.add("state", ctx.ID().getText());
               } else {
                  res.add("state", "'" + ctx.ID().getText() + "'");
               }
               stat.add("stat", res.render());
            } else {
               res.add("state", ctx.ID().getText() + ".key");
               stat.add("stat", res.render());
            }
         } else if(ctx.state_combo() == null && getAllVarsTypes().get(ctx.ID().getText()).equals("grid")){
            ST res = stg.getInstanceOf("showGrid");

            res.add("animationName", checkCurrentAnimation());
            res.add("gridName", ctx.ID().getText());

            stat.add("stat", res.render());
         }
         else {
            ST res = stg.getInstanceOf("showTransition");
            String stateCombo = ctx.state_combo().getText();

            res.add("animationName", checkCurrentAnimation());
            res.add("transition", stateCombo);

            stat.add("stat", res.render());
         }
      } else {

         String stateName = ctx.ID().getText();
         currentState = stateName;
         String stateProp = visit(ctx.list_key_value()).render(); // get key-values
         String[] statePropArray = stateProp.split(";");
         for (String prop : statePropArray) {
            ST res = stg.getInstanceOf("showState");
            res.add("animationName", checkCurrentAnimation());
            if (!states.contains(stateName)){
               if (testingWindow.get(forDepth) != null && testingWindow.get(forDepth) == true) {
                  res.add("state", stateName);
               } else {
                  res.add("state", "'" + stateName + "'");
               }
            } else {
               res.add("state", stateName + ".key");
            }

            String[] propArray = prop.split(":");
            res.add("label", propArray[0]);
            res.add("labelValue", propArray[1]);

            stat.add("stat", res.render());

         }
      }
      return stat;
   }

   @Override
   public ST visitViewport_pause_statement(advParser.Viewport_pause_statementContext ctx) {
      ST res = stg.getInstanceOf("pause");
      res.add("animationName", checkCurrentAnimation());
      return res;
   }

   @Override
   public ST visitViewport_word_decl_statement(advParser.Viewport_word_decl_statementContext ctx) {
      return visit(ctx.viewport_word_decl());
   }

   @Override
   public ST visitViewport_word_decl(advParser.Viewport_word_declContext ctx) {
      ST res;
      if (ctx.viewport_word_decl(0) != null) {
         // ST stats = stg.getInstanceOf("stats");
         // String point1 = visit(ctx.view_point_decl(0)).render();
         // String point2 = visit(ctx.view_point_decl(1)).render();

         // stats.add("stat", point1);
         // stats.add("stat", point2);

         // return stats;

      } else if (ctx.viewport_word_assign() != null) { 
         res = stg.getInstanceOf("word");
         String wordName = ctx.viewport_word_assign().ID().getText();
         String listValue = visit(ctx.viewport_word_assign()).render();
         String [] listValueArray = listValue.split(";");
         for (String value : listValueArray) {
            String [] valueArray = value.split(":", 2);
            res.add("varName", wordName);
            res.add("prompt", valueArray[1].replace("\"", ""));
         }
         
         return res;
         // ST stats = stg.getInstanceOf("stats");
         // ST res = stg.getInstanceOf("addPoint");
         // String viewName = checkCurrentView();
         // String pointName = ctx.view_point_assign().ID().getText();
         // res.add("viewVarName", viewName);
         // res.add("pointName", pointName);
         // stats.add("stat", res.render());

         // varTypes.get(forDepth).put(pointName, "coordName");

         // stats.add("stat", visit(ctx.view_point_assign()).render());

         // return stats;

      } else if (ctx.ID() != null) {
         // ST res = stg.getInstanceOf("addPoint");
         // String viewName = checkCurrentView();
         // String pointName = ctx.ID().getText();
         // res.add("viewVarName", viewName);
         // res.add("pointName", pointName);

         // varTypes.get(forDepth).put(pointName, "coordName");
         // return res;
      }
      return null;
   }

   @Override
   public ST visitViewport_state_assign(advParser.Viewport_state_assignContext ctx) {
      ST res = stg.getInstanceOf("getState");
      states.add(ctx.ID().getText());
      String varName = ctx.ID().getText();
      String state = ctx.expr().getText();
      res.add("varName", varName);
      res.add("state", state);
      res.add("animation", checkCurrentAnimation());
      return res;
      // return res;
   }

   @Override
   public ST visitStat(advParser.StatContext ctx) {
      ST res = visit(ctx.play_stat());
      return res;
      // return res;
   }

   @Override
   public ST visitPlay_stat(advParser.Play_statContext ctx) {
      ST res = stg.getInstanceOf("functionCall");
      String functionName = ctx.ID().getText();
      res.add("functionName", functionName);
      return res;
   }

   @Override
   public ST visitState_combo(advParser.State_comboContext ctx) {
      ST elem = stg.getInstanceOf("element");
      String id1 = ctx.ID(0).getText();
      String id2 = ctx.ID(1).getText();
      elem.add("elem", "'<" + id1 + "," + id2 + ">'");
      return elem;
   }

   @Override
   public ST visitFor_statement(advParser.For_statementContext ctx) {
      ST loopHeader = stg.getInstanceOf("element");
      String id = ctx.ID().getText();
      String expr = visit(ctx.expr()).render();

      loopHeader.add("elem", "for " + id + " in " + expr + ":");

      return loopHeader;
      // return res;
   }

   @Override
   public ST visitViewport_next_state(advParser.Viewport_next_stateContext ctx) {
      ST res = stg.getInstanceOf("getNextState");
      res.add("varName", highlightedState);
      res.add("animationName", checkCurrentAnimation());
      res.add("OldVar", highlightedState + ".key");
      res.add("letter", visit(ctx.expr()).render());
      res.add("label", "highlighted");
      res.add("labelValue", "true");
      return res;
   }

   public String checkCurrentAutomata() {
      for (Map.Entry<String, Boolean> entry : automatas.entrySet()) {
         if (entry.getValue()) {
            return entry.getKey();
         }
      }
      return null;
   }

   public String checkCurrentView() {
      for (Map.Entry<String, Boolean> entry : views.entrySet()) {
         if (entry.getValue()) {
            return entry.getKey();
         }
      }
      return null;
   }

   public String checkCurrentAnimation() {
      for (Map.Entry<String, Boolean> entry : animations.entrySet()) {
         if (entry.getValue()) {
            return entry.getKey();
         }
      }
      return null;
   }

   private String CalculateValidOperations(String op1, String op2, String operator) {
      if (op1 == "coordName") {
         op1 = "coord";
      }
      if (op2 == "coordName") {
         op2 = "coord";
      }

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

   private String CalculateValidPlusSubtract(String op1, String op2) {
      String ops = op1 + op2;
      if (op1 == "read" || op2 == "read")
         return op1 == "read" ? op2 : op1;

      switch (ops) {
         case "coordcoord":
         case "statecoord":
         case "coordstate":
         case "statestate":
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

   private String CalculateValidMultiplyDivide(String op1, String op2) {
      String ops = op1 + op2;

      if (op1 == "read" || op2 == "read")
         return op1 == "read" ? op2 : op1;

      switch (ops) {
         case "coordcoord":
         case "coordint":
         case "coordfloat":
         case "intcoord":
         case "floatcoord":
         case "stateint":
         case "statefloat":
         case "intstate":
         case "floatstate":
         case "statecoord":
         case "coordstate":
         case "statestate":
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

   private HashMap<String,String> getAllVarsTypes(){
      HashMap<String, String> fullVarMap = new HashMap<String, String>();
      for (HashMap<String, String> vars : varTypes.values()){
         for (String var : vars.keySet()){
            fullVarMap.put(var, vars.get(var));
         }
      }
      return fullVarMap;
   }

   public void writeToFile(String string, String render) {
   }
}
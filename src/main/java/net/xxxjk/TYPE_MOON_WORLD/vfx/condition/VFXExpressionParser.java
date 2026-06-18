package net.xxxjk.TYPE_MOON_WORLD.vfx.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.util.Mth;

public final class VFXExpressionParser {
   private VFXExpressionParser() {
   }

   public static VFXExpression parse(String expression) {
      Parser parser = new Parser(expression == null || expression.isBlank() ? "0" : expression);
      VFXExpression result = parser.parseExpression();
      parser.expectEnd();
      return result;
   }

   private static final class Parser {
      private final String input;
      private int index;

      Parser(String input) {
         this.input = input;
      }

      VFXExpression parseExpression() {
         return parseOr();
      }

      void expectEnd() {
         skipWhitespace();
         if (index < input.length()) {
            throw new IllegalArgumentException("Unexpected token at " + index + " in expression: " + input);
         }
      }

      private VFXExpression parseOr() {
         VFXExpression left = parseAnd();
         while (match("||")) {
            VFXExpression leftNode = left;
            VFXExpression right = parseAnd();
            left = (vars, random) -> leftNode.eval(vars, random) > 0.0F || right.eval(vars, random) > 0.0F ? 1.0F : 0.0F;
         }
         return left;
      }

      private VFXExpression parseAnd() {
         VFXExpression left = parseComparison();
         while (match("&&")) {
            VFXExpression leftNode = left;
            VFXExpression right = parseComparison();
            left = (vars, random) -> leftNode.eval(vars, random) > 0.0F && right.eval(vars, random) > 0.0F ? 1.0F : 0.0F;
         }
         return left;
      }

      private VFXExpression parseComparison() {
         VFXExpression left = parseAdditive();
         while (true) {
            if (match(">=")) {
               left = compare(left, parseAdditive(), ">=");
            } else if (match("<=")) {
               left = compare(left, parseAdditive(), "<=");
            } else if (match("==")) {
               left = compare(left, parseAdditive(), "==");
            } else if (match("!=")) {
               left = compare(left, parseAdditive(), "!=");
            } else if (match(">")) {
               left = compare(left, parseAdditive(), ">");
            } else if (match("<")) {
               left = compare(left, parseAdditive(), "<");
            } else {
               return left;
            }
         }
      }

      private VFXExpression parseAdditive() {
         VFXExpression left = parseMultiplicative();
         while (true) {
            if (match("+")) {
               VFXExpression leftNode = left;
               VFXExpression right = parseMultiplicative();
               left = (vars, random) -> leftNode.eval(vars, random) + right.eval(vars, random);
            } else if (match("-")) {
               VFXExpression leftNode = left;
               VFXExpression right = parseMultiplicative();
               left = (vars, random) -> leftNode.eval(vars, random) - right.eval(vars, random);
            } else {
               return left;
            }
         }
      }

      private VFXExpression parseMultiplicative() {
         VFXExpression left = parseUnary();
         while (true) {
            if (match("*")) {
               VFXExpression leftNode = left;
               VFXExpression right = parseUnary();
               left = (vars, random) -> leftNode.eval(vars, random) * right.eval(vars, random);
            } else if (match("/")) {
               VFXExpression leftNode = left;
               VFXExpression right = parseUnary();
               left = (vars, random) -> {
                  float divisor = right.eval(vars, random);
                  return divisor == 0.0F ? 0.0F : leftNode.eval(vars, random) / divisor;
               };
            } else if (match("%")) {
               VFXExpression leftNode = left;
               VFXExpression right = parseUnary();
               left = (vars, random) -> {
                  float divisor = right.eval(vars, random);
                  return divisor == 0.0F ? 0.0F : leftNode.eval(vars, random) % divisor;
               };
            } else {
               return left;
            }
         }
      }

      private VFXExpression parseUnary() {
         if (match("!")) {
            VFXExpression inner = parseUnary();
            return (vars, random) -> inner.eval(vars, random) > 0.0F ? 0.0F : 1.0F;
         }
         if (match("-")) {
            VFXExpression inner = parseUnary();
            return (vars, random) -> -inner.eval(vars, random);
         }
         return parsePrimary();
      }

      private VFXExpression parsePrimary() {
         skipWhitespace();
         if (match("(")) {
            VFXExpression expression = parseExpression();
            if (!match(")")) {
               throw new IllegalArgumentException("Missing ')' in expression: " + input);
            }
            return expression;
         }
         if (peekDigit()) {
            return parseNumber();
         }
         String ident = parseIdentifier();
         if (match("(")) {
            List<VFXExpression> args = new ArrayList<>();
            if (!match(")")) {
               do {
                  args.add(parseExpression());
               } while (match(","));
               if (!match(")")) {
                  throw new IllegalArgumentException("Missing ')' after function " + ident + " in expression: " + input);
               }
            }
            return function(ident, args);
         }
         return variable(ident);
      }

      private VFXExpression parseNumber() {
         int start = index;
         while (index < input.length() && (Character.isDigit(input.charAt(index)) || input.charAt(index) == '.')) {
            index++;
         }
         float value = Float.parseFloat(input.substring(start, index));
         return (vars, random) -> value;
      }

      private String parseIdentifier() {
         skipWhitespace();
         int start = index;
         while (index < input.length() && (Character.isLetterOrDigit(input.charAt(index)) || input.charAt(index) == '_')) {
            index++;
         }
         if (start == index) {
            throw new IllegalArgumentException("Expected identifier at " + index + " in expression: " + input);
         }
         return input.substring(start, index).toLowerCase(Locale.ROOT);
      }

      private boolean peekDigit() {
         return index < input.length() && (Character.isDigit(input.charAt(index)) || input.charAt(index) == '.');
      }

      private boolean match(String token) {
         skipWhitespace();
         if (input.startsWith(token, index)) {
            index += token.length();
            return true;
         }
         return false;
      }

      private void skipWhitespace() {
         while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
         }
      }
   }

   private static VFXExpression variable(String name) {
      return switch (name) {
         case "true" -> (vars, random) -> 1.0F;
         case "false" -> (vars, random) -> 0.0F;
         case "pi" -> (vars, random) -> (float)Math.PI;
         case "tau" -> (vars, random) -> (float)(Math.PI * 2.0);
         default -> (vars, random) -> vars.getOrDefault(name, 0.0F);
      };
   }

   private static VFXExpression function(String name, List<VFXExpression> args) {
      return (vars, random) -> switch (name) {
         case "random" -> random.nextFloat();
         case "sin" -> Mth.sin(arg(args, 0, vars, random));
         case "cos" -> Mth.cos(arg(args, 0, vars, random));
         case "tan" -> (float)Math.tan(arg(args, 0, vars, random));
         case "sqrt" -> Mth.sqrt(Math.max(0.0F, arg(args, 0, vars, random)));
         case "abs" -> Math.abs(arg(args, 0, vars, random));
         case "floor" -> (float)Math.floor(arg(args, 0, vars, random));
         case "ceil" -> (float)Math.ceil(arg(args, 0, vars, random));
         case "round" -> Math.round(arg(args, 0, vars, random));
         case "frac" -> arg(args, 0, vars, random) - (float)Math.floor(arg(args, 0, vars, random));
         case "sign" -> Math.signum(arg(args, 0, vars, random));
         case "mod" -> mod(arg(args, 0, vars, random), arg(args, 1, vars, random));
         case "pow" -> (float)Math.pow(arg(args, 0, vars, random), arg(args, 1, vars, random));
         case "exp" -> (float)Math.exp(arg(args, 0, vars, random));
         case "log" -> {
            float value = arg(args, 0, vars, random);
            yield value <= 0.0F ? 0.0F : (float)Math.log(value);
         }
         case "min" -> Math.min(arg(args, 0, vars, random), arg(args, 1, vars, random));
         case "max" -> Math.max(arg(args, 0, vars, random), arg(args, 1, vars, random));
         case "clamp" -> Mth.clamp(arg(args, 0, vars, random), arg(args, 1, vars, random), arg(args, 2, vars, random));
         case "lerp" -> Mth.lerp(arg(args, 0, vars, random), arg(args, 1, vars, random), arg(args, 2, vars, random));
         case "step" -> arg(args, 1, vars, random) < arg(args, 0, vars, random) ? 0.0F : 1.0F;
         case "smoothstep" -> smoothstep(arg(args, 0, vars, random), arg(args, 1, vars, random), arg(args, 2, vars, random));
         case "radians" -> (float)Math.toRadians(arg(args, 0, vars, random));
         case "deg" -> (float)Math.toDegrees(arg(args, 0, vars, random));
         default -> throw new IllegalArgumentException("Unknown function '" + name + "'");
      };
   }

   private static VFXExpression compare(VFXExpression left, VFXExpression right, String op) {
      return (vars, random) -> {
         float l = left.eval(vars, random);
         float r = right.eval(vars, random);
         return switch (op) {
            case ">=" -> l >= r ? 1.0F : 0.0F;
            case "<=" -> l <= r ? 1.0F : 0.0F;
            case "==" -> Math.abs(l - r) < 1.0E-6F ? 1.0F : 0.0F;
            case "!=" -> Math.abs(l - r) >= 1.0E-6F ? 1.0F : 0.0F;
            case ">" -> l > r ? 1.0F : 0.0F;
            case "<" -> l < r ? 1.0F : 0.0F;
            default -> 0.0F;
         };
      };
   }

   private static float arg(List<VFXExpression> args, int index, Map<String, Float> vars, Random random) {
      return index < args.size() ? args.get(index).eval(vars, random) : 0.0F;
   }

   private static float mod(float value, float divisor) {
      return divisor == 0.0F ? 0.0F : value % divisor;
   }

   private static float smoothstep(float edge0, float edge1, float x) {
      if (edge0 == edge1) {
         return x < edge0 ? 0.0F : 1.0F;
      }
      float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }
}

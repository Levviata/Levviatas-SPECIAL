package com.levviata.lspecial;

import java.util.Map;

/**
 * safe arithmetic-only expression evaluator; never executes Java code.
 */
public final class LSPECIALExpression {

    private LSPECIALExpression() {
    }

    /**
     * evaluates an arithmetic expression using the supplied variables.
     *
     * @param source   the expression to evaluate
     * @param vars     variable names and their values
     * @param fallback value returned when the expression is invalid
     * @return the evaluated result, or {@code fallback} if evaluation fails
     */
    public static double eval(String source, Map<String, Double> vars, double fallback) {
        try {
            Parser parser = new Parser(source, vars);
            double result = parser.expression();

            parser.skipWhitespace();

            return parser.index == parser.source.length() && Double.isFinite(result) ? result : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static final class Parser {

        private final String source;
        private final Map<String, Double> variables;
        private int index;

        private Parser(String source, Map<String, Double> variables) {
            if (source == null) {
                throw new IllegalArgumentException("Expression cannot be null.");
            }

            this.source = source;
            this.variables = variables;
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            skipWhitespace();

            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }

            return false;
        }

        /*
         * grammar:
         * expression = term (('+' | '-') term)*
         */
        private double expression() {
            double value = term();

            while (true) {
                if (consume('+')) {
                    value += term();
                } else if (consume('-')) {
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        /*
         * term = unary (('*' | '/' | '%') unary)*
         */
        private double term() {
            double value = unary();

            while (true) {
                if (consume('*')) {
                    value *= unary();
                } else if (consume('/')) {
                    double divisor = unary();

                    if (divisor == 0) {
                        throw new ArithmeticException("Division by zero.");
                    }

                    value /= divisor;
                } else if (consume('%')) {
                    double divisor = unary();

                    if (divisor == 0) {
                        throw new ArithmeticException("Modulo by zero.");
                    }

                    value %= divisor;
                } else {
                    return value;
                }
            }
        }

        private double unary() {
            if (consume('+')) {
                return unary();
            }

            if (consume('-')) {
                return -unary();
            }

            return atom();
        }

        private double atom() {
            skipWhitespace();

            if (consume('(')) {
                double value = expression();

                if (!consume(')')) {
                    throw new IllegalArgumentException("Missing closing parenthesis.");
                }

                return value;
            }

            if (index < source.length()
                    && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) {
                return number();
            }

            String name = identifier();

            if (name.isEmpty()) {
                throw new IllegalArgumentException("Expected a number, variable, or function.");
            }

            if (consume('(')) {
                return function(name);
            }

            Double value = variables.get(name);

            if (value == null) {
                throw new IllegalArgumentException("Unknown variable: " + name);
            }

            return value;
        }

        private double function(String name) {
            double firstArgument = expression();

            if (consume(',')) {
                double secondArgument = expression();

                if (!consume(')')) {
                    throw new IllegalArgumentException("Missing closing parenthesis.");
                }

                if ("min".equals(name)) {
                    return Math.min(firstArgument, secondArgument);
                }

                if ("max".equals(name)) {
                    return Math.max(firstArgument, secondArgument);
                }

                throw new IllegalArgumentException("Unknown two-argument function: " + name);
            }

            if (!consume(')')) {
                throw new IllegalArgumentException("Missing closing parenthesis.");
            }

            if ("abs".equals(name)) {
                return Math.abs(firstArgument);
            }

            if ("sqrt".equals(name) && firstArgument >= 0) {
                return Math.sqrt(firstArgument);
            }

            if ("floor".equals(name)) {
                return Math.floor(firstArgument);
            }

            if ("ceil".equals(name)) {
                return Math.ceil(firstArgument);
            }

            if ("round".equals(name)) {
                return Math.rint(firstArgument);
            }

            throw new IllegalArgumentException("Unknown or invalid one-argument function: " + name);
        }

        private String identifier() {
            skipWhitespace();

            int start = index;

            while (index < source.length()) {
                char character = source.charAt(index);

                if (!Character.isLetterOrDigit(character) && character != '_') {
                    break;
                }

                index++;
            }

            return source.substring(start, index);
        }

        private double number() {
            skipWhitespace();

            int start = index;

            while (index < source.length()) {
                char character = source.charAt(index);

                if (!Character.isDigit(character) && character != '.') {
                    break;
                }

                index++;
            }

            if (index < source.length()
                    && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                index++;

                if (index < source.length()
                        && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                }

                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }

            return Double.parseDouble(source.substring(start, index));
        }
    }
}
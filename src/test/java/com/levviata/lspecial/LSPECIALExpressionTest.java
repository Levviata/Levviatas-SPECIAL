package com.levviata.lspecial;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-logic tests for the config formula evaluator, no Minecraft classes needed.
 */
class LSPECIALExpressionTest {
    private static final double FALLBACK = -1.0D;
    private static final double DELTA = 1.0E-9D;

    private static double eval(String source) {
        return LSPECIALExpression.eval(source, Collections.emptyMap(), FALLBACK);
    }

    @Test
    void respectsOperatorPrecedence() {
        assertEquals(14.0D, eval("2 + 3 * 4"), DELTA);
        assertEquals(20.0D, eval("(2 + 3) * 4"), DELTA);
        assertEquals(1.0D, eval("7 % 3"), DELTA);
    }

    @Test
    void handlesUnarySigns() {
        assertEquals(2.0D, eval("-3 + 5"), DELTA);
        assertEquals(3.0D, eval("--3"), DELTA);
        assertEquals(3.0D, eval("+3"), DELTA);
    }

    @Test
    void readsVariables() {
        Map<String, Double> vars = new HashMap<>();
        vars.put("strength", 10.0D);
        vars.put("originalDamage", 4.0D);
        assertEquals(5.0D, LSPECIALExpression.eval("strength * 0.5", vars, FALLBACK), DELTA);
        assertEquals(9.0D, LSPECIALExpression.eval("originalDamage + strength / 2", vars, FALLBACK), DELTA);
    }

    @Test
    void supportsTheDocumentedFunctions() {
        assertEquals(2.0D, eval("min(2, 5)"), DELTA);
        assertEquals(5.0D, eval("max(2, 5)"), DELTA);
        assertEquals(3.0D, eval("abs(-3)"), DELTA);
        assertEquals(3.0D, eval("floor(3.9)"), DELTA);
        assertEquals(4.0D, eval("sqrt(16)"), DELTA);
    }

    @Test
    void returnsTheFallbackForInvalidInput() {
        assertEquals(FALLBACK, eval("1 / 0"), DELTA);
        assertEquals(FALLBACK, eval("5 % 0"), DELTA);
        assertEquals(FALLBACK, eval("unknownVariable + 1"), DELTA);
        assertEquals(FALLBACK, eval("(1 + 2"), DELTA);
        assertEquals(FALLBACK, eval("1 + 2)"), DELTA);
        assertEquals(FALLBACK, eval("1 +"), DELTA);
        assertEquals(FALLBACK, eval(""), DELTA);
        assertEquals(FALLBACK, eval("nope(3)"), DELTA);
        assertEquals(FALLBACK, LSPECIALExpression.eval(null, Collections.emptyMap(), FALLBACK), DELTA);
    }
}

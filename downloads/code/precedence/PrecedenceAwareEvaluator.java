package example;

import java.util.Stack;

/**
 * Given a list of terms whose parts are combined using operators with varying precedence, evaluate all terms
 * that need evaluating, and return the combined result.
 * <p>
 * This particular implementation is a BOOLEAN evaluator, ie expects the junction.operator to be either
 * AND or OR, and does some special optimisations to avoid evaluating terms in some cases. However the
 * general approach here should be applicable to more general sets of operators, eg evaluating a complex
 * term where the values are integers and the operators are the traditional integer math operators.
 * </p>
 */
public class PrecedenceAwareEvaluator {

    public boolean evaluate(Term expr) {
        EvalContext ctx = new EvalContext();
        return ctx.eval(expr);
    }

    // ============================================================================================

    private interface Value {
        boolean get();
    }

    private static class LazySimpleTermValue implements Value {
        final TermSimple term;

        LazySimpleTermValue(TermSimple term) {
            this.term = term;
        }

        public boolean get() {
            return TermSimpleEvaluator.evaluate(term);
        }
    }

    private static class LazyCompoundTermValue implements Value {
        final Term expression;

        LazyCompoundTermValue(Term expression) {
            this.expression = expression;
        }

        public boolean get() {
            // Recursively evaluate a nested condition
            EvalContext ctx = new EvalContext();
            return ctx.eval(expression);
        }
    }

    private static class LiteralValue implements Value {
        final boolean value;
        LiteralValue(boolean v) {
            value = v;
        }
        public boolean get() {
            return value;
        }
    }

    private static class EvalContext {
        // yes, Stack is deprecated in favour of Deque - but the name "Stack" makes sense here..
        private final Stack<Value> values = new Stack<>();
        private final Stack<Operator> ops = new Stack<>();
        
        boolean eval(Term expr) {
            if (expr instanceof TermSimple) {
                return TermSimpleEvaluator.evaluate((TermSimple) expr);
            }

            TermCompound compoundTerm = (TermCompound) expr;

            Term initial = compoundTerm.getInitial();
            pushValue(initial);

            for(Junction j : compoundTerm.getJunctions()) {
                Term term = j.getTerm();
                pushValue(j.getTerm());

                Operator boolOp = j.getOperator();
                reduce(boolOp.getPrecedence());
                ops.push(boolOp);
            }
            
            reduce(Integer.MIN_VALUE);
            
            if (values.size() != 1) {
                throw new IllegalStateException();
            }
            
            return values.get(0).get();
        }

        private void pushValue(Term term) {
            if (term instanceof TermSimple) {
                values.push(new LazySimpleTermValue((TermSimple) term));
            } else {
                values.push(new LazyCompoundTermValue(term));
            }
        }

        private void reduce(int nextPrec) {
            // while the latest op on the stack has higher precedence than the upcoming one, eval it
            while (!ops.isEmpty() && ops.peek().getPrecedence() >= nextPrec) {
                Operator op = ops.pop();
                Value rhs = values.pop();
                Value lhs = values.pop();
                boolean b = apply(op, lhs, rhs);
                values.push(new LiteralValue(b));
            }
        }
        
        private boolean apply(Operator op, Value lhs, Value rhs) {
            switch(op) {
            case AND: {
                // do short-circuit evaluation if possible (only eval rhs when needed)
                boolean val = lhs.get();
                if (val == true) {
                    val = rhs.get();
                }
                return val;
            }
            
            case OR: {
                // do short-circuit evaluation if possible (only eval rhs when needed)
                boolean val = lhs.get();
                if (val == false) {
                    val = rhs.get();
                }
                return val;
            }
            
            default:
                throw new UnsupportedOperationException("Unknown boolean operation!");
            }
        }
    }
}

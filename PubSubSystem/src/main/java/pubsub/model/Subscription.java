package pubsub.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Subscription implements Serializable {
    private final String subscriberId;
    private final List<Filter> filters;

    public Subscription(String subscriberId, List<Filter> filters) {
        this.subscriberId = Objects.requireNonNull(subscriberId);
        this.filters      = List.copyOf(filters);
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("subscriber=").append(subscriberId).append("; ");
        for (int i = 0; i < filters.size(); i++) {
            sb.append(filters.get(i));
            if (i < filters.size() - 1) sb.append("; ");
        }
        sb.append("}");
        return sb.toString();
    }

    public record Filter(String field, Operator op, String value) implements Serializable {
            public Filter(String field, Operator op, String value) {
                this.field = Objects.requireNonNull(field);
                this.op = Objects.requireNonNull(op);
                this.value = Objects.requireNonNull(value);
            }

        public String getField() {
            return field;
        }

        public Operator getOp() {
            return op;
        }

        public String getValue() {
            return value;
        }

        @Override
            public String toString() {
                boolean quote = field.equals("city")
                        || field.equals("direction")
                        || field.equals("date");
                return "(" + field + op.symbol + (quote ? "\"" + value + "\"" : value) + ")";
            }
        }

    public enum Operator {
        EQ("=",  "=="),
        NE("!=", "!="),
        GT(">",  ">"),
        LT("<",  "<"),
        GE(">=", ">="),
        LE("<=", "<=");

        public final String symbol;
        public final String desc;

        private static final Map<String,Operator> BY_SYMBOL = new HashMap<>();
        static {
            for (Operator op : values()) {
                BY_SYMBOL.put(op.symbol, op);
            }
        }

        Operator(String symbol, String desc) {
            this.symbol = symbol;
            this.desc   = desc;
        }

        public static Operator fromSymbol(String sym) {
            Operator op = BY_SYMBOL.get(sym);
            if (op == null) throw new IllegalArgumentException("Unknown operator: " + sym);
            return op;
        }
    }
}

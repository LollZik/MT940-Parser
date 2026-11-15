package mt940parser.model;
import java.math.BigDecimal;

public enum DebitCredit {
    CREDIT,
    DEBIT;

    public BigDecimal applyTo(BigDecimal amount) {
        return switch (this) {
            case CREDIT -> amount;
            case DEBIT -> amount.negate();
        };
    }
}

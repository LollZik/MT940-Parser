package mt940parser.parser;

import java.util.List;
import java.util.Objects;

record RawStatement(List<RawField> fields) {
    RawStatement {
        Objects.requireNonNull(fields, "List of raw fields must not be null");
        fields = List.copyOf(fields);
    }
}
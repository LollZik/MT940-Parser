package mt940parser.parser;

import java.util.Objects;

record RawField(String tag, String value, int line) {
    RawField{
        Objects.requireNonNull(tag, "Tag must not be null");
        Objects.requireNonNull(value, "Value must not be null");
    }
}


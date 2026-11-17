package mt940parser.validation;

import mt940parser.model.Statement;

import java.util.Objects;

public record ValidationIssue(IssueType issue, Statement statement, String message) {
    public ValidationIssue {
        Objects.requireNonNull(issue, "Issue must not be null");
        Objects.requireNonNull(statement, "Statement must not be null");
        Objects.requireNonNull(message, "Message must not be null");
    }
}

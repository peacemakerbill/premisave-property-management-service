package com.premisave.property.email;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Everything a transactional email says, independent of how it looks.
 * rendered by templates/email/transaction.html, and also flattened into the
 * plain-text alternative, so the two can never drift apart.
 */
@Getter
@Builder
public class EmailContent {

    @Builder.Default
    private final EmailTone tone = EmailTone.INFO;

    private final String preheader;         // inbox preview text
    private final String title;
    private final String subtitle;
    private final String greetingName;      // null => "there"

    @Singular("paragraph")
    private final List<String> paragraphs;

    private final String amountLabel;
    private final String amount;            // already formatted, e.g. "1,500.00"
    private final String currency;

    @Singular("section")
    private final List<Section> sections;

    private final Callout callout;
    private final String ctaLabel;
    private final String ctaUrl;
    private final String footnote;

    @Value
    public static class Row {
        String label;
        String value;
        String badge;           // optional pill next to the value
        EmailTone badgeTone;

        public static Row of(String label, String value) {
            return new Row(label, value, null, null);
        }

        public static Row withBadge(String label, String value, String badge, EmailTone badgeTone) {
            return new Row(label, value, badge, badgeTone);
        }
    }

    @Value
    public static class Section {
        String title;
        List<Row> rows;

        public static Section of(String title, List<Row> rows) {
            return new Section(title, rows);
        }
    }

    @Value
    public static class Callout {
        EmailTone tone;
        String title;
        String text;
    }

    public String toPlainText(String brandName, String supportEmail) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append('\n');
        if (subtitle != null && !subtitle.isBlank()) {
            sb.append(subtitle).append('\n');
        }
        sb.append("\nHi ").append(greetingName != null && !greetingName.isBlank() ? greetingName : "there")
                .append(",\n\n");
        for (String paragraph : paragraphs) {
            sb.append(paragraph).append("\n\n");
        }
        if (amount != null) {
            sb.append(amountLabel != null ? amountLabel : "Amount").append(": ")
                    .append(currency != null ? currency + " " : "").append(amount).append("\n\n");
        }
        for (Section section : sections) {
            if (section.getTitle() != null) {
                sb.append(section.getTitle().toUpperCase()).append('\n');
            }
            for (Row row : section.getRows()) {
                sb.append("- ").append(row.getLabel()).append(": ").append(row.getValue());
                if (row.getBadge() != null) {
                    sb.append(" [").append(row.getBadge()).append(']');
                }
                sb.append('\n');
            }
            sb.append('\n');
        }
        if (callout != null) {
            if (callout.getTitle() != null) {
                sb.append(callout.getTitle()).append(": ");
            }
            sb.append(callout.getText()).append("\n\n");
        }
        if (ctaUrl != null) {
            sb.append(ctaLabel != null ? ctaLabel : "Open").append(": ").append(ctaUrl).append("\n\n");
        }
        if (footnote != null && !footnote.isBlank()) {
            sb.append(footnote).append("\n\n");
        }
        sb.append("--\n").append(brandName).append(" | ").append(supportEmail).append('\n')
                .append("This is an automated message. Please do not reply directly to this email.\n");
        return sb.toString();
    }
}
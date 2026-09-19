package com.premisave.property.email;

/** Colour + icon theme for an email (hero icon, amount card, callouts, badges). */
public enum EmailTone {

    SUCCESS("\u2713", "#1E7A4F", "#E7F3EC", "#CFE6D9", "#14532D"),
    WARNING("!",      "#B7791F", "#FFF6E0", "#F3DFA8", "#7A4E0B"),
    DANGER("\u2715",  "#C0392B", "#FDECEC", "#F8D7D7", "#7A2E2E"),
    INFO("i",         "#1A3C34", "#EEF3F1", "#D6E2DE", "#1A3C34");

    private final String icon;
    private final String accent;
    private final String soft;
    private final String border;
    private final String ink;

    EmailTone(String icon, String accent, String soft, String border, String ink) {
        this.icon = icon;
        this.accent = accent;
        this.soft = soft;
        this.border = border;
        this.ink = ink;
    }

    public String getIcon() { return icon; }
    public String getAccent() { return accent; }
    public String getSoft() { return soft; }
    public String getBorder() { return border; }
    public String getInk() { return ink; }
}
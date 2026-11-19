package com.premisave.property.email;

/**
 * Colour and icon theme for an email (hero icon, amount card, callouts, badges). The colours match the auth service emails.
 * css is the class the layout's dark mode rules know about, so a callout stays readable on a dark background.
 */
public enum EmailTone {

    SUCCESS("✓", "#1F8A4C", "#EAF8EF", "#BFE6CD", "#14532D", "ok"),
    WARNING("!",      "#B7791F", "#FFF7E0", "#F4DC9B", "#6B4A00", "amber"),
    DANGER("✕",  "#D92D4A", "#FFECEF", "#F6C3CC", "#7A1428", "red"),
    INFO("i",         "#4A3FDB", "#EEEBFF", "#D9D4FA", "#2F2A80", "info");

    private final String icon;
    private final String accent;
    private final String soft;
    private final String border;
    private final String ink;
    private final String css;

    EmailTone(String icon, String accent, String soft, String border, String ink, String css) {
        this.icon = icon;
        this.accent = accent;
        this.soft = soft;
        this.border = border;
        this.ink = ink;
        this.css = css;
    }

    public String getIcon() { return icon; }
    public String getAccent() { return accent; }
    public String getSoft() { return soft; }
    public String getBorder() { return border; }
    public String getInk() { return ink; }
    public String getCss() { return css; }
}

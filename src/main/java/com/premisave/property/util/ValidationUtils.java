package com.premisave.property.util;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        return phone.matches("^\\+?[0-9]{10,15}$");
    }

    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private ValidationUtils() {}
}
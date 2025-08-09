package com.oddlabs.matchmaking;

import com.oddlabs.util.Utils;

import java.io.Serializable;

public final strictfp class LoginDetails implements Serializable {
    private static final long serialVersionUID = 1;

    public static final int MAX_EMAIL_LENGTH = 60;

    private final String email;

    public LoginDetails(String email) {
        this.email = email;
    }

    public final boolean equals(Object other) {
        if (!(other instanceof LoginDetails)) return false;
        LoginDetails other_login = (LoginDetails) other;
        return other_login.getEmail().equals(email);
    }

    public final int hashCode() {
        return email.hashCode();
    }

    public final boolean isValid() {
        return email != null
                && email.length() <= MAX_EMAIL_LENGTH
                && email.matches(Utils.EMAIL_PATTERN);
    }

    public final String getEmail() {
        return email;
    }
}

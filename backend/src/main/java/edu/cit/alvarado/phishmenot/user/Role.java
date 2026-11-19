package edu.cit.alvarado.phishmenot.user;

/** User roles defined in SRS Appendix C (USERS.role). */
public enum Role {
    USER,
    ADMIN;

    /** Spring Security authority name, e.g. {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }
}

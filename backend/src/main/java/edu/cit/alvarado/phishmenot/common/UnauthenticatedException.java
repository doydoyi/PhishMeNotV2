package edu.cit.alvarado.phishmenot.common;

/** The request has no valid session, or the session's account no longer exists. */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("Authentication required");
    }
}

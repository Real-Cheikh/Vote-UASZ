package sn.uasz.voteuasz.common;

public class VoteException extends RuntimeException {
    public VoteException(String message) {
        super(message);
    }

    public VoteException(String message, Throwable cause) {
        super(message, cause);
    }
}

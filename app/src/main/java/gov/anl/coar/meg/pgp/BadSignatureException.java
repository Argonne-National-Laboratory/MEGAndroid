package gov.anl.coar.meg.pgp;

/**
 * Created by greg on 5/10/16.
 */
public class BadSignatureException extends Exception{
    public BadSignatureException(String s) {
        super(s);
    }
}

package gov.anl.coar.meg.exception;

import com.github.kevinsawicki.http.HttpRequest;

/**
 * Created by greg on 3/5/16.
 */
public class NoInstanceIdException extends Exception{
    HttpRequest cHttpRequest;
    public NoInstanceIdException(HttpRequest request) {
        super("Unable to get GCM instance id. code:" + request.code());
        cHttpRequest = request;
    }
}

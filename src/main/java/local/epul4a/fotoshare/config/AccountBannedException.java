package local.epul4a.fotoshare.config;

import org.springframework.security.authentication.AccountStatusException;

public class AccountBannedException extends AccountStatusException {
    
    public AccountBannedException(String msg) {
        super(msg);
    }
    
    public AccountBannedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}


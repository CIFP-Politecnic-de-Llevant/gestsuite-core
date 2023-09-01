package cat.politecnicllevant.core.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface AuthService {
    GoogleIdToken verifyGoogleUser(String token) throws GeneralSecurityException, IOException;
}

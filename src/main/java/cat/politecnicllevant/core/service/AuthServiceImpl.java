package cat.politecnicllevant.core.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;


@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Value("${gc.clientId}")
    private String clientId;

    public GoogleIdToken verifyGoogleUser(String token) throws GeneralSecurityException, IOException {
        log.info("Token Google: "+token);
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        GoogleIdTokenVerifier tokenVerifier = new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance())
                // Specify the CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(this.clientId))
                .build();
        return tokenVerifier.verify(token);
    }

}


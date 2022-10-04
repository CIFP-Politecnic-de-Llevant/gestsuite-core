package cat.iesmanacor.core.controller;

import cat.iesmanacor.common.model.Notificacio;
import cat.iesmanacor.common.model.NotificacioTipus;
import cat.iesmanacor.core.dto.gestib.RolDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.service.AuthService;
import cat.iesmanacor.core.service.TokenManager;
import cat.iesmanacor.core.service.UsuariService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private TokenManager tokenManager;

    @Value("${gc.adminUser}")
    private String administrador;

    @Value("${gc.adminDeveloper}")
    private String adminDeveloper;

    @Value("${centre.domini.principal}")
    private String dominiPrincipal;

    @PostMapping("/auth/google/login")
    public ResponseEntity<?> loginUserGoogle(@RequestBody String token) throws GeneralSecurityException, IOException {

        log.info("Token:" + token);

        GoogleIdToken idToken = authService.verifyGoogleUser(token);

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();

            // Print user identifier
            String userId = payload.getSubject();
            System.out.println("User ID: " + userId);

            // Get profile information from payload
            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            String locale = (String) payload.get("locale");
            String familyName = (String) payload.get("family_name");
            String givenName = (String) payload.get("given_name");

            UsuariDto usuari = usuariService.findByEmail(email);

            if (emailVerified && usuari != null) {
                System.out.println("create token with e-mail: " + email);
                Set<RolDto> rols = new HashSet<>();
                if (usuari.getRols() != null) {
                    rols.addAll(usuari.getRols());
                }

                if (usuari.getGestibProfessor() != null && usuari.getGestibProfessor()) {
                    rols.add(RolDto.PROFESSOR);
                }

                if (usuari.getGestibAlumne() != null && usuari.getGestibAlumne()) {
                    rols.add(RolDto.ALUMNE);
                }
                return new ResponseEntity<>(tokenManager.createToken(email,rols.stream().map(Enum::toString).collect(Collectors.toList())), HttpStatus.OK);
            } else{
                List<UsuariDto> usuaris = usuariService.findAll();

                //Comprovem si la BBDD és buida i es tracta de la primera càrrega
                //o bé és l'usuari desenvolupador
                if( (email.equals(this.administrador) && usuaris.isEmpty()) || email.equals(this.adminDeveloper)){
                    Set<RolDto> rols = new HashSet<>();
                    rols.add(RolDto.ADMINISTRADOR);
                    return new ResponseEntity<>(tokenManager.createToken(email,rols.stream().map(Enum::toString).collect(Collectors.toList())), HttpStatus.OK);
                }
            }
        } else {
            Notificacio notificacio = new Notificacio();
            notificacio.setNotifyMessage("Token erroni");
            notificacio.setNotifyType(NotificacioTipus.ERROR);

            return new ResponseEntity<>(notificacio, HttpStatus.UNAUTHORIZED);
        }

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Usuari no autoritzat!");
        notificacio.setNotifyType(NotificacioTipus.ERROR);

        return new ResponseEntity<>(notificacio, HttpStatus.UNAUTHORIZED);
    }
}

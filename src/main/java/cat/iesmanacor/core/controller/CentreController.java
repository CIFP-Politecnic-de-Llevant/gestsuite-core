package cat.iesmanacor.core.controller;

import cat.iesmanacor.common.model.Notificacio;
import cat.iesmanacor.common.model.NotificacioTipus;
import cat.iesmanacor.core.dto.gestib.CentreDto;
import cat.iesmanacor.core.service.CentreService;
import cat.iesmanacor.core.service.GMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
public class CentreController {

    @Autowired
    private CentreService centreService;

    @Autowired
    private GMailService gMailService;

    @Value("${centre.usuaris.passwordinicial}")
    private String passwordInicial;

    @PostMapping("/proves/gmail")
    public void provesGmail() throws MessagingException, GeneralSecurityException, IOException {
        gMailService.helloWorld();
    }

    @GetMapping("/proves/notificacio")
    public ResponseEntity<Notificacio> provesNotificacio() throws MessagingException, GeneralSecurityException, IOException {
        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Prova de notificació. Cap acció realitzada.");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);
        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @GetMapping("/centre/{id}")
    public ResponseEntity<CentreDto> getCentreByIdentificador(@PathVariable("id") String identificador) {
        CentreDto p = centreService.findByIdentificador(identificador);
        return new ResponseEntity<>(p, HttpStatus.OK);
    }

    @GetMapping("/centre/password-inicial")
    public ResponseEntity<String> getPasswordInicial() {
        return new ResponseEntity<>(this.passwordInicial, HttpStatus.OK);
    }

}
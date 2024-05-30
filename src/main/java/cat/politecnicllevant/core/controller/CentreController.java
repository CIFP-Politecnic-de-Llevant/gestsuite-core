package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.core.dto.gestib.CentreDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.service.CentreService;
import cat.politecnicllevant.core.service.GMailService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

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

    /*@GetMapping("/centre/{id}")
    public ResponseEntity<CentreDto> getCentreByIdentificador(@PathVariable("id") String identificador) {
        CentreDto p = centreService.findByIdentificador(identificador);
        return new ResponseEntity<>(p, HttpStatus.OK);
    }*/

    @GetMapping("/centre")
    public ResponseEntity<CentreDto> getCentre() {
        CentreDto c = centreService.findAll().get(0);
        return new ResponseEntity<>(c, HttpStatus.OK);
    }

    @GetMapping("/centre/password-inicial")
    public ResponseEntity<String> getPasswordInicial() {
        return new ResponseEntity<>(this.passwordInicial, HttpStatus.OK);
    }

    @PostMapping("/public/centre/sincronitzant")
    public ResponseEntity<Boolean> isSincronitzant() {
        List<CentreDto> centres = centreService.findAll();
        if(!centres.isEmpty()){
            return new ResponseEntity<>(centres.get(0).getSincronitzant(), HttpStatus.OK);
        }
        return new ResponseEntity<>(false, HttpStatus.OK);
    }

}
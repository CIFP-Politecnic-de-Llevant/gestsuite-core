package cat.iesmanacor.core.controller;

import cat.iesmanacor.common.model.Notificacio;
import cat.iesmanacor.common.model.NotificacioTipus;
import cat.iesmanacor.core.dto.gestib.CentreDto;
import cat.iesmanacor.core.dto.gestib.SessioDto;
import cat.iesmanacor.core.service.CentreService;
import cat.iesmanacor.core.service.GMailService;
import cat.iesmanacor.core.service.SessioService;
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
import java.util.List;

@RestController
public class SessioController {

    @Autowired
    private SessioService sessioService;


    @GetMapping("/public/sessio/llistat")
    public ResponseEntity<List<SessioDto>> getSessions() {
        List<SessioDto> sessions = sessioService.findAll();
        return new ResponseEntity<>(sessions, HttpStatus.OK);
    }

    @GetMapping("/public/sessio/{idgrup}")
    public ResponseEntity<List<SessioDto>> getSessionsByGrup(@PathVariable("idgrup") Long idgrup) {
        List<SessioDto> sessions = sessioService.findByGrup(idgrup);
        return new ResponseEntity<>(sessions, HttpStatus.OK);
    }

    @GetMapping("/sessio/pares")
    public ResponseEntity<List<SessioDto>> getSessionsAtencioPares() {
        List<SessioDto> sessions = sessioService.findSessionsPares();
        return new ResponseEntity<>(sessions, HttpStatus.OK);
    }

}
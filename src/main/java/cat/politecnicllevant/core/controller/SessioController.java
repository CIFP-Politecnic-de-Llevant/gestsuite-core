package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.gestib.SessioDto;
import cat.politecnicllevant.core.service.SessioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
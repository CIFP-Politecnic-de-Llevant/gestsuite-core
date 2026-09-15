package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.core.dto.gestib.CursDto;
import cat.politecnicllevant.core.dto.gestib.GrupDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.service.*;
import cat.politecnicllevant.core.service.CursService;
import cat.politecnicllevant.core.service.GrupService;
import cat.politecnicllevant.core.service.UsuariService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class CursController {

    @Autowired
    private CursService cursService;

    @Autowired
    private GrupService grupService;

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private AlumneGSuiteService alumneGSuiteService;

    @Autowired
    private Gson gson;


    @GetMapping("/curs/getById/{id}")
    public ResponseEntity<CursDto> getCursById(@PathVariable("id") Long identificador) {
        CursDto curs = cursService.findById(identificador);
        return new ResponseEntity<>(curs, HttpStatus.OK);
    }

    @GetMapping("/curs/getByCodiGestib/{id}")
    public ResponseEntity<CursDto> getCursByCodiGestib(@PathVariable("id") String identificador) {
        CursDto curs = cursService.findByGestibIdentificador(identificador);
        return new ResponseEntity<>(curs, HttpStatus.OK);
    }

    @GetMapping({"/curs/llistat","/public/curs/llistat"})
    public ResponseEntity<List<CursDto>> getCursos() {
        List<CursDto> cursos = cursService.findAll().stream().filter(c->c.getActiu()).collect(Collectors.toList());
        return new ResponseEntity<>(cursos, HttpStatus.OK);
    }

    @PostMapping("/curs/desa")
    public ResponseEntity<Notificacio> desaCurs(@RequestBody String json) throws InterruptedException {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        Long idCurs = jsonObject.get("id").getAsLong();

        CursDto curs = cursService.findById(idCurs);

        List<GrupDto> grups = grupService.findAll().stream().filter(g -> g.getActiu() && g.getGestibCurs().equals(curs.getGestibIdentificador())).collect(Collectors.toList());

        //Un alumne pot ser a més d'un grup del curs: el processam una sola vegada
        Map<Long, UsuariDto> alumnes = new LinkedHashMap<>();
        for (GrupDto grup : grups) {
            for (UsuariDto alumne : usuariService.findUsuarisByGestibGrup(grup.getGestibIdentificador())) {
                alumnes.putIfAbsent(alumne.getIdusuari(), alumne);
            }
        }

        //Si un alumne falla continuam amb la resta i avisam al final
        Notificacio notificacio = alumneGSuiteService.actualitzaAlumnes(alumnes.values(), "Curs desat.");

        cursService.save(curs);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

}
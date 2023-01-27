package cat.iesmanacor.core.controller;

import cat.iesmanacor.core.dto.gestib.CursDto;
import cat.iesmanacor.core.dto.gestib.GrupDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.service.CursService;
import cat.iesmanacor.core.service.GrupService;
import cat.iesmanacor.core.service.TokenManager;
import cat.iesmanacor.core.service.UsuariService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class GrupController {

    @Autowired
    private CursService cursService;

    @Autowired
    private GrupService grupService;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private UsuariService usuariService;

    @GetMapping({"/grup/llistat","/public/grup/llistat"})
    public ResponseEntity<List<GrupDto>> getGrups() {
        List<GrupDto> grups = grupService.findAll().stream().filter(GrupDto::getActiu).collect(Collectors.toList());
        return new ResponseEntity<>(grups, HttpStatus.OK);
    }

    @GetMapping("/grup/findByCurs/{idcurs}")
    public ResponseEntity<List<GrupDto>> getGrupsByCurs(@PathVariable("idcurs") Long idcurs){
        CursDto curs = cursService.findById(idcurs);
        List<GrupDto> grups = grupService.findAll().stream().filter(g->g.getActiu() && g.getGestibCurs().equals(curs.getGestibIdentificador())).collect(Collectors.toList());
        return new ResponseEntity<>(grups, HttpStatus.OK);
    }

    @GetMapping("/grup/getById/{idgrup}")
    public ResponseEntity<GrupDto> getById(@PathVariable("idgrup") Long idgrup){
        GrupDto grup = grupService.findById(idgrup);
        return new ResponseEntity<>(grup, HttpStatus.OK);
    }

    @GetMapping("/grup/getByGestibIdentificador/{idgrup}")
    public ResponseEntity<GrupDto> getByGestibIdentificador(@PathVariable("idgrup") String idgrup){
        GrupDto grup = grupService.findByGestibIdentificador(idgrup);
        return new ResponseEntity<>(grup, HttpStatus.OK);
    }

    @GetMapping("/grup/getGrupsByTutor/{idusuari}")
    public ResponseEntity<List<GrupDto>> getGrupsByTutor(@PathVariable("idusuari") Long idusuari){
        UsuariDto usuari = usuariService.findById(idusuari);
        List<GrupDto> grups = grupService.findByTutor(usuari);
        return new ResponseEntity<>(grups, HttpStatus.OK);
    }

}
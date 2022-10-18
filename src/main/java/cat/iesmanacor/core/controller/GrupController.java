package cat.iesmanacor.core.controller;

import cat.iesmanacor.core.dto.gestib.CursDto;
import cat.iesmanacor.core.dto.gestib.GrupDto;
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

    @GetMapping("grup/findByCurs/{idcurs}")
    public ResponseEntity<List<GrupDto>> getGrupsByCurs(@PathVariable("idcurs") Long idcurs){
        CursDto curs = cursService.findById(idcurs);
        List<GrupDto> grups = grupService.findAll().stream().filter(g->g.getActiu() && g.getGestibCurs().equals(curs.getGestibIdentificador())).collect(Collectors.toList());
        return new ResponseEntity<>(grups, HttpStatus.OK);
    }

}
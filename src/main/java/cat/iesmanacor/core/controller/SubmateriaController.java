package cat.iesmanacor.core.controller;

import cat.iesmanacor.core.dto.gestib.CursDto;
import cat.iesmanacor.core.dto.gestib.GrupDto;
import cat.iesmanacor.core.dto.gestib.SubmateriaDto;
import cat.iesmanacor.core.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SubmateriaController {

    @Autowired
    private SubmateriaService submateriaService;


    @GetMapping({"/submateria/llistat","/public/submateria/llistat"})
    public ResponseEntity<List<SubmateriaDto>> getGrups() {
        List<SubmateriaDto> submateries = submateriaService.findAll();
        return new ResponseEntity<>(submateries, HttpStatus.OK);
    }


}
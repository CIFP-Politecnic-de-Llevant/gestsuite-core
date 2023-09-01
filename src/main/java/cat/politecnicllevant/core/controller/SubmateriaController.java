package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.gestib.SubmateriaDto;
import cat.iesmanacor.core.service.*;
import cat.politecnicllevant.core.service.SubmateriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
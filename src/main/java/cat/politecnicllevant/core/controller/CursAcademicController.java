package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.common.service.UtilService;
import cat.politecnicllevant.core.dto.gestib.CursAcademicDto;
import cat.politecnicllevant.core.dto.gestib.CursDto;
import cat.politecnicllevant.core.dto.gestib.GrupDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.service.*;
import com.google.api.services.directory.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CursAcademicController {

    @Autowired
    private CursAcademicService cursAcademicService;

    @Autowired
    private Gson gson;


    @GetMapping("/cursAcademic/getById/{id}")
    public ResponseEntity<CursAcademicDto> getCursAcademicById(@PathVariable("id") Long identificador) {
        CursAcademicDto cursAcademic = cursAcademicService.findById(identificador);
        return new ResponseEntity<>(cursAcademic, HttpStatus.OK);
    }

    @GetMapping("/cursAcademic/actual")
    public ResponseEntity<CursAcademicDto> getActualCursAcademic() {
        CursAcademicDto cursAcademic = cursAcademicService.findActual();
        return new ResponseEntity<>(cursAcademic, HttpStatus.OK);
    }

}
package cat.iesmanacor.core.controller;

import cat.iesmanacor.common.model.Notificacio;
import cat.iesmanacor.common.model.NotificacioTipus;
import cat.iesmanacor.core.dto.gestib.DepartamentDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.service.DepartamentService;
import cat.iesmanacor.core.service.UsuariService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DepartamentController {

    @Autowired
    private DepartamentService departamentService;

    @Autowired
    private UsuariService usuariService;


    @Autowired
    private Gson gson;


    @GetMapping("/departament/getById/{id}")
    public ResponseEntity<DepartamentDto> getDepartamentById(@PathVariable("id") Long identificador) {
        DepartamentDto departament = departamentService.findById(identificador);
        return new ResponseEntity<>(departament, HttpStatus.OK);
    }

    @GetMapping("/departament/getByCodiGestib/{id}")
    public ResponseEntity<DepartamentDto> getDepartamentByCodiGestib(@PathVariable("id") String identificador) {
        DepartamentDto departament = departamentService.findByGestibIdentificador(identificador);
        return new ResponseEntity<>(departament, HttpStatus.OK);
    }

    @GetMapping("/departament/llistat")
    public ResponseEntity<List<DepartamentDto>> getDepartaments() {
        List<DepartamentDto> departaments = departamentService.findAll();
        return new ResponseEntity<>(departaments, HttpStatus.OK);
    }

    @PostMapping("/departament/desa")
    public ResponseEntity<Notificacio> desa(@RequestBody String json) {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        Long idDepartament = null;
        Long idCapDepartament = null;

        if(jsonObject.get("id")!=null && !jsonObject.get("id").isJsonNull()){
            idDepartament = jsonObject.get("id").getAsLong();
        }

        if(jsonObject.get("capDepartament")!=null && !jsonObject.get("capDepartament").isJsonNull()){
            idCapDepartament = jsonObject.get("capDepartament").getAsJsonObject().get("id").getAsLong();
        }

        if(idDepartament!=null && idCapDepartament!=null){
            DepartamentDto departament = departamentService.findById(idDepartament);
            UsuariDto capDepartament = usuariService.findById(idCapDepartament);

            departament.setCapDepartament(capDepartament);
            departamentService.save(departament);
        }

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Departament desat correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);
        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

}
package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.common.service.UtilService;
import cat.politecnicllevant.core.dto.gestib.CursDto;
import cat.politecnicllevant.core.dto.gestib.GrupDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.service.*;
import cat.politecnicllevant.core.service.CursService;
import cat.politecnicllevant.core.service.GSuiteService;
import cat.politecnicllevant.core.service.GrupService;
import cat.politecnicllevant.core.service.UsuariService;
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
public class CursController {

    @Autowired
    private CursService cursService;

    @Autowired
    private GrupService grupService;

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private GSuiteService gSuiteService;


    @Autowired
    private Gson gson;

    @Value("${centre.gsuite.fullname.professors}")
    private String formatNomGSuiteProfessors;

    @Value("${centre.gsuite.fullname.alumnes}")
    private String formatNomGSuiteAlumnes;


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

        for(GrupDto grup: grups){
            List<UsuariDto> alumnes = usuariService.findUsuarisByGestibGrup(grup.getGestibIdentificador());

            for(UsuariDto alumne: alumnes){

                String nom = alumne.getGestibNom();
                String cognoms = alumne.getGestibCognom1() + " " + alumne.getGestibCognom2();

                if(formatNomGSuiteAlumnes.equals("nomcognom1cognom2")){
                    nom = UtilService.capitalize(nom);
                    cognoms = UtilService.capitalize(cognoms);
                } else if(formatNomGSuiteAlumnes.equals("nomcognom1cognom2cursgrup")){
                    if (curs.getGestibNom() == null || curs.getGestibNom().isEmpty() || grup.getGestibNom() == null || grup.getGestibNom().isEmpty()) {
                        cognoms = alumne.getGestibCognom1() + " " + alumne.getGestibCognom2();
                    } else {
                        cognoms = alumne.getGestibCognom1() + " " + alumne.getGestibCognom2() + " " + curs.getGestibNom() + grup.getGestibNom();
                    }
                    nom = UtilService.capitalize(nom);
                    cognoms = UtilService.capitalize(cognoms);
                }

                User user = gSuiteService.updateUser(alumne.getGsuiteEmail(), nom, cognoms , alumne.getGestibCodi(), grup.getGsuiteUnitatOrganitzativa());

                if(user == null){
                    Notificacio notificacio = new Notificacio();
                    notificacio.setNotifyMessage("Error desant l'alumne "+alumne.getGestibNom() + " " + alumne.getGestibCognom1() + " " + alumne.getGestibCognom2() + ". Comprovi que la Unitat Organitzativa és correcte.");
                    notificacio.setNotifyType(NotificacioTipus.ERROR);
                    return new ResponseEntity<>(notificacio, HttpStatus.NOT_ACCEPTABLE);
                }
            }
        }

        cursService.save(curs);

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Curs desat correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);
        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

}
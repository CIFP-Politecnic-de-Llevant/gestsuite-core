package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.common.service.UtilService;
import cat.politecnicllevant.core.dto.gestib.CursDto;
import cat.politecnicllevant.core.dto.gestib.GrupDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.service.*;
import com.google.api.services.directory.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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

    @Autowired
    private GSuiteService gSuiteService;

    @Autowired
    private Gson gson;

    @Value("${centre.gsuite.fullname.alumnes}")
    private String formatNomGSuiteAlumnes;


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

    @PostMapping("/grup/desaUO")
    public ResponseEntity<Notificacio> desaUnitatOrganitzativa(@RequestBody String json) throws InterruptedException {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        Long idGrup = jsonObject.get("idgrup").getAsLong();
        String unitatOrganitzativa = jsonObject.get("uo").getAsString();

        GrupDto grup = grupService.findById(idGrup);
        grup.setGsuiteUnitatOrganitzativa(unitatOrganitzativa);
        grupService.save(grup);

        //Actualitzem nom, cognoms i UO dels alumnes
        List<UsuariDto> alumnes = usuariService.findUsuarisByGestibGrup(grup.getGestibIdentificador());

        for(UsuariDto alumne: alumnes){

            String nom = alumne.getGestibNom();
            String cognoms = alumne.getGestibCognom1() + " " + alumne.getGestibCognom2();

            if(formatNomGSuiteAlumnes.equals("nomcognom1cognom2")){
                nom = UtilService.capitalize(nom);
                cognoms = UtilService.capitalize(cognoms);
            } else if(formatNomGSuiteAlumnes.equals("nomcognom1cognom2cursgrup")){
                CursDto curs = cursService.findByGestibIdentificador(grup.getGestibCurs());
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

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Unitat Organitzativa del grup desada correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);
        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }
}
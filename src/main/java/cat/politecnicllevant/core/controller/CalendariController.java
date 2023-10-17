package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.dto.google.*;
import cat.politecnicllevant.core.dto.google.*;
import cat.politecnicllevant.core.service.CalendariService;
import cat.politecnicllevant.core.service.GSuiteService;
import cat.politecnicllevant.core.service.GrupCorreuService;
import cat.politecnicllevant.core.service.UsuariService;
import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.directory.model.CalendarResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@RestController
public class CalendariController {

    @Autowired
    private GSuiteService gSuiteService;

    @Autowired
    private CalendariService calendariService;

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private GrupCorreuService grupCorreuService;

    @GetMapping("/calendari/llistat")
    public ResponseEntity<List<CalendariDto>> getCalendaris(HttpServletRequest request) throws GeneralSecurityException, IOException {
        /*
        Sincronització GSuite -> BBDD
        Si el grup NO existeix creem el grup a la BBDD i associem els usuaris
        Si el grup SI existeix actualitzem els membres de la BBDD
         */
        List<CalendarResource> calendarisGSuite = gSuiteService.getCalendars();

        if(calendarisGSuite != null) {
            for (CalendarResource calendariGSuite : calendarisGSuite) {

                CalendariDto calendari = calendariService.findByEmail(calendariGSuite.getResourceEmail());

                if (calendari == null) {
                    //Creem el calendari a la BBDD
                    calendariService.save(calendariGSuite.getResourceEmail(), calendariGSuite.getResourceName(), calendariGSuite.getResourceDescription(), null);
                }
            }
        }

        List<CalendariDto> calendaris = calendariService.findAll();

        return new ResponseEntity<>(calendaris, HttpStatus.OK);
    }

    @GetMapping("calendari/findByEmail/{email}")
    public ResponseEntity<CalendariDto> getCalendariByEmail(@PathVariable("email") String email) throws GeneralSecurityException, IOException {
        CalendariDto calendari = calendariService.findByEmail(email);

        List<UsuariDto> usuaris = usuariService.findAll();
        List<GrupCorreuDto> grupsCorreu = grupCorreuService.findAll();

        List<AclRule> usauarisPermis = gSuiteService.getUsersByCalendar(email);

        List<UsuariDto> usuarisLectura = new ArrayList<>();
        List<UsuariDto> usuarisEscriptura = new ArrayList<>();
        List<GrupCorreuDto> grupsLectura = new ArrayList<>();
        List<GrupCorreuDto> grupsEscriptura = new ArrayList<>();

        for (AclRule rule : usauarisPermis) {
            if(rule.getRole().equals(CalendariRolDto.LECTOR.getRol()) && rule.getScope().getType().equals(CalendariTipusUsuariDto.USUARI.getTipus())){
                UsuariDto usuari = usuaris.stream().filter(u->u.getGsuiteEmail()!=null && u.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
                if(usuari!=null) {
                    usuarisLectura.add(usuari);
                }
            } else if(rule.getRole().equals(CalendariRolDto.LECTOR_ESCRIPTOR.getRol()) && rule.getScope().getType().equals(CalendariTipusUsuariDto.USUARI.getTipus())) {
                UsuariDto usuari = usuaris.stream().filter(u->u.getGsuiteEmail()!=null && u.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
                if(usuari!=null) {
                    usuarisEscriptura.add(usuari);
                }
            }else if(rule.getRole().equals(CalendariRolDto.LECTOR.getRol()) && rule.getScope().getType().equals(CalendariTipusUsuariDto.GRUP.getTipus())) {
                GrupCorreuDto grupCorreu = grupsCorreu.stream().filter(gc->gc.getGsuiteEmail()!=null && gc.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
                if(grupCorreu!=null) {
                    grupsLectura.add(grupCorreu);
                }
            }else if(rule.getRole().equals(CalendariRolDto.LECTOR_ESCRIPTOR.getRol()) && rule.getScope().getType().equals(CalendariTipusUsuariDto.GRUP.getTipus())) {
                GrupCorreuDto grupCorreu = grupsCorreu.stream().filter(gc->gc.getGsuiteEmail()!=null && gc.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
                if(grupCorreu!=null) {
                    grupsEscriptura.add(grupCorreu);
                }
            }

            if(calendari!=null) {
                calendari.setUsuarisLectura(new HashSet<>(usuarisLectura));
                calendari.setUsuarisEdicio(new HashSet<>(usuarisEscriptura));
                calendari.setGrupCorreuLectura(new HashSet<>(grupsLectura));
                calendari.setGrupCorreuEdicio(new HashSet<>(grupsEscriptura));
            }

            //System.out.println(rule.getId() + ": " + rule.getRole()+"::"+rule.getScope().getValue()+"::"+rule.getScope().getType());
        }

        return new ResponseEntity<>(calendari, HttpStatus.OK);
    }

    @PostMapping("/calendari/sync")
    public void sincronitzaCalendaris(){
        List<CalendariDto> calendaris = calendariService.findAll();

        for(CalendariDto calendariDto: calendaris){
            System.out.println(calendariDto.getGsuiteNom());
            for(GrupCorreuDto grupCorreuDto: calendariDto.getGrupCorreuEdicio()){
                gSuiteService.insertUserCalendar(grupCorreuDto.getGsuiteEmail(),calendariDto.getGsuiteEmail(),CalendariRolDto.LECTOR_ESCRIPTOR,CalendariTipusUsuariDto.USUARI);
            }
            for(GrupCorreuDto grupCorreuDto: calendariDto.getGrupCorreuLectura()){
                gSuiteService.insertUserCalendar(grupCorreuDto.getGsuiteEmail(),calendariDto.getGsuiteEmail(),CalendariRolDto.LECTOR,CalendariTipusUsuariDto.USUARI);
            }
            for(UsuariDto usuariDto: calendariDto.getUsuarisEdicio()){
                gSuiteService.insertUserCalendar(usuariDto.getGsuiteEmail(),calendariDto.getGsuiteEmail(),CalendariRolDto.LECTOR_ESCRIPTOR,CalendariTipusUsuariDto.USUARI);
            }
            for(UsuariDto usuariDto: calendariDto.getUsuarisLectura()){
                gSuiteService.insertUserCalendar(usuariDto.getGsuiteEmail(),calendariDto.getGsuiteEmail(),CalendariRolDto.LECTOR,CalendariTipusUsuariDto.USUARI);
            }
        }

    }
}
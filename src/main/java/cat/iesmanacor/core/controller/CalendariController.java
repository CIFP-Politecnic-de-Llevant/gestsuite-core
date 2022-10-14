package cat.iesmanacor.core.controller;

import cat.iesmanacor.core.dto.gestib.CursDto;
import cat.iesmanacor.core.dto.gestib.GrupDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.dto.google.*;
import cat.iesmanacor.core.service.CalendariService;
import cat.iesmanacor.core.service.GSuiteService;
import cat.iesmanacor.core.service.GrupCorreuService;
import cat.iesmanacor.core.service.UsuariService;
import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.directory.model.CalendarResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

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

        for (CalendarResource calendariGSuite : calendarisGSuite) {

            CalendariDto calendari = calendariService.findByEmail(calendariGSuite.getResourceEmail());

            if (calendari == null) {
                //Creem el calendari a la BBDD
                calendariService.save(calendariGSuite.getResourceEmail(), calendariGSuite.getResourceName(), calendariGSuite.getResourceDescription(), null, CalendariTipusDto.GENERAL);
            }
        }

        List<CalendariDto> calendaris = calendariService.findAll();

        return new ResponseEntity<>(calendaris, HttpStatus.OK);
    }

    @GetMapping("calendari/findByEmail/{email}")
    public ResponseEntity<CalendariDto> getGrupsByCurs(@PathVariable("email") String email) throws InterruptedException, GeneralSecurityException, IOException {
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
                UsuariDto usuari = usuaris.stream().filter(u->u.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
                if(usuari!=null) {
                    usuarisLectura.add(usuari);
                }
            } else if(rule.getRole().equals(CalendariRolDto.LECTOR_ESCRIPTOR.getRol()) && rule.getScope().getType().equals(CalendariTipusUsuariDto.USUARI.getTipus())) {
                UsuariDto usuari = usuaris.stream().filter(u->u.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
                if(usuari!=null) {
                    usuarisEscriptura.add(usuari);
                }
            }else if(rule.getRole().equals(CalendariRolDto.LECTOR.getRol()) && rule.getScope().getType().equals(CalendariTipusUsuariDto.GRUP.getTipus())) {
                GrupCorreuDto grupCorreu = grupsCorreu.stream().filter(gc->gc.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
                if(grupCorreu!=null) {
                    grupsLectura.add(grupCorreu);
                }
            }else if(rule.getRole().equals(CalendariRolDto.LECTOR_ESCRIPTOR.getRol()) && rule.getScope().getType().equals(CalendariTipusUsuariDto.GRUP.getTipus())) {
                GrupCorreuDto grupCorreu = grupsCorreu.stream().filter(gc->gc.getGsuiteEmail().equals(rule.getScope().getValue())).findFirst().orElse(null);
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

}
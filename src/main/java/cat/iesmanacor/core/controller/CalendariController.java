package cat.iesmanacor.core.controller;

import cat.iesmanacor.core.dto.google.CalendariDto;
import cat.iesmanacor.core.dto.google.CalendariTipusDto;
import cat.iesmanacor.core.service.CalendariService;
import cat.iesmanacor.core.service.GSuiteService;
import com.google.api.services.directory.model.CalendarResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
public class CalendariController {

    @Autowired
    private GSuiteService gSuiteService;

    @Autowired
    private CalendariService calendariService;

    @GetMapping("/calendari/llistat")
    public ResponseEntity<List<CalendariDto>> getGrups(HttpServletRequest request) throws GeneralSecurityException, IOException {
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

}
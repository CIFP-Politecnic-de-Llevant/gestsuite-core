package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.core.dto.gestib.DepartamentDto;
import cat.politecnicllevant.core.dto.gestib.RolDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.service.DepartamentService;
import cat.politecnicllevant.core.service.GSuiteService;
import cat.politecnicllevant.core.service.TokenManager;
import cat.politecnicllevant.core.service.UsuariService;
import com.google.api.services.directory.model.Group;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class UsuariController {

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private GSuiteService gSuiteService;

    @Autowired
    private DepartamentService departamentService;

    @Autowired
    private TokenManager tokenManager;

    @Value("${gc.adminUser}")
    private String administrador;

    @Value("${gc.adminDeveloper}")
    private String adminDeveloper;

    @Value("${centre.usuaris.passwordinicial}")
    private String passwordInicial;

    @GetMapping("/usuaris/llistat/actius")
    public ResponseEntity<List<UsuariDto>> getUsuarisActius() {
        List<UsuariDto> usuaris = usuariService.findUsuarisActius();
        return new ResponseEntity<>(usuaris, HttpStatus.OK);
    }

    @GetMapping("/usuaris/llistat/pendentssuspendre")
    public ResponseEntity<List<UsuariDto>> getUsuarisPendentsSuspendre() {
        List<UsuariDto> usuaris = usuariService.findUsuarisNoActius().stream().filter(usuari -> (usuari.getGsuiteSuspes() == null || !usuari.getGsuiteSuspes()) && (usuari.getGsuiteEliminat() == null || !usuari.getGsuiteEliminat())).collect(Collectors.toList());
        return new ResponseEntity<>(usuaris, HttpStatus.OK);
    }

    @GetMapping("/usuaris/llistat/suspesos")
    public ResponseEntity<List<UsuariDto>> getUsuarisSuspesos() {
        List<UsuariDto> usuaris = usuariService.findUsuarisNoActius().stream().filter(usuari -> usuari.getGsuiteSuspes() != null && usuari.getGsuiteSuspes()).collect(Collectors.toList());
        return new ResponseEntity<>(usuaris, HttpStatus.OK);
    }

    @GetMapping("/usuaris/llistat/eliminats")
    public ResponseEntity<List<UsuariDto>> getUsuarisEliminats() {
        List<UsuariDto> usuaris = usuariService.findUsuarisNoActius().stream().filter(usuari -> usuari.getGsuiteEliminat() != null && usuari.getGsuiteEliminat()).collect(Collectors.toList());
        return new ResponseEntity<>(usuaris, HttpStatus.OK);
    }

    @GetMapping("/usuaris/findByDepartament/{id}")
    public ResponseEntity<List<UsuariDto>> getUsuarisByDepartament(@PathVariable("id") Long idDepartament) {
        DepartamentDto departament = departamentService.findById(idDepartament);
        List<UsuariDto> usuaris = usuariService.findUsuarisByDepartament(departament);
        return new ResponseEntity<>(usuaris, HttpStatus.OK);
    }

    @PostMapping("/usuaris/suspendre")
    public ResponseEntity<Notificacio> suspendreUsuaris(@RequestBody List<UsuariDto> usuaris) throws GeneralSecurityException, IOException, InterruptedException {

        for (UsuariDto usuari : usuaris) {
            //Esborrem grups
            log.info("Usuari:" + usuari.getGsuiteFullName() + "Email:" + usuari.getGsuiteEmail());
            List<Group> grups = gSuiteService.getUserGroups(usuari.getGsuiteEmail());
            for (Group grup : grups) {
                log.info("Esborrant grup de correu " + grup.getEmail() + " de l'usuari " + usuari.getGsuiteEmail());
                gSuiteService.deleteMember(usuari.getGsuiteEmail(), grup.getEmail());
            }
            gSuiteService.suspendreUser(usuari.getGsuiteEmail(), true);

            //Suspenem a la BBDD
            usuariService.suspendreUsuari(usuari);
        }

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Usuaris suspesos correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @PostMapping("/usuaris/reactivar")
    public ResponseEntity<Notificacio> reactivarUsuaris(@RequestBody List<UsuariDto> usuaris) throws InterruptedException {
        for (UsuariDto usuari : usuaris) {
            gSuiteService.suspendreUser(usuari.getGsuiteEmail(), false);

            //Reactiva a la BBDD
            usuariService.reactivaUsuari(usuari);
        }

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Usuaris suspesos correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @GetMapping({"/usuaris/profile/rol", "/auth/profile/rol"})
    public ResponseEntity<Set<RolDto>> getRolsUsuari(HttpServletRequest request) {
        Claims claims = tokenManager.getClaims(request);

        String email = (String) claims.get("email");
        System.out.println("Email usuari profile: " + email);
        UsuariDto usuari = usuariService.findByEmail(email);

        if (usuari == null || email == null) {
            List<UsuariDto> usuaris = usuariService.findAll();

            //Comprovem si la BBDD és buida i es tracta de la primera càrrega
            //o bé es tracta de l'usuari desenvolupador
            if (usuaris.isEmpty() || email.equals(this.adminDeveloper)) {
                Set<RolDto> rols = new HashSet<>();
                rols.add(RolDto.ADMINISTRADOR);
                return new ResponseEntity<>(rols, HttpStatus.OK);
            }

            System.out.println("Usuari o email són null");
            return null;
        }
        System.out.println("Email rol: " + email + " Usuari rol: " + usuari.getGsuiteEmail() + " " + usuari.getGsuiteFullName());

        Set<RolDto> rols = new HashSet<>();
        if (usuari.getRols() != null) {
            rols.addAll(usuari.getRols());
        }

        if (usuari.getGestibProfessor() != null && usuari.getGestibProfessor()) {
            rols.add(RolDto.PROFESSOR);
        }

        if (usuari.getGestibAlumne() != null && usuari.getGestibAlumne()) {
            rols.add(RolDto.ALUMNE);
        }

        return new ResponseEntity<>(rols, HttpStatus.OK);
    }

    @GetMapping("/usuaris/profile/{id}")
    public ResponseEntity<UsuariDto> getProfile(@PathVariable("id") String idUsuari, HttpServletRequest request) throws Exception {
        Claims claims = tokenManager.getClaims(request);
        String myEmail = (String) claims.get("email");

        UsuariDto myUser = usuariService.findByEmail(myEmail);
        UsuariDto usuari = usuariService.findById(Long.valueOf(idUsuari));

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        if (
                myEmail.equals(this.adminDeveloper) ||
                (
                        myUser != null && usuari != null && myUser.getGsuiteEmail() != null && usuari.getGsuiteEmail() != null &&
                        (myUser.getGsuiteEmail().equals(usuari.getGsuiteEmail()) || myUser.getRols().contains(RolDto.ADMINISTRADOR) || myUser.getRols().contains(RolDto.DIRECTOR) || myUser.getRols().contains(RolDto.CAP_ESTUDIS))
                )
        ) {
            return new ResponseEntity<>(usuari, HttpStatus.OK);
        } else {
            throw new Exception("Sense permisos");
        }
    }

    @GetMapping("/usuaris/profile")
    public ResponseEntity<UsuariDto> getProfile(HttpServletRequest request) throws Exception {
        Claims claims = tokenManager.getClaims(request);
        String myEmail = (String) claims.get("email");

        UsuariDto myUser = usuariService.findByEmail(myEmail);

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        if (myUser != null && myUser.getGsuiteEmail() != null) {
            return new ResponseEntity<>(myUser, HttpStatus.OK);
        } else {
            throw new Exception("Sense permisos");
        }
    }

    @GetMapping("/usuaris/profile-by-gestib-codi/{id}")
    public ResponseEntity<UsuariDto> getUsuariByGestibCodi(@PathVariable("id") String gestibCodi, HttpServletRequest request) throws Exception {
        Claims claims = tokenManager.getClaims(request);
        String myEmail = (String) claims.get("email");

        UsuariDto myUser = usuariService.findByEmail(myEmail);
        UsuariDto usuari = usuariService.findByGestibCodi(gestibCodi);

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        if (
                myEmail.equals(this.adminDeveloper) ||
                (
                    myUser != null && usuari != null && myUser.getGsuiteEmail() != null && usuari.getGsuiteEmail() != null &&
                    (myUser.getGsuiteEmail().equals(usuari.getGsuiteEmail()) || myUser.getRols().contains(RolDto.ADMINISTRADOR) || myUser.getRols().contains(RolDto.DIRECTOR) || myUser.getRols().contains(RolDto.CAP_ESTUDIS))
                )
        ) {
            return new ResponseEntity<>(usuari, HttpStatus.OK);
        }

        if (usuari == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/usuaris/profile-by-email/{id}")
    public ResponseEntity<UsuariDto> getUsuariByEmail(@PathVariable("id") String email, HttpServletRequest request) throws Exception {
        Claims claims = tokenManager.getClaims(request);
        String myEmail = (String) claims.get("email");

        System.out.println("email"+email+"myEmail"+myEmail);

        UsuariDto myUser = usuariService.findByEmail(myEmail);
        UsuariDto usuari = usuariService.findByEmail(email);

        System.out.println("email"+usuari+"myEmail"+myUser);

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        if (
                myEmail.equals(this.adminDeveloper) ||
                        (
                                myUser != null && usuari != null && myUser.getGsuiteEmail() != null && usuari.getGsuiteEmail() != null &&
                                        (myUser.getGsuiteEmail().equals(usuari.getGsuiteEmail()) || myUser.getRols().contains(RolDto.ADMINISTRADOR) || myUser.getRols().contains(RolDto.DIRECTOR) || myUser.getRols().contains(RolDto.CAP_ESTUDIS))
                        )
        ) {
            return new ResponseEntity<>(usuari, HttpStatus.OK);
        } else {
            throw new Exception("Sense permisos");
        }
    }

    @PostMapping("/usuari/reset")
    public ResponseEntity<Notificacio> resetPassword(@RequestBody Map<String, String> json) throws GeneralSecurityException, IOException, InterruptedException {
        gSuiteService.resetPassword(json.get("usuari"), this.passwordInicial);

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Contrasenya canviada correctament. La nova contrasenya és: " + this.passwordInicial);
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @GetMapping("/public/usuaris/profile/{id}")
    public ResponseEntity<UsuariDto> getPublicProfile(@PathVariable("id") String idUsuari) throws Exception {
        UsuariDto usuari = usuariService.findById(Long.valueOf(idUsuari));

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        if (usuari != null && usuari.getGestibProfessor() != null && usuari.getGestibProfessor()) {
            return new ResponseEntity<>(usuari, HttpStatus.OK);
        } else {
            throw new Exception("Sense permisos");
        }
    }
}
package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.core.dto.gestib.*;
import cat.politecnicllevant.core.service.*;
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
import java.util.*;
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
    private GrupService grupService;

    @Autowired
    private CursService cursService;

    @Autowired
    private SessioService sessioService;

    @Autowired
    private SubmateriaService submateriaService;

    @Autowired
    private ActivitatService activitatService;

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

    @PostMapping("/usuaris/desaRols")
    public ResponseEntity<Notificacio> desaRols(@RequestBody UsuariDto usuariDto) {
//        UsuariDto usuariOriginal = usuariService.findById(usuariDto.getIdusuari());
//        usuariOriginal.setRols(usuariDto.getRols());

        usuariService.save(usuariDto);

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Rols d'usuari desats correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);
        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }
    
    @GetMapping("/usuaris/findbynumexpedient/{numexpedient}")
    public ResponseEntity<UsuariDto> getUsuariByNumExpedient(@PathVariable("numexpedient") String numExpedient) {
        UsuariDto usuari = usuariService.findUsuariByGestibExpedient(numExpedient);
        return new ResponseEntity<>(usuari, HttpStatus.OK);
    }

    @GetMapping("/usuaris/profile/{id}")
    public ResponseEntity<UsuariDto> getProfile(@PathVariable("id") String idUsuari, HttpServletRequest request) throws Exception {
        Claims claims = tokenManager.getClaims(request);
        String myEmail = (String) claims.get("email");

        UsuariDto myUser = usuariService.findByEmail(myEmail);
        UsuariDto usuari = usuariService.findById(Long.valueOf(idUsuari));

        List<String> rolsClaim = (List<String>)claims.get("rols");
        Set<RolDto> rols = rolsClaim.stream().map(RolDto::valueOf).collect(Collectors.toSet());

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        /** TODO: PROFESSORS FILTRAR MILLOR **/
        if (
                myEmail.equals(this.adminDeveloper) ||
                        (
                                myUser != null &&
                                        usuari != null &&
                                        myUser.getGsuiteEmail() != null &&
//                                        usuari.getGsuiteEmail() != null &&
                                        (
                                                myUser.getGsuiteEmail().equals(usuari.getGsuiteEmail()) ||
                                                        rols.contains(RolDto.ADMINISTRADOR) ||
                                                        rols.contains(RolDto.DIRECTOR) ||
                                                        rols.contains(RolDto.CAP_ESTUDIS) ||
                                                        rols.contains(RolDto.PROFESSOR)
                                        )
                        )
        ) {
            return new ResponseEntity<>(usuari, HttpStatus.OK);
        }

        if (usuari == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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

    @GetMapping("/usuaris/tutorfct")
    public ResponseEntity<List<UsuariDto>> getTutorFCT() {
        List<UsuariDto> tutorsFCT = new ArrayList<>();

        // Carreguem d'una vegada les submatèries i activitats FCT i els professors que les imparteixen,
        // en lloc de consultar-les sessió a sessió per a cada professor
        Set<String> codisSubmateriesFCT = submateriaService.findAll().stream()
                .filter(s -> esFCT(s.getGestibNom(), s.getGestibNomCurt()))
                .map(SubmateriaDto::getGestibIdentificador)
                .collect(Collectors.toSet());
        Set<String> codisActivitatsFCT = activitatService.findAll().stream()
                .filter(a -> esFCT(a.getGestibNom(), a.getGestibNomCurt()))
                .map(ActivitatDto::getGestibIdentificador)
                .collect(Collectors.toSet());

        Set<String> codisProfessorsFCT = sessioService
                .findSessionsProfessorsBySubmateriesOrActivitats(codisSubmateriesFCT, codisActivitatsFCT).stream()
                .map(SessioDto::getGestibProfessor)
                .collect(Collectors.toSet());

        List<UsuariDto> professors = usuariService.findProfessors();
        for (UsuariDto profe : professors) {
            if (!Boolean.TRUE.equals(profe.getActiu())) {
                continue;
            }

            // Imparteix alguna submatèria o activitat FCT
            boolean imparteixFCT = profe.getGestibCodi() != null && codisProfessorsFCT.contains(profe.getGestibCodi());

            //Comprovem també si té el rol de Tutor FCT
            boolean rolTutorFCT = profe.getRols() != null && profe.getRols().contains(RolDto.TUTOR_FCT);

            if (imparteixFCT || rolTutorFCT) {
                tutorsFCT.add(profe);
            }
        }

        return new ResponseEntity<>(tutorsFCT, HttpStatus.OK);
    }

    @GetMapping("/usuaris/tutorfct-by-codigrup/{cursgrup}")
    public ResponseEntity<List<UsuariDto>> getTutorFCTByCodiGrup(@PathVariable("cursgrup") String cursGrup) {
        String codiCurs = cursGrup.substring(0, cursGrup.length() - 1);
        String codiGrup = cursGrup.substring(cursGrup.length() - 1);

        System.out.println("Curs: " + codiCurs + " Grup: " + codiGrup);

        List<UsuariDto> tutorsFCT = new ArrayList<>();

        List<CursDto> cursos = cursService.findByGestibNom(codiCurs);
        if (cursos == null || cursos.isEmpty()) {
            return new ResponseEntity<>(tutorsFCT, HttpStatus.OK);
        }
        CursDto curs = cursos.get(0);

        List<GrupDto> grups = grupService.findByGestibNomAndCurs(codiGrup, curs.getGestibIdentificador());
        if (grups == null || grups.isEmpty()) {
            return new ResponseEntity<>(tutorsFCT, HttpStatus.OK);
        }
        GrupDto grup = grups.get(0);

        // Carreguem d'una vegada les submatèries del curs, les activitats FCT i les sessions que les imparteixen,
        // en lloc de consultar-les sessió a sessió per a cada professor
        List<SubmateriaDto> submateriesCurs = submateriaService.findByCurs(curs.getGestibIdentificador());
        Set<String> codisSubmateriesCurs = submateriesCurs.stream()
                .map(SubmateriaDto::getGestibIdentificador)
                .collect(Collectors.toSet());
        Set<String> codisSubmateriesFCT = submateriesCurs.stream()
                .filter(s -> esFCT(s.getGestibNom(), s.getGestibNomCurt()))
                .map(SubmateriaDto::getGestibIdentificador)
                .collect(Collectors.toSet());
        Set<String> codisActivitatsFCT = activitatService.findAll().stream()
                .filter(a -> esFCT(a.getGestibNom(), a.getGestibNomCurt()))
                .map(ActivitatDto::getGestibIdentificador)
                .collect(Collectors.toSet());

        Map<String, List<SessioDto>> sessionsPerProfessor = sessioService
                .findSessionsProfessorsBySubmateriesOrActivitats(codisSubmateriesCurs, codisActivitatsFCT).stream()
                .collect(Collectors.groupingBy(SessioDto::getGestibProfessor));

        List<UsuariDto> professors = usuariService.findProfessors();
        for (UsuariDto profe : professors) {
            String codiProfe = profe.getGestibCodi();
            boolean actiu = Boolean.TRUE.equals(profe.getActiu());
            List<SessioDto> sessions = codiProfe != null ? sessionsPerProfessor.getOrDefault(codiProfe, List.of()) : List.of();

            // Imparteix la submatèria FCT del curs
            boolean imparteixSubmateriaFCT = sessions.stream()
                    .anyMatch(s -> codisSubmateriesFCT.contains(s.getGestibSubmateria()));

            // Té una activitat FCT i és tutor del grup
            boolean activitatFCTiTutor = codiProfe != null
                    && sessions.stream().anyMatch(s -> codisActivitatsFCT.contains(s.getGestibActivitat()))
                    && (conte(grup.getGestibTutor1(), codiProfe) || conte(grup.getGestibTutor2(), codiProfe) || conte(grup.getGestibTutor3(), codiProfe));

            // És tutor del grup
            boolean esTutorGrup = codiProfe != null && grups.stream().anyMatch(g ->
                    codiProfe.equals(g.getGestibTutor1()) || codiProfe.equals(g.getGestibTutor2()) || codiProfe.equals(g.getGestibTutor3()));

            // Té el rol de Tutor FCT i imparteix alguna submatèria del curs
            boolean rolTutorFCT = profe.getRols() != null && profe.getRols().contains(RolDto.TUTOR_FCT)
                    && sessions.stream().anyMatch(s -> codisSubmateriesCurs.contains(s.getGestibSubmateria()));

            if ((actiu && (imparteixSubmateriaFCT || activitatFCTiTutor || rolTutorFCT)) || esTutorGrup) {
                tutorsFCT.add(profe);
            }
        }

        return new ResponseEntity<>(tutorsFCT, HttpStatus.OK);
    }

    private static boolean esFCT(String nom, String nomCurt) {
        return nom != null && nomCurt != null &&
                (nom.contains("Formació en centres de treball") || nom.contains("FCT") || nomCurt.contains("Formació en centres de treball") || nomCurt.contains("FCT"));
    }

    private static boolean conte(String text, String valor) {
        return text != null && text.contains(valor);
    }


    @GetMapping("/usuaris/alumnes-by-codigrup/{cursgrup}")
    public ResponseEntity<List<UsuariDto>> getAlumnesByCodiGrup(@PathVariable("cursgrup") String cursGrup) {
        String codiCurs = cursGrup.substring(0, cursGrup.length() - 1);
        String codiGrup = cursGrup.substring(cursGrup.length() - 1);

        System.out.println("Curs: " + codiCurs + " Grup: " + codiGrup);

        List<UsuariDto> alumnesGrup = new ArrayList<>();

        List<CursDto> cursos = cursService.findByGestibNom(codiCurs);
        if (cursos != null && !cursos.isEmpty()) {
            CursDto curs = cursos.get(0);
            List<GrupDto> grups = grupService.findByGestibNomAndCurs(codiGrup, curs.getGestibIdentificador());
            GrupDto grup = grups.get(0);

            List<UsuariDto> alumnes = usuariService.findAlumnes(false);
            for (UsuariDto alumne : alumnes) {
                System.out.println("Usuari:" + alumne.getGsuiteFullName() + "Email:" + alumne.getGsuiteEmail());
                if(alumne.getGestibGrup() != null && alumne.getGestibGrup().equals(grup.getGestibIdentificador())){
                    alumnesGrup.add(alumne);
                } else if(alumne.getGestibGrup2() != null && alumne.getGestibGrup2().equals(grup.getGestibIdentificador())){
                    alumnesGrup.add(alumne);
                } else if(alumne.getGestibGrup3() != null && alumne.getGestibGrup3().equals(grup.getGestibIdentificador())){
                    alumnesGrup.add(alumne);
                }
            }
        }

        return new ResponseEntity<>(alumnesGrup, HttpStatus.OK);
    }

    @GetMapping("/usuaris/profile-by-gestib-codi/{id}")
    public ResponseEntity<UsuariDto> getUsuariByGestibCodi(@PathVariable("id") String gestibCodi, HttpServletRequest request) throws Exception {
        Claims claims = tokenManager.getClaims(request);
        String myEmail = (String) claims.get("email");

        List<String> rolsClaim = (List<String>)claims.get("rols");
        Set<RolDto> rols = rolsClaim.stream().map(RolDto::valueOf).collect(Collectors.toSet());

        UsuariDto myUser = usuariService.findByEmail(myEmail);
        UsuariDto usuari = usuariService.findByGestibCodi(gestibCodi);

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        /** TODO: PROFESSORS FILTRAR MILLOR **/
        if (
            myEmail.equals(this.adminDeveloper) ||
            (
                myUser != null &&
                usuari != null &&
                myUser.getGsuiteEmail() != null &&
//                usuari.getGsuiteEmail() != null &&
                (
                    myUser.getGsuiteEmail().equals(usuari.getGsuiteEmail()) ||
                    rols.contains(RolDto.ADMINISTRADOR) ||
                    rols.contains(RolDto.DIRECTOR) ||
                    rols.contains(RolDto.CAP_ESTUDIS) ||
                    rols.contains(RolDto.PROFESSOR)
                )
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

        System.out.println("email" + email + "myEmail" + myEmail);
        List<String> rolsClaim = (List<String>)claims.get("rols");
        Set<RolDto> rols = rolsClaim.stream().map(RolDto::valueOf).collect(Collectors.toSet());

        UsuariDto myUser = usuariService.findByEmail(myEmail);
        UsuariDto usuari = usuariService.findByEmail(email);

        System.out.println("email" + usuari + "myEmail" + myUser);

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        /** TODO: PROFESSORS FILTRAR MILLOR **/
        if (
                myEmail.equals(this.adminDeveloper) ||
                (
                    myUser != null &&
                    usuari != null &&
                    myUser.getGsuiteEmail() != null &&
//                    usuari.getGsuiteEmail() != null &&
                    (
                            myUser.getGsuiteEmail().equals(usuari.getGsuiteEmail()) ||
                            rols.contains(RolDto.ADMINISTRADOR) ||
                            rols.contains(RolDto.DIRECTOR) ||
                            rols.contains(RolDto.CAP_ESTUDIS) ||
                            rols.contains(RolDto.PROFESSOR)
                    )
                )
        ) {
            return new ResponseEntity<>(usuari, HttpStatus.OK);
        }

        return new ResponseEntity<>(usuari, HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/usuaris/profile-by-email/{id}/{token}")
    public ResponseEntity<UsuariDto> getUsuariByEmailSystem(@PathVariable("id") String email, @PathVariable("token") String token) throws Exception {
        Claims claims = tokenManager.getClaims(token);
        String myEmail = (String) claims.get("email");

        System.out.println("email" + email + "myEmail" + myEmail);
        List<String> rolsClaim = (List<String>)claims.get("rols");
        Set<RolDto> rols = rolsClaim.stream().map(RolDto::valueOf).collect(Collectors.toSet());

        UsuariDto myUser = usuariService.findByEmail(myEmail);
        UsuariDto usuari = usuariService.findByEmail(email);

        System.out.println("email" + usuari + "myEmail" + myUser);

        //Si l'usuari que fa la consulta és el mateix o bé si té rol de cap d'estudis, director o administrador
        /** TODO: PROFESSORS FILTRAR MILLOR **/
        if (
                myEmail.equals(this.adminDeveloper) ||
                        (
                                myUser != null &&
                                        usuari != null &&
                                        myUser.getGsuiteEmail() != null &&
//                                        usuari.getGsuiteEmail() != null &&
                                        (
                                                myUser.getGsuiteEmail().equals(usuari.getGsuiteEmail()) ||
                                                        rols.contains(RolDto.ADMINISTRADOR) ||
                                                        rols.contains(RolDto.DIRECTOR) ||
                                                        rols.contains(RolDto.CAP_ESTUDIS) ||
                                                        rols.contains(RolDto.PROFESSOR)
                                        )
                        )
        ) {
            return new ResponseEntity<>(usuari, HttpStatus.OK);
        }

        return new ResponseEntity<>(usuari, HttpStatus.UNAUTHORIZED);
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
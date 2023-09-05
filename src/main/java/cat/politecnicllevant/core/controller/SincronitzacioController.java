package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.common.service.UtilService;
import cat.politecnicllevant.core.dto.gestib.*;
import cat.politecnicllevant.core.dto.gestib.*;
import cat.politecnicllevant.core.dto.google.GrupCorreuDto;
import cat.politecnicllevant.core.dto.google.GrupCorreuTipusDto;
import cat.politecnicllevant.core.service.*;
import cat.politecnicllevant.core.service.*;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.Member;
import com.google.api.services.directory.model.User;
import com.sun.istack.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class SincronitzacioController {

    @Autowired
    private CentreService centreService;

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private CursService cursService;

    @Autowired
    private GrupService grupService;

    @Autowired
    private SessioService sessioService;

    @Autowired
    private DepartamentService departamentService;

    @Autowired
    private ActivitatService activitatService;

    @Autowired
    private SubmateriaService submateriaService;

    @Autowired
    private GrupCorreuService grupCorreuService;

    @Autowired
    private GSuiteService gSuiteService;

    @Autowired
    private GMailService gMailService;

    @Autowired
    private ObservacioService observacioService;

    @Autowired
    private UsuariGrupCorreuService usuariGrupCorreuService;

    @Autowired
    private CalendariService calendariService;

    @Value("${centre.usuaris.passwordinicial}")
    private String passwordInicial;

    @Value("${centre.domini.principal}")
    private String dominiPrincipal;

    @Value("${centre.domini.alumnat}")
    private String dominiAlumnat;

    @Value("${gc.adminUser}")
    private String adminUser;

    @Value("${server.tmp}")
    private String tmpPath;

    @Value("${centre.gsuite.unitatorganitzativa.professors.default}")
    private String defaultUOProfessors;

    @Value("${centre.gsuite.unitatorganitzativa.alumnes.default}")
    private String defaultUOAlumnes;

    @Value("${centre.gestib.sync.notify.professors}")
    private String notifyProfessors;

    @Value("${centre.gestib.sync.notify.alumnes}")
    private String notifyAlumnes;

    @Value("${centre.nom}")
    private String nomCentre;

    @Value("${centre.gsuite.email.professors}")
    private String formatEmailProfessors;

    @Value("${centre.gsuite.email.alumnes}")
    private String formatEmailAlumnes;

    @Value("${centre.gsuite.fullname.professors}")
    private String formatNomGSuiteProfessors;

    @Value("${centre.gsuite.fullname.alumnes}")
    private String formatNomGSuiteAlumnes;

    @PostMapping("/sync/cancelupload")
    public ResponseEntity<Notificacio> cancelUpload(HttpServletRequest request) {
        List<CentreDto> centres = centreService.findAll();
        for (CentreDto centre : centres) {
            centre.setSincronitzar(false);
            centreService.save(centre);
        }

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Sincronització cancel·lada");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @PostMapping("/sync/uploadfile")
    public ResponseEntity<Notificacio> uploadFitxerGestib(@RequestParam("syncprofessors") Boolean sincronitzaProfessors, @RequestParam("syncalumnes") Boolean sincronitzaAlumnes, HttpServletRequest request) {
        try {
            Part filePart = request.getPart("arxiu");

            InputStream is = filePart.getInputStream();

            // Reads the file into memory
            /*
             * Path path = Paths.get(audioPath); byte[] data = Files.readAllBytes(path);
             * ByteString audioBytes = ByteString.copyFrom(data);
             */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] readBuf = new byte[4096];
            while (is.available() > 0) {
                int bytesRead = is.read(readBuf);
                os.write(readBuf, 0, bytesRead);
            }

            // Passam l'arxiu a dins una carpeta
            String fileName = this.tmpPath + "/arxiu.xml";

            OutputStream outputStream = new FileOutputStream(fileName);
            os.writeTo(outputStream);

            File f = new File(fileName);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc;

            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(f);
            doc.getDocumentElement().normalize();

            String codiCentre = doc.getDocumentElement().getAttributeNode("codi").getValue();
            String anyAcademicCentre = doc.getDocumentElement().getAttributeNode("any").getValue();

            //Comprovem si és la primera càrrega
            List<CentreDto> centres = centreService.findAll();
            if (centres.isEmpty()) {
                LocalDateTime ara = LocalDateTime.now();

                CentreDto centre = new CentreDto();
                centre.setDataSincronitzacio(ara);
                centre.setAnyAcademic(anyAcademicCentre);
                centre.setSincronitzar(true);
                centre.setSincronitzaAlumnes(sincronitzaAlumnes);
                centre.setSincronitzaProfessors(sincronitzaProfessors);
                centre.setIdentificador(codiCentre);
                centre.setNom(this.nomCentre);

                centreService.save(centre);

                this.sincronitzar();

                //Una vegada sincronitzat, creem l'administrador
                UsuariDto admin = usuariService.findByEmail(this.adminUser);

                Set<RolDto> rolAdmin = new HashSet<>();
                rolAdmin.add(RolDto.ADMINISTRADOR);

                if (admin != null) {
                    admin.setRols(rolAdmin);
                    usuariService.save(admin);
                } else {
                    UsuariDto usuariDto = new UsuariDto();
                    usuariDto.setGsuiteEmail(this.adminUser);
                    usuariDto.setRols(rolAdmin);
                    usuariDto.setGestibAlumne(false);
                    usuariDto.setGestibProfessor(false);
                    usuariDto.setActiu(true);
                    usuariDto.setGsuiteAdministrador(true);
                    usuariDto.setBloquejaGsuiteUnitatOrganitzativa(false);
                    usuariService.save(usuariDto);
                }


                Notificacio notificacio = new Notificacio();
                notificacio.setNotifyMessage("Primera sincronització finalitzada correctament. Surti del programa i torni a entrar.");
                notificacio.setNotifyType(NotificacioTipus.SUCCESS);
                notificacio.setRedirectToLogin(true);

                return new ResponseEntity<>(notificacio, HttpStatus.OK);
            }

            //Comprovacions de seguretat
            CentreDto centre = centreService.findByIdentificador(codiCentre);

            if (centre == null || !centre.getIdentificador().equals(codiCentre) || !centre.getAnyAcademic().equals(anyAcademicCentre)) {
                Notificacio notificacio = new Notificacio();
                notificacio.setNotifyMessage("L'arxiu XML no correspon amb el centre o any acadèmic de la base de dades. Centre:" + centre.getIdentificador() + " - Any acadèmic:" + centre.getAnyAcademic());
                notificacio.setNotifyType(NotificacioTipus.ERROR);

                return new ResponseEntity<>(notificacio, HttpStatus.CONFLICT);
            }

            //Actualitzem el flag
            centre.setSincronitzar(true);
            centre.setSincronitzaAlumnes(sincronitzaAlumnes);
            centre.setSincronitzaProfessors(sincronitzaProfessors);
            centreService.save(centre);

            Notificacio notificacio = new Notificacio();
            notificacio.setNotifyMessage("Arxiu carregat correctament. Les dades s'actualitzaran en 24 hores.");
            notificacio.setNotifyType(NotificacioTipus.SUCCESS);

            return new ResponseEntity<>(notificacio, HttpStatus.OK);
        } catch (IOException | ServletException | ParserConfigurationException | SAXException | MessagingException |
                 GeneralSecurityException | InterruptedException e) {
            Notificacio notificacio = new Notificacio();
            notificacio.setNotifyMessage(e.getMessage());
            notificacio.setNotifyType(NotificacioTipus.ERROR);

            return new ResponseEntity<>(notificacio, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/sync/importgsuiteusers")
    public ResponseEntity<Notificacio> getGSuiteUsers() throws InterruptedException {
        //this.gsuiteuserstodatabase();

        //Crear nous
        /*
        List<UsuariDto> usuaris = usuariService.findAll();
        List<User> usuarisGSuite = gSuiteService.getUsers();

        for(UsuariDto usuari: usuaris){
            boolean trobat = false;
            for(User usuariGSuite: usuarisGSuite){
                if(usuari.getGsuiteEmail().equals(usuariGSuite.getPrimaryEmail())){
                    trobat = true;
                }
            }
            if(!trobat){
                User userSaved = gSuiteService.createUser(usuari.getGsuiteEmail(),usuari.getGsuiteGivenName(),usuari.getGsuiteFamilyName(),"","/alumnat");
                System.out.println(userSaved.getPrimaryEmail()+","+userSaved.getName().getGivenName()+","+userSaved.getName().getFamilyName()+","+userSaved.getName().getFullName());
            }
        }
         */

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Usuaris de GSuite importats correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @PostMapping("/sync/reassignarGrups")
    public ResponseEntity<Notificacio> reassignarGrups(@RequestBody List<UsuariDto> usuaris) throws InterruptedException {
        /*List<UsuariDto> professors = new ArrayList<>();
        List<UsuariDto> alumnes = new ArrayList<>();

        for (UsuariDto usuari : usuaris) {
            if (usuari.getGestibProfessor() != null && usuari.getGestibProfessor()) {
                professors.add(usuari);
            } else if (usuari.getGestibAlumne() != null && usuari.getGestibAlumne()) {
                alumnes.add(usuari);
            }
        }
        this.reassignarGrupsProfessor(professors);
        this.reassignarGrupsAlumne(alumnes);*/
        this.reassignarGrupsProfessor();
        this.reassignarGrupsAlumne();
        this.esborrarGrupsUsuarisNoActius();

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Reassignació de grups d'usuaris finalitzat correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @PostMapping("/sync/reassignarGrupsProfessors")
    public ResponseEntity<Notificacio> reassignarGrupsProfessorsTots() throws InterruptedException {
        //List<UsuariDto> professors = usuariService.findProfessors();
        //this.reassignarGrupsProfessor(professors);
        this.reassignarGrupsProfessor();
        this.esborrarGrupsUsuarisNoActius();

        log.info("Actualitzant Grups de Correu a la base de dades...");
        this.createGrupsCorreuGSuiteToDatabase();
        this.deleteGrupsCorreuGSuiteToDatabase();
        this.updateGrupsCorreuGSuiteToDatabase();

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Reassignació de grups de professors finalitzat correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        log.info("Reassignació de grups de professors finalitzat correctament");

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @PostMapping("/sync/reassignarGrupsAlumnes")
    public ResponseEntity<Notificacio> reassignarGrupsAlumnesTots() throws InterruptedException {
        //List<UsuariDto> alumnes = usuariService.findAlumnes(false);
        //this.reassignarGrupsAlumne(alumnes);
        this.reassignarGrupsAlumne();
        this.esborrarGrupsUsuarisNoActius();

        log.info("Actualitzant Grups de Correu a la base de dades...");
        this.createGrupsCorreuGSuiteToDatabase();
        this.deleteGrupsCorreuGSuiteToDatabase();
        this.updateGrupsCorreuGSuiteToDatabase();

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Reassignació de grups d'alumnes finalitzat correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        log.info("Reassignació de grups d'alumnes finalitzat correctament");

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @PostMapping("/sync/reassignarGrupsCorreuGSuiteToDatabase")
    public ResponseEntity<Notificacio> reassignarGrupsCorreuGSuiteToDatabase() throws InterruptedException {
        log.info("Actualitzant Grups de Correu a la base de dades...");
        this.createGrupsCorreuGSuiteToDatabase();
        this.deleteGrupsCorreuGSuiteToDatabase();
        this.updateGrupsCorreuGSuiteToDatabase();

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Reassignació de grups de correus de GSuite a la BBDD finalitzat correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    /*
    Second Minute Hour Day-of-Month
    second, minute, hour, day(1-31), month(1-12), weekday(1-7) SUN-SAT
    0 0 2 * * * = a les 2AM de cada dia
     */
    @Scheduled(cron = "0 0 2 * * *")
    @PostMapping("/sync/sincronitza")
    public void sincronitzar() throws MessagingException, GeneralSecurityException, IOException, InterruptedException {
        List<CentreDto> centres = centreService.findAll();
        CentreDto centre = centres.get(0);

        if (centre.getSincronitzar()) {
            List<String> logSimulacio = new ArrayList<>();
            logSimulacio.add("Resultat sincronització v. 2.0");
            logSimulacio.addAll(this.simular());

            List<UsuariDto> usuarisNoActiusBeforeSync = usuariService.findUsuarisNoActius();
            if (usuarisNoActiusBeforeSync == null) {
                usuarisNoActiusBeforeSync = new ArrayList<>();
            }
            this.desactivarUsuaris();
            log.info("Actualitzant XML a la base de dades...");
            List<UsuariDto> usuarisGestib = this.gestibxmltodatabase(centre, usuarisNoActiusBeforeSync);

            log.info("Actualitzant usuaris GSuite a la base de dades...");
            List<UsuariDto> usuarisGSuite = this.gsuiteuserstodatabase();

            List<UsuariDto> usuarisUpdate = new ArrayList<>();
            usuarisUpdate.addAll(usuarisGestib);
            usuarisUpdate.addAll(usuarisGSuite);

            log.info("Creant usuaris nous...");
            this.createNewUsers(usuarisUpdate, centre);

            log.info("Reassignar grups professors i alumnes");
            if (centre.getSincronitzaProfessors()) {
                this.reassignarGrupsProfessor();
            }
            if (centre.getSincronitzaAlumnes()) {
                this.reassignarGrupsAlumne();
            }

            log.info("Esborrant grups d'usuaris no actius");
            this.esborrarGrupsUsuarisNoActius();

            log.info("Actualitzant Grups de Correu a la base de dades...");
            this.createGrupsCorreuGSuiteToDatabase();
            this.deleteGrupsCorreuGSuiteToDatabase();
            this.updateGrupsCorreuGSuiteToDatabase();

            log.info("Actualitació de centre. Sincronització acabada");
            this.updateCentre(centre);

            gMailService.sendMessage("Sincronització log", String.join("<br>", logSimulacio), this.adminUser);
        }
    }

    @PostMapping("/sync/simular")
    public List<String> simular() throws MessagingException, GeneralSecurityException, IOException, InterruptedException {
        List<CentreDto> centres = centreService.findAll();
        CentreDto centre = centres.get(0);

        List<UsuariDto> usuarisNoActiusBeforeSync = usuariService.findUsuarisNoActius();
        if (usuarisNoActiusBeforeSync == null) {
            usuarisNoActiusBeforeSync = new ArrayList<>();
        }

        return this.simula(centre, usuarisNoActiusBeforeSync);
    }

    private void desactivarUsuaris() {
        //Desactivar tots els usuaris (activem només els que hi ha dins l'xml)
        usuariService.desactivarUsuaris();
    }

    private void updateCentre(CentreDto centre) {
        LocalDateTime ara = LocalDateTime.now();

        centre.setSincronitzar(false);
        centre.setDataSincronitzacio(ara);
        centreService.save(centre);
    }

    private List<String> simula(@NotNull CentreDto centre, List<UsuariDto> usuarisNoActiusBeforeSync) {
        // Passam l'arxiu a dins una carpeta
        String fileName = this.tmpPath + "/arxiu.xml";
        List<String> resultat = new ArrayList<>();

        File f = new File(fileName);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(f);
            doc.getDocumentElement().normalize();

            log.info("LLegint el XML del Centre: " + centre.getNom());
            log.info("Simulació...");


            //Cursos i Grups
            resultat.add("CURSOS I GRUPS");
            resultat.add("");

            NodeList nodesCurs = doc.getElementsByTagName("CURS");

            for (int i = 0; i < nodesCurs.getLength(); i++) {
                Node node = nodesCurs.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eCurs = (Element) node;
                    String codiCurs = eCurs.getAttribute("codi");
                    String descCurs = eCurs.getAttribute("descripcio");

                    CursDto c = cursService.findByGestibIdentificador(codiCurs);

                    if (c == null) {
                        resultat.add("Nou curs: " + descCurs);
                    }

                    // Grup
                    NodeList nodesGrup = eCurs.getElementsByTagName("GRUP");

                    for (int j = 0; j < nodesGrup.getLength(); j++) {
                        Node node2 = nodesGrup.item(j);
                        if (node2.getNodeName().equals("GRUP")) {
                            Element eGrup = (Element) node2;
                            String codiGrup = eGrup.getAttribute("codi");
                            String nomGrup = eGrup.getAttribute("nom");
                            String tutor1 = eGrup.getAttribute("tutor");
                            String tutor2 = eGrup.getAttribute("tutor2");
                            String tutor3 = eGrup.getAttribute("tutor3");

                            GrupDto g = grupService.findByGestibIdentificador(codiGrup);
                            CursDto cursGrup = cursService.findByGestibIdentificador(codiCurs);

                            if(cursGrup==null){
                                resultat.add("Avís! El grup " + codiGrup + " - " + nomGrup + " no té curs.");
                            } else if (g == null) {
                                resultat.add("Nou grup: " + nomGrup + " del curs " + cursGrup.getGestibNom());
                            }
                        }
                    }

                }
            }


            //Professors
            if (centre.getSincronitzaProfessors()) {
                resultat.add("PROFESSORAT");
                resultat.add("");

                NodeList nodesProfessor = doc.getElementsByTagName("PROFESSOR");
                for (int i = 0; i < nodesProfessor.getLength(); i++) {
                    Node node = nodesProfessor.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element eProfe = (Element) node;
                        String codi = eProfe.getAttribute("codi");
                        String nom = eProfe.getAttribute("nom");
                        String ap1 = eProfe.getAttribute("ap1");
                        String ap2 = eProfe.getAttribute("ap2");
                        String usuari = eProfe.getAttribute("username");
                        String departament = eProfe.getAttribute("departament");

                        if (departament.length() == 0) {
                            resultat.add("Avís! El professor/a " + codi + " - " + nom + " " + ap1 + " " + ap2 + "  no té departament.");
                        }
                        //Podria passar que algú creàs a mà l'usuari a GSuite (correctament) però no haguesin passat l'xml, per tant..
                        //Dit d'una altra manera, si abans estava NO actiu i ara existeix, necessitam actualitzar els grups.
                        UsuariDto u = usuariService.findByGSuitePersonalID(codi);
                        if (u != null) {
                            //Si estava inactiu i ara passa a actiu
                            if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                resultat.add("El professor " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                            } else {
                                u = usuariService.findByGestibCodi(codi);
                                //Si estava inactiu i ara passa a actiu
                                if (u != null) {
                                    //Si estava inactiu i ara passa a actiu
                                    if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                        resultat.add("El professor " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                                    }
                                } else {
                                    resultat.add("El professor " + nom + " " + ap1 + " " + ap2 + " no existeix. Es crearà un compte d'usuari a GSuite");
                                }
                            }
                        } else {
                            resultat.add("El professor " + nom + " " + ap1 + " " + ap2 + " no existeix. Es crearà un compte d'usuari a GSuite");
                        }


                    }
                }
                //Professors eliminats
                List<UsuariDto> allProfessors = usuariService.findProfessors();
                for (int i = 0; i < nodesProfessor.getLength(); i++) {
                    Node node = nodesProfessor.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element eProfe = (Element) node;
                        String codi = eProfe.getAttribute("codi");
                        UsuariDto u = usuariService.findByGestibCodi(codi);
                        allProfessors.remove(u);
                    }
                }
                for (UsuariDto professor : allProfessors) {
                    if (professor.getActiu()) {
                        resultat.add("El professor " + professor.getGestibNom() + " " + professor.getGestibCognom1() + " " + professor.getGestibCognom2() + " s'ha eliminat.");
                    }
                }
            }

            //Alumnes
            if (centre.getSincronitzaAlumnes()) {
                resultat.add("ALUMNAT");
                resultat.add("");

                //Esborrem els grups, els tornarem a calcular
                //Fem una còpia dels alumnes en aquest estat per saber després si ha canviat el grup original o no.
                List<UsuariDto> alumnesOld = new ArrayList<>();
                List<UsuariDto> alumnes = usuariService.findAlumnes(false);
                for (UsuariDto alumne : alumnes) {
                    alumnesOld.add(alumne.clone());
                }

                NodeList nodesAlumne = doc.getElementsByTagName("ALUMNE");

                for (int i = 0; i < nodesAlumne.getLength(); i++) {
                    Node node = nodesAlumne.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element eAlumne = (Element) node;
                        String codi = eAlumne.getAttribute("codi");
                        String nom = eAlumne.getAttribute("nom");
                        String ap1 = eAlumne.getAttribute("ap1");
                        String ap2 = eAlumne.getAttribute("ap2");
                        String exp = eAlumne.getAttribute("expedient");
                        String grup = eAlumne.getAttribute("grup");

                        //Podria passar que algú creàs a mà l'usuari a GSuite (correctament) però no haguesin passat l'xml, per tant...
                        UsuariDto u = usuariService.findByGSuitePersonalID(codi);
                        if (u != null) {
                            //Si estava inactiu i ara passa a actiu
                            if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                resultat.add("L'alumne " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                            }

                            //Si ha canviat de grup l'actualitzem
                            Long idusuari = u.getIdusuari();
                            UsuariDto alumneOld = alumnesOld.stream().filter(a -> a.getIdusuari().equals(idusuari)).findFirst().orElse(null);
                            if (!this.usuariTeGrup(alumneOld, grup) && u.getGsuiteEmail() != null) {
                                String infoAlumne = "L'alumne " + u.getGsuiteEmail() + " ha canviat de grup.";

                                GrupDto grupOld = null;
                                if (alumneOld != null) {
                                    grupOld = grupService.findByGestibIdentificador(alumneOld.getGestibGrup());
                                }
                                if (grupOld != null) {
                                    CursDto cursAlumne = cursService.findByGestibIdentificador(grupOld.getGestibCurs());
                                    if (cursAlumne != null) {
                                        infoAlumne += " Grup antic: " + cursAlumne.getGestibNom() + grupOld.getGestibNom();
                                    }
                                }

                                GrupDto grupNew = grupService.findByGestibIdentificador(grup);
                                if (grupNew != null) {
                                    CursDto cursAlumne = cursService.findByGestibIdentificador(grupNew.getGestibCurs());
                                    if (cursAlumne != null) {
                                        infoAlumne += " Grup nou: " + cursAlumne.getGestibNom() + grupNew.getGestibNom();
                                    }
                                }

                                resultat.add(infoAlumne);
                            }
                        } else {
                            u = usuariService.findByGestibCodi(codi);

                            if (u != null) {
                                //Si estava inactiu i ara passa a actiu
                                if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                    resultat.add("L'alumne " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                                }

                                //Si ha canviat de grup l'actualitzem
                                Long idusuari = u.getIdusuari();
                                UsuariDto alumneOld = alumnesOld.stream().filter(a -> a.getIdusuari().equals(idusuari)).findFirst().orElse(null);
                                if (!this.usuariTeGrup(alumneOld, grup) && u.getGsuiteEmail() != null) {
                                    String infoAlumne = "L'alumne " + u.getGsuiteEmail() + " ha canviat de grup.";

                                    GrupDto grupOld = null;
                                    if (alumneOld != null) {
                                        grupOld = grupService.findByGestibIdentificador(alumneOld.getGestibGrup());
                                    }
                                    if (grupOld != null) {
                                        CursDto cursAlumne = cursService.findByGestibIdentificador(grupOld.getGestibCurs());
                                        if (cursAlumne != null) {
                                            infoAlumne += " Grup antic: " + cursAlumne.getGestibNom() + grupOld.getGestibNom();
                                        }
                                    }

                                    GrupDto grupNew = grupService.findByGestibIdentificador(grup);
                                    if (grupNew != null) {
                                        CursDto cursAlumne = cursService.findByGestibIdentificador(grupNew.getGestibCurs());
                                        if (cursAlumne != null) {
                                            infoAlumne += " Grup nou: " + cursAlumne.getGestibNom() + grupNew.getGestibNom();
                                        }
                                    }

                                    resultat.add(infoAlumne);
                                }
                            } else {
                                resultat.add("L'alumne " + nom + " " + ap1 + " " + ap2 + " no existeix. Es crearà un compte d'usuari a GSuite");
                            }
                        }


                    }
                }

                //Professors eliminats
                List<UsuariDto> allAlumnes = usuariService.findAlumnes(false);
                for (int i = 0; i < nodesAlumne.getLength(); i++) {
                    Node node = nodesAlumne.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element eAlumne = (Element) node;
                        String codi = eAlumne.getAttribute("codi");
                        UsuariDto u = usuariService.findByGestibCodi(codi);
                        allAlumnes.remove(u);
                    }
                }
                for (UsuariDto alumne : allAlumnes) {
                    if (alumne.getActiu()) {
                        resultat.add("L'alumne " + alumne.getGestibNom() + " " + alumne.getGestibCognom1() + " " + alumne.getGestibCognom2() + " s'ha eliminat.");
                    }
                }
            }


            //Departaments
            resultat.add("DEPARTAMENTS");
            resultat.add("");

            NodeList nodesDepartament = doc.getElementsByTagName("DEPARTAMENT");

            for (int i = 0; i < nodesDepartament.getLength(); i++) {
                Node node = nodesDepartament.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eDepartament = (Element) node;
                    String codi = eDepartament.getAttribute("codi");
                    String nom = eDepartament.getAttribute("descripcio");

                    DepartamentDto d = departamentService.findByGestibIdentificador(codi);
                    if (d == null) {
                        resultat.add("Nou departament: " + nom);
                    }
                }
            }

            //Activitats
            resultat.add("ACTIVITATS");
            resultat.add("");

            NodeList nodesActivitat = doc.getElementsByTagName("ACTIVITAT");

            for (int i = 0; i < nodesActivitat.getLength(); i++) {
                Node node = nodesActivitat.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eActivitat = (Element) node;
                    String codi = eActivitat.getAttribute("codi");
                    String nom = eActivitat.getAttribute("descripcio");
                    String nomCurt = eActivitat.getAttribute("curta");

                    ActivitatDto a = activitatService.findByGestibIdentificador(codi);
                    if (a == null) {
                        resultat.add("Nova activitat: " + nom + " (" + nomCurt + ")");
                    }
                }
            }

            //Submatèries
            /*
                <SUBMATERIA
                    codi="2066737"
                    curs="62"
                    descripcio="Biologia i geologia-A"
                    curta="BG-A"
                />
             */
            resultat.add("SUBMATÈRIES");
            resultat.add("");

            NodeList nodesSubmateria = doc.getElementsByTagName("SUBMATERIA");

            for (int i = 0; i < nodesSubmateria.getLength(); i++) {
                Node node = nodesSubmateria.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eSubmateria = (Element) node;
                    String codi = eSubmateria.getAttribute("codi");
                    String nom = eSubmateria.getAttribute("descripcio");
                    String nomCurt = eSubmateria.getAttribute("curta");
                    String curs = eSubmateria.getAttribute("curs");

                    SubmateriaDto s = submateriaService.findByGestibIdentificador(codi);
                    CursDto cursSubmateria = cursService.findByGestibIdentificador(curs);
                    if(cursSubmateria == null){
                        resultat.add("Avís! La submateria " + codi + " - " + nom + " no té curs.");
                    } else if (s == null) {
                        resultat.add("Nova submatèria: " + nom + " (" + nomCurt + ") del curs " + cursSubmateria.getGestibNom());
                    }
                }
            }


            //Gsuite
            resultat.add("SINCRONITZACIÓ AMB GSUITE");
            resultat.add("");

            List<User> gsuiteUsers = gSuiteService.getUsers();

            for (User gsuiteUser : gsuiteUsers) {

                String email = gsuiteUser.getPrimaryEmail();
                Boolean isAdmin = gsuiteUser.getIsAdmin();
                Boolean isSuspes = gsuiteUser.getSuspended();
                String givenName = gsuiteUser.getName().getGivenName();
                String familyName = gsuiteUser.getName().getFamilyName();
                String fullName = gsuiteUser.getName().getFullName();
                String unitatOrganitzativa = gsuiteUser.getOrgUnitPath();
                String personalIdKey = "";

                try {
                    List personalIDValueOrganization = (ArrayList) gsuiteUser.getExternalIds();
                    ArrayMap userKey = (ArrayMap) personalIDValueOrganization.get(0);

                    String valueKey = userKey.getKey(0).toString();
                    String valueValue = userKey.getValue(0).toString();

                    String organizationKey = userKey.getKey(1).toString();
                    String organizationValue = userKey.getValue(1).toString();


                    //System.out.println(valueKey + "<<->>" + valueValue + "<<->>" + organizationKey + "<<->>" + organizationValue+ "<<->>");

                    if (valueKey.equals("value") && organizationKey.equals("type") && organizationValue.equals("organization")) {
                        personalIdKey = valueValue;
                    }
                } catch (Exception e) {
                }


                UsuariDto u = usuariService.findByGestibCodiOrEmail(personalIdKey, email);

                if (u != null) {
                    //Si l'han activat a GSuite però no està sincronitzat amb la BBDD cal actualitzar l'usuari
                    if (
                            ((u.getGsuiteSuspes() != null && u.getGsuiteSuspes()) || (u.getGsuiteEliminat() != null && u.getGsuiteEliminat()))
                                    && !isSuspes && u.getGsuiteEmail() != null) {
                        resultat.add("Usuari " + u.getGsuiteEmail() + " actiu a GSuite però no a la BBDD. Es crearà l'usuari a la BBDD");
                    }
                } else {
                    resultat.add("L'usuari no existeix. Creant usuari " + email + " amb clau " + personalIdKey);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | CloneNotSupportedException |
                 InterruptedException ex) {
            log.error(ex.getMessage());
        }

        log.info("Simulació correcte");
        return resultat;
    }

    private List<UsuariDto> gestibxmltodatabase(@NotNull CentreDto centre, List<UsuariDto> usuarisNoActiusBeforeSync) {
        List<UsuariDto> usuarisUpdate = new ArrayList<>();

        // Passam l'arxiu a dins una carpeta
        String fileName = this.tmpPath + "/arxiu.xml";

        File f = new File(fileName);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(f);
            doc.getDocumentElement().normalize();

            log.info("LLegint el XML del Centre: " + centre.getNom());
            log.info("Començant a processar...");


            //Cursos i Grups
            this.cursService.deshabilitarCursos();
            this.grupService.deshabilitarGrups();

            NodeList nodesCurs = doc.getElementsByTagName("CURS");

            for (int i = 0; i < nodesCurs.getLength(); i++) {
                Node node = nodesCurs.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eCurs = (Element) node;
                    String codiCurs = eCurs.getAttribute("codi");
                    String descCurs = eCurs.getAttribute("descripcio");

                    CursDto c = cursService.findByGestibIdentificador(codiCurs);

                    if (c != null) {
                        c.setActiu(true);
                        cursService.save(c);
                    } else {
                        List<CursDto> cursosByNom = cursService.findByGestibNom(descCurs);

                        //Si trobem el curs per nom però NO per codi (curs anterior). P ex: 1r ESO. Possiblement sigui un canvi de curs acadèmic
                        if (cursosByNom != null && !cursosByNom.isEmpty()) {
                            for (CursDto cursDto : cursosByNom) {
                                //Hem d'actualitzar les referències de Sessio, Grup i Submateria.
                                //Sessió i Submatèria no cal perquè feim "deleteAll" i ja es posarà la referència correcte
                                //Grup: Cerquem els grups amb el codi anterior i actualitzem al nou codi
                                List<GrupDto> grupsUpdate = grupService.findByGestibCurs(cursDto.getGestibIdentificador());
                                for (GrupDto grupDto : grupsUpdate) {
                                    grupDto.setGestibCurs(codiCurs);
                                    grupService.save(grupDto);
                                }

                                cursDto.setGestibIdentificador(codiCurs);
                                cursDto.setActiu(true);
                                cursService.save(cursDto);
                            }
                        } else {
                            //Curs nou
                            cursService.save(codiCurs, descCurs);
                        }
                    }

                    // Grup
                    NodeList nodesGrup = eCurs.getElementsByTagName("GRUP");

                    for (int j = 0; j < nodesGrup.getLength(); j++) {
                        Node node2 = nodesGrup.item(j);
                        if (node2.getNodeName().equals("GRUP")) {
                            Element eGrup = (Element) node2;
                            String codiGrup = eGrup.getAttribute("codi");
                            String nomGrup = eGrup.getAttribute("nom");
                            String tutor1 = eGrup.getAttribute("tutor");
                            String tutor2 = eGrup.getAttribute("tutor2");
                            String tutor3 = eGrup.getAttribute("tutor3");

                            GrupDto g = grupService.findByGestibIdentificador(codiGrup);

                            if (g != null) {
                                g.setGestibNom(nomGrup);
                                g.setGestibCurs(codiCurs);
                                g.setGestibTutor1(tutor1);
                                g.setGestibTutor2(tutor2);
                                g.setGestibTutor3(tutor3);
                                g.setActiu(true);
                                grupService.save(g);
                            } else {
                                List<GrupDto> grupsByNom = grupService.findByGestibNomAndCurs(nomGrup, codiCurs);

                                //Si trobem el grup per nom i curs però NO per codi (curs anterior). P. ex: 1r ESO A. Possiblement sigui un canvi d'any acadèmic
                                if (grupsByNom != null && !grupsByNom.isEmpty()) {
                                    for (GrupDto grupDto : grupsByNom) {
                                        //Hem d'actualitzar les referències de Calendari, Sessio, Usuari.
                                        //Sessió no cal perquè feim "deleteAll" i ja es posarà la referència correcte
                                        //Usuari no cal perquè sobreescrivim el grup amb l'actual

                                        grupDto.setGestibIdentificador(codiGrup);
                                        grupDto.setGestibNom(nomGrup);
                                        grupDto.setGestibCurs(codiCurs);
                                        grupDto.setGestibTutor1(tutor1);
                                        grupDto.setGestibTutor2(tutor2);
                                        grupDto.setGestibTutor3(tutor3);
                                        grupDto.setActiu(true);
                                        grupService.save(grupDto);
                                    }
                                } else {
                                    //Grup nou
                                    grupService.save(codiGrup, nomGrup, codiCurs, tutor1, tutor2, tutor3, this.defaultUOAlumnes);
                                }
                            }
                        }
                    }

                }
            }


            //Professors
            if (centre.getSincronitzaProfessors()) {
                NodeList nodesProfessor = doc.getElementsByTagName("PROFESSOR");
                for (int i = 0; i < nodesProfessor.getLength(); i++) {
                    Node node = nodesProfessor.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element eProfe = (Element) node;
                        String codi = eProfe.getAttribute("codi");
                        String nom = eProfe.getAttribute("nom");
                        String ap1 = eProfe.getAttribute("ap1");
                        String ap2 = eProfe.getAttribute("ap2");
                        String usuari = eProfe.getAttribute("username");
                        String departament = eProfe.getAttribute("departament");

                        if (departament.length() == 0) {
                            log.info("ALERTA! El professor/a no té departament" + codi + " - " + nom + " " + ap1 + " " + ap2 + " - " + usuari + " - " + departament);
                        }
                        //Podria passar que algú creàs a mà l'usuari a GSuite (correctament) però no haguesin passat l'xml, per tant..
                        //Dit d'una altra manera, si abans estava NO actiu i ara existeix, necessitam actualitzar els grups.
                        UsuariDto u = usuariService.findByGSuitePersonalID(codi);
                        if (u != null) {
                            //Si estava inactiu i ara passa a actiu
                            if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                log.info("Usuari " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                                usuarisUpdate.add(u);
                            }
                            usuariService.saveGestib(u, codi, nom, ap1, ap2, usuari, null, null, departament, true, false);
                        }

                        u = usuariService.findByGestibCodi(codi);
                        if (u == null) {
                            usuariService.saveGestib(codi, nom, ap1, ap2, usuari, null, null, departament, true, false);
                        } else {
                            //Si estava inactiu i ara passa a actiu
                            if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                log.info("L'usuari " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                                usuarisUpdate.add(u);
                            }
                            u.setActiu(true);
                            usuariService.save(u);
                        }

                    }
                }
            }

            //Alumnes

            if (centre.getSincronitzaAlumnes()) {
                //Esborrem els grups, els tornarem a calcular
                //Fem una còpia dels alumnes en aquest estat per saber després si ha canviat el grup original o no.
                List<UsuariDto> alumnesOld = new ArrayList<>();
                List<UsuariDto> alumnes = usuariService.findAlumnes(false);
                for (UsuariDto alumne : alumnes) {
                    alumnesOld.add(alumne.clone());
                    alumne.setGestibGrup(null);
                    alumne.setGestibGrup2(null);
                    alumne.setGestibGrup3(null);
                    usuariService.save(alumne);
                }

                NodeList nodesAlumne = doc.getElementsByTagName("ALUMNE");

                for (int i = 0; i < nodesAlumne.getLength(); i++) {
                    Node node = nodesAlumne.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element eAlumne = (Element) node;
                        String codi = eAlumne.getAttribute("codi");
                        String nom = eAlumne.getAttribute("nom");
                        String ap1 = eAlumne.getAttribute("ap1");
                        String ap2 = eAlumne.getAttribute("ap2");
                        String exp = eAlumne.getAttribute("expedient");
                        String grup = eAlumne.getAttribute("grup");

                        //Podria passar que algú creàs a mà l'usuari a GSuite (correctament) però no haguesin passat l'xml, per tant...
                        UsuariDto u = usuariService.findByGSuitePersonalID(codi);
                        if (u != null) {
                            //Si estava inactiu i ara passa a actiu
                            if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                log.info("L'usuari " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                                usuarisUpdate.add(u);
                            }

                            //Si ha canviat de grup l'actualitzem
                            Long idusuari = u.getIdusuari();
                            UsuariDto alumneOld = alumnesOld.stream().filter(a -> a.getIdusuari().equals(idusuari)).findFirst().orElse(null);
                            if (!this.usuariTeGrup(alumneOld, grup) && u.getGsuiteEmail() != null) {
                                log.info("L'alumne " + u.getGsuiteEmail() + " ha canviat de grup");
                                //Actualitzem el grup
                                usuarisUpdate.add(u);
                            }

                            UsuariDto alumne = usuariService.saveGestib(u, codi, nom, ap1, ap2, null, exp, grup, null, false, true);

                            usuariService.save(alumne);
                        }

                        u = usuariService.findByGestibCodi(codi);
                        if (u == null) {
                            UsuariDto alumne = usuariService.saveGestib(codi, nom, ap1, ap2, null, exp, grup, null, false, true);
                            //usuariService.save(alumne);
                        } else {
                            //Si estava inactiu i ara passa a actiu
                            if (usuarisNoActiusBeforeSync.contains(u) && u.getGsuiteEmail() != null) {
                                log.info("L'usuari " + u.getGsuiteEmail() + " passa d'inactiu a actiu");
                                usuarisUpdate.add(u);
                            }

                            //Si ha canviat de grup l'actualitzem
                            Long idusuari = u.getIdusuari();
                            if (!this.usuariTeGrup(alumnesOld.stream().filter(a -> a.getIdusuari().equals(idusuari)).findFirst().orElse(null), grup) && u.getGsuiteEmail() != null) {
                                log.info("L'alumne " + u.getGsuiteEmail() + " ha canviat de grup");
                                //Actualitzem el grup
                                usuarisUpdate.add(u);
                            }
                            u.setGestibNom(nom);
                            u.setGestibCognom1(ap1);
                            u.setGestibCognom2(ap2);
                            if (u.getGestibGrup() == null) {
                                u.setGestibGrup(grup);
                            } else if (!u.getGestibGrup().equals(grup) && u.getGestibGrup2() == null) {
                                u.setGestibGrup2(grup);
                            } else if (!u.getGestibGrup().equals(grup) && !u.getGestibGrup2().equals(grup) && u.getGestibGrup3() == null) {
                                u.setGestibGrup3(grup);
                            }
                            u.setGestibExpedient(exp);
                            u.setActiu(true);
                            usuariService.save(u);
                        }
                    }
                }
            }


            //Departaments
            NodeList nodesDepartament = doc.getElementsByTagName("DEPARTAMENT");

            for (int i = 0; i < nodesDepartament.getLength(); i++) {
                Node node = nodesDepartament.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eDepartament = (Element) node;
                    String codi = eDepartament.getAttribute("codi");
                    String nom = eDepartament.getAttribute("descripcio");

                    DepartamentDto d = departamentService.findByGestibIdentificador(codi);
                    if (d == null) {
                        departamentService.save(codi, nom);
                    }
                }
            }

            //Activitats

            //Esborrem les activitats per tornar-les a crear
            activitatService.deleteAllActivitats();
            NodeList nodesActivitat = doc.getElementsByTagName("ACTIVITAT");

            for (int i = 0; i < nodesActivitat.getLength(); i++) {
                Node node = nodesActivitat.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eActivitat = (Element) node;
                    String codi = eActivitat.getAttribute("codi");
                    String nom = eActivitat.getAttribute("descripcio");
                    String nomCurt = eActivitat.getAttribute("curta");

                    ActivitatDto a = activitatService.findByGestibIdentificador(codi);
                    if (a == null) {
                        activitatService.save(codi, nom, nomCurt);
                    }
                }
            }

            //Submatèries
            /*
                <SUBMATERIA
                    codi="2066737"
                    curs="62"
                    descripcio="Biologia i geologia-A"
                    curta="BG-A"
                />
             */
            //Eliminem totes les submateries per tornar-les a crear de nou
            submateriaService.deleteAllSubmateries();
            NodeList nodesSubmateria = doc.getElementsByTagName("SUBMATERIA");

            for (int i = 0; i < nodesSubmateria.getLength(); i++) {
                Node node = nodesSubmateria.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eSubmateria = (Element) node;
                    String codi = eSubmateria.getAttribute("codi");
                    String nom = eSubmateria.getAttribute("descripcio");
                    String nomCurt = eSubmateria.getAttribute("curta");
                    String curs = eSubmateria.getAttribute("curs");

                    SubmateriaDto s = submateriaService.findByGestibIdentificador(codi);
                    if (s == null) {
                        submateriaService.save(codi, nom, nomCurt, curs);
                    }
                }
            }

            //Sessió
            /*
                Sessió Professors
                <SESSIO
                    professor="XXXXXX"
                    curs="33"
                    grup="3333"
                    dia="3"
                    hora="08:00"
                    durada="55"
                    aula="333"
                    submateria="333333"
                    activitat=""
                    placa="3333"
                />

                Sessió Alumnes
                <SESSIO
                    alumne="XXXXXXX"
                    dia="3"
                    hora="08:00"
                    durada="55"
                    aula="55"
                    submateria="555555"
                />

                Sessió Grup
                <SESSIO
                    curs="563"
                    grup="557859"
                    dia="3"
                    hora="09:50"
                    durada="55"
                    aula="12707"
                    submateria="2065627"
                    activitat=""
                />
             */
            //Eliminem totes les sessions per tornar-les a crear de nou
            sessioService.deleteAllSessions();
            NodeList nodesSessio = doc.getElementsByTagName("SESSIO");

            for (int i = 0; i < nodesSessio.getLength(); i++) {
                Node node = nodesSessio.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eSessio = (Element) node;
                    String professor = eSessio.getAttribute("professor");
                    String alumne = eSessio.getAttribute("alumne");
                    String curs = eSessio.getAttribute("curs");
                    String grup = eSessio.getAttribute("grup");
                    String dia = eSessio.getAttribute("dia");
                    String hora = eSessio.getAttribute("hora");
                    String durada = eSessio.getAttribute("durada");
                    String aula = eSessio.getAttribute("aula");
                    String submateria = eSessio.getAttribute("submateria");
                    String activitat = eSessio.getAttribute("activitat");
                    String placa = eSessio.getAttribute("placa");

                    sessioService.saveSessio(professor, alumne, curs, grup, dia, hora, durada, aula, submateria, activitat, placa);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException | CloneNotSupportedException ex) {
            log.error(ex.getMessage());
        }

        log.info("Sincronització correcte");
        return usuarisUpdate;
    }

    private List<UsuariDto> gsuiteuserstodatabase() throws InterruptedException {
        List<UsuariDto> usuarisUpdate = new ArrayList<>();

        //Sincronitzem la BBDD amb la informació de GSuite
        List<User> gsuiteUsers = gSuiteService.getUsers();
        List<UsuariDto> usuarisGestib = usuariService.findAll(true);


        //Mirem si hi ha algun usuari que no quadri i s'hagi d'eliminar
        //Pot passar que el personalIdKey no coincideixi, per què?
        //Idò pot passar que hagin ESBORRAT l'usuari a GSuite, aleshores el programa en fa un de nou
        //I clar, a la BBDD no coincideix, solució? Si no coincideix el personal ID key, passa a eliminat amb una observació.
        for(UsuariDto usuariGestib: usuarisGestib){
            boolean trobat = true;

            //Si el mail i personal id existeixen vol dir que hi ha d'haver algú a GSuite amb aquestes característiques,
            //Si no és així, eliminem l'usuari
            if(usuariGestib.getGsuiteEmail()!=null &&
                    !usuariGestib.getGsuiteEmail().isEmpty() &&
                    usuariGestib.getGsuitePersonalID()!=null &&
                    !usuariGestib.getGsuitePersonalID().isEmpty()
            ){
                trobat = false;
                for (User gsuiteUser : gsuiteUsers) {

                    String email = gsuiteUser.getPrimaryEmail();
                    String personalIdKey = "";

                    try {
                        List personalIDValueOrganization = (ArrayList) gsuiteUser.getExternalIds();
                        ArrayMap userKey = (ArrayMap) personalIDValueOrganization.get(0);

                        String valueKey = userKey.getKey(0).toString();
                        String valueValue = userKey.getValue(0).toString();

                        String organizationKey = userKey.getKey(1).toString();
                        String organizationValue = userKey.getValue(1).toString();


                        //System.out.println(valueKey + "<<->>" + valueValue + "<<->>" + organizationKey + "<<->>" + organizationValue+ "<<->>");

                        if (valueKey.equals("value") && organizationKey.equals("type") && organizationValue.equals("organization")) {
                            personalIdKey = valueValue;
                        }
                    } catch (Exception e) {
                    }

                    if(usuariGestib.getGsuiteEmail().equals(email) && usuariGestib.getGsuitePersonalID().equals(personalIdKey)){
                        trobat=true;
                        break;
                    }
                }
            }

            if(!trobat){
                UsuariDto u = usuariService.findById(usuariGestib.getIdusuari());
                log.info("Eliminant usuari "+usuariGestib.getIdusuari());

                String descripcio = "L'usuari de l'aplicació no coincideix amb el de GSuite. Es procedeix a donar esborrar-lo. Correu antic: " + u.getGsuiteEmail() + ". Personal ID antic: " + u.getGsuitePersonalID();

                u.setGsuiteEmail(null);
                u.setGsuitePersonalID(null);
                u.setGsuiteEliminat(true);
                UsuariDto usuariDto = usuariService.save(u);

                //Creem una observació
                observacioService.save(descripcio, ObservacioTipusDto.ESBORRAT, usuariDto);
            }
        }


        //Actualitzem els usuaris de la BBDD
        for (User gsuiteUser : gsuiteUsers) {

            String email = gsuiteUser.getPrimaryEmail();
            Boolean isAdmin = gsuiteUser.getIsAdmin();
            Boolean isSuspes = gsuiteUser.getSuspended();
            String givenName = gsuiteUser.getName().getGivenName();
            String familyName = gsuiteUser.getName().getFamilyName();
            String fullName = gsuiteUser.getName().getFullName();
            String unitatOrganitzativa = gsuiteUser.getOrgUnitPath();
            String personalIdKey = "";

            try {
                List personalIDValueOrganization = (ArrayList) gsuiteUser.getExternalIds();
                ArrayMap userKey = (ArrayMap) personalIDValueOrganization.get(0);

                String valueKey = userKey.getKey(0).toString();
                String valueValue = userKey.getValue(0).toString();

                String organizationKey = userKey.getKey(1).toString();
                String organizationValue = userKey.getValue(1).toString();


                //System.out.println(valueKey + "<<->>" + valueValue + "<<->>" + organizationKey + "<<->>" + organizationValue+ "<<->>");

                if (valueKey.equals("value") && organizationKey.equals("type") && organizationValue.equals("organization")) {
                    personalIdKey = valueValue;
                }
            } catch (Exception e) {
            }


            UsuariDto u = usuariService.findByGestibCodiOrEmail(personalIdKey, email);
            log.info("Personal Key:" + personalIdKey + "E-mail:" + email);

            if (u != null && (u.getGsuiteEliminat() == null || !u.getGsuiteEliminat())) {
                log.info("Actualitzant usuari " + u.getGestibNom() + " " + u.getGestibCognom1() + " " + u.getGestibCognom2());

                //Si l'han activat a GSuite però no està sincronitzat amb la BBDD cal actualitzar l'usuari
                if (
                        ((u.getGsuiteSuspes() != null && u.getGsuiteSuspes()) || (u.getGsuiteEliminat() != null && u.getGsuiteEliminat()))
                                && !isSuspes && u.getGsuiteEmail() != null) {
                    log.info("Si l'han activat a GSuite però no està sincronitzat amb la BBDD cal actualitzar l'usuari. Usuari: " + u.getGsuiteEmail() + " - " + u.getGsuiteSuspes() + " - " + u.getGsuiteEliminat() + " - " + isSuspes);
                    usuarisUpdate.add(u);
                }

                u.setGsuiteEmail(email);
                u.setGsuiteAdministrador(isAdmin);
                u.setGsuitePersonalID(personalIdKey);
                u.setGsuiteSuspes(isSuspes);
                u.setGsuiteUnitatOrganitzativa(unitatOrganitzativa);
                u.setGsuiteGivenName(givenName);
                u.setGsuiteFamilyName(familyName);
                u.setGsuiteFullName(fullName);

                //Si és algú del PAS ho podem conèixer perquè no tindrà pesonalIDKey, aleshores ha d'estar actiu però
                //no tindrà usuari Gestib.
                if (personalIdKey == null || personalIdKey.isEmpty()) {
                    u.setActiu(true);
                }

                usuariService.save(u);
            } else {
                log.info("L'usuari no existeix. Creant usuari " + email + " amb clau " + personalIdKey);
                boolean actiu = personalIdKey == null || personalIdKey.isEmpty();
                usuariService.saveGSuite(email, isAdmin, personalIdKey, isSuspes, unitatOrganitzativa, givenName, familyName, fullName, actiu);
            }

        }

        log.info("Sincronització correcte amb GSuite cap a la BBDD");

        return usuarisUpdate;
    }

    private void createNewUsers(List<UsuariDto> usuarisUpdate, CentreDto centre) throws IOException, MessagingException, InterruptedException, GeneralSecurityException {

        List<UsuariDto> professorsNous = new ArrayList<>();
        List<UsuariDto> alumnesNous = new ArrayList<>();
        Set<UsuariDto> professorsUpdate = new HashSet<>();
        Set<UsuariDto> alumnesUpdate = new HashSet<>();

        //Els usuaris sense correu són usuaris nous
        List<UsuariDto> usuarisSenseCorreu = usuariService.findUsuarisSenseCorreu();
        for (UsuariDto usuari : usuarisSenseCorreu) {
            //Comprovam si està actiu pels usuaris eliminats, no hem de crear el de tothom
            if (centre.getSincronitzaProfessors()!=null && centre.getSincronitzaProfessors() &&
                    usuari.getGestibProfessor() != null &&  usuari.getGestibProfessor() &&
                    usuari.getActiu() != null && usuari.getActiu() &&
                    usuari.getGsuiteEmail() == null) {
                String username = this.generateUsername(usuari, this.formatEmailProfessors);

                log.info("Username: " + username);
                String nom = usuari.getGestibNom();
                String cognoms = usuari.getGestibCognom1() + " " + usuari.getGestibCognom2();

                if (formatNomGSuiteProfessors.equals("nomcognom1cognom2")) {
                    nom = UtilService.capitalize(nom);
                    cognoms = UtilService.capitalize(cognoms);
                }

                User usuariGSuite = gSuiteService.createUser(username, nom, cognoms, usuari.getGestibCodi(), this.defaultUOProfessors);

                if (usuariGSuite != null) {
                    log.info("Usuari " + usuariGSuite.getPrimaryEmail() + " creat correctament com a professor a GSuite");

                    //Actualitzar usuari Gestib
                    usuari.setActiu(true);
                    usuari.setGsuiteEmail(usuariGSuite.getPrimaryEmail());
                    usuari.setGsuiteAdministrador(usuariGSuite.getIsAdmin());
                    usuari.setGsuitePersonalID(usuari.getGestibCodi());
                    usuari.setGsuiteSuspes(usuariGSuite.getSuspended());
                    usuari.setGsuiteUnitatOrganitzativa(usuariGSuite.getOrgUnitPath());
                    usuari.setGsuiteGivenName(usuariGSuite.getName().getGivenName());
                    usuari.setGsuiteFamilyName(usuariGSuite.getName().getFamilyName());
                    usuari.setGsuiteFullName(usuariGSuite.getName().getFullName());

                    usuariService.save(usuari);

                    professorsNous.add(usuari);

                } else {
                    String missatge = "Error creant el correu del professor " + usuari.getGestibNom() + " " + usuari.getGestibCognom1() + " " + usuari.getGestibCognom2();
                    log.error(missatge);
                }
            } else if (centre.getSincronitzaAlumnes()!=null && centre.getSincronitzaAlumnes() &&
                    usuari.getGestibAlumne() !=null && usuari.getGestibAlumne() &&
                    usuari.getActiu() !=null && usuari.getActiu() &&
                    usuari.getGsuiteEmail() == null) {
                //Username
                String username = this.generateUsername(usuari, this.formatEmailAlumnes);

                //Unitat organitzativa de l'alumne
                GrupDto grup = grupService.findByGestibIdentificador(usuari.getGestibGrup());
                if (grup != null) {
                    CursDto curs = cursService.findByGestibIdentificador(grup.getGestibCurs());
                    String rutaUnitat = "";
                    if (grup.getGsuiteUnitatOrganitzativa() == null || grup.getGsuiteUnitatOrganitzativa().isEmpty()) {
                        rutaUnitat = this.defaultUOAlumnes;
                    } else {
                        rutaUnitat = grup.getGsuiteUnitatOrganitzativa();
                    }

                    String nom = usuari.getGestibNom();
                    String cognoms = usuari.getGestibCognom1() + " " + usuari.getGestibCognom2();

                    if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2")) {
                        nom = UtilService.capitalize(nom);
                        cognoms = UtilService.capitalize(cognoms);
                    } else if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2cursgrup")) {
                        if (curs == null || curs.getGestibNom() == null || curs.getGestibNom().isEmpty() || grup.getGestibNom() == null || grup.getGestibNom().isEmpty()) {
                            cognoms = usuari.getGestibCognom1() + " " + usuari.getGestibCognom2();
                        } else {
                            cognoms = usuari.getGestibCognom1() + " " + usuari.getGestibCognom2() + " " + curs.getGestibNom() + grup.getGestibNom();
                        }
                        nom = UtilService.capitalize(nom);
                        cognoms = UtilService.capitalize(cognoms);
                    }

                    log.info(username + "---" + usuari.getGestibNom() + "---" + usuari.getGestibCognom1() + " " + usuari.getGestibCognom2() + " " + grup.getGsuiteUnitatOrganitzativa() + "---" + usuari.getGestibCodi() + "---" + rutaUnitat);

                    User usuariGSuite = gSuiteService.createUser(username, nom, cognoms, usuari.getGestibCodi(), rutaUnitat);

                    if (usuariGSuite != null) {
                        log.info("Usuari" + usuariGSuite.getPrimaryEmail() + " creat correctament a GSuite com alumne");

                        //Actualitzar usuari Gestib
                        usuari.setActiu(true);
                        usuari.setGsuiteEmail(usuariGSuite.getPrimaryEmail());
                        usuari.setGsuiteAdministrador(usuariGSuite.getIsAdmin());
                        usuari.setGsuitePersonalID(usuari.getGestibCodi());
                        usuari.setGsuiteSuspes(usuariGSuite.getSuspended());
                        usuari.setGsuiteUnitatOrganitzativa(usuariGSuite.getOrgUnitPath());
                        usuari.setGsuiteGivenName(usuariGSuite.getName().getGivenName());
                        usuari.setGsuiteFamilyName(usuariGSuite.getName().getFamilyName());
                        usuari.setGsuiteFullName(usuariGSuite.getName().getFullName());

                        usuariService.save(usuari);

                        alumnesNous.add(usuari);
                    } else {
                        String missatge = "Error creant el correu de l'alumne " + usuari.getGestibNom() + " " + usuari.getGestibCognom1() + " " + usuari.getGestibCognom2();
                        log.error(missatge);
                    }
                }
            } else {
                log.error("Error a l'usuari " + usuari.getGestibNom() + usuari.getGestibCognom1());
            }
        }

        //També pot passar que hi hagi alumnes o professors (sobretot professors) que ja tinguin compte però
        // estàssin deshabilitats a GSuite i tornin al centre
        List<UsuariDto> usuarisSuspesos = usuariService.findUsuarisGSuiteSuspesos();
        List<UsuariDto> usuarisActius = usuariService.findUsuarisActius();

        for (UsuariDto usuariGestib : usuarisActius) {
            for (UsuariDto usuariSuspes : usuarisSuspesos) {
                if (usuariGestib.getIdusuari().equals(usuariSuspes.getIdusuari())) {
                    usuariSuspes.setGsuiteEliminat(false);
                    usuariSuspes.setGsuiteSuspes(false);
                    usuariSuspes.setActiu(true);
                    usuariService.save(usuariSuspes);

                    //Actualitzem GSuite
                    User usuariGsuite = gSuiteService.getUserById(usuariSuspes.getGsuiteEmail());
                    if (usuariGsuite != null) {
                        gSuiteService.suspendreUser(usuariSuspes.getGsuiteEmail(), false);
                    }

                    if (centre.getSincronitzaProfessors() && usuariGestib.getGestibProfessor() != null && usuariGestib.getGestibProfessor()) {
                        professorsNous.add(usuariGestib);
                    } else if (centre.getSincronitzaAlumnes() && usuariGestib.getGestibAlumne() != null && usuariGestib.getGestibAlumne()) {
                        alumnesNous.add(usuariGestib);
                    }
                }
            }
        }

        //També pot passar que hagin habilitat l'usuari a GSuite i no estigui sincronitzat amb la BBDD. Actualitzem els usuaris
        for (UsuariDto usuari : usuarisUpdate) {
            usuari.setGsuiteEliminat(false);
            usuari.setGsuiteSuspes(false);
            usuari.setActiu(true);
            usuariService.save(usuari);

            if (centre.getSincronitzaProfessors() && usuari.getGestibProfessor() != null && usuari.getGestibProfessor()) {
                professorsUpdate.add(usuari);
            } else if (centre.getSincronitzaAlumnes() && usuari.getGestibAlumne() != null && usuari.getGestibAlumne()) {
                alumnesUpdate.add(usuari);
            }
        }


        if (!professorsNous.isEmpty()) {
            log.info("Hi ha " + professorsNous.size() + " profes nous");

            //Notificacions
            StringBuilder body = new StringBuilder();
            for (UsuariDto professor : professorsNous) {
                StringBuilder bodyProfessor = new StringBuilder();
                DepartamentDto departament = null;
                if (professor.getGestibDepartament() != null && !professor.getGestibDepartament().isEmpty()) {
                    departament = departamentService.findByGestibIdentificador(professor.getGestibDepartament());
                }
                bodyProfessor.append(professor.getGsuiteFullName()).append(" Correu: ").append(professor.getGsuiteEmail()).append(" - Contrasenya: ").append(this.passwordInicial);
                if (departament != null) {
                    bodyProfessor.append(" Departament: ").append(departament.getGestibNom());
                } else {
                    bodyProfessor.append(" Sense Departament");
                }
                body.append(bodyProfessor);
                body.append("<br><br>");

                //Avisem al cap de departament
                if (departament != null && departament.getCapDepartament() != null && departament.getCapDepartament().getGsuiteEmail() != null && !departament.getCapDepartament().getGsuiteEmail().isEmpty()) {
                    gMailService.sendMessage("Nous professors donats d'alta a GSuite del seu departament", bodyProfessor.toString(), departament.getCapDepartament().getGsuiteEmail());
                }

            }
            String[] notifyProfessors = this.notifyProfessors.split(",");
            for (String email : notifyProfessors) {
                gMailService.sendMessage("Nous professors donats d'alta a GSuite", body.toString(), email);
            }
        }

        if (!alumnesNous.isEmpty()) {
            log.info("Hi ha " + alumnesNous.size() + " alumnes nous");

            //Notificacions
            StringBuilder bodyAll = new StringBuilder();
            Map<UsuariDto, String> tutors = new HashMap<>();

            for (UsuariDto alumne : alumnesNous) {
                String infoAlumne = alumne.getGsuiteFullName() + ". Correu: " + alumne.getGsuiteEmail() + " - Contrasenya: " + this.passwordInicial + "\n\n";

                GrupDto grup = grupService.findByGestibIdentificador(alumne.getGestibGrup());
                if (grup != null) {
                    UsuariDto tutor1 = usuariService.findByGestibCodi(grup.getGestibTutor1());
                    UsuariDto tutor2 = usuariService.findByGestibCodi(grup.getGestibTutor2());
                    UsuariDto tutor3 = usuariService.findByGestibCodi(grup.getGestibTutor3());

                    if (tutor1 != null) {
                        tutors.merge(tutor1, infoAlumne, (a, b) -> a + "<br><br>" + b);
                    }

                    if (tutor2 != null) {
                        tutors.merge(tutor2, infoAlumne, (a, b) -> a + "<br><br>" + b);
                    }

                    if (tutor3 != null) {
                        tutors.merge(tutor3, infoAlumne, (a, b) -> a + "<br><br>" + b);
                    }
                }
            }

            for (Map.Entry<UsuariDto, String> entry : tutors.entrySet()) {
                UsuariDto tutor = entry.getKey();
                String missatge = entry.getValue();
                if (tutor.getGsuiteEmail() != null) {
                    gMailService.sendMessage("Nou alumnat donat d'alta a GSuite a càrrec del/de la tutor/a " + tutor.getGsuiteFullName(), missatge, tutor.getGsuiteEmail());
                }
            }

            String[] notifyAlumnes = this.notifyAlumnes.split(",");
            for (String email : notifyAlumnes) {
                gMailService.sendMessage("Nous alumnes donats d'alta a GSuite", bodyAll.toString(), email);
            }
        }


        if (!professorsUpdate.isEmpty()) {
            log.info("Hi ha " + professorsUpdate.size() + " profes a actualitzar");

            //Notificacions
            StringBuilder body = new StringBuilder();
            for (UsuariDto professor : professorsUpdate) {
                body.append(professor.getGsuiteFullName()).append(" correu: ").append(professor.getGsuiteEmail()).append("<br><br>");
            }
        }

        if (!alumnesUpdate.isEmpty()) {
            log.info("Hi ha " + alumnesUpdate.size() + " alumnes a actualitzar");

            //Notificacions
            StringBuilder bodyAll = new StringBuilder();

            for (UsuariDto alumne : alumnesUpdate) {
                bodyAll.append(alumne.getGsuiteFullName())
                        .append(" correu: ")
                        .append(alumne.getGsuiteEmail())
                        .append("<br><br>");
            }
        }

        log.info("El procés de nous usuaris creats i actualitzats ha finalitzat correctament");
    }

    private void createGrupsCorreuGSuiteToDatabase() throws InterruptedException {
        List<Group> groups = gSuiteService.getGroups();

        for (Group grup : groups) {

            GrupCorreuDto grupCorreu = grupCorreuService.findByEmail(grup.getEmail());

            if (grupCorreu == null) {
                //Creem el grup de correu a la BBDD
                grupCorreuService.save(null, grup.getName(), grup.getEmail(), grup.getDescription(), GrupCorreuTipusDto.GENERAL);
            }
        }

        log.info("Acaba creació grups de correu.");
    }

    private void deleteGrupsCorreuGSuiteToDatabase() throws InterruptedException {
        List<Group> groups = gSuiteService.getGroups();

        List<GrupCorreuDto> grupsCorreu = grupCorreuService.findAll();

        //Esborrem a la base de dades els grups que NO siguin a GSuite
        for (GrupCorreuDto grupCorreu : grupsCorreu) {
            boolean trobat = false;
            for (Group grup : groups) {
                if (grupCorreu.getGsuiteEmail().equals(grup.getEmail())) {
                    trobat = true;
                }
            }
            if (!trobat) {
                //Esborrem els usuaris del grup de correu
                grupCorreuService.esborrarUsuarisNoBloquejatsGrupCorreu(grupCorreu);

                //Esborrem els grups de correu del grup de correu
                grupCorreuService.esborrarGrupsCorreuGrupCorreu(grupCorreu);

                //Esborrem el grup
                grupCorreuService.esborrarGrup(grupCorreu);

                log.info("Grup de correu " + grupCorreu.getGsuiteEmail() + " esborrat amb èxit");
                grupCorreuService.esborrarGrup(grupCorreu);
            }
        }

        log.info("Acaba esborrat grups de correu.");

    }

    private void updateGrupsCorreuGSuiteToDatabase() throws InterruptedException {

        List<GrupCorreuDto> grupsCorreu = grupCorreuService.findAll();

        for (GrupCorreuDto grupCorreu : grupsCorreu) {
            System.out.println("PROCESSANT GRUP" + grupCorreu.getGsuiteEmail());

            //Esborrem els usuaris del grup de correu
            List<UsuariGrupCorreuDto> usuarisBloquejats = grupCorreuService.esborrarUsuarisNoBloquejatsGrupCorreu(grupCorreu);
            //grupCorreu.setUsuaris(new HashSet<>());
            grupCorreu.setUsuarisGrupCorreu(new HashSet<>(usuarisBloquejats));

            //Esborrem els grups de correu del grup de correu
            grupCorreuService.esborrarGrupsCorreuGrupCorreu(grupCorreu);
            grupCorreu.setGrupCorreus(new HashSet<>());

            List<Member> members = gSuiteService.getMembers(grupCorreu.getGsuiteEmail());

            for (Member member : members) {
                GrupCorreuDto grupCorreuMember = grupCorreuService.findByEmail(member.getEmail());

                if (grupCorreuMember != null) {
                    grupCorreuService.insertGrupCorreu(grupCorreu, grupCorreuMember);
                    grupCorreu.getGrupCorreus().add(grupCorreuMember);
                }
            }

            grupCorreuService.save(grupCorreu);

            //Com que controlem la relació N-M Usuari-Grup Correu amb una clase apart, hem de fer les modificacions
            //DESPRÉS de guardar el grup de correu, sinó no ho guarda bé
            for (Member member : members) {
                UsuariDto usuari = usuariService.findByEmail(member.getEmail());
                if (usuari != null) {
                    UsuariGrupCorreuDto usuariBloquejat = grupCorreu.getUsuarisGrupCorreu().stream().filter(ug->ug.getUsuari().getIdusuari().equals(usuari.getIdusuari())).findFirst().orElse(null);

                    grupCorreuService.insertUsuari(grupCorreu, usuari, usuariBloquejat!=null);
                }
            }

            //Afegim els bloquejats
            for(UsuariGrupCorreuDto usuaribloquejat: usuarisBloquejats){
                grupCorreuService.insertUsuari(grupCorreu, usuaribloquejat.getUsuari(), true);
            }
        }
        log.info("Acaba processat grups de correu.");
    }

    private void reassignarGrupsProfessor() throws InterruptedException {
        List<UsuariDto> professors = usuariService.findProfessors();

        for (UsuariDto usuari : professors) {
            if (usuari.getGestibProfessor() && usuari.getActiu()) {
                log.info("Reassignant grups usuari: " + usuari.getGsuiteEmail() + " - " + usuari.getIdusuari());

                List<Group> grupsProfessorOld = gSuiteService.getUserGroups(usuari.getGsuiteEmail());
                List<GrupCorreuDto> grupsProfessorNew = new ArrayList<>();

                //Afegir al departament
                if (usuari.getGestibDepartament() != null && !usuari.getGestibDepartament().isEmpty()) {
                    DepartamentDto departamentUsuari = departamentService.findByGestibIdentificador(usuari.getGestibDepartament());
                    if (departamentUsuari != null) {
                        List<GrupCorreuDto> correusDepartament = grupCorreuService.findByDepartament(departamentUsuari);
                        for (GrupCorreuDto grupCorreuDepartament : correusDepartament) {
                            boolean pertanyAlGrup = this.pertanyAlGrup(grupCorreuDepartament.getGsuiteEmail(), grupsProfessorOld);

                            if (!pertanyAlGrup) {
                                gSuiteService.createMember(usuari.getGsuiteEmail(), grupCorreuDepartament.getGsuiteEmail());
                            }
                            grupsProfessorNew.add(grupCorreuDepartament);

                        }
                    } else {
                        log.error("El departament amb codi " + usuari.getGestibDepartament() + " no existeix.");
                    }
                }


                //Afegir com a tutor
                List<GrupDto> grupsTutoria = grupService.findByTutor(usuari);
                List<GrupCorreuDto> grupsCorreuTutoria = grupCorreuService.findAll().stream().filter(gc -> gc.getGrupCorreuTipus().equals(GrupCorreuTipusDto.TUTORS)).collect(Collectors.toList());
                if (grupsTutoria != null && !grupsTutoria.isEmpty()) {
                    for (GrupCorreuDto grupCorreu : grupsCorreuTutoria) {
                        Set<GrupDto> grupsClasseGrupCorreu = grupCorreu.getGrups();
                        for (GrupDto grupClasseGrupCorreu : grupsClasseGrupCorreu) {
                            for (GrupDto grupTutoria : grupsTutoria) {
                                if (grupClasseGrupCorreu.getIdgrup().equals(grupTutoria.getIdgrup())) {
                                    boolean pertanyAlGrup = this.pertanyAlGrup(grupCorreu.getGsuiteEmail(), grupsProfessorOld);

                                    if (!pertanyAlGrup) {
                                        gSuiteService.createMember(usuari.getGsuiteEmail(), grupCorreu.getGsuiteEmail());
                                    }
                                    grupsProfessorNew.add(grupCorreu);
                                }
                            }
                        }
                    }
                } else {
                    log.info("L'usuari " + usuari.getGsuiteEmail() + " no és tutor");
                }

                // Afegir al grup de claustre
                List<GrupCorreuDto> grupsClaustre = grupCorreuService.findAll().stream().filter(gc -> gc.getGrupCorreuTipus().equals(GrupCorreuTipusDto.CLAUSTRE)).collect(Collectors.toList());
                if (usuari.getGestibProfessor() && usuari.getActiu()) {
                    for (GrupCorreuDto grupClaustre : grupsClaustre) {
                        boolean pertanyAlGrup = this.pertanyAlGrup(grupClaustre.getGsuiteEmail(), grupsProfessorOld);

                        if (!pertanyAlGrup) {
                            gSuiteService.createMember(usuari.getGsuiteEmail(), grupClaustre.getGsuiteEmail());
                        }
                        grupsProfessorNew.add(grupClaustre);
                    }
                }


                //Afegir al grup d'equips educatius
                List<SessioDto> sessions = sessioService.findSessionsProfessor(usuari);
                Set<String> grupsProfe = new HashSet<>();
                for (SessioDto sessio : sessions) {
                    String codiGrup = sessio.getGestibGrup();
                    if (codiGrup != null && !codiGrup.isEmpty()) {
                        grupsProfe.add(codiGrup);
                    }
                }
                for (String grupProfe : grupsProfe) {
                    List<GrupCorreuDto> grupsCorreuProfe = grupCorreuService.findByCodiGrupGestib(grupProfe);
                    for (GrupCorreuDto grupCorreu : grupsCorreuProfe) {
                        if (grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.PROFESSORAT)) {
                            boolean pertanyAlGrup = this.pertanyAlGrup(grupCorreu.getGsuiteEmail(), grupsProfessorOld);

                            if (!pertanyAlGrup) {
                                gSuiteService.createMember(usuari.getGsuiteEmail(), grupCorreu.getGsuiteEmail());
                            }
                            grupsProfessorNew.add(grupCorreu);
                        }
                    }
                }

                /* TODO: AFEGIR PROFESSOR ALS GRUPS FCT */

                /* TODO: AFEGIR PROFESSOR ALS GRUPS DE COORDINACIO */

                /* TODO: AFEGIR PROFESSOR ALS CALENDARIS DE L'ESCOLA */

                //Esborrem grups que no hem trobat
                for (Group grupOld : grupsProfessorOld) {
                    boolean trobat = false;
                    for (GrupCorreuDto grupNew : grupsProfessorNew) {
                        if (grupOld.getEmail().equals(grupNew.getGsuiteEmail())) {
                            trobat = true;
                            break;
                        }
                    }

                    if (!trobat) {
                        GrupCorreuDto grupCorreu = grupCorreuService.findByEmail(grupOld.getEmail());
                        if (grupCorreu != null) {
                            boolean isGrupClaustre = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.CLAUSTRE);
                            boolean isGrupProfessors = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.PROFESSORAT);
                            boolean isGrupTutors = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.TUTORS);
                            boolean isGrupDepartament = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.DEPARTAMENT);
                            boolean isGrupTutorsFCT = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.TUTORS_FCT);
                            boolean isGrupCoordinacions = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.COORDINACIO);
                            boolean isGrupAlumnat = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.ALUMNAT);
                            boolean isBloquejat = grupCorreu.getUsuarisGrupCorreu().stream().filter(ugc->ugc.getUsuari().getIdusuari().equals(usuari.getIdusuari()) && ugc.isBloquejat()).collect(Collectors.toList()).size()>0;
                            if (isGrupAlumnat || isGrupClaustre || isGrupProfessors || isGrupTutors || isGrupDepartament || isGrupTutorsFCT || isGrupCoordinacions) {
                                if (usuari.getGsuiteEmail() != null && grupOld.getEmail() != null && !isBloquejat) {
                                    gSuiteService.deleteMember(usuari.getGsuiteEmail(), grupOld.getEmail());
                                }
                            }
                        }
                    }
                }
            }
        }
        log.info("Acaba sincronització professors");
    }

    private void reassignarGrupsAlumne() throws InterruptedException {
        List<UsuariDto> alumnes = usuariService.findAlumnes(false);

        for (UsuariDto usuari : alumnes) {
            if (usuari.getGestibAlumne() && usuari.getActiu()) {
                List<Group> grupsAlumneOld = gSuiteService.getUserGroups(usuari.getGsuiteEmail());
                List<GrupCorreuDto> grupsAlumneNew = new ArrayList<>();

                List<String> grupsAlumne = new ArrayList<>();
                if (usuari.getGestibGrup() != null) {
                    grupsAlumne.add(usuari.getGestibGrup());
                }
                if (usuari.getGestibGrup2() != null) {
                    grupsAlumne.add(usuari.getGestibGrup2());
                }
                if (usuari.getGestibGrup3() != null) {
                    grupsAlumne.add(usuari.getGestibGrup3());
                }

                for (String grupAlumne : grupsAlumne) {
                    GrupDto grup = grupService.findByGestibIdentificador(grupAlumne);
                    if (grup != null) {

                        /* TODO: CALENDARI GRUPS ALUMNES */
                        //Afegir als seus calendaris de grup
                        /*String grupCalendari = curs.getGsuiteUnitatOrganitzativa() + grup.getGestibNom();
                        grupCalendari = grupCalendari.toLowerCase();
                        CalendariDto calendariGrup = calendariService.findByGestibGrup(grupCalendari);

                        if (calendariGrup != null && calendariGrup.getGsuiteEmail() != null) {
                            gSuiteService.insertUserCalendar(alumne.getGsuiteEmail(), calendariGrup.getGsuiteEmail());
                        }*/
                    }

                    List<GrupCorreuDto> grupsCorreuAlumne = grupCorreuService.findByCodiGrupGestib(grupAlumne);
                    for (GrupCorreuDto grupCorreu : grupsCorreuAlumne) {
                        if (grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.ALUMNAT)) {
                            boolean pertanyAlGrup = this.pertanyAlGrup(grupCorreu.getGsuiteEmail(), grupsAlumneOld);

                            if (!pertanyAlGrup) {
                                //Creem l'alumne dins el seu grup
                                gSuiteService.createMember(usuari.getGsuiteEmail(), grupCorreu.getGsuiteEmail());
                            }
                            grupsAlumneNew.add(grupCorreu);

                            //Actualitzem la unitat organitzativa de l'alumne i el nom a GSuite
                            if (grup != null) {
                                CursDto curs = cursService.findByGestibIdentificador(grup.getGestibCurs());

                                String rutaUnitat = "";
                                if (grup.getGsuiteUnitatOrganitzativa() == null || grup.getGsuiteUnitatOrganitzativa().isEmpty()) {
                                    rutaUnitat = this.defaultUOAlumnes;
                                } else {
                                    rutaUnitat = grup.getGsuiteUnitatOrganitzativa();
                                }

                                String nom = usuari.getGestibNom();
                                String cognoms = usuari.getGestibCognom1() + " " + usuari.getGestibCognom2();

                                if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2")) {
                                    nom = UtilService.capitalize(nom);
                                    cognoms = UtilService.capitalize(cognoms);
                                } else if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2cursgrup")) {
                                    if (curs == null || curs.getGestibNom() == null || curs.getGestibNom().isEmpty() || grup.getGestibNom() == null || grup.getGestibNom().isEmpty()) {
                                        cognoms = usuari.getGestibCognom1() + " " + usuari.getGestibCognom2();
                                    } else {
                                        cognoms = usuari.getGestibCognom1() + " " + usuari.getGestibCognom2() + " " + curs.getGestibNom() + grup.getGestibNom();
                                    }
                                    nom = UtilService.capitalize(nom);
                                    cognoms = UtilService.capitalize(cognoms);
                                }

                                if (!usuari.getGsuiteGivenName().equals(nom) || !usuari.getGsuiteFamilyName().equals(cognoms)) {
                                    User usuariGSuite = gSuiteService.updateUser(usuari.getGsuiteEmail(), nom, cognoms, usuari.getGestibCodi(), rutaUnitat);

                                    if (usuariGSuite != null) {
                                        log.info("Usuari" + usuariGSuite.getPrimaryEmail() + " modificat correctament a GSuite");
                                    } else {
                                        log.error("Error modificant usuari " + usuari.getGsuiteEmail(), "Error modificant usuari " + usuari.getGsuiteEmail(), this.adminUser);
                                    }
                                }
                            }
                        }
                    }
                }


                //Esborrem grups que no hem trobat
                for (Group grupOld : grupsAlumneOld) {
                    boolean trobat = false;
                    for (GrupCorreuDto grupNew : grupsAlumneNew) {
                        if (grupOld.getEmail().equals(grupNew.getGsuiteEmail())) {
                            trobat = true;
                            break;
                        }
                    }

                    if (!trobat) {
                        GrupCorreuDto grupCorreu = grupCorreuService.findByEmail(grupOld.getEmail());
                        boolean isGrupClaustre = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.CLAUSTRE);
                        boolean isGrupProfessors = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.PROFESSORAT);
                        boolean isGrupTutors = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.TUTORS);
                        boolean isGrupDepartament = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.DEPARTAMENT);
                        boolean isGrupTutorsFCT = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.TUTORS_FCT);
                        boolean isGrupCoordinacions = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.COORDINACIO);
                        boolean isGrupAlumnat = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.ALUMNAT);
                        boolean isBloquejat = grupCorreu.getUsuarisGrupCorreu().stream().filter(ugc->ugc.getUsuari().getIdusuari().equals(usuari.getIdusuari()) && ugc.isBloquejat()).collect(Collectors.toList()).size()>0;
                        if (isGrupAlumnat || isGrupClaustre || isGrupProfessors || isGrupTutors || isGrupDepartament || isGrupTutorsFCT || isGrupCoordinacions) {
                            if (usuari.getGsuiteEmail() != null && grupOld.getEmail() != null && !isBloquejat) {
                                gSuiteService.deleteMember(usuari.getGsuiteEmail(), grupOld.getEmail());
                            }
                        }
                    }
                }

            }
        }

        log.info("Acaba sincronització alumnes");
    }

    private boolean pertanyAlGrup(String email, List<Group> grupUsuaris) {
        boolean pertanyAlGrup = false;
        for (Group grup : grupUsuaris) {
            if (grup.getEmail().equals(email)) {
                pertanyAlGrup = true;
                break;
            }
        }
        return pertanyAlGrup;
    }

    private void esborrarGrupsUsuarisNoActius() throws InterruptedException {
        List<UsuariDto> usuarisNoActius = usuariService.findUsuarisNoActius().stream().filter(u -> u.getGsuiteSuspes() == null || !u.getGsuiteSuspes()).collect(Collectors.toList());

        log.info("Esborrem grups de Alumnes, Claustre, Professors, Tutors, Departament, Tutors FCT i Coordinacions");
        for (UsuariDto usuari : usuarisNoActius) {
            List<Group> grups = gSuiteService.getUserGroups(usuari.getGsuiteEmail());
            for (Group grup : grups) {
                GrupCorreuDto grupCorreu = grupCorreuService.findByEmail(grup.getEmail());
                if (grupCorreu != null) {
                    //Esborrem grup de professors, tutors, departament, tutors FCT i coordinacions
                    boolean isGrupClaustre = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.CLAUSTRE);
                    boolean isGrupProfessors = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.PROFESSORAT);
                    boolean isGrupTutors = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.TUTORS);
                    boolean isGrupDepartament = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.DEPARTAMENT);
                    boolean isGrupTutorsFCT = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.TUTORS_FCT);
                    boolean isGrupCoordinacions = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.COORDINACIO);
                    boolean isGrupAlumnes = grupCorreu.getGrupCorreuTipus().equals(GrupCorreuTipusDto.ALUMNAT);
                    boolean isSuspes = false;
                    if (usuari.getGsuiteSuspes() != null) {
                        isSuspes = usuari.getGsuiteSuspes();
                    }
                    if (isSuspes || isGrupAlumnes || isGrupClaustre || isGrupProfessors || isGrupTutors || isGrupDepartament || isGrupTutorsFCT || isGrupCoordinacions) {
                        if (usuari.getGsuiteEmail() != null && grup.getEmail() != null) {
                            gSuiteService.deleteMember(usuari.getGsuiteEmail(), grup.getEmail());
                        }
                    }
                }
            }
        }
    }

    private String generateUsername(UsuariDto usuari, String format) throws InterruptedException {

        String nom = removeAccents(usuari.getGestibNom());
        String cognom1 = removeAccents(usuari.getGestibCognom1());
        String cognom2 = removeAccents(usuari.getGestibCognom2());

        //Default
        String username = nom.trim() + cognom1.trim() + cognom2.trim();
        String domini = "@" + this.dominiPrincipal;

        if (usuari.getGestibAlumne()) {
            domini = "@" + this.dominiAlumnat;
        }

        switch (format) {
            case "ncognom1" -> username = nom.charAt(0) + cognom1.trim();
            case "ncognom1exp" -> username = nom.charAt(0) + cognom1.trim() + usuari.getGestibExpedient();
            case "n.cognom1cognom2" -> username = nom.charAt(0) + "." + cognom1.trim() + cognom2.trim();
        }

        username = username.trim().toLowerCase();

        User u = gSuiteService.getUserById(username + domini);

        if (u == null) {
            return username + domini;
        }

        //Si existeix cerquem el més proper
        int i = 0;
        while (u != null) {
            i++; //Incrementem aquí perquè quan surti del bucle ha de mantenir el mateix valor.
            u = gSuiteService.getUserById(username + i + domini);
        }

        return username + i + domini;
    }

    private String removeAccents(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String accentRemoved = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        accentRemoved = accentRemoved.replaceAll("'", "");
        accentRemoved = accentRemoved.replaceAll("\\s", "");
        return accentRemoved;
    }

    private boolean usuariTeGrup(UsuariDto u, String grup) {
        if (u == null || grup == null) {
            return false;
        }
        return (grup.equals(u.getGestibGrup()) || grup.equals(u.getGestibGrup2()) || grup.equals(u.getGestibGrup3()));
    }
}
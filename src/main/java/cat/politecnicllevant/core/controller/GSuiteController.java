package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.common.service.UtilService;
import cat.politecnicllevant.core.dto.gestib.CursDto;
import cat.politecnicllevant.core.dto.gestib.GrupDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.service.*;
import cat.politecnicllevant.core.service.*;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.directory.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class GSuiteController {
    @Autowired
    private GSuiteService gSuiteService;

    @Autowired
    private GrupCorreuService grupCorreuService;

    @Autowired
    private CalendariService calendariService;

    @Autowired
    private DepartamentService departamentService;

    @Autowired
    private CursService cursService;

    @Autowired
    private GrupService grupService;

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private GMailService gMailService;

    @Autowired
    private Gson gson;

    @Value("${public.password}")
    private String publicPassword;

    @Value("${centre.gsuite.fullname.professors}")
    private String formatNomGSuiteProfessors;

    @Value("${centre.gsuite.fullname.alumnes}")
    private String formatNomGSuiteAlumnes;


    @PostMapping("/external/gsuite/sendemailattachment")
    public void sendExternalEmail(@RequestParam("password") String password, @RequestParam("to") String to, @RequestParam("assumpte") String assumpte, @RequestParam("body") String bodyHTML, HttpServletRequest request) throws ServletException, IOException, MessagingException, GeneralSecurityException {

        if(!publicPassword.equals(password)){
            System.out.println("Sense autorització");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sense autorització per enviar un correu");
        }


        Part filePart = request.getPart("arxiu");

        InputStream is = filePart.getInputStream();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] readBuf = new byte[4096];
        while (is.available() > 0) {
            int bytesRead = is.read(readBuf);
            os.write(readBuf, 0, bytesRead);
        }

        // Passam l'arxiu a dins una carpeta
        String fileName = "/tmp/"+filePart.getSubmittedFileName();

        OutputStream outputStream = new FileOutputStream(fileName);
        os.writeTo(outputStream);

        File f = new File(fileName);

        System.out.println("Params: "+to+" - "+assumpte+" - "+bodyHTML);
        log.info("Params: "+to+" - "+assumpte+" - "+bodyHTML);

        gMailService.sendMessageWithAttachment(assumpte,bodyHTML,to,true,f);
        //gMailService.sendMessage(assumpte,bodyHTML,to);
    }


    @PostMapping("/gsuite/normalize-noms")
    public ResponseEntity<Notificacio> normalitzarNomsGsuite() throws InterruptedException {
        List<UsuariDto> usuarisGestib = usuariService.findAll().stream().filter(u->u.getGestibAlumne()!=null && u.getGestibProfessor()!=null).collect(Collectors.toList());

        List<User> usuarisGSuite = gSuiteService.getUsers();

        for(UsuariDto usuariGestib: usuarisGestib){
            for(User usuariGSuite: usuarisGSuite){
                if( (usuariGestib.getGestibAlumne() || usuariGestib.getGestibProfessor()) && usuariGestib.getGsuiteEmail() != null && usuariGSuite.getPrimaryEmail() != null && !usuariGestib.getGsuiteEmail().isEmpty()) {
                    String personalIdKey = "";

                    try {
                        List personalIDValueOrganization = (ArrayList) usuariGSuite.getExternalIds();
                        ArrayMap userKey = (ArrayMap) personalIDValueOrganization.get(0);

                        String valueKey = userKey.getKey(0).toString();
                        String valueValue = userKey.getValue(0).toString();

                        String organizationKey = userKey.getKey(1).toString();
                        String organizationValue = userKey.getValue(1).toString();

                        if (valueKey.equals("value") && organizationKey.equals("type") && organizationValue.equals("organization")) {
                            personalIdKey = valueValue;
                        }
                    } catch (Exception e) {
                        log.error("Error al obtenir el personalIdKey de "+usuariGestib.getGsuiteEmail());
                    }

                    boolean correctPersonalIDKey = ((usuariGestib.getGestibAlumne() || usuariGestib.getGestibProfessor()) && !personalIdKey.isEmpty()) || (!usuariGestib.getGestibAlumne() && !usuariGestib.getGestibProfessor());
                    if ( correctPersonalIDKey && usuariGestib.getGestibCodi().equals(usuariGestib.getGsuitePersonalID()) && usuariGestib.getGestibCodi().equals(personalIdKey) && usuariGSuite.getPrimaryEmail().equals(usuariGestib.getGsuiteEmail())) {
                        log.info("Actualitzant nom de "+usuariGestib.getGsuiteEmail());

                        String nom =  UtilService.capitalize(usuariGSuite.getName().getGivenName());
                        String cognoms = UtilService.capitalize(usuariGSuite.getName().getFamilyName());

                        if(usuariGestib.getGestibAlumne() && !usuariGestib.getGestibProfessor()) {
                            if(usuariGestib.getGestibNom()!=null) {
                                nom = usuariGestib.getGestibNom();
                            }

                            if(usuariGestib.getGestibCognom1()!=null) {
                                cognoms = usuariGestib.getGestibCognom1();
                            }
                            if(usuariGestib.getGestibCognom2()!=null) {
                                cognoms += " " + usuariGestib.getGestibCognom2();
                            }

                            if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2")) {
                                nom = UtilService.capitalize(nom);
                                cognoms = UtilService.capitalize(cognoms);

                            } else if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2cursgrup")) {
                                GrupDto grup = grupService.findByGestibIdentificador(usuariGestib.getGestibGrup());
                                CursDto curs = null;
                                if (grup != null) {
                                    curs = cursService.findByGestibIdentificador(grup.getGestibCurs());
                                    if (curs.getGestibNom() != null && !curs.getGestibNom().isEmpty() && grup.getGestibNom() != null && !grup.getGestibNom().isEmpty()) {
                                        cognoms += " " + curs.getGestibNom() + grup.getGestibNom();
                                    }
                                    nom = UtilService.capitalize(nom);
                                    cognoms = UtilService.capitalize(cognoms);
                                }
                            }
                        } else if(usuariGestib.getGestibProfessor()){
                            nom =  usuariGSuite.getName().getGivenName();
                            cognoms = usuariGSuite.getName().getFamilyName();

                            if(formatNomGSuiteProfessors.equals("nomcognom1cognom2")){
                                nom = UtilService.capitalize(nom);
                                cognoms = UtilService.capitalize(cognoms);
                            }
                        }

                        User userGsuite = gSuiteService.updateUser(usuariGSuite.getPrimaryEmail(),nom,cognoms,personalIdKey,usuariGSuite.getOrgUnitPath());

                        if(userGsuite!=null) {
                            usuariGestib.setGsuiteFamilyName(usuariGSuite.getName().getFamilyName());
                            usuariGestib.setGsuiteGivenName(usuariGSuite.getName().getGivenName());
                            usuariGestib.setGsuiteFullName(usuariGSuite.getName().getFullName());
                            usuariService.save(usuariGestib);
                        } else {
                            log.error("L'usuari "+usuariGestib.getGsuiteEmail()+" és nul a GSuite");
                        }
                    }
                }
            }
        }


        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage("Normalització d'usuaris finalitzat correctament");
        notificacio.setNotifyType(NotificacioTipus.SUCCESS);

        log.info("Normalització d'usuaris finalitzat correctament");

        return new ResponseEntity<>(notificacio, HttpStatus.OK);
    }

    @PostMapping("/gsuite/sendemail")
    public void sendEmail(@RequestBody String json) throws IOException, MessagingException, GeneralSecurityException {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        String assumpte = jsonObject.get("assumpte").getAsString();
        String missatge = jsonObject.get("missatge").getAsString();
        String to = jsonObject.get("to").getAsString();
        gMailService.sendMessage(assumpte,missatge,to);
    }

    @PostMapping(value="/gsuite/sendemailattachment")
    public void sendEmail(@RequestParam("to") String to, @RequestParam("assumpte") String assumpte, @RequestParam("body") String bodyHTML, @RequestParam File file) throws IOException, MessagingException, GeneralSecurityException {
        InputStream is = new FileInputStream(file);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] readBuf = new byte[4096];
        while (is.available() > 0) {
            int bytesRead = is.read(readBuf);
            os.write(readBuf, 0, bytesRead);
        }

        // Passam l'arxiu a dins una carpeta
        String fileName = "/tmp/"+file.getName();

        OutputStream outputStream = new FileOutputStream(fileName);
        os.writeTo(outputStream);

        File f = new File(fileName);

        System.out.println("Params: "+to+" - "+assumpte+" - "+bodyHTML);
        log.info("Params: "+to+" - "+assumpte+" - "+bodyHTML);

        gMailService.sendMessageWithAttachment(assumpte,bodyHTML,to,true,f);
        //gMailService.sendMessage(assumpte,bodyHTML,to);
    }

    @PostMapping(value="/gsuite/sendemailattachment-path")
    public void sendEmailAttachmentPath(@RequestParam("to") String to, @RequestParam("assumpte") String assumpte, @RequestParam("body") String bodyHTML, @RequestParam("path") String filepath) throws IOException, MessagingException, GeneralSecurityException {
        File file = new File(filepath);
        this.sendEmail(to,assumpte,bodyHTML,file);
    }

/*
    @PostMapping("/auth/samplenotification")
    public ResponseEntity<Notificacio> sampleNotification() throws InterruptedException {
        Thread.sleep(2000);
        Notificacio n = new Notificacio();
        n.setNotifyMessage("Prova notificació");
        n.setNotifyType(NotificacioTipus.WARNING);
        return new ResponseEntity<>(n, HttpStatus.OK);
    }

    @GetMapping("/gsuite/users")
    public ResponseEntity<List<User>> getUsers() throws GeneralSecurityException, IOException {
        List<User> users = gSuiteService.getUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/gsuite/groups")
    public ResponseEntity<List<Group>> getGroups() throws GeneralSecurityException, IOException {
        List<Group> groups = gSuiteService.getGroups();
        return new ResponseEntity<>(groups, HttpStatus.OK);
    }

    @GetMapping("/gsuite/group/{id}/members")
    public ResponseEntity<List<Member>> getGroupMembers(@PathVariable("id") String idgrup) throws GeneralSecurityException, IOException {
        List<Member> members = gSuiteService.getMembers(idgrup);
        return new ResponseEntity<>(members, HttpStatus.OK);
    }

    @GetMapping("/gsuite/groups/create")
    public ResponseEntity<String> getGroupsCreate() throws GeneralSecurityException, IOException {
        List<Group> groups = gSuiteService.getGroups();
        for (Group g : groups) {
            GrupCorreu gc = grupCorreuService.findByEmail(g.getEmail());
            if (gc == null) {
                grupCorreuService.save(null, g.getName(), g.getEmail(), g.getDescription(), GrupCorreuTipus.GENERAL);
            }
        }
        return new ResponseEntity<>("Grups creats correctament", HttpStatus.OK);
    }



    @GetMapping("/gsuite/calendar/load")
    @Transactional
    public ResponseEntity<String> loadCalendarGrups() {

        Multimap<String, String> calendarsGrups = LinkedHashMultimap.create();

        // 1r ESO
        calendarsGrups.put("eso1a", "c_188dcf6qc5u7qgeqmehnnclofb74e4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso1b", "c_188apa4lu1l8gh61nc0ohc9hn50ns4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso1c", "c_18849i6pmv8jgh52go6qvuvocsmmm4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso1d", "c_188d8b7a3cv72g55m965hk5liphgi4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso1e", "c_1885ct6mfh8kggnuhp53v0oqhts8g4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso1f", "c_188ft83ij8ckqgp9jgqql630lfu844ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso1g", "c_188e9ovqeet9ejdqgeoe2ofv8nqjk4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso1h", "c_1880mf7khrf8cgs0kio18pal43e4e4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // 2n ESO
        calendarsGrups.put("eso2a", "c_1882usr9lehg6jmal3be65bta8aqa4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso2b", "c_188db946f8q0aikqkoo2l53kdgkd44ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso2c", "c_1885pu0b8m688gi4n9sm19sjv5s024ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso2d", "c_1889s4o1pj3lmhimj2s9ndhdir0ku4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso2e", "c_1881ao2f58eiiim8htbfa6b9r8d8a4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso2f", "c_188aa77as6f2ui5vnl7h0gu5nafk24ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso2g", "c_18855tr1sdeesh7vg0vp9ls4e8op84ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // 3r ESO
        calendarsGrups.put("eso3a", "c_188bi2gfq1pm0g0nhe11f0rg6qg4s4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso3b", "c_1888ti2kpombkhqsnq95q3rr6usuo4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso3c", "c_1889rp621rdc8j8onc1e4vcmc16924ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso3d", "c_1885v7l14rn60g8jg909e833oufbq4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso3e", "c_188bsbd2gc91aic9nic4437nbtrv24ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso3f", "c_1886et3cjs9oej43i2lspgbk0aohm4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // 4t ESO
        calendarsGrups.put("eso4a", "c_188fjpr8vb4g4gf9mu51r873q8tr64ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso4b", "c_188bplcv2127sjdhklpc90k0b57544ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso4c", "c_188e3ppqur5oci66kt4qejnvnsfv24ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso4d", "c_18862oop2r6u4g6knkuobcu1il2rc4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso4e", "c_188577oiaa9goganinmj56ms5e0nq4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("eso4f", "c_1888kp8aijiooh1lkmi4h7tkvkd5q4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // 1r Batx
        calendarsGrups.put("batx1a", "c_1883r33jar55ei6fh87a0ag9orb3u4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("batx1b", "c_1885citehufiajjugi1cbk0phlfkc4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("batx1c", "c_18889gkkg50h6jh1gmplmn4jbjmri4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("batx1d", "c_188af98o05rsgj4om2dn85prvosma4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("batx1e", "c_188arfv29u0peghhg97ksm16pj1504ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // 2n Batx
        calendarsGrups.put("batx2a", "c_1889dckj25jb2g0umd6ppnufhd9no4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("batx2b", "c_1889vk37te9g0hstlkg5fbq8eieii4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("batx2c", "c_188etp7jsqqamhlek913ndihgfuc04ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("batx2d", "c_188916scijs76gvfm0j0bbbo6v1c24ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // ADG21
        calendarsGrups.put("adg21a", "c_1889lvtsqf29qhlik4mn6tfponiio4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("adg21b", "c_188f36mphhe6ehbdjlh1q4vtts3g04ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("adg21c", "c_188289g04hk9mgjoj43n4djch7g6g4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // ADG32
        calendarsGrups.put("adg32a", "c_188b7b4o3n7n6i66j79v6vh4dmbim4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("adg32b", "c_18825qt5bsou2hidjtl864hnmpplc4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("adg32c", "c_18854779j6h00hitk7bmec5nmjpt04ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // COM11
        calendarsGrups.put("com11a", "c_188dd04g3dblkgk1m5bbb3jdpcso04ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("com11b", "c_18867mtv3pkkigeom7ieuq947oore4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("com11c", "c_188auhbas4b5chq2l0grpektjm9l04ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // COM21
        calendarsGrups.put("com21a", "c_18863jlabrbm0i6lijdlhk6m6ln184ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("com21b", "c_1886tlpnhq6fkj5ki1qrnccrngviu4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("com21c", "c_1880ek582a3n8hppmb2lf6q04dedk4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // COM31
        calendarsGrups.put("com31a", "c_18878mm6r74cihgbh575m2p3f0f044ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("com31b", "c_1889q9pl38seuidegi2t5jh5e9mks4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("com31c", "c_188bn6subrpq6gr4ir4gk9m9bo06s4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // TMV11
        calendarsGrups.put("tmv11a", "c_18859deji9ptggpkg0mvr77m3o6p24ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv11b", "c_188bvie2jf2q2i83m5baa5jp2rii04ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv11c", "c_188cmuf71f300g9emnif2u26rishq4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // TMV21
        calendarsGrups.put("tmv21a", "c_1887q0bdt5l1qgn8n47vbifvmeau04ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv21b", "c_1888sio4pg47qjaon7harovucuauc4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv21c", "c_188cotj3c4c08gqcn2djj2pllpblk4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv21d", "c_1883966d4eqd8isbnlv3o4le68to64ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // TMV22
        calendarsGrups.put("tmv22a", "c_188bluc0k9c2ci2pmnhmof1g7vglc4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv22b", "c_18814dqqf7etii37g886779gge4dq4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv22c", "c_1884i8fuuv6g0g6rk5ja64lqtgl464ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv22d", "c_1886o4rg6epcch51jraa2e95uc2n24ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv22e", "c_188atnqs9jq2cg2uk7n0glfgf07qi4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // TMV31
        calendarsGrups.put("tmv31a", "c_1881uvpg8kv62ivsimjji2c9p39404ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv31b", "c_18880rbja7rfuhvtio2iqevnatroe4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("tmv31c", "c_1887el2qeimnmhm0mn8r2v6d2gudk4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // ELE21
        calendarsGrups.put("ele21a", "c_188akicvc3f68ggpk5jqr4lus8m064ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ele21b", "c_1882f9mlb37okj5clmkenvavhb94i4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ele21c", "c_18802mbffkp4ugpsibcmfbies5fm44ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // ELE31
        calendarsGrups.put("ele31a", "c_18811dt3rrrpojqngst2nlprl35vg4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ele31b", "c_1886k33ef0u7ui2bhrf75mu0423u24ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ele31c", "c_188egas7vvj8ggi6k357gvue96ntg4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // IFC21
        calendarsGrups.put("ifc21a", "c_18848d5tmddesjpgl9opsov5kitrc4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc21b", "c_18849epqhonr6ikogj1sh8733o4ti4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc21c", "c_1883s9vtdk8iihslgl8pjg6v3k40g4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc21d", "c_188f1cq0gc58ej1rm1j7733ku759m4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc21e", "c_188d8paija0cgiitgfoammh7bdl2s4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // IFC31
        calendarsGrups.put("ifc31a", "c_1887l4ipima70gpfn7a14bj55sfbu4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc31b", "c_18813l33g0cp8jcuhr2n3okoor5fe4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc31c", "c_188cb1inrkjgoj4jigfqie7nmbcrc4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // IFC33
        calendarsGrups.put("ifc33a", "c_188bugl5859cmjacmheh9ag7mhvba4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc33b", "c_188dunoes5a2uhfmhdavqidfofbgu4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        calendarsGrups.put("ifc33c", "c_1880eg9rckn1aj58lgr3jc5r9v7404ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");
        // EEBASICA
        calendarsGrups.put("esoeea", "c_188ed8a9m0go8iclg8ub31uv3ibhs4ged5in6rb1dpgm6rri5phm2t0@resource.calendar.google.com");

        for (Map.Entry calendar : calendarsGrups.entries()) {
            System.out.println("Calendar" + calendar.getValue());
            Calendari c = calendariService.findByEmail(calendar.getValue().toString());
            if (c != null) {
                System.out.println("Entra. Calendar:" + calendar.getKey());
                c.setGestibGrup(calendar.getKey().toString());
                calendariService.save(c);
            }
        }

        return new ResponseEntity<>("Assignació de grups a calendari correcte", HttpStatus.OK);
    }



*/

}

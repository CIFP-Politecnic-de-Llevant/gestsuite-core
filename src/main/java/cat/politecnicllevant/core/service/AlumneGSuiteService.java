package cat.politecnicllevant.core.service;

import cat.politecnicllevant.common.model.Notificacio;
import cat.politecnicllevant.common.model.NotificacioTipus;
import cat.politecnicllevant.common.service.UtilService;
import cat.politecnicllevant.core.dto.gestib.CursDto;
import cat.politecnicllevant.core.dto.gestib.GrupDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import com.google.api.services.directory.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Calcula la unitat organitzativa i el nom a GSuite d'un alumne a partir dels seus grups del GestIB.
 * S'utilitza des de la sincronització i des de les pantalles de cursos i grups perquè totes apliquin la mateixa regla.
 */
@Service
@Slf4j
public class AlumneGSuiteService {

    public enum Resultat {ACTUALITZAT, SENSE_CANVIS, OMES, ERROR}

    public record NomGSuite(String nom, String cognoms) {}

    @Autowired
    private GrupService grupService;

    @Autowired
    private CursService cursService;

    @Autowired
    private UsuariService usuariService;

    @Autowired
    private GSuiteService gSuiteService;

    @Value("${centre.gsuite.fullname.alumnes}")
    private String formatNomGSuiteAlumnes;

    @Value("${centre.gsuite.unitatorganitzativa.alumnes.default}")
    private String defaultUOAlumnes;

    //Un alumne pot tenir fins a 3 grups. Agafam el primer que tengui una UO pròpia (diferent de la genèrica),
    //i si cap en té, el primer grup existent. Així la UO no depèn de l'ordre en què es processen els grups.
    public GrupDto grupPrincipal(UsuariDto alumne) {
        GrupDto primerGrup = null;
        for (String codiGrup : Stream.of(alumne.getGestibGrup(), alumne.getGestibGrup2(), alumne.getGestibGrup3()).filter(Objects::nonNull).toList()) {
            GrupDto grup = grupService.findByGestibIdentificador(codiGrup);
            if (grup == null) {
                continue;
            }
            if (primerGrup == null) {
                primerGrup = grup;
            }
            if (teUnitatOrganitzativaPropia(grup)) {
                return grup;
            }
        }
        return primerGrup;
    }

    public String unitatOrganitzativa(GrupDto grup) {
        if (grup == null || grup.getGsuiteUnitatOrganitzativa() == null || grup.getGsuiteUnitatOrganitzativa().isBlank()) {
            return this.defaultUOAlumnes;
        }
        return grup.getGsuiteUnitatOrganitzativa().trim();
    }

    public NomGSuite nomGSuite(UsuariDto alumne, GrupDto grup) {
        String nom = alumne.getGestibNom();
        String cognoms = alumne.getGestibCognom1() + " " + alumne.getGestibCognom2();

        if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2")) {
            nom = UtilService.capitalize(nom);
            cognoms = UtilService.capitalize(cognoms);
        } else if (formatNomGSuiteAlumnes.equals("nomcognom1cognom2cursgrup")) {
            CursDto curs = grup != null ? cursService.findByGestibIdentificador(grup.getGestibCurs()) : null;
            if (curs != null && curs.getGestibNom() != null && !curs.getGestibNom().isEmpty() && grup.getGestibNom() != null && !grup.getGestibNom().isEmpty()) {
                cognoms = cognoms + " " + curs.getGestibNom() + grup.getGestibNom();
            }
            nom = UtilService.capitalize(nom);
            cognoms = UtilService.capitalize(cognoms);
        }

        return new NomGSuite(nom, cognoms);
    }

    /**
     * Actualitza nom i UO de l'alumne a GSuite segons el seu grup principal i desa el resultat a la BBDD.
     *
     * @param forca si és false, només es crida GSuite quan el nom o la UO desats a la BBDD no coincideixen.
     */
    public Resultat actualitzaNomIUnitatOrganitzativa(UsuariDto alumne, boolean forca) throws InterruptedException {
        if (alumne.getGsuiteEmail() == null || alumne.getGsuiteEmail().isEmpty()) {
            return Resultat.OMES;
        }

        GrupDto grup = this.grupPrincipal(alumne);
        if (grup == null) {
            return Resultat.OMES;
        }

        //Si la UO està bloquejada no en tocam la UO: passam null i GSuite conserva la que té (la de la BBDD pot estar desfasada)
        boolean bloquejat = Boolean.TRUE.equals(alumne.getBloquejaGsuiteUnitatOrganitzativa());
        String rutaUnitat = bloquejat ? null : this.unitatOrganitzativa(grup);

        NomGSuite nomGSuite = this.nomGSuite(alumne, grup);

        boolean canviNom = !Objects.equals(nomGSuite.nom(), alumne.getGsuiteGivenName())
                || !Objects.equals(nomGSuite.cognoms(), alumne.getGsuiteFamilyName());
        boolean canviUnitat = !bloquejat && !rutaUnitat.equals(alumne.getGsuiteUnitatOrganitzativa());

        if (!canviNom && !canviUnitat && (!forca || bloquejat)) {
            return Resultat.SENSE_CANVIS;
        }

        User usuariGSuite = gSuiteService.updateUser(alumne.getGsuiteEmail(), nomGSuite.nom(), nomGSuite.cognoms(), alumne.getGestibCodi(), rutaUnitat);

        if (usuariGSuite == null) {
            log.error("Error actualitzant nom i unitat organitzativa de l'alumne " + alumne.getGsuiteEmail() + " a " + rutaUnitat);
            return Resultat.ERROR;
        }

        alumne.setGsuiteUnitatOrganitzativa(usuariGSuite.getOrgUnitPath());
        alumne.setGsuiteGivenName(usuariGSuite.getName().getGivenName());
        alumne.setGsuiteFamilyName(usuariGSuite.getName().getFamilyName());
        alumne.setGsuiteFullName(usuariGSuite.getName().getFullName());
        usuariService.save(alumne);

        log.info("Alumne " + alumne.getGsuiteEmail() + " actualitzat a GSuite. UO: " + usuariGSuite.getOrgUnitPath());
        return Resultat.ACTUALITZAT;
    }

    /**
     * Actualitza nom i UO a GSuite d'un conjunt d'alumnes (pantalles de cursos i grups).
     * Si un alumne falla es continua amb la resta i la notificació resumeix el resultat.
     */
    public Notificacio actualitzaAlumnes(Collection<UsuariDto> alumnes, String missatgeInicial) throws InterruptedException {
        int actualitzats = 0;
        List<String> errors = new ArrayList<>();
        List<String> senseCompte = new ArrayList<>();
        List<String> bloquejats = new ArrayList<>();

        for (UsuariDto alumne : alumnes) {
            if (alumne.getActiu() == null || !alumne.getActiu()) {
                continue;
            }
            String nomComplet = alumne.getGestibNom() + " " + alumne.getGestibCognom1() + " " + alumne.getGestibCognom2();
            if (Boolean.TRUE.equals(alumne.getBloquejaGsuiteUnitatOrganitzativa())) {
                bloquejats.add(nomComplet);
            }
            Resultat resultat = this.actualitzaNomIUnitatOrganitzativa(alumne, true);
            if (resultat == Resultat.ACTUALITZAT) {
                actualitzats++;
            } else if (resultat == Resultat.ERROR) {
                errors.add(nomComplet);
            } else if (resultat == Resultat.OMES && (alumne.getGsuiteEmail() == null || alumne.getGsuiteEmail().isEmpty())) {
                senseCompte.add(nomComplet);
            }
        }

        StringBuilder missatge = new StringBuilder(missatgeInicial).append(" Alumnes actualitzats a GSuite: ").append(actualitzats).append(".");
        if (!bloquejats.isEmpty()) {
            missatge.append(" Amb la UO bloquejada (no es mouen): ").append(String.join(", ", bloquejats)).append(".");
        }
        if (!senseCompte.isEmpty()) {
            missatge.append(" Sense compte de GSuite: ").append(String.join(", ", senseCompte)).append(".");
        }
        if (!errors.isEmpty()) {
            missatge.append(" Error actualitzant: ").append(String.join(", ", errors)).append(". Comprovi que la Unitat Organitzativa existeix a la consola de Google.");
        }

        Notificacio notificacio = new Notificacio();
        notificacio.setNotifyMessage(missatge.toString());
        if (!errors.isEmpty()) {
            notificacio.setNotifyType(actualitzats == 0 ? NotificacioTipus.ERROR : NotificacioTipus.WARNING);
        } else if (!senseCompte.isEmpty()) {
            notificacio.setNotifyType(NotificacioTipus.WARNING);
        } else {
            notificacio.setNotifyType(NotificacioTipus.SUCCESS);
        }
        return notificacio;
    }

    private boolean teUnitatOrganitzativaPropia(GrupDto grup) {
        String uo = grup.getGsuiteUnitatOrganitzativa();
        return uo != null && !uo.isBlank() && !uo.trim().equals(this.defaultUOAlumnes);
    }
}

package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.DepartamentDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.model.gestib.Usuari;
import cat.politecnicllevant.core.repository.gestib.DepartamentRepository;
import cat.politecnicllevant.core.repository.gestib.UsuariRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsuariService {
    @Autowired
    private UsuariRepository usuariRepository;

    @Autowired
    private DepartamentRepository departamentRepository;

    @Transactional
    public UsuariDto save(UsuariDto usuari) {
        ModelMapper modelMapper = new ModelMapper();

        // Abans de desar comprovam si ja existeix un usuari amb el mateix
        // correu de GSuite. En aquest cas actualitzam l'usuari existent per
        // evitar l'excepció de clau duplicada i mantenir la unicitat del camp.
        Usuari entity;
        if (usuari.getGsuiteEmail() != null && !usuari.getGsuiteEmail().isEmpty()) {
            Usuari existent = usuariRepository.findUsuariByGsuiteEmail(usuari.getGsuiteEmail());
            if (existent != null && (usuari.getIdusuari() == null || !existent.getIdusuari().equals(usuari.getIdusuari()))) {
                // Mapegem els canvis sobre l'entitat existent per a conservar la resta de camps.
                // Cal assegurar que l'identificador de la instància gestionada no es modifica,
                // ja que Hibernate no permet canviar la clau primària d'un entity que està en
                // el context de persistència. El DTO pot contenir un id diferent (o nul) i,
                // si es copia directament, provocarà l'excepció
                // "identifier of an instance was altered".
                entity = existent;
                Long idOriginal = entity.getIdusuari();
                modelMapper.map(usuari, entity);
                // Restablim l'id original per a evitar que s'alteri l'identificador
                entity.setIdusuari(idOriginal);
            } else {
                entity = modelMapper.map(usuari, Usuari.class);
            }
        } else {
            entity = modelMapper.map(usuari, Usuari.class);
        }

        Usuari usuariSaved = usuariRepository.save(entity);

        return modelMapper.map(usuariSaved,UsuariDto.class);
    }

    @Transactional
    public UsuariDto saveGestib(String codi, String nom, String cognom1, String cognom2, String username, String expedient, String grup, String departament, Boolean esProfessor, Boolean esAlumne, String gestibAlumneUsuari) {
        Usuari u = new Usuari();
        u.setActiu(true);

        /* Gestib */
        u.setGestibCodi(codi);
        u.setGestibNom(nom);
        u.setGestibCognom1(cognom1);
        u.setGestibCognom2(cognom2);
        u.setGestibUsername(username);
        u.setGestibExpedient(expedient);
        u.setGestibGrup(grup);
        u.setGestibProfessor(esProfessor);
        u.setGestibAlumne(esAlumne);
        u.setGestibAlumneUsuari(gestibAlumneUsuari);
        u.setGestibDepartament(departament);

        if(u.getBloquejaGsuiteUnitatOrganitzativa()==null){
            u.setBloquejaGsuiteUnitatOrganitzativa(false);
        }

        Usuari usuariSaved = usuariRepository.save(u);

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(usuariSaved,UsuariDto.class);
    }
    @Transactional
    public UsuariDto saveGestib(UsuariDto u, String codi, String nom, String cognom1, String cognom2, String username, String expedient, String grup, String departament, Boolean esProfessor, Boolean esAlumne, String gestibAlumneUsuari) {
        u.setIdusuari(u.getIdusuari());
        u.setActiu(true);

        /* Gestib */
        u.setGestibCodi(codi);
        u.setGestibNom(nom);
        u.setGestibCognom1(cognom1);
        u.setGestibCognom2(cognom2);
        u.setGestibUsername(username);
        u.setGestibExpedient(expedient);
        if (u.getGestibGrup() == null) {
            u.setGestibGrup(grup);
        } else if (!u.getGestibGrup().equals(grup) && u.getGestibGrup2() == null) {
            u.setGestibGrup2(grup);
        } else if (!u.getGestibGrup().equals(grup) && !u.getGestibGrup2().equals(grup) && u.getGestibGrup3() == null) {
            u.setGestibGrup3(grup);
        }
        u.setGestibProfessor(esProfessor);
        u.setGestibAlumne(esAlumne);
        u.setGestibAlumneUsuari(gestibAlumneUsuari);
        u.setGestibDepartament(departament);

        if(u.getBloquejaGsuiteUnitatOrganitzativa()==null){
            u.setBloquejaGsuiteUnitatOrganitzativa(false);
        }


        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = modelMapper.map(u,Usuari.class);

        Usuari usuariSaved = usuariRepository.save(usuari);

        return modelMapper.map(usuariSaved,UsuariDto.class);
    }
    @Transactional
    public UsuariDto saveGSuite(String email, Boolean esAdministrador, String personalID, Boolean suspes, String unitatOrganitzativa, String givenName, String familyName, String fullName, Boolean actiu) {
        Usuari u = new Usuari();

        u.setActiu(actiu);

        /* GSuite */
        u.setGsuiteEmail(email);
        u.setGsuiteAdministrador(esAdministrador);
        u.setGsuitePersonalID(personalID);
        u.setGsuiteSuspes(suspes);
        u.setGsuiteUnitatOrganitzativa(unitatOrganitzativa);
        u.setGsuiteGivenName(givenName);
        u.setGsuiteFamilyName(familyName);
        u.setGsuiteFullName(fullName);

        if(u.getBloquejaGsuiteUnitatOrganitzativa()==null){
            u.setBloquejaGsuiteUnitatOrganitzativa(false);
        }

        Usuari usuariSaved = usuariRepository.save(u);

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(usuariSaved,UsuariDto.class);
    }

    public UsuariDto findByGestibCodi(String codi) {
        try {
            ModelMapper modelMapper = new ModelMapper();
            Usuari usuari = usuariRepository.findUsuariByGestibCodi(codi);
            if (usuari != null) {
                return modelMapper.map(usuari, UsuariDto.class);
            }
            return null;
        } catch (Exception ex) {
            log.error("Error cercant usuari per codi Gestib: " + codi, ex);
            return null;
        }
    }

    public UsuariDto findByGSuitePersonalID(String codi) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = usuariRepository.findUsuariByGsuitePersonalID(codi);
        if(usuari!=null) {
            return modelMapper.map(usuari, UsuariDto.class);
        }
        return null;
    }

    public UsuariDto findById(Long id) {
        Usuari usuari = usuariRepository.findById(id).orElse(null);
        if(usuari != null) {
            ModelMapper modelMapper = new ModelMapper();
            return modelMapper.map(usuari, UsuariDto.class);
        }
        return null;
    }

    public UsuariDto findByEmail(String email) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = usuariRepository.findUsuariByGsuiteEmail(email);
        if(usuari!=null) {
            return modelMapper.map(usuari, UsuariDto.class);
        }
        return null;
    }

    public UsuariDto findUsuariByGestibExpedient(String expedient) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = usuariRepository.findUsuariByGestibExpedientAndActiuIsTrueAndGestibAlumneIsTrue(expedient);
        if(usuari!=null) {
            return modelMapper.map(usuari, UsuariDto.class);
        }
        return null;
    }

    /**
     * Cerquem primer per codi i si no existeix per email
     */
    public UsuariDto findByGestibCodiOrEmail(String codi, String email) {
        Usuari usuari = null;
        if (codi != null && !codi.isEmpty()) {
            usuari = usuariRepository.findUsuariByGestibCodi(codi);
        }
        if (usuari == null && email != null && !email.isEmpty()) {
            usuari = usuariRepository.findUsuariByGsuiteEmail(email);
        }

        if(usuari!=null) {
            ModelMapper modelMapper = new ModelMapper();
            return modelMapper.map(usuari, UsuariDto.class);
        }
        return null;
    }

    public List<UsuariDto> findByNomCognom1Cognom2(String nom, String cognom1, String cognom2) {
        List<Usuari> usuaris = usuariRepository.findAll();
        ModelMapper modelMapper = new ModelMapper();
        return usuaris.stream()
                .filter(u -> {
                    String gSuiteNom = "";
                    String gSuiteCognoms = "";
                    String gSuiteNomComplet = "";

                    String gestibNom = "";
                    String gestibCognom1 = "";
                    String gestibCognom2 = "";

                    String paramNom = removeAccents(nom.toUpperCase().trim());
                    String paramCognom1 = removeAccents(cognom1.toUpperCase().trim());
                    String paramCognom2 = removeAccents(cognom2.toUpperCase().trim());

                    if (u.getGsuiteGivenName() != null) {
                        gSuiteNom = removeAccents(u.getGsuiteGivenName().toUpperCase().trim());
                    }
                    if (u.getGsuiteFamilyName() != null) {
                        gSuiteCognoms = removeAccents(u.getGsuiteFamilyName().toUpperCase().trim());
                    }
                    if (u.getGsuiteFullName() != null) {
                        gSuiteNomComplet = removeAccents(u.getGsuiteFullName().toUpperCase().trim());
                    }

                    if (u.getGestibNom() != null) {
                        gestibNom = removeAccents(u.getGestibNom().toUpperCase().trim());
                    }
                    if (u.getGestibCognom1() != null) {
                        gestibCognom1 = removeAccents(u.getGestibCognom1().toUpperCase().trim());
                    }
                    if (u.getGestibCognom2() != null) {
                        gestibCognom2 = removeAccents(u.getGestibCognom2().toUpperCase().trim());
                    }

                    boolean nomComplet1 = (gSuiteNom+gSuiteCognoms).trim().equals((paramNom+paramCognom1+paramCognom2).trim());
                    boolean nomComplet2 = (gSuiteNomComplet).trim().equals((paramNom+paramCognom1+paramCognom2).trim());
                    boolean nomComplet3 = (gSuiteNomComplet).trim().equals((paramNom+" "+paramCognom1+" "+paramCognom2).trim());
                    boolean nomComplet4 = (gSuiteNomComplet).trim().equals((paramCognom1+" "+paramCognom2 + " "+paramNom).trim());
                    boolean nomComplet5 = (gSuiteNomComplet).trim().equals((paramCognom1+" "+paramCognom2 + ", "+paramNom).trim());

                    boolean nomComplet6 = (paramNom+paramCognom1+paramCognom2).trim().equals((gestibNom+gestibCognom1+gestibCognom2).trim());

                    return nomComplet1 || nomComplet2 || nomComplet3 || nomComplet4 || nomComplet5 || nomComplet6;
                })
                .map(u -> modelMapper.map(u, UsuariDto.class))
                .collect(Collectors.toList());
    }

    public List<UsuariDto> findAll() {
        return this.findAll(false);
    }

    public List<UsuariDto> findAll(boolean inclouSuspesos) {
        ModelMapper modelMapper = new ModelMapper();
        if(inclouSuspesos){
            return usuariRepository.findAll().stream().map(u -> modelMapper.map(u, UsuariDto.class)).collect(Collectors.toList());
        } else {
            return usuariRepository.findAll().stream().filter(u -> u.getGsuiteSuspes() == null || !u.getGsuiteSuspes()).map(u -> modelMapper.map(u, UsuariDto.class)).collect(Collectors.toList());
        }
    }

    public List<UsuariDto> findProfessors() {
        ModelMapper modelMapper = new ModelMapper();
        return usuariRepository.findAllByGestibProfessorTrue().stream().filter(u->u.getGsuiteSuspes()==null || !u.getGsuiteSuspes()).map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findAlumnes(boolean inclouSuspesos) {
        ModelMapper modelMapper = new ModelMapper();
        if (inclouSuspesos) {
            return usuariRepository.findAllByGestibAlumneTrue().stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
        }
        return usuariRepository.findAllByGestibAlumneTrue().stream().filter(u->u.getGsuiteSuspes()==null || !u.getGsuiteSuspes()).map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findUsuarisSenseCorreu() {
        ModelMapper modelMapper = new ModelMapper();
        return usuariRepository.findAllByGsuiteEmailIsNull().stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findUsuarisGSuiteEliminats() {
        ModelMapper modelMapper = new ModelMapper();
        return usuariRepository.findAllByGsuiteEliminatTrue().stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findUsuarisGSuiteSuspesos() {
        ModelMapper modelMapper = new ModelMapper();
        return usuariRepository.findAllByGsuiteSuspesTrue().stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findUsuarisByGestibGrup(String grupGestib) {
        ModelMapper modelMapper = new ModelMapper();
        return usuariRepository.findAllByGestibGrupOrGestibGrup2OrGestibGrup3(grupGestib, grupGestib, grupGestib).stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findUsuarisActius() {
        ModelMapper modelMapper = new ModelMapper();
        return usuariRepository.findAllByActiu(true).stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findUsuarisNoActius() {
        ModelMapper modelMapper = new ModelMapper();
        return usuariRepository.findAllByActiu(false).stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    public List<UsuariDto> findUsuarisByDepartament(DepartamentDto departament){
        List<Usuari> usuaris = usuariRepository.findAllByGestibDepartament(departament.getGestibIdentificador());

        ModelMapper modelMapper = new ModelMapper();
        return usuaris.stream().map(u->modelMapper.map(u,UsuariDto.class)).collect(Collectors.toList());
    }

    @Transactional
    public void desactivarUsuaris() {
        List<Usuari> usuaris = usuariRepository.findAll();
        for (Usuari usuari : usuaris) {
            usuari.setActiu(false);
            usuariRepository.save(usuari);
        }
    }

    @Transactional
    public void restaurarActius(List<UsuariDto> usuarisOriginals) {
        if (usuarisOriginals == null) {
            return;
        }

        for (UsuariDto usuariOriginal : usuarisOriginals) {
            if (usuariOriginal == null || usuariOriginal.getIdusuari() == null) {
                continue;
            }

            usuariRepository.findById(usuariOriginal.getIdusuari()).ifPresent(usuari -> {
                usuari.setActiu(usuariOriginal.getActiu());
                usuariRepository.save(usuari);
            });
        }
    }
    @Transactional
    public void suspendreUsuari(UsuariDto usuari) {
        usuari.setGsuiteSuspes(true);
        usuari.setActiu(false);

        ModelMapper modelMapper = new ModelMapper();
        Usuari u = modelMapper.map(usuari,Usuari.class);
        usuariRepository.save(u);
    }

    @Transactional
    public void reactivaUsuari(UsuariDto usuari) {
        usuari.setGsuiteSuspes(false);

        ModelMapper modelMapper = new ModelMapper();
        Usuari u = modelMapper.map(usuari,Usuari.class);
        usuariRepository.save(u);
    }

    private String removeAccents(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String accentRemoved = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        accentRemoved = accentRemoved.replaceAll("'", "");
        accentRemoved = accentRemoved.replaceAll("\\s", "");
        return accentRemoved;
    }
}


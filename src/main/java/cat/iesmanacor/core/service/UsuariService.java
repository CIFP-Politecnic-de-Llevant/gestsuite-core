package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.DepartamentDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.model.gestib.Usuari;
import cat.iesmanacor.core.repository.gestib.DepartamentRepository;
import cat.iesmanacor.core.repository.gestib.UsuariRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuariService {
    @Autowired
    private UsuariRepository usuariRepository;

    @Autowired
    private DepartamentRepository departamentRepository;

    @Transactional
    public UsuariDto save(UsuariDto usuari) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari u = modelMapper.map(usuari,Usuari.class);
        Usuari usuariSaved = usuariRepository.save(u);

        return modelMapper.map(usuariSaved,UsuariDto.class);
    }

    @Transactional
    public UsuariDto saveGestib(String codi, String nom, String cognom1, String cognom2, String username, String expedient, String grup, String departament, Boolean esProfessor, Boolean esAlumne) {
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
        u.setGestibDepartament(departament);

        if(u.getBloquejaGsuiteUnitatOrganitzativa()==null){
            u.setBloquejaGsuiteUnitatOrganitzativa(false);
        }

        Usuari usuariSaved = usuariRepository.save(u);

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(usuariSaved,UsuariDto.class);
    }
    @Transactional
    public UsuariDto saveGestib(UsuariDto u, String codi, String nom, String cognom1, String cognom2, String username, String expedient, String grup, String departament, Boolean esProfessor, Boolean esAlumne) {
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
        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = usuariRepository.findUsuariByGestibCodi(codi);
        if(usuari!=null) {
            return modelMapper.map(usuari, UsuariDto.class);
        }
        return null;
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
    public void suspendreUsuari(UsuariDto usuari) {
        usuari.setGsuiteSuspes(true);
        usuari.setActiu(false);

        ModelMapper modelMapper = new ModelMapper();
        Usuari u = modelMapper.map(usuari,Usuari.class);
        usuariRepository.save(u);
    }
}


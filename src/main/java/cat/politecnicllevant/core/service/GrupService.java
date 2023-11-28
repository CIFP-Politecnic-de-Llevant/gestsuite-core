package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.*;
import cat.politecnicllevant.core.model.gestib.Activitat;
import cat.politecnicllevant.core.model.gestib.Grup;
import cat.politecnicllevant.core.model.gestib.Sessio;
import cat.politecnicllevant.core.model.gestib.Submateria;
import cat.politecnicllevant.core.repository.gestib.ActivitatRepository;
import cat.politecnicllevant.core.repository.gestib.GrupRepository;
import cat.politecnicllevant.core.repository.gestib.SessioRepository;
import cat.politecnicllevant.core.repository.gestib.SubmateriaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GrupService {
    @Autowired
    private GrupRepository grupRepository;

    @Autowired
    private SessioRepository sessioRepository;

    @Autowired
    private SubmateriaRepository submateriaRepository;

    @Autowired
    private ActivitatRepository activitatRepository;

    @Transactional
    public GrupDto save (GrupDto g){
        ModelMapper modelMapper = new ModelMapper();
        Grup grup = modelMapper.map(g,Grup.class);
        Grup grupSaved = grupRepository.save(grup);
        return modelMapper.map(grupSaved,GrupDto.class);
    }

    @Transactional
    public GrupDto save(String identificador, String nom, String codiCurs, String tutor1, String tutor2, String tutor3, String unitatOrganitzativa) {
        ModelMapper modelMapper = new ModelMapper();

        Grup g = new Grup();
        g.setGestibNom(nom);
        g.setGestibIdentificador(identificador);
        g.setGestibCurs(codiCurs);
        g.setGestibTutor1(tutor1);
        g.setGestibTutor2(tutor2);
        g.setGestibTutor3(tutor3);
        g.setActiu(true);
        g.setGsuiteUnitatOrganitzativa(unitatOrganitzativa);

        Grup grupSaved = grupRepository.save(g);
        return modelMapper.map(grupSaved,GrupDto.class);
    }

    public GrupDto findById(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        Grup grup = grupRepository.findById(id).orElse(null);
        if(grup!=null) {
            return modelMapper.map(grup, GrupDto.class);
        }
        return null;
    }

    public GrupDto findByGestibIdentificador(String identificador) {
        ModelMapper modelMapper = new ModelMapper();
        Grup grup = grupRepository.findGrupByGestibIdentificador(identificador);
        if(grup!=null) {
            return modelMapper.map(grup, GrupDto.class);
        }
        return null;
    }

    public List<GrupDto> findByGestibNomAndCurs(String nom, String curs) {
        ModelMapper modelMapper = new ModelMapper();
        List<Grup> grups = grupRepository.findAllByGestibNomAndGestibCurs(nom,curs);
        if(grups!=null && !grups.isEmpty()) {
            return grups.stream().map(g->modelMapper.map(g,GrupDto.class)).collect(Collectors.toList());
        } else{
            return null;
        }
    }

    public List<GrupDto> findByGestibCurs(String curs) {
        ModelMapper modelMapper = new ModelMapper();
        List<Grup> grups = grupRepository.findAllByGestibCurs(curs);
        if(grups!=null && !grups.isEmpty()) {
            return grups.stream().map(g->modelMapper.map(g,GrupDto.class)).collect(Collectors.toList());
        } else{
            return null;
        }
    }

    public List<GrupDto> findByTutor(UsuariDto u) {
        ModelMapper modelMapper = new ModelMapper();
        return grupRepository.findAllByGestibTutor1OrGestibTutor2OrGestibTutor3(u.getGestibCodi(), u.getGestibCodi(), u.getGestibCodi()).stream().map(g->modelMapper.map(g,GrupDto.class)).collect(Collectors.toList());
    }

    public List<GrupDto> findAll(){
        ModelMapper modelMapper = new ModelMapper();
        return grupRepository.findAll().stream().map(g->modelMapper.map(g,GrupDto.class)).collect(Collectors.toList());
    }

    public List<GrupDto> findAllGropusFCT(){
        ModelMapper modelMapper = new ModelMapper();
        List<GrupDto> groupsFCT = this.findAll().stream().filter(g->{
            List<SessioDto> sessionsGroup = sessioRepository.findAllByGestibGrup(g.getGestibIdentificador()).stream().map(s->modelMapper.map(s,SessioDto.class)).toList();
            boolean found = false;
            for (SessioDto sessio : sessionsGroup) {
                String codiGestibSubmateria = sessio.getGestibSubmateria();
                if (codiGestibSubmateria != null && !codiGestibSubmateria.isEmpty()) {
                    Submateria submateria = submateriaRepository.findSubmateriaByGestibIdentificador(codiGestibSubmateria);

                    if (submateria != null && submateria.getGestibNom() != null && submateria.getGestibNomCurt() != null &&
                            (
                                    submateria.getGestibNom().contains("Formació en centres de treball") ||
                                            submateria.getGestibNom().contains("FCT") ||
                                            submateria.getGestibNomCurt().contains("Formació en centres de treball") ||
                                            submateria.getGestibNomCurt().contains("FCT")
                            )
                    ) {
                        found=true;
                        break;
                    }
                }
                String codiGestibActivitat = sessio.getGestibActivitat();
                if (codiGestibActivitat != null && !codiGestibActivitat.isEmpty()) {
                    Activitat activitat = activitatRepository.findActivitatByGestibIdentificador(codiGestibActivitat);

                    if (activitat != null && activitat.getGestibNom() != null && activitat.getGestibNomCurt() != null &&
                            (
                                    activitat.getGestibNom().contains("Formació en centres de treball") ||
                                            activitat.getGestibNom().contains("FCT") ||
                                            activitat.getGestibNomCurt().contains("Formació en centres de treball") ||
                                            activitat.getGestibNomCurt().contains("FCT")

                            )
                    ) {
                        found = true;
                        break;
                    }
                }
            }
            return found;
        }).toList();
        return groupsFCT;
    }

    @Transactional
    public void deshabilitarGrups(){
        List<GrupDto> grups = this.findAll();
        for(GrupDto grup: grups){
            grup.setActiu(false);
            this.save(grup);
        }
    }

    @Transactional
    public void deleteAllGrups() {
        grupRepository.deleteAllInBatch();
        grupRepository.deleteAll();
    }
}


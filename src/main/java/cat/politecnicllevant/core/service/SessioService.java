package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.SessioDto;
import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.model.gestib.Activitat;
import cat.politecnicllevant.core.model.gestib.Grup;
import cat.politecnicllevant.core.model.gestib.Sessio;
import cat.politecnicllevant.core.repository.gestib.ActivitatRepository;
import cat.politecnicllevant.core.repository.gestib.GrupRepository;
import cat.politecnicllevant.core.repository.gestib.SessioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessioService {
    @Autowired
    private SessioRepository sessioRepository;

    @Autowired
    private ActivitatRepository activitatRepository;

    @Autowired
    private GrupRepository grupRepository;

    /*
    <SESSIO
        professor="944BA534D00BE45FE040D70A59055935"
        curs="94"
        grup="557870"
        dia="3"
        hora="08:00"
        durada="55"
        aula="5938"
        submateria="2066797"
        activitat=""
        placa="10255"
            />
     */
    @Transactional
    public SessioDto saveSessio(String professor, String alumne, String curs, String grup, String dia, String hora, String durada, String aula, String submateria, String activitat, String placa) {
        Sessio s = new Sessio();
        s.setGestibProfessor(professor);
        s.setGestibAlumne(alumne);
        s.setGestibCurs(curs);
        s.setGestibGrup(grup);
        s.setGestibDia(dia);
        s.setGestibHora(hora);
        s.setGestibDurada(durada);
        s.setGestibAula(aula);
        s.setGestibSubmateria(submateria);
        s.setGestibActivitat(activitat);
        s.setGestibPlaca(placa);

        Sessio sessioSaved = sessioRepository.save(s);
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(sessioSaved,SessioDto.class);
    }

    @Transactional
    public void deleteAllSessions() {
        sessioRepository.deleteAllInBatch();
        sessioRepository.deleteAll();
    }

    public List<SessioDto> findSessionsProfessor(UsuariDto professor) {
        ModelMapper modelMapper = new ModelMapper();
        if (professor.getGestibProfessor() != null && professor.getGestibProfessor()) {
            return sessioRepository.findAllByGestibProfessor(professor.getGestibCodi()).stream().map(s->modelMapper.map(s,SessioDto.class)).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public List<SessioDto> findSessionsAlumne(UsuariDto alumne) {
        ModelMapper modelMapper = new ModelMapper();
        if (alumne.getGestibAlumne() != null && alumne.getGestibAlumne()) {
            return sessioRepository.findAllByGestibAlumne(alumne.getGestibCodi()).stream().map(s->modelMapper.map(s,SessioDto.class)).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public List<SessioDto> findSessionsPares(){
        ModelMapper modelMapper = new ModelMapper();
        List<Activitat> activitatsPares = activitatRepository.findAll().stream().filter(s->s.getGestibNom().toLowerCase().contains("pares")).collect(Collectors.toList());
        List<Sessio> sessions = sessioRepository.findAll();
        List<Sessio> result = new ArrayList<>();

        for(Activitat activitat:activitatsPares){
            for(Sessio sessio: sessions){
                if(sessio.getGestibActivitat()!=null && !sessio.getGestibActivitat().isEmpty() && activitat.getGestibIdentificador().equals(sessio.getGestibActivitat())){
                    result.add(sessio);
                }
            }
        }
        System.out.println("Num sessions"+result.size()+"idactivitat pares"+activitatsPares.size());
        return result.stream().map(s->modelMapper.map(s,SessioDto.class)).collect(Collectors.toList());
    }

    public List<SessioDto> findAll(){
        ModelMapper modelMapper = new ModelMapper();
        List<Sessio> sessions = sessioRepository.findAll();
        return sessions.stream().map(s->modelMapper.map(s,SessioDto.class)).collect(Collectors.toList());
    }

    public List<SessioDto> findByGrup(Long idGrup){
        ModelMapper modelMapper = new ModelMapper();

        Grup grup = grupRepository.findById(idGrup).orElse(null);

        if(grup!=null) {
            List<Sessio> sessions = sessioRepository.findAll().stream().filter(s -> s.getGestibGrup().equals(grup.getGestibIdentificador())).collect(Collectors.toList());
            return sessions.stream().map(s -> modelMapper.map(s, SessioDto.class)).collect(Collectors.toList());
        }
        return null;
    }
}


package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.SubmateriaDto;
import cat.politecnicllevant.core.model.gestib.Submateria;
import cat.politecnicllevant.core.repository.gestib.SubmateriaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubmateriaService {
    @Autowired
    private SubmateriaRepository submateriaRepository;

    @Transactional
    public SubmateriaDto save(String identificador, String nom, String nomCurt, String codiCurs) {
        Submateria s = new Submateria();
        s.setGestibIdentificador(identificador);
        s.setGestibNom(nom);
        s.setGestibNomCurt(nomCurt);
        s.setGestibCurs(codiCurs);

        Submateria submateriaSaved = submateriaRepository.save(s);
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(submateriaSaved, SubmateriaDto.class);
    }

    public SubmateriaDto findByGestibIdentificador(String identificador) {
        ModelMapper modelMapper = new ModelMapper();
        Submateria submateria = submateriaRepository.findSubmateriaByGestibIdentificador(identificador);
        if(submateria!=null) {
            return modelMapper.map(submateria, SubmateriaDto.class);
        }
        return null;
    }

    @Transactional
    public void deleteAllSubmateries() {
        submateriaRepository.deleteAllInBatch();
        submateriaRepository.deleteAll();
    }

    public List<SubmateriaDto> findAll(){
        ModelMapper modelMapper = new ModelMapper();
        return submateriaRepository.findAll().stream().map(s->modelMapper.map(s,SubmateriaDto.class)).collect(Collectors.toList());
    }

}


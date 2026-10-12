package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.ActivitatDto;
import cat.politecnicllevant.core.model.gestib.Activitat;
import cat.politecnicllevant.core.repository.gestib.ActivitatRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivitatService {
    @Autowired
    private ActivitatRepository activitatRepository;

    public ActivitatDto save(String identificador, String nom, String nomCurt) {
        Activitat a = new Activitat();
        a.setGestibIdentificador(identificador);
        a.setGestibNom(nom);
        a.setGestibNomCurt(nomCurt);

        Activitat activitatSaved = activitatRepository.save(a);

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(activitatSaved,ActivitatDto.class);
    }

    public ActivitatDto save(ActivitatDto a) {
        ModelMapper modelMapper = new ModelMapper();
        Activitat activitat = modelMapper.map(a,Activitat.class);
        Activitat activitatSaved =  activitatRepository.save(activitat);
        return modelMapper.map(activitatSaved,ActivitatDto.class);
    }

    public ActivitatDto findByGestibIdentificador(String identificador) {
        ModelMapper modelMapper = new ModelMapper();
        Activitat activitat = activitatRepository.findActivitatByGestibIdentificador(identificador);
        if(activitat!=null) {
            return modelMapper.map(activitat, ActivitatDto.class);
        }
        return null;
    }

    public List<ActivitatDto> findAll() {
        ModelMapper modelMapper = new ModelMapper();
        return activitatRepository.findAll().stream().map(a->modelMapper.map(a,ActivitatDto.class)).collect(Collectors.toList());
    }

    @Transactional
    public void deleteAllActivitats() {
        activitatRepository.deleteAllInBatch();
        activitatRepository.deleteAll();
    }
}


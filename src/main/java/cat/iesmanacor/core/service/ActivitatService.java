package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.ActivitatDto;
import cat.iesmanacor.core.model.gestib.Activitat;
import cat.iesmanacor.core.repository.gestib.ActivitatRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void deleteAllActivitats() {
        activitatRepository.deleteAllInBatch();
        activitatRepository.deleteAll();
    }
}


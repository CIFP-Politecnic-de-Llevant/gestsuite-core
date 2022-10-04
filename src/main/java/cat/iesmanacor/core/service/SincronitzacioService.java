package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.CentreDto;
import cat.iesmanacor.core.model.gestib.Centre;
import cat.iesmanacor.core.repository.gestib.CentreRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SincronitzacioService {
    @Autowired
    private CentreRepository centreRepository;

    @Transactional
    public CentreDto save(String identificador, String nom, String municipi) {
        Centre c = new Centre();
        c.setIdentificador(identificador);
        c.setNom(nom);
        Centre centreSaved = centreRepository.save(c);

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(centreSaved, CentreDto.class);
    }

    public CentreDto findByIdentificador(String identificador) {
        ModelMapper modelMapper = new ModelMapper();
        Centre centre =  centreRepository.findCentreByIdentificador(identificador);
        return modelMapper.map(centre,CentreDto.class);
    }

}


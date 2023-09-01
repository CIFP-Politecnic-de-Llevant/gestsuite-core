package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.CentreDto;
import cat.politecnicllevant.core.model.gestib.Centre;
import cat.politecnicllevant.core.repository.gestib.CentreRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CentreService {
    @Autowired
    private CentreRepository centreRepository;

    @Transactional
    public CentreDto save(CentreDto c) {
        ModelMapper modelMapper = new ModelMapper();

        Centre centre = modelMapper.map(c,Centre.class);
        Centre centreSaved = centreRepository.save(centre);

        return modelMapper.map(centreSaved, CentreDto.class);
    }

    @Transactional
    public CentreDto save(String identificador, String nom) {
        Centre c = new Centre();
        c.setIdentificador(identificador);
        c.setNom(nom);
        Centre centreSaved = centreRepository.save(c);

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(centreSaved, CentreDto.class);
    }

    public CentreDto findByIdentificador(String identificador) {
        ModelMapper modelMapper = new ModelMapper();
        Centre centre = centreRepository.findCentreByIdentificador(identificador);
        return modelMapper.map(centre,CentreDto.class);
    }

    public List<CentreDto> findAll() {
        ModelMapper modelMapper = new ModelMapper();
        return centreRepository.findAll().stream().map(c->modelMapper.map(c,CentreDto.class)).collect(Collectors.toList());
    }

}


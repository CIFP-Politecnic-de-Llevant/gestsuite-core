package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.google.CalendariDto;
import cat.politecnicllevant.core.model.google.Calendari;
import cat.politecnicllevant.core.repository.google.CalendariRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendariService {
    @Autowired
    private CalendariRepository calendariRepository;

    @Transactional
    public CalendariDto save(CalendariDto c) {
        ModelMapper modelMapper = new ModelMapper();
        Calendari calendari = modelMapper.map(c,Calendari.class);
        Calendari calendariSaved = calendariRepository.save(calendari);

        return modelMapper.map(calendariSaved,CalendariDto.class);
    }

    @Transactional
    public CalendariDto save(String email, String nom, String descripcio, String grup) {
        ModelMapper modelMapper = new ModelMapper();

        Calendari c = new Calendari();
        c.setGsuiteEmail(email);
        c.setGsuiteNom(nom);
        c.setGsuiteDescripcio(descripcio);

        Calendari calendariSaved = calendariRepository.save(c);

        return modelMapper.map(calendariSaved,CalendariDto.class);
    }

    public CalendariDto findByEmail(String email) {
        ModelMapper modelMapper = new ModelMapper();
        Calendari calendari = calendariRepository.findCalendariByGsuiteEmail(email);
        return modelMapper.map(calendari,CalendariDto.class);
    }

    public List<CalendariDto> findAll() {
        ModelMapper modelMapper = new ModelMapper();
        return calendariRepository.findAll().stream().map(c->modelMapper.map(c,CalendariDto.class)).collect(Collectors.toList());
    }

}


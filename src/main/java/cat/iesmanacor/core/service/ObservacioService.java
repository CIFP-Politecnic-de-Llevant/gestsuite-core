package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.CentreDto;
import cat.iesmanacor.core.dto.gestib.ObservacioDto;
import cat.iesmanacor.core.dto.gestib.ObservacioTipusDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.model.gestib.Centre;
import cat.iesmanacor.core.model.gestib.Observacio;
import cat.iesmanacor.core.model.gestib.ObservacioTipus;
import cat.iesmanacor.core.model.gestib.Usuari;
import cat.iesmanacor.core.repository.gestib.CentreRepository;
import cat.iesmanacor.core.repository.gestib.ObservacioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ObservacioService {
    @Autowired
    private ObservacioRepository observacioRepository;

    @Transactional
    public ObservacioDto save(ObservacioDto o) {

        ModelMapper modelMapper = new ModelMapper();

        Observacio observacio = modelMapper.map(o,Observacio.class);
        Observacio observacioSaved = observacioRepository.save(observacio);

        return modelMapper.map(observacioSaved,ObservacioDto.class);
    }

    @Transactional
    public ObservacioDto save(String descripcio, ObservacioTipusDto tipusDto, UsuariDto usuariDto) {
        ModelMapper modelMapper = new ModelMapper();

        Usuari usuari = modelMapper.map(usuariDto,Usuari.class);
        ObservacioTipus tipus = modelMapper.map(tipusDto,ObservacioTipus.class);

        Observacio observacio = new Observacio();
        observacio.setDescripcio(descripcio);
        observacio.setUsuari(usuari);
        observacio.setTipus(tipus);
        observacio.setDataCreacio(LocalDateTime.now());

        Observacio observacioSaved = observacioRepository.save(observacio);

        return modelMapper.map(observacioSaved, ObservacioDto.class);
    }

    public List<ObservacioDto> findByUsuari(UsuariDto usuariDto) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = modelMapper.map(usuariDto,Usuari.class);

        return observacioRepository.findAllByUsuari(usuari).stream().map(u->modelMapper.map(u,ObservacioDto.class)).collect(Collectors.toList());
    }
}


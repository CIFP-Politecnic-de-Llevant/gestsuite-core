package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.dto.google.LlistatGoogleDto;
import cat.iesmanacor.core.dto.google.LlistatGoogleTipusDto;
import cat.iesmanacor.core.model.gestib.Usuari;
import cat.iesmanacor.core.model.google.LlistatGoogle;
import cat.iesmanacor.core.model.google.LlistatGoogleTipus;
import cat.iesmanacor.core.repository.gestib.UsuariRepository;
import cat.iesmanacor.core.repository.google.LlistatGoogleRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LlistatGoogleService {
    @Autowired
    private LlistatGoogleRepository llistatGoogleRepository;

    @Autowired
    private UsuariRepository usuariRepository;

    @Transactional
    public LlistatGoogleDto save(LlistatGoogleDto l) {
        ModelMapper modelMapper = new ModelMapper();
        LlistatGoogle llistatGoogle = modelMapper.map(l,LlistatGoogle.class);
        LlistatGoogle llistatGoogleSaved = llistatGoogleRepository.save(llistatGoogle);
        return modelMapper.map(llistatGoogleSaved,LlistatGoogleDto.class);
    }

    @Transactional
    public LlistatGoogleDto save(String identificador, String nom, String url, LlistatGoogleTipusDto llistatGoogleTipusDto, UsuariDto propietariDto) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari propietari = usuariRepository.findById(propietariDto.getIdusuari()).orElse(null);
        LlistatGoogleTipus llistatGoogleTipus = modelMapper.map(llistatGoogleTipusDto,LlistatGoogleTipus.class);

        LocalDateTime ara = LocalDateTime.now();
        LlistatGoogle l = new LlistatGoogle();
        l.setIdentificador(identificador);
        l.setDataCreacio(ara);
        l.setNom(nom);
        l.setPropietari(propietari);
        l.setUrl(url);
        l.setLlistatGoogleTipus(llistatGoogleTipus);

        LlistatGoogle llistatGoogleSaved = llistatGoogleRepository.save(l);

        return modelMapper.map(llistatGoogleSaved,LlistatGoogleDto.class);
    }

    public LlistatGoogleDto findByIdentificadorAndPropietari(String identificador, Usuari propietari) {
        ModelMapper modelMapper = new ModelMapper();
        LlistatGoogle llistatGoogle = llistatGoogleRepository.findLlistatGoogleByIdentificadorAndPropietari(identificador, propietari);
        return modelMapper.map(llistatGoogle,LlistatGoogleDto.class);
    }

    public List<LlistatGoogleDto> findAllByPropietari(Usuari propietari) {
        ModelMapper modelMapper = new ModelMapper();
        return llistatGoogleRepository.findAllByPropietari(propietari).stream().map(l->modelMapper.map(l,LlistatGoogleDto.class)).collect(Collectors.toList());
    }

    public List<LlistatGoogleDto> findAllByTipusAndPropietar(LlistatGoogleTipus llistatGoogleTipus, Usuari propietari) {
        ModelMapper modelMapper = new ModelMapper();
        return llistatGoogleRepository.findAllByLlistatGoogleTipusAndPropietari(llistatGoogleTipus, propietari).stream().map(l->modelMapper.map(l,LlistatGoogleDto.class)).collect(Collectors.toList());
    }

}


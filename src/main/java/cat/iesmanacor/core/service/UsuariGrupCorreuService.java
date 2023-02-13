package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.GrupDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.dto.gestib.UsuariGrupCorreuDto;
import cat.iesmanacor.core.dto.google.GrupCorreuDto;
import cat.iesmanacor.core.model.gestib.Usuari;
import cat.iesmanacor.core.model.gestib.UsuariGrupCorreu;
import cat.iesmanacor.core.model.google.GrupCorreu;
import cat.iesmanacor.core.repository.gestib.UsuariRepository;
import cat.iesmanacor.core.repository.google.GrupCorreuRepository;
import cat.iesmanacor.core.repository.google.UsuariGrupCorreuRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuariGrupCorreuService {
    @Autowired
    private UsuariGrupCorreuRepository usuariGrupCorreuRepository;

    @Autowired
    private UsuariRepository usuariRepository;

    @Autowired
    private GrupCorreuRepository grupCorreuRepository;



    public UsuariGrupCorreuDto findByUsuariAndGrupCorreu(UsuariDto usuariDto, GrupCorreuDto grupCorreuDto) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = usuariRepository.findById(usuariDto.getIdusuari()).orElse(null);
        GrupCorreu grupCorreu = grupCorreuRepository.findById(grupCorreuDto.getIdgrup()).orElse(null);

        UsuariGrupCorreu usuariGrupCorreu = usuariGrupCorreuRepository.findByUsuariAndGrupCorreu(usuari, grupCorreu);
        if(usuariGrupCorreu!=null) {
            return modelMapper.map(usuariGrupCorreu, UsuariGrupCorreuDto.class);
        }
        return null;
    }

}


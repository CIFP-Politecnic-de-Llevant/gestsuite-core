package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.UsuariDto;
import cat.politecnicllevant.core.dto.gestib.UsuariGrupCorreuDto;
import cat.politecnicllevant.core.dto.google.GrupCorreuDto;
import cat.politecnicllevant.core.model.gestib.Usuari;
import cat.politecnicllevant.core.model.gestib.UsuariGrupCorreu;
import cat.politecnicllevant.core.model.google.GrupCorreu;
import cat.politecnicllevant.core.repository.gestib.UsuariRepository;
import cat.politecnicllevant.core.repository.google.GrupCorreuRepository;
import cat.politecnicllevant.core.repository.google.UsuariGrupCorreuRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        UsuariGrupCorreu usuariGrupCorreu = usuariGrupCorreuRepository.findByUsuari_IdusuariAndGrupCorreu_Idgrup(usuari.getIdusuari(), grupCorreu.getIdgrup());
        if(usuariGrupCorreu!=null) {
            return modelMapper.map(usuariGrupCorreu, UsuariGrupCorreuDto.class);
        }
        return null;
    }

}


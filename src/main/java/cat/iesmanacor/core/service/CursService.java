package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.CursDto;
import cat.iesmanacor.core.model.gestib.Curs;
import cat.iesmanacor.core.repository.gestib.CursRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CursService {
    @Autowired
    private CursRepository cursRepository;

    @Transactional
    public CursDto save(CursDto c) {
        ModelMapper modelMapper = new ModelMapper();
        Curs curs = modelMapper.map(c,Curs.class);
        Curs cursSaved = cursRepository.save(curs);
        return modelMapper.map(cursSaved,CursDto.class);
    }

    @Transactional
    public CursDto save(String identificador, String nom, String unitatOrganitzativa) {
        ModelMapper modelMapper = new ModelMapper();

        Curs c = new Curs();
        c.setGestibIdentificador(identificador);
        c.setGestibNom(nom);
        c.setGsuiteUnitatOrganitzativa(unitatOrganitzativa);
        c.setActiu(true);

        Curs cursSaved = cursRepository.save(c);
        return modelMapper.map(cursSaved,CursDto.class);
    }

    public CursDto findByGestibIdentificador(String identificador) {
        ModelMapper modelMapper = new ModelMapper();
        Curs curs = cursRepository.findCursByGestibIdentificador(identificador);
        if(curs!=null) {
            return modelMapper.map(curs, CursDto.class);
        } else{
            return null;
        }
    }

    public List<CursDto> findByGestibNom(String nom) {
        ModelMapper modelMapper = new ModelMapper();
        List<Curs> cursos = cursRepository.findAllByGestibNom(nom);
        if(cursos!=null && !cursos.isEmpty()) {
            return cursos.stream().map(c->modelMapper.map(c,CursDto.class)).collect(Collectors.toList());
        } else{
            return null;
        }
    }

    public CursDto findById(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        //Ha de ser findById i no getById perquè getById és Lazy
        Curs curs = cursRepository.findById(id).orElse(null);
        return modelMapper.map(curs,CursDto.class);
    }

    public List<CursDto> findAll(){
        ModelMapper modelMapper = new ModelMapper();
        return cursRepository.findAll().stream().map(c->modelMapper.map(c,CursDto.class)).collect(Collectors.toList());
    }

    @Transactional
    public void deshabilitarCursos(){
        List<CursDto> cursos = this.findAll();
        for(CursDto curs: cursos){
            curs.setActiu(false);
            this.save(curs);
        }
    }
}


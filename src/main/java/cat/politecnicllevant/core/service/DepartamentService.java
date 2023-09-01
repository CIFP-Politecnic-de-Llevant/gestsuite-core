package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.gestib.DepartamentDto;
import cat.politecnicllevant.core.model.gestib.Departament;
import cat.politecnicllevant.core.repository.gestib.DepartamentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartamentService {
    @Autowired
    private DepartamentRepository departamentRepository;

    @Transactional
    public DepartamentDto save(String identificador, String nom) {
        Departament d = new Departament();
        d.setGestibIdentificador(identificador);
        d.setGestibNom(nom);

        Departament departamentSaved = departamentRepository.save(d);

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(departamentSaved,DepartamentDto.class);
    }

    @Transactional
    public DepartamentDto save(DepartamentDto d) {
        ModelMapper modelMapper = new ModelMapper();
        Departament departament = modelMapper.map(d,Departament.class);
        Departament departamentSaved = departamentRepository.save(departament);
        return modelMapper.map(departamentSaved,DepartamentDto.class);
    }

    public DepartamentDto findByGestibIdentificador(String identificador) {
        ModelMapper modelMapper = new ModelMapper();
        Departament departament = departamentRepository.findDepartamentByGestibIdentificador(identificador);
        if(departament!=null) {
            return modelMapper.map(departament, DepartamentDto.class);
        }
        return null;
    }

    public DepartamentDto findById(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        //Ha de ser findById i no getById perquè getById és Lazy
        Departament departament = departamentRepository.findById(id).orElse(null);
        return modelMapper.map(departament,DepartamentDto.class);
    }

    public List<DepartamentDto> findAll(){
        ModelMapper modelMapper = new ModelMapper();
        return departamentRepository.findAll().stream().map(d->modelMapper.map(d,DepartamentDto.class)).collect(Collectors.toList());
    }

}


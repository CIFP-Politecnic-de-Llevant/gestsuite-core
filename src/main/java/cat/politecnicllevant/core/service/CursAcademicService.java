package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.controller.CursController;
import cat.politecnicllevant.core.dto.gestib.CursAcademicDto;
import cat.politecnicllevant.core.model.gestib.CursAcademic;
import cat.politecnicllevant.core.repository.gestib.CursAcademicRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CursAcademicService {
    @Autowired
    private CursAcademicRepository cursAcademicRepository;
    @Autowired
    private CursController cursController;

    @Transactional
    public CursAcademicDto save(CursAcademicDto c) {
        ModelMapper modelMapper = new ModelMapper();
        CursAcademic cursAcademic = modelMapper.map(c,CursAcademic.class);

        if(Boolean.TRUE.equals(cursAcademic.getActual())) {
            cursAcademicRepository.findAll().forEach(existingCursAcademic -> {
                existingCursAcademic.setActual(false);
                cursAcademicRepository.save(existingCursAcademic);
            });
        }

        CursAcademic cursAcademicSaved = cursAcademicRepository.save(cursAcademic);
        return modelMapper.map(cursAcademicSaved,CursAcademicDto.class);
    }

    @Transactional
    public CursAcademicDto save(Long id, String nom, Boolean isActual) {
        ModelMapper modelMapper = new ModelMapper();

        CursAcademic cursAcademic = cursAcademicRepository.findById(id).orElse(null);

        if(cursAcademic==null){
            cursAcademic = new CursAcademic();
        }

        if(Boolean.TRUE.equals(isActual)) {
            cursAcademicRepository.findAll().forEach(existingCursAcademic -> {
                existingCursAcademic.setActual(false);
                cursAcademicRepository.save(existingCursAcademic);
            });
        }

        cursAcademic.setActual(isActual);
        cursAcademic.setNom(nom);

        CursAcademic cursAcademicSaved = cursAcademicRepository.save(cursAcademic);
        return modelMapper.map(cursAcademicSaved,CursAcademicDto.class);
    }

    public CursAcademicDto findById(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        //Ha de ser findById i no getById perquè getById és Lazy
        CursAcademic cursAcademic = cursAcademicRepository.findById(id).orElse(null);
        return modelMapper.map(cursAcademic,CursAcademicDto.class);
    }

    public List<CursAcademicDto> findAll(){
        ModelMapper modelMapper = new ModelMapper();
        return cursAcademicRepository.findAll().stream()
                .sorted(Comparator.comparing(CursAcademic::getNom).reversed())
                .map(c->modelMapper.map(c,CursAcademicDto.class))
                .collect(Collectors.toList());
    }

    public CursAcademicDto findActual(){
        ModelMapper modelMapper = new ModelMapper();
        CursAcademic cursAcademic = cursAcademicRepository.findAll().stream().filter(CursAcademic::getActual).findFirst().orElse(null);
        if(cursAcademic == null) {
            return null;
        }
        return modelMapper.map(cursAcademic,CursAcademicDto.class);
    }

    @Transactional
    public CursAcademicDto create(CursAcademicDto c) {
        ModelMapper modelMapper = new ModelMapper();

        cursAcademicRepository.findByNom(c.getNom()).ifPresent(existing -> {
            throw new IllegalArgumentException("Ja existeix un curs acadèmic amb aquest nom");
        });

        if(Boolean.TRUE.equals(c.getActual())) {
            cursAcademicRepository.findAll().forEach(existingCursAcademic -> {
                existingCursAcademic.setActual(false);
                cursAcademicRepository.save(existingCursAcademic);
            });
        }

        CursAcademic cursAcademic = new CursAcademic();
        cursAcademic.setNom(c.getNom());
        cursAcademic.setActual(Boolean.TRUE.equals(c.getActual()));

        CursAcademic saved = cursAcademicRepository.save(cursAcademic);
        return modelMapper.map(saved, CursAcademicDto.class);
    }
}

package cat.iesmanacor.core.service;

import cat.iesmanacor.core.dto.gestib.DepartamentDto;
import cat.iesmanacor.core.dto.gestib.GrupDto;
import cat.iesmanacor.core.dto.gestib.UsuariDto;
import cat.iesmanacor.core.dto.gestib.UsuariGrupCorreuDto;
import cat.iesmanacor.core.dto.google.GrupCorreuDto;
import cat.iesmanacor.core.dto.google.GrupCorreuTipusDto;
import cat.iesmanacor.core.model.gestib.Grup;
import cat.iesmanacor.core.model.gestib.Usuari;
import cat.iesmanacor.core.model.gestib.UsuariGrupCorreu;
import cat.iesmanacor.core.model.google.GrupCorreu;
import cat.iesmanacor.core.model.google.GrupCorreuTipus;
import cat.iesmanacor.core.repository.gestib.UsuariRepository;
import cat.iesmanacor.core.repository.google.GrupCorreuRepository;
import cat.iesmanacor.core.repository.google.UsuariGrupCorreuRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GrupCorreuService {
    @Autowired
    private GrupCorreuRepository grupCorreuRepository;

    @Autowired
    private UsuariRepository usuariRepository;

    @Autowired
    private UsuariGrupCorreuRepository usuariGrupCorreuRepository;

    @Transactional
    public GrupCorreuDto save(GrupCorreuDto gc) {
        ModelMapper modelMapper = new ModelMapper();
        GrupCorreu grupCorreu = modelMapper.map(gc, GrupCorreu.class);
        GrupCorreu grupCorreuSaved = grupCorreuRepository.save(grupCorreu);
        return modelMapper.map(grupCorreuSaved,GrupCorreuDto.class);
    }

    @Transactional
    public GrupCorreuDto save(String gestibGrup, String nom, String email, String descripcio, GrupCorreuTipusDto grupCorreuTipusDto) {
        ModelMapper modelMapper = new ModelMapper();
        GrupCorreuTipus grupCorreuTipus = modelMapper.map(grupCorreuTipusDto, GrupCorreuTipus.class);

        GrupCorreu gc = new GrupCorreu();
        gc.setGestibGrup(gestibGrup);
        gc.setGsuiteNom(nom);
        gc.setGsuiteEmail(email);
        gc.setGsuiteDescripcio(descripcio);
        gc.setGrupCorreuTipus(grupCorreuTipus);

        GrupCorreu grupCorreuSaved = grupCorreuRepository.save(gc);
        return modelMapper.map(grupCorreuSaved,GrupCorreuDto.class);
    }

    public List<GrupCorreuDto> findAll() {
        ModelMapper modelMapper = new ModelMapper();
        return grupCorreuRepository.findAll().stream().map(gc->modelMapper.map(gc,GrupCorreuDto.class)).collect(Collectors.toList());
    }

    public GrupCorreuDto findById(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        GrupCorreu grupCorreu = grupCorreuRepository.findById(id).get();
        //return grupCorreuRepository.getById(id);
        return modelMapper.map(grupCorreu,GrupCorreuDto.class);
    }

    public GrupCorreuDto findByEmail(String email) {
        ModelMapper modelMapper = new ModelMapper();
        GrupCorreu grupCorreu = grupCorreuRepository.findGrupCorreuByGsuiteEmail(email);
        if(grupCorreu!=null) {
            return modelMapper.map(grupCorreu, GrupCorreuDto.class);
        }
        return null;
    }

    public List<GrupCorreuDto> findByCodiGrupGestib(String codiGrupGestib) {
        ModelMapper modelMapper = new ModelMapper();
        return grupCorreuRepository
                .findAll()
                .stream()
                .filter(gc -> gc.getGrups().stream().anyMatch(g -> g.getGestibIdentificador().equals(codiGrupGestib)))
                .map(gc->modelMapper.map(gc,GrupCorreuDto.class))
                .collect(Collectors.toList());
    }

    public List<GrupCorreuDto> findByUsuari(UsuariDto usuari) {
        ModelMapper modelMapper = new ModelMapper();
        return grupCorreuRepository
                .findAll()
                .stream()
                .filter(gc -> gc.getUsuarisGrupsCorreu().stream().anyMatch(u -> u.getUsuari().getIdusuari().equals(usuari.getIdusuari())))
                .map(gc->modelMapper.map(gc,GrupCorreuDto.class))
                .collect(Collectors.toList());
    }

    public List<GrupCorreuDto> findByDepartament(DepartamentDto departament){
        ModelMapper modelMapper = new ModelMapper();
        return grupCorreuRepository
                .findAll()
                .stream()
                .filter(gc -> gc.getGrupCorreuTipus().equals(GrupCorreuTipus.DEPARTAMENT) && gc.getDepartaments().stream().anyMatch(d -> d.getIddepartament().equals(departament.getIddepartament())))
                .map(gc->modelMapper.map(gc,GrupCorreuDto.class))
                .collect(Collectors.toList());
    }



   @Transactional
    public UsuariGrupCorreuDto insertUsuari(GrupCorreuDto grupCorreuDto, UsuariDto usuariDto, boolean bloquejat) {
        ModelMapper modelMapper = new ModelMapper();

       Usuari usuari = usuariRepository.findById(usuariDto.getIdusuari()).orElse(null);
       GrupCorreu grupCorreu = grupCorreuRepository.findById(grupCorreuDto.getIdgrup()).orElse(null);

       UsuariGrupCorreu usuariGrupCorreu = usuariGrupCorreuRepository.findByUsuari_IdusuariAndGrupCorreu_Idgrup(usuari.getIdusuari(),grupCorreu.getIdgrup());

       if(usuariGrupCorreu==null){
           usuariGrupCorreu = new UsuariGrupCorreu();
           usuariGrupCorreu.setGrupCorreu(grupCorreu);
           usuariGrupCorreu.setUsuari(usuari);
           usuariGrupCorreu.setBloquejat(bloquejat);
       } else {
           usuariGrupCorreu.setBloquejat(bloquejat);
       }

       UsuariGrupCorreu usuariGrupCorreuSaved = usuariGrupCorreuRepository.save(usuariGrupCorreu);
       return modelMapper.map(usuariGrupCorreuSaved,UsuariGrupCorreuDto.class);
    }

    @Transactional
    public void esborrarUsuari(GrupCorreuDto grupCorreDto, UsuariDto usuariDto) {
        ModelMapper modelMapper = new ModelMapper();
        Usuari usuari = modelMapper.map(usuariDto,Usuari.class);

        List<UsuariGrupCorreu> usuarisGrupCorreus = grupCorreuRepository.findById(grupCorreDto.getIdgrup()).get().getUsuarisGrupsCorreu()
                .stream().filter(ug->ug.getUsuari().getIdusuari().equals(usuari.getIdusuari()) && ug.getGrupCorreu().getIdgrup().equals(grupCorreDto.getIdgrup()))
                .collect(Collectors.toList());
        usuariGrupCorreuRepository.deleteAll(usuarisGrupCorreus);
    }

    @Transactional
    public List<UsuariGrupCorreuDto> esborrarUsuarisNoBloquejatsGrupCorreu(GrupCorreuDto grupCorreuDto) {
        ModelMapper modelMapper = new ModelMapper();

        List<UsuariGrupCorreu> usuarisGrupCorreus = grupCorreuRepository
                .findById(grupCorreuDto.getIdgrup())
                .get().getUsuarisGrupsCorreu()
                .stream().filter(ug->ug.getGrupCorreu().getIdgrup().equals(grupCorreuDto.getIdgrup()))
                .collect(Collectors.toList());

        //Esborrem els no bloquejats
        usuariGrupCorreuRepository.deleteAll(usuarisGrupCorreus.stream().filter(ug->!ug.getBloquejat()).collect(Collectors.toList()));

        //Retornem els bloquejats
        List<UsuariGrupCorreu> usuariGrupCorreusBloquejats = usuarisGrupCorreus.stream().filter(ug->ug.getBloquejat()).collect(Collectors.toList());

        return usuariGrupCorreusBloquejats.stream()
                .map(gc->modelMapper.map(gc,UsuariGrupCorreuDto.class))
                .collect(Collectors.toList());
    }



    @Transactional
    public void insertGrup(GrupCorreuDto grupCorreu, GrupDto grupDto) {
        ModelMapper modelMapper = new ModelMapper();
        Grup grup = modelMapper.map(grupDto,Grup.class);
        grupCorreuRepository.findById(grupCorreu.getIdgrup()).get().getGrups().add(grup);
    }

    @Transactional
    public void esborrarGrupsGrupCorreu(GrupCorreuDto grupCorreuDto) {
        Set<Grup> grups = new HashSet<>(grupCorreuRepository.findById(grupCorreuDto.getIdgrup()).get().getGrups());
        for (Grup grup : grups) {
            grupCorreuRepository.findById(grupCorreuDto.getIdgrup()).get().getGrups().remove(grup);
        }
    }


    @Transactional
    public void insertGrupCorreu(GrupCorreuDto grupCorreu, GrupCorreuDto membreGrupCorreuDto) {
        ModelMapper modelMapper = new ModelMapper();
        GrupCorreu membreGrupCorreu = modelMapper.map(membreGrupCorreuDto,GrupCorreu.class);
        grupCorreuRepository.findById(grupCorreu.getIdgrup()).get().getGrupCorreus().add(membreGrupCorreu);
    }


    @Transactional
    public void esborrarGrupsCorreuGrupCorreu(GrupCorreuDto grupCorreu) {
        Set<GrupCorreu> grupsCorreu = new HashSet<>(grupCorreuRepository.findById(grupCorreu.getIdgrup()).get().getGrupCorreus());
        for (GrupCorreu grupsCorreusMembers : grupsCorreu) {
            grupCorreuRepository.findById(grupCorreu.getIdgrup()).get().getGrupCorreus().remove(grupsCorreusMembers);
        }
    }



    @Transactional
    public void esborrarGrup(GrupCorreuDto grupCorreuDto){
        ModelMapper modelMapper = new ModelMapper();
        GrupCorreu grupCorreu = modelMapper.map(grupCorreuDto,GrupCorreu.class);
        grupCorreuRepository.delete(grupCorreu);
    }
}


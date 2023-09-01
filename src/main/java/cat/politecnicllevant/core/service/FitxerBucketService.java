package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.google.FitxerBucketDto;
import cat.politecnicllevant.core.model.google.FitxerBucket;
import cat.politecnicllevant.core.repository.google.FitxerBucketRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FitxerBucketService {
    @Autowired
    private FitxerBucketRepository fitxerBucketRepository;

    public FitxerBucketDto findById(Long id){
        ModelMapper modelMapper = new ModelMapper();
        //Ha de ser findById i no getById perquè getById és Lazy
        FitxerBucket fitxerBucket = fitxerBucketRepository.findById(id).orElse(null);

        if(fitxerBucket!=null) {
            return modelMapper.map(fitxerBucket, FitxerBucketDto.class);
        }
        return null;
    }

    @Transactional
    public FitxerBucketDto save(FitxerBucketDto fitxerBucketDto) {
        ModelMapper modelMapper = new ModelMapper();
        FitxerBucket fitxerBucket = modelMapper.map(fitxerBucketDto,FitxerBucket.class);
        FitxerBucket fitxerBucketSaved = fitxerBucketRepository.save(fitxerBucket);
        return modelMapper.map(fitxerBucketSaved,FitxerBucketDto.class);
    }

    @Transactional
    public void esborrar(FitxerBucketDto fitxerBucketDto) {
        ModelMapper modelMapper = new ModelMapper();
        FitxerBucket fitxerBucket = modelMapper.map(fitxerBucketDto,FitxerBucket.class);
        fitxerBucketRepository.delete(fitxerBucket);
    }


}


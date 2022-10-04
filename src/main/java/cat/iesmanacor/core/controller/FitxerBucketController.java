package cat.iesmanacor.core.controller;

import cat.iesmanacor.core.dto.google.FitxerBucketDto;
import cat.iesmanacor.core.service.FitxerBucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class FitxerBucketController {


    @Autowired
    private FitxerBucketService fitxerBucketService;


    @GetMapping("/fitxerbucket/{id}")
    public ResponseEntity<FitxerBucketDto> getFitxerBucketById(@PathVariable("id") Long idfitxerBucket) {
        FitxerBucketDto fitxerBucketDto = fitxerBucketService.findById(idfitxerBucket);
        return new ResponseEntity<>(fitxerBucketDto, HttpStatus.OK);
    }

    @PostMapping("/fitxerbucket/save")
    public ResponseEntity<FitxerBucketDto> save(@RequestBody FitxerBucketDto fitxerBucket) throws IOException {
        FitxerBucketDto fitxerBucketSaved = fitxerBucketService.save(fitxerBucket);
        return new ResponseEntity<>(fitxerBucketSaved, HttpStatus.OK);
    }

    @PostMapping("/fitxerbucket/delete")
    public void delete(@RequestBody FitxerBucketDto fitxerBucket) {
        fitxerBucketService.esborrar(fitxerBucket);
    }

}

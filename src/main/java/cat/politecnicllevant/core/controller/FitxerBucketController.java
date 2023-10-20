package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.google.FitxerBucketDto;
import cat.politecnicllevant.core.service.FitxerBucketService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Random;

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

    @PostMapping("/public/fitxerbucket/uploadlocal")
    public ResponseEntity<String> handleFileUpload(@RequestPart(value = "file") final MultipartFile uploadfile) throws IOException {
        String path = saveUploadedFiles(uploadfile);
        return new ResponseEntity<>(path, HttpStatus.OK);
    }

    private String saveUploadedFiles(final MultipartFile file) throws IOException {

        Date ara = new Date();

        Random random = new Random();
        //Generate numbers between 0 and 1.000.000.000
        int randomValue = random.nextInt(1000000001);

        String absolutePath = StringUtils.stripAccents("/tmp/" + ara.getTime() + "_" +randomValue+"_"+ file.getOriginalFilename());
        absolutePath = absolutePath.replaceAll("\\s+","");

        final byte[] bytes = file.getBytes();
        final Path path = Paths.get(absolutePath);
        Files.write(path, bytes);

        System.out.println(absolutePath);
        return absolutePath;
    }

}

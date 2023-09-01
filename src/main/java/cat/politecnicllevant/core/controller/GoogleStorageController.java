package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.google.FitxerBucketDto;
import cat.politecnicllevant.core.service.GoogleStorageService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
public class GoogleStorageController {

    @Autowired
    private GoogleStorageService googleStorageService;

    @Autowired
    private Gson gson;

    @PostMapping(value = "/googlestorage/generate-signed-url")
    public ResponseEntity<String> generateSignedURL(@RequestBody String json) throws IOException {
        JsonObject jsonFitxerBucket = gson.fromJson(json, JsonObject.class);

        FitxerBucketDto fitxerBucket = new FitxerBucketDto();
        fitxerBucket.setIdfitxer(jsonFitxerBucket.get("idfitxer").getAsLong());
        fitxerBucket.setNom(jsonFitxerBucket.get("nom").getAsString());
        fitxerBucket.setPath(jsonFitxerBucket.get("path").getAsString());
        fitxerBucket.setBucket(jsonFitxerBucket.get("bucket").getAsString());

        String url = googleStorageService.generateV4GetObjectSignedUrl(fitxerBucket);
        return new ResponseEntity<>(url, HttpStatus.OK);
    }


    @PostMapping("/googlestorage/uploadobject")
    public ResponseEntity<FitxerBucketDto> uploadObject(@RequestParam("objectName") String objectName, @RequestParam("filePath") String filePath, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException {
        FitxerBucketDto fitxerBucketDto = googleStorageService.uploadObject(objectName, filePath, bucket);
        return new ResponseEntity<>(fitxerBucketDto, HttpStatus.OK);
    }

    @PostMapping("/googlestorage/delete")
    public void deleteObject(@RequestParam("objectName") String objectName, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException {
        googleStorageService.deleteObject(objectName,bucket);
    }


}

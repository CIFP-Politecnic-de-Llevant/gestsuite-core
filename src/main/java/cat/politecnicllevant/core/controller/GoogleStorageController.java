package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.google.FitxerBucketDto;
import cat.politecnicllevant.core.service.GoogleStorageService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Random;

@RestController
public class GoogleStorageController {

    @Autowired
    private GoogleStorageService googleStorageService;

    @Autowired
    private Gson gson;

    @PostMapping(value = "/googlestorage/generate-signed-url")
    public ResponseEntity<String> generateSignedURL(@RequestBody String json, @RequestHeader(value = "User-Agent") String ua) throws IOException {
        JsonObject data = gson.fromJson(json, JsonObject.class);

        boolean download = true;
        if (data.get("download") != null) {
            download = data.get("download").getAsBoolean();
        }

        JsonObject jsonFitxerBucket = data.getAsJsonObject("fitxerBucket");

        FitxerBucketDto fitxerBucket = new FitxerBucketDto();
        fitxerBucket.setIdfitxer(jsonFitxerBucket.get("idfitxer").getAsLong());
        fitxerBucket.setNom(jsonFitxerBucket.get("nom").getAsString());
        fitxerBucket.setPath(jsonFitxerBucket.get("path").getAsString());
        fitxerBucket.setBucket(jsonFitxerBucket.get("bucket").getAsString());

        String url = googleStorageService.generateV4GetObjectSignedUrl(fitxerBucket, download, ua);
        return new ResponseEntity<>(url, HttpStatus.OK);
    }


    @PostMapping("/googlestorage/uploadobject")
    public ResponseEntity<FitxerBucketDto> uploadObject(@RequestParam("objectName") String objectName, @RequestParam("filePath") String filePath, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException{
        FitxerBucketDto fitxerBucketDto = googleStorageService.uploadObject(objectName, filePath, bucket);
        return new ResponseEntity<>(fitxerBucketDto, HttpStatus.OK);
    }

    @PostMapping("/googlestorage/delete")
    public void deleteObject(@RequestParam("objectName") String objectName, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException {
        googleStorageService.deleteObject(objectName,bucket);
    }


}

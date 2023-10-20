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
    public ResponseEntity<FitxerBucketDto> uploadObject(@RequestParam("objectName") String objectName, @RequestParam("filePath") String filePath, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException{
        FitxerBucketDto fitxerBucketDto = googleStorageService.uploadObject(objectName, filePath, bucket);
        return new ResponseEntity<>(fitxerBucketDto, HttpStatus.OK);
    }

    @PostMapping(value="/googlestorage/uploadobjectfile", consumes = "multipart/form-data")
    public ResponseEntity<FitxerBucketDto> uploadObjectFile(@RequestParam("objectName") String objectName, @RequestParam("bucket") String bucket, @RequestPart(value = "file") final File uploadfile) throws IOException, GeneralSecurityException {
        InputStream is = new FileInputStream(uploadfile);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] readBuf = new byte[4096];
        while (is.available() > 0) {
            int bytesRead = is.read(readBuf);
            os.write(readBuf, 0, bytesRead);
        }

        // Passam l'arxiu a dins una carpeta
        String fileName = "/tmp/"+uploadfile.getName();

        OutputStream outputStream = new FileOutputStream(fileName);
        os.writeTo(outputStream);

        File f = new File(fileName);

        FitxerBucketDto fitxerBucketDto = googleStorageService.uploadObject(objectName, f.getAbsolutePath(), bucket);
        return new ResponseEntity<>(fitxerBucketDto, HttpStatus.OK);
    }

    @PostMapping("/googlestorage/delete")
    public void deleteObject(@RequestParam("objectName") String objectName, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException {
        googleStorageService.deleteObject(objectName,bucket);
    }


}

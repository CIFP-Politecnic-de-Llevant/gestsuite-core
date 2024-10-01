package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.google.FitxerBucketDto;
import cat.politecnicllevant.core.service.GoogleStorageService;
import cat.politecnicllevant.core.service.pdfbox.PdfService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bouncycastle.cms.CMSException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.*;

@RestController
public class GoogleStorageController {
    private final GoogleStorageService googleStorageService;
    private final Gson gson;
    private final PdfService pdfService;

    public GoogleStorageController(
            GoogleStorageService googleStorageService,
            Gson gson,
            PdfService pdfService) {
        this.googleStorageService = googleStorageService;
        this.gson = gson;
        this.pdfService = pdfService;
    }

    @PostMapping(value = "/googlestorage/generate-signed-url")
    public ResponseEntity<String> generateSignedURL(@RequestBody String json, @RequestHeader(value = "User-Agent") String ua) throws IOException {
        System.out.println("GOOGLE STORAGE SIGNED URL: " + json);
        JsonObject data = gson.fromJson(json, JsonObject.class);

        boolean download = true;
        if (data.get("download") != null) {
            download = data.get("download").getAsBoolean();
        }

        JsonObject jsonFitxerBucket = data.getAsJsonObject("fitxerBucket");

        if(jsonFitxerBucket!=null && jsonFitxerBucket.get("idfitxer") != null && jsonFitxerBucket.get("nom") != null && jsonFitxerBucket.get("path") != null && jsonFitxerBucket.get("bucket") != null) {
            FitxerBucketDto fitxerBucket = new FitxerBucketDto();
            fitxerBucket.setIdfitxer(jsonFitxerBucket.get("idfitxer").getAsLong());
            fitxerBucket.setNom(jsonFitxerBucket.get("nom").getAsString());
            fitxerBucket.setPath(jsonFitxerBucket.get("path").getAsString());
            fitxerBucket.setBucket(jsonFitxerBucket.get("bucket").getAsString());

            String url = googleStorageService.generateV4GetObjectSignedUrl(fitxerBucket, download, ua);
            return new ResponseEntity<>(url, HttpStatus.OK);
        } else if(data.get("idfitxer") != null && data.get("nom") != null && data.get("path") != null && data.get("bucket") != null) {
            FitxerBucketDto fitxerBucket = new FitxerBucketDto();
            fitxerBucket.setIdfitxer(data.get("idfitxer").getAsLong());
            fitxerBucket.setNom(data.get("nom").getAsString());
            fitxerBucket.setPath(data.get("path").getAsString());
            fitxerBucket.setBucket(data.get("bucket").getAsString());

            String url = googleStorageService.generateV4GetObjectSignedUrl(fitxerBucket, download, ua);
            return new ResponseEntity<>(url, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/googlestorage/uploadobject")
    public ResponseEntity<FitxerBucketDto> uploadObject(@RequestParam("objectName") String objectName, @RequestParam("filePath") String filePath, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException{
        FitxerBucketDto fitxerBucketDto = googleStorageService.uploadObject(objectName, filePath, bucket);
        return new ResponseEntity<>(fitxerBucketDto, HttpStatus.OK);
    }

    @PostMapping("/googlestorage/signatures")
    public ResponseEntity<Set<String>> getSignatures(@RequestBody String json) throws IOException, CMSException {
        JsonObject data = gson.fromJson(json, JsonObject.class);
        String bucket = data.get("bucket").getAsString();
        String file = data.get("nom").getAsString().split("/")[2];
        String destPath = "/tmp/" + bucket + LocalDate.now() + "-" + file;
        String path = data.get("path").getAsString();

        googleStorageService.downloadObject(bucket, path, destPath);

        File document = new File(destPath);
        PDDocument pdf = Loader.loadPDF(document);
        Set<String> names = pdfService.getSignatureNames(pdf);
        pdf.close();

        return new ResponseEntity<>(names, HttpStatus.OK);
    }

    @PostMapping("/googlestorage/delete")
    public void deleteObject(@RequestParam("objectName") String objectName, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException {
        googleStorageService.deleteObject(objectName,bucket);
    }


}

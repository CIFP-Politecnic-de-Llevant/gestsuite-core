package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.dto.google.FitxerBucketDto;
import cat.politecnicllevant.core.service.GoogleStorageService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.PdfSignature;
import com.itextpdf.signatures.SignatureUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.pkcs.EncryptedData;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.util.Store;
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
import java.util.*;

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

    @PostMapping("/googlestorage/signatures")
    public ResponseEntity<List<String>> getSignatures(@RequestBody String json) throws IOException, CMSException {
        JsonObject data = gson.fromJson(json, JsonObject.class);
        String bucket = data.get("bucket").getAsString();
        String file = data.get("nom").getAsString().split("/")[2];
        String destPath = bucket + file;
        String path = data.get("path").getAsString();
        PDDocument pdf = googleStorageService.downloadObject(bucket, path, destPath);

        List<String> names = new ArrayList<>();
        for (PDSignature signature : pdf.getSignatureDictionaries()) {

            COSDictionary sigDict = signature.getCOSObject();
            for (COSName key : sigDict.keySet()) {
                if (key.getName().equals("Contents")) {
                    COSString cos = (COSString) sigDict.getDictionaryObject(key);
                    CMSSignedData signedData = new CMSSignedData(cos.getBytes());
                    List<SignerInformation> signers = signedData.getSignerInfos().getSigners().stream().toList();

                    Store<X509CertificateHolder> certs = signedData.getCertificates();
                    for (SignerInformation signer : signers) {
                        Collection<X509CertificateHolder> certCollection = certs.getMatches(signer.getSID());
                        for (X509CertificateHolder certHolder : certCollection) {
                            List<String> signerInfo = Arrays.stream(
                                    certHolder.getSubject().toString().split(","))
                                    .filter(s -> s.contains("GIVENNAME") || s.contains("SURNAME"))
                                    .toList();
                            String fullName = signerInfo.get(0)
                                    .substring(signerInfo.get(0).indexOf("=") + 1).concat(" "+ signerInfo.get(1)
                                                    .substring(signerInfo.get(1).indexOf("=") + 1));

                            names.add(fullName);
                        }
                    }
                }
            }
        }
        pdf.close();

        return new ResponseEntity<>(names, HttpStatus.OK);
    }

    @PostMapping("/googlestorage/delete")
    public void deleteObject(@RequestParam("objectName") String objectName, @RequestParam("bucket") String bucket) throws IOException, GeneralSecurityException {
        googleStorageService.deleteObject(objectName,bucket);
    }


}

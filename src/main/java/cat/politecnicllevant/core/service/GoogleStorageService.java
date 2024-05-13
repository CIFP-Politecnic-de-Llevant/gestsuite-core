package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.dto.google.FitxerBucketDto;
import com.google.api.services.storage.StorageScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


@Service
public class GoogleStorageService {
    @Value("${gc.projectid}")
    private String projectId;
    @Value("${gc.keyfile}")
    private String keyFile;
    @Value("${gc.adminUser}")
    private String adminUser;

    public FitxerBucketDto uploadObject(String objectName, String filePath, String bucketName) throws IOException, GeneralSecurityException {
        String[] scopes = {StorageScopes.DEVSTORAGE_READ_WRITE};
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        //Per arxius petits podem fer simplement un storage.create
        //storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));

        //Per arxius grans hem de pujar-ho amb "chunks"
        try (WriteChannel writer = storage.writer(blobInfo)) {
            File uploadFrom = new File(filePath);
            byte[] buffer = new byte[10_240];
            try (InputStream input = Files.newInputStream(uploadFrom.toPath())) {
                int limit;
                while ((limit = input.read(buffer)) >= 0) {
                    writer.write(ByteBuffer.wrap(buffer, 0, limit));
                }
            }
        }

        LocalDateTime ara = LocalDateTime.now();

        FitxerBucketDto fitxerBucket = new FitxerBucketDto();
        fitxerBucket.setNom(filePath);
        fitxerBucket.setPath(objectName);
        fitxerBucket.setBucket(bucketName);
        fitxerBucket.setDataCreacio(ara);

        return fitxerBucket;
    }

    public String generateV4GetObjectSignedUrl(FitxerBucketDto fitxerBucket, boolean withDownload, String ua) throws StorageException, IOException {
        String projectId = this.projectId;
        String bucketName = fitxerBucket.getBucket();
        String objectName = fitxerBucket.getPath();

        System.out.println("Generant URL "+projectId+"---"+bucketName+"---"+objectName);

        String[] scopes = {StorageScopes.DEVSTORAGE_READ_WRITE,StorageScopes.DEVSTORAGE_READ_ONLY,StorageScopes.DEVSTORAGE_FULL_CONTROL};
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();

        URL url;

        // headers per evitar descàrrega automàtica (firefox és un cas a part)
        if (withDownload) {
            if (ua.contains("Firefox")) {
                HashMap<String, String> params = new HashMap<>();
                params.put("response-content-disposition", "attachment");

                url = storage.signUrl(blobInfo, 60, TimeUnit.MINUTES,
                        Storage.SignUrlOption.withQueryParams(params),
                        Storage.SignUrlOption.withV4Signature()
                );
            }
            else {
                url = storage.signUrl(blobInfo, 60, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature());
            }
        }
        else {
            HashMap<String, String> params = new HashMap<>();
            params.put("response-content-disposition", "inline");
            params.put("response-content-type", "application/pdf");

            url = storage.signUrl(blobInfo, 60, TimeUnit.MINUTES,
                    Storage.SignUrlOption.withQueryParams(params),
                    Storage.SignUrlOption.withV4Signature()
            );
        }

        System.out.println("Generated GET signed URL:");
        System.out.println(url);

        return url.toString();
    }

    public void deleteObject(String objectName, String bucketName) throws IOException, GeneralSecurityException {
        String[] scopes = {StorageScopes.DEVSTORAGE_READ_WRITE};
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        storage.delete(bucketName, objectName);

        System.out.println("Object " + objectName + " was deleted from " + bucketName);
    }


}

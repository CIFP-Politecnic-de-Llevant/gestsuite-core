package cat.politecnicllevant.core.controller;

import cat.politecnicllevant.core.service.GoogleStorageService;
import cat.politecnicllevant.core.service.TokenManager;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@Slf4j
public class AdministradorController {

    @Autowired
    private GoogleStorageService googleStorageService;

    @Autowired
    private TokenManager tokenManager;

    @Value("${spring.datasource.username}")
    private String userDatabase;

    @Value("${spring.datasource.password}")
    private String passwordDatabase;

    @Value("${server.database.name}")
    private String nameDatabases;

    @Value("${server.tmp}")
    private String pathTemporal;

    @Value("${gc.projectid}")
    private String projectId;

    @Value("${gc.storage.bucketnamebackup}")
    private String bucketName;

    @Value("${gc.adminUser}")
    private String adminEmail;

    @PostMapping("/administrator/backupdatabase")
    public void backupDatabaseAuth(HttpServletRequest request) throws GeneralSecurityException, IOException, InterruptedException {
        Claims claims = tokenManager.getClaims(request);
        String myEmail = (String) claims.get("email");

        if (myEmail.equals(adminEmail)) {
            this.backupDatabase();
        }
    }

    /*
        Second Minute Hour Day-of-Month
        second, minute, hour, day(1-31), month(1-12), weekday(1-7) SUN-SAT
        0 0 1 * * * = a les 1AM de cada dia
    */
    @Scheduled(cron = "0 0 0 * * *")
    public void backupDatabase() throws InterruptedException, IOException, GeneralSecurityException {
        LocalDate localDate = LocalDate.now();
        String today = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String[] databases = this.nameDatabases.split(",");

        for(String nameDatabase: databases){
            // The path to your file to upload
            String filePath = pathTemporal + "/backup_" +nameDatabase+"_"+ today + ".sql";

            String dump = "mysqldump -u" + userDatabase + " -p" + passwordDatabase + " " + nameDatabase + " > " + filePath;
            String[] cmdarray = {"/bin/sh", "-c", dump};
            Process p = Runtime.getRuntime().exec(cmdarray);
            if (p.waitFor() == 0) {
                // Everything went fine

                // The ID of your GCS object
                String objectName = "backup_" +nameDatabase+"_"+ today + ".sql";

                googleStorageService.uploadObject(objectName, filePath,"application/sql", bucketName );

                log.info("File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);

                String deleteBackup = "rm -fr " + pathTemporal + "/*.sql";
                String[] cmdarrayDeleteBackup = {"/bin/sh", "-c", deleteBackup};
                Process pDeleteBackup = Runtime.getRuntime().exec(cmdarrayDeleteBackup);
                if (pDeleteBackup.waitFor() == 0) {
                    // Everything went fine
                    log.info("Arxius SQL esborrats amb èxit");
                } else {
                    // Something went wrong
                    log.error("Error esborrant els arxius SQL. Comanda: "+deleteBackup);
                }
            } else {
                // Something went wrong
                log.error("Error fent el backup");
            }
        }


    }


}
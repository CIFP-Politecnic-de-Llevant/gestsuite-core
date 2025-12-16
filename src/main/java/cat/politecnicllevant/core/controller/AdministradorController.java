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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            if (nameDatabase.isEmpty()) continue;
            String fileName = "backup_" + nameDatabase + "_" + today + ".sql";
            Path filePath = Paths.get(pathTemporal, fileName);

            String command = "mysqldump --column-statistics=0 --no-tablespaces -u" + userDatabase + " " + nameDatabase;
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", command);

            // Set MYSQL_PWD environment variable for the password. This is more secure.
            processBuilder.environment().put("MYSQL_PWD", passwordDatabase);

            // Redirect the output of the process to the backup file.
            processBuilder.redirectOutput(filePath.toFile());

            // Redirect error stream to prevent the process from blocking.
            // We can discard it if we are not interested in the error output for now.
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Backup for database '{}' created successfully at '{}'", nameDatabase, filePath);

                // The ID of your GCS object
                String objectName = fileName;

                googleStorageService.uploadObject(objectName, filePath.toString(),"application/sql", bucketName );
                log.info("File {} uploaded to bucket {} as {}", filePath, bucketName, objectName);

                // Delete the local backup file in a safer way
                Files.deleteIfExists(filePath);
                log.info("Local backup file '{}' deleted successfully.", filePath);

            } else {
                log.error("Error creating backup for database '{}'. mysqldump exited with code: {}", nameDatabase, exitCode);
            }
        }
    }
}
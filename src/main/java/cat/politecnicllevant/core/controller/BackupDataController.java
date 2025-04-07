package cat.politecnicllevant.core.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class BackupDataController {

    @Value("${spring.datasource.host}")
    private String dbHost;

    @Value("${spring.datasource.port}")
    private String dbPort;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.dbname}")
    private String dbName;

    @GetMapping(path = "/getDataSourceData")
    public ResponseEntity<Map<String, String>> getDataSourceData() {

        Map<String, String> map = new HashMap<>();

        map.put("dbHost", dbHost);
        map.put("dbPort", dbPort);
        map.put("dbUser", dbUser);
        map.put("dbPassword", dbPassword);
        map.put("dbName", dbName);

        return ResponseEntity.ok(map);
    }
}
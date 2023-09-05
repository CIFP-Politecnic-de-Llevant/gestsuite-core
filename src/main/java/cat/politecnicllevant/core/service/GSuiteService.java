package cat.politecnicllevant.core.service;

import cat.politecnicllevant.core.model.gestib.Usuari;
import cat.politecnicllevant.core.repository.gestib.UsuariRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Acl;
import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class GSuiteService {

    @Autowired
    private UsuariRepository usuariRepository;

    @Value("${gc.keyfile}")
    private String keyFile;
    @Value("${gc.adminUser}")
    private String adminUser;
    @Value("${centre.usuaris.passwordinicial}")
    private String passwordInicial;

    @Value("${gc.nomprojecte}")
    private String nomProjecte;

    @Value("${centre.domini.principal}")
    private String dominiPrincipal;

    @Value("${centre.domini.alumnat}")
    private String dominiAlumnat;

    //USUARIS
    public List<User> getUsers() throws InterruptedException {
        return getUsers(0);
    }

    public List<User> getUsers(int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            List<User> result = new ArrayList<>();

            String[] dominis = {this.dominiPrincipal, this.dominiAlumnat};

            for (String domain : dominis) {
                Users query = service.users().list()
                        .setDomain(domain)
                        .execute();

                List<User> users = query.getUsers();
                String pageToken = query.getNextPageToken();

                //Resultat
                if(users!=null) {
                    result.addAll(users);

                    while (pageToken != null) {
                        Users query2 = service.users().list()
                                .setDomain(domain)
                                .setPageToken(pageToken)
                                .execute();
                        List<User> users2 = query2.getUsers();
                        pageToken = query2.getNextPageToken();

                        if (users2 != null) {
                            result.addAll(users2);
                        }
                    }
                }
            }
            return result;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return getUsers(retry + 1);
            }
            log.error("Error aconseguint els usuaris de GSuite");
        }
        return new ArrayList<>();
    }

    public User getUserById(String id) throws InterruptedException {
        return getUserById(id, 0);
    }

    public User getUserById(String id, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            return service.users().get(id).execute();
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return getUserById(id, retry + 1);
            }
            log.error("Error getUserById. Id=" + id);
        }
        return null;
    }

    public User createUser(String email, String nom, String cognoms, String personalID, String unitatOrganitzativa) throws InterruptedException {
        return createUser(email, nom, cognoms, personalID, unitatOrganitzativa, 0);
    }

    public User createUser(String email, String nom, String cognoms, String personalID, String unitatOrganitzativa, int retry) throws InterruptedException {

        if (!EmailValidator.getInstance().isValid(email)) {
            return null;
        }

        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            UserName userName = new UserName();
            userName.setGivenName(nom);
            userName.setFamilyName(cognoms);
            userName.setFullName(nom + ' ' + cognoms);

            ArrayMap userKey = new ArrayMap();
            userKey.set(0, "value", personalID);
            userKey.set(1, "type", "organization");

            List<ArrayMap> list = new ArrayList<>();
            list.add(userKey);


            User u = new User();
            u.setName(userName);
            u.setExternalIds(list);
            u.setPassword(this.passwordInicial);
            u.setChangePasswordAtNextLogin(true);
            u.setPrimaryEmail(email);
            u.setOrgUnitPath(unitatOrganitzativa);

            return service.users().insert(u).execute();
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return createUser(email, nom, cognoms, personalID, unitatOrganitzativa, retry + 1);
            }
            log.error("Error createUser: " + email + "-" + nom + "-" + cognoms + "-" + personalID + "-" + unitatOrganitzativa);
            log.error(e.getMessage());
        }
        return null;
    }

    public User updateUser(String email, String nom, String cognoms, String personalID, String unitatOrganitzativa) throws InterruptedException {
        return updateUser(email, nom, cognoms, personalID, unitatOrganitzativa, 0);
    }

    public User updateUser(String email, String nom, String cognoms, String personalID, String unitatOrganitzativa, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY};
            GoogleCredentials credentials = null;

            credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            UserName userName = new UserName();
            userName.setGivenName(nom);
            userName.setFamilyName(cognoms);
            userName.setFullName(nom + " " + cognoms);

            ArrayMap userKey = new ArrayMap();
            userKey.set(0, "value", personalID);
            userKey.set(1, "type", "organization");

            List<ArrayMap> list = new ArrayList<>();
            list.add(userKey);

            Usuari usuari = usuariRepository.findUsuariByGsuiteEmail(email);

            if (usuari != null) {
                User u = service.users().get(email).execute();
                u.setName(userName);
                u.setExternalIds(list);
                u.setPrimaryEmail(usuari.getGsuiteEmail());
                u.setOrgUnitPath(unitatOrganitzativa);

                return service.users().update(email, u).execute();
            }
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return updateUser(email, nom, cognoms, personalID, unitatOrganitzativa, retry + 1);
            }
            log.error("Error updateUser: " + email + "-" + nom + "-" + cognoms + "-" + personalID + "-" + unitatOrganitzativa);
        }
        return null;
    }

    public User suspendreUser(String email, boolean suspes) throws InterruptedException {
        return suspendreUser(email, suspes, 0);
    }

    public User suspendreUser(String email, boolean suspes, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            Usuari usuari = usuariRepository.findUsuariByGsuiteEmail(email);

            User u = service.users().get(email).execute();
            u.setSuspended(suspes);

            return service.users().update(email, u).execute();
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return suspendreUser(email, suspes, retry + 1);
            }
            log.error("Error suspendreUser: " + email + "-" + suspes);
        }
        return null;
    }

    public User resetPassword(String email, String password) throws InterruptedException {
        return resetPassword(email, password, 0);
    }

    public User resetPassword(String email, String password, int retry) throws InterruptedException {
        try {
            log.info("Reset " + email + " amb el password " + password);
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            Usuari usuari = usuariRepository.findUsuariByGsuiteEmail(email);

            User u = service.users().get(email).execute();
            u.setPassword(password);
            u.setChangePasswordAtNextLogin(true);

            service.users().update(email, u).execute();

            return u;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return resetPassword(email, password, retry + 1);
            }
            log.error("Error resetPassword: " + email + "-" + password);
        }
        return null;
    }

    //GRUPS
    public List<Group> getGroups() throws InterruptedException {
        return getGroups(0);
    }

    public List<Group> getGroups(int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            Groups query = service.groups().list()
                    .setDomain(this.dominiPrincipal)
                    .execute();
            List<Group> groups = query.getGroups();
            String pageToken = query.getNextPageToken();

            //Resultat
            List<Group> result = new ArrayList<>();
            result.addAll(groups);

            while (pageToken != null) {
                Groups query2 = service.groups().list()
                        .setDomain(this.dominiPrincipal)
                        .setPageToken(pageToken)
                        .execute();
                List<Group> groups2 = query2.getGroups();
                pageToken = query2.getNextPageToken();

                if(groups2!=null) {
                    result.addAll(groups2);
                }
            }

            return result;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return getGroups(retry + 1);
            }
            log.error("Error getGroups");
        }
        return new ArrayList<>();
    }

    public Group getGroupById(String idgrup) throws InterruptedException {
        return getGroupById(idgrup, 0);
    }

    public Group getGroupById(String idgrup, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();


            Group group = service.groups().get(idgrup).execute();

            return group;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return getGroupById(idgrup, retry + 1);
            }
            log.error("Error getGroupById. " + idgrup);
        }
        return null;
    }

    public List<Member> getMembers(String group) throws InterruptedException {
        return getMembers(group, 0);
    }

    public List<Member> getMembers(String group, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            Members query = service.members().list(group).execute();
            List<Member> members = query.getMembers();
            String pageToken = query.getNextPageToken();

            //Resultat
            List<Member> result = new ArrayList<>();
            if (members != null) {
                result.addAll(members);
            }

            while (pageToken != null) {
                Members query2 = service.members().list(group)
                        .setPageToken(pageToken)
                        .execute();

                List<Member> members2 = query2.getMembers();
                pageToken = query2.getNextPageToken();

                if(members2!=null) {
                    result.addAll(members2);
                }
            }

            return result;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return getMembers(group, retry + 1);
            }
            log.error("Error getMembers. " + group);
        }
        return new ArrayList<>();
    }

    public List<Group> getUserGroups(String emailUser) throws InterruptedException {
        return getUserGroups(emailUser, 0);
    }

    public List<Group> getUserGroups(String emailUser, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();


            Groups query = service.groups().list()
                    .setDomain(this.dominiPrincipal)
                    .setUserKey(emailUser)
                    .execute();
            List<Group> groups = query.getGroups();
            String pageToken = query.getNextPageToken();

            //Resultat
            List<Group> result = new ArrayList<>();
            if (groups != null) {
                result.addAll(groups);
            }

            while (pageToken != null) {
                Groups query2 = service.groups().list()
                        .setDomain(this.dominiPrincipal)
                        .setUserKey(emailUser)
                        .setPageToken(pageToken)
                        .execute();
                List<Group> groups2 = query2.getGroups();
                pageToken = query2.getNextPageToken();

                if (groups2 != null) {
                    result.addAll(groups2);
                }
            }

            return result;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return getUserGroups(emailUser, retry + 1);
            }
            log.error("Error aconseguint els grups d'usuari de " + emailUser);
        }
        return new ArrayList<>();
    }

    public void deleteMember(String emailUser, String emailGrup) throws InterruptedException {
        deleteMember(emailUser, emailGrup, 0);
    }

    public void deleteMember(String emailUser, String emailGrup, int retry) throws InterruptedException {
        if (emailUser == null) {
            log.error("Error eliminant l'usuari. Email d'usuari null");
        } else if (emailGrup == null) {
            log.error("Error eliminant l'usuari. Email del grup null");
        } else {
            try {
                String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
                GoogleCredentials credentials = null;

                credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

                HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

                Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

                service.members().delete(emailGrup, emailUser).execute();
                log.info("S'ha eliminat l'usuari " + emailUser + " del grup " + emailGrup);
            } catch (GeneralSecurityException | IOException e) {
                if (retry < 5) {
                    TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                    deleteMember(emailUser, emailGrup, retry + 1);
                }
                log.error("Error deleteMember. " + emailUser + " - " + emailGrup);
            }
        }
    }

    public void createMember(String emailUser, String emailGrup) throws InterruptedException {
        createMember(emailUser, emailGrup, 0);
    }

    public void createMember(String emailUser, String emailGrup, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
            GoogleCredentials credentials = null;

            credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            Member m = new Member();
            m.setEmail(emailUser);
            m.setRole("MEMBER");
            service.members().insert(emailGrup, m).execute();
            log.info("S'ha afegit l'usuari " + emailUser + " al grup " + emailGrup);
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                createMember(emailUser, emailGrup, retry + 1);
            }
            log.error("Error createMember. " + emailUser + " - " + emailGrup);
        }
    }

    public Group createGroup(String email, String nom, String descripcio) throws InterruptedException {
        return createGroup(email, nom, descripcio, 0);
    }

    public Group createGroup(String email, String nom, String descripcio, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            Group g = new Group();
            g.setEmail(email);
            g.setDescription(descripcio);
            g.setName(nom);
            service.groups().insert(g).execute();

            return g;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return createGroup(email, nom, descripcio, retry + 1);
            }
            log.error("Error createGroup. " + email + " - " + nom + " - " + descripcio);
        }
        return null;
    }

    public Group updateGroup(String email, String nom, String descripcio) throws InterruptedException {
        return updateGroup(email, nom, descripcio, 0);
    }

    public Group updateGroup(String email, String nom, String descripcio, int retry) throws InterruptedException {
        try {
            String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_GROUP, DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY};
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            Group g = service.groups().get(email).execute();
            g.setDescription(descripcio);
            g.setName(nom);
            service.groups().update(email, g).execute();

            return g;
        } catch (GeneralSecurityException | IOException e) {
            if (retry < 5) {
                TimeUnit.MILLISECONDS.sleep(((2 ^ retry) * 1000L) + getRandomMilliseconds());
                return updateGroup(email, nom, descripcio, retry + 1);
            }
            log.error("Error updateGroup. " + email + " - " + nom + " - " + descripcio);
        }
        return null;
    }


    //CALENDARIS
    public List<CalendarResource> getCalendars() throws GeneralSecurityException, IOException {
        String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR, DirectoryScopes.ADMIN_DIRECTORY_RESOURCE_CALENDAR_READONLY};
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

        CalendarResources query = service.resources().calendars().list("my_customer").execute();
        List<CalendarResource> calendaris = query.getItems();
        String pageToken = query.getNextPageToken();


        //Resultat
        List<CalendarResource> result = new ArrayList<>();
        result.addAll(calendaris);

        while (pageToken != null) {
            CalendarResources query2 = service.resources().calendars().list(this.adminUser)
                    .setPageToken(pageToken)
                    .execute();

            List<CalendarResource> calendaris2 = query2.getItems();
            pageToken = query2.getNextPageToken();

            if(calendaris2!=null) {
                result.addAll(calendaris2);
            }
        }

        return result;
    }

    public List<AclRule> getUsersByCalendar(String emailCalendar) throws IOException, GeneralSecurityException {
        String[] scopes = {CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY};
        GoogleCredentials credentials = null;

        credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

        Acl acl = service.acl().list(emailCalendar).execute();

        /*for (AclRule rule : acl.getItems()) {
            System.out.println(rule.getId() + ": " + rule.getRole()+"::"+rule.getScope().getValue()+"::"+rule.getScope().getType());
        }*/

        return acl.getItems();
    }

    public void insertUserCalendar(String emailUser, String emailCalendar) {
        try {
            String[] scopes = {CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY};
            GoogleCredentials credentials = null;

            credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);

            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Calendar service = new Calendar.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

            AclRule.Scope scope = new AclRule.Scope();
            scope.setType("user");
            scope.setValue(emailUser);

            AclRule aclRule = new AclRule();
            aclRule.setRole("reader");
            aclRule.setScope(scope);

            service.acl().insert(emailCalendar, aclRule).execute();

            System.out.println("S'ha afegit l'usuari " + emailUser + " al calendari " + emailCalendar);
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("email: " + emailUser + " calendari: " + emailCalendar + " error: " + e.getMessage());
        }
    }

    //Devices
    public List<ChromeOsDevice> getChromeOSDevicesByUser(User user) throws GeneralSecurityException, IOException {
        String[] scopes = {DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, DirectoryScopes.ADMIN_DIRECTORY_DEVICE_CHROMEOS, DirectoryScopes.ADMIN_DIRECTORY_DEVICE_CHROMEOS_READONLY};
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(this.keyFile)).createScoped(scopes).createDelegated(this.adminUser);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Directory service = new Directory.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName(this.nomProjecte).build();

        ChromeOsDevices query = service.chromeosdevices().list(user.getCustomerId()).execute();
        List<ChromeOsDevice> chromes = query.getChromeosdevices();
        String pageToken = query.getNextPageToken();


        //Resultat
        List<ChromeOsDevice> result = new ArrayList<>();
        if(chromes!=null) {
            result.addAll(chromes);
        }

        while (pageToken != null) {
            ChromeOsDevices query2 = service.chromeosdevices().list(user.getCustomerId())
                    .setPageToken(pageToken)
                    .execute();

            List<ChromeOsDevice> chromes2 = query2.getChromeosdevices();
            pageToken = query2.getNextPageToken();

            if(chromes2!=null) {
                result.addAll(chromes2);
            }
        }

        return result;
    }

    private int getRandomMilliseconds() {
        Random r = new Random();
        int low = 0;
        int high = 1000;
        int result = r.nextInt(high - low) + low;
        return result;
    }
}


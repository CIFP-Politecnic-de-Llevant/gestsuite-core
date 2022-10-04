package cat.iesmanacor.core.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class UtilService {

    public static @NotNull String capitalize(String str) {

        if(str==null || str.isEmpty()){
            return "";
        }

        String[] excepcions = {"da", "de", "di", "do", "del", "la", "las", "le", "los", "mac", "mc", "van", "von", "y", "i", "san", "santa","al","el"};
        List<String> excepcionsList = Arrays.asList(excepcions);

        String s = str.toLowerCase().trim();
        String[] sarr = s.split(" ");

        List<String> result = new ArrayList<>();

        for(String part: sarr){
            if(excepcionsList.contains(part) || part.length()<2){
                result.add(part);
            } else {
                result.add(part.substring(0, 1).toUpperCase() + part.substring(1));
            }
        }

        return String.join(" ", result);
    }

}


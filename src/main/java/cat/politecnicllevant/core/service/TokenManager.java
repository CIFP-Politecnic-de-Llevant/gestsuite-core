package cat.politecnicllevant.core.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class TokenManager {


    @Value("${jwt.secret}")
    private String jwtSecret;


    public String createToken(String email, List<String> rols) {
        return Jwts.builder()
                .claim("email", email)
                .claim("rols",rols)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000 * 24 * 7)) //1 setmana
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encodeToString(this.jwtSecret.getBytes()))
                .compact();
    }

    public TokenResponse validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(this.jwtSecret.getBytes()) //.setSigningKey(this.jwtSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
            return TokenResponse.OK;
        } catch (ExpiredJwtException e) {
            return TokenResponse.EXPIRED;
        } catch (Exception e) {
            return TokenResponse.ERROR;
        }
    }

    public Claims getClaims(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        String token = auth.replace("Bearer ", "");
        return getClaims(token);
    }

    public Claims getClaims(String token) {
        Claims claims = null;
        try {
            claims = Jwts.parser()
                    .setSigningKey(this.jwtSecret.getBytes()) //.setSigningKey(this.jwtSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return claims;
    }

}
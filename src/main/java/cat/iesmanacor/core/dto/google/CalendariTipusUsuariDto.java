package cat.iesmanacor.core.dto.google;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum CalendariTipusUsuariDto {
    PUBLIC ("default"),DOMINI("domain" ),USUARI("user"),GRUP("group");

    private final String tipus;
}

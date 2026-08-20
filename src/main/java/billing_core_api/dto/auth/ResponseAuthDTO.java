package billing_core_api.dto.auth;

public record ResponseAuthDTO(String token,
                              Long expiresIn) {
}

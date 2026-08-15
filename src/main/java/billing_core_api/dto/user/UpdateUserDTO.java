package billing_core_api.dto.user;

public record UpdateUserDTO(String name,
                            String email,
                            String password) {
}

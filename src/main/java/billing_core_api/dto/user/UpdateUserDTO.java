package billing_core_api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserDTO(@NotBlank(message = "Nome nao pode ser vazio")
                            String name,
                            @NotBlank(message = "Email nao pode ser vazio")
                            @Email(message = "Email invalido")
                            String email,
                            String password) {
}

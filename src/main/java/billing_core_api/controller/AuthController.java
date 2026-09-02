package billing_core_api.controller;

import billing_core_api.dto.auth.LoginRequestDTO;
import billing_core_api.dto.auth.ResponseAuthDTO;
import billing_core_api.dto.auth.ResponseCreatedUser;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ResponseCreatedUser> register(@RequestBody @Valid CreateUserDTO request) {
        var user = authService.register(request);

        var response = new ResponseCreatedUser(user.getName(), user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseAuthDTO login(@RequestBody @Valid LoginRequestDTO request) throws BadRequestException {
        return authService.login(request);
    }
}

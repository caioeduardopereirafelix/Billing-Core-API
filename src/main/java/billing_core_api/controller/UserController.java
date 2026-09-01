package billing_core_api.controller;

import billing_core_api.config.SecurityUtils;
import billing_core_api.domain.mapper.UserMapper;
import billing_core_api.domain.user.User;
import billing_core_api.dto.user.CreateUserDTO;
import billing_core_api.dto.user.ResponseUserDTO;
import billing_core_api.dto.user.UpdateUserDTO;
import billing_core_api.repository.UserRepository;
import billing_core_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ResponseUserDTO> createUser(@Valid @RequestBody CreateUserDTO dto) {

        var user = userService.createUser(dto);

        ResponseUserDTO responseUserDTO = mapper.toUserResponse(user);

        return new ResponseEntity(responseUserDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity getDetails(@PathVariable String userId){

        var authenticatedUser = securityUtils.getAuthenticatedUser();

        if (!authenticatedUser.getId().toString().equals(userId)
                && !authenticatedUser.getRoles().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var idUser = UUID.fromString(userId);
        Optional<User> userOptional = userService.findById(idUser);

        if (userOptional.isPresent()){
            var userPresent = userOptional.get();
            var response = mapper.toUserResponse(userPresent);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity deleteUser(@PathVariable("userId") String id){

        var authenticatedUser = securityUtils.getAuthenticatedUser();

        if (!authenticatedUser.getId().toString().equals(id)
                && !authenticatedUser.getRoles().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var idUser = UUID.fromString(id);

        Optional<User> optionalUser = userService.findById(idUser);

        if (optionalUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(optionalUser.get());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity updateUser(
            @PathVariable("userId")String id,
            @RequestBody UpdateUserDTO updateUserDTO){

        User authenticatedUser = securityUtils.getAuthenticatedUser();

        if (!authenticatedUser.getId().toString().equals(id)
                && !authenticatedUser.getRoles().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var idUser = UUID.fromString(id);

        User userUpdate = userService.updateUser(idUser, updateUserDTO);

        return ResponseEntity.ok(mapper.toUserResponse(userUpdate));
    }
}


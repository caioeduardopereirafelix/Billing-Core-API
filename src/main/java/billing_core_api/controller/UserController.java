package billing_core_api.controller;

import billing_core_api.config.SecurityUtils;
import billing_core_api.domain.mapper.UserMapper;
import billing_core_api.domain.user.User;
import billing_core_api.dto.user.DepositRequest;
import billing_core_api.dto.user.ResponseUserDTO;
import billing_core_api.dto.user.UpdateUserDTO;
import billing_core_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    public ResponseEntity<ResponseUserDTO> me() {
        UUID id = securityUtils.getAuthenticatedUser().getId();
        return ResponseEntity.ok(mapper.toUserResponse(userService.getById(id)));
    }

    @PostMapping("/me/deposit")
    public ResponseEntity<ResponseUserDTO> deposit(@Valid @RequestBody DepositRequest request) {
        UUID id = securityUtils.getAuthenticatedUser().getId();
        return ResponseEntity.ok(mapper.toUserResponse(userService.deposit(id, request.amount())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ResponseUserDTO> getDetails(@PathVariable String userId) {
        UUID id = UUID.fromString(userId);
        securityUtils.requireOwnerOrAdmin(id);

        Optional<User> user = userService.findById(id);
        return user
                .map(u -> ResponseEntity.ok(mapper.toUserResponse(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") String userId) {
        UUID id = UUID.fromString(userId);
        securityUtils.requireOwnerOrAdmin(id);

        Optional<User> user = userService.findById(id);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(user.get());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ResponseUserDTO> updateUser(
            @PathVariable("userId") String userId,
            @RequestBody UpdateUserDTO updateUserDTO) {

        UUID id = UUID.fromString(userId);
        securityUtils.requireOwnerOrAdmin(id);

        User updated = userService.updateUser(id, updateUserDTO);
        return ResponseEntity.ok(mapper.toUserResponse(updated));
    }
}

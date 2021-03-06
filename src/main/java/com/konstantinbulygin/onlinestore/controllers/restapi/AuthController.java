package com.konstantinbulygin.onlinestore.controllers.restapi;

import com.konstantinbulygin.onlinestore.config.jwt.JwtUtils;
import com.konstantinbulygin.onlinestore.model.Role;
import com.konstantinbulygin.onlinestore.model.RoleEnum;
import com.konstantinbulygin.onlinestore.model.User;
import com.konstantinbulygin.onlinestore.model.restmodel.JwtResponse;
import com.konstantinbulygin.onlinestore.model.restmodel.MessageResponse;
import com.konstantinbulygin.onlinestore.model.restmodel.SignUpRequestUser;
import com.konstantinbulygin.onlinestore.model.restmodel.UserRequest;
import com.konstantinbulygin.onlinestore.service.RoleRepoService;
import com.konstantinbulygin.onlinestore.service.UserDetailsImpl;
import com.konstantinbulygin.onlinestore.service.UserRepoService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(value = "*", maxAge = 3600)
@ApiModel("Auth controller for all users")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepoService userRepoService;

    @Autowired
    RoleRepoService roleRepoService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Allow to login registered user")
    public ResponseEntity<JwtResponse> authUser(@RequestBody UserRequest userRequest) {
        Authentication authentication =
                authenticationManager
                        .authenticate(new UsernamePasswordAuthenticationToken(
                                userRequest.getUserName(),
                                userRequest.getUserPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getUserEmail(),
                roles
        ));
    }

    @PostMapping(value = "/logout")
    @ApiOperation("Logout user")
    public ResponseEntity<MessageResponse> logoutUser() {
        return ResponseEntity.ok(new MessageResponse("User logged out"));
    }

    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Allow to register new user")
    public ResponseEntity<MessageResponse> registerUser(@RequestBody SignUpRequestUser signUpRequestUser) {
        if (userRepoService.existsByUserName(signUpRequestUser.getUserName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: username exists"));
        }

        User user = new User(signUpRequestUser.getUserName(), signUpRequestUser.getUserEmail(),
                passwordEncoder.encode(signUpRequestUser.getUserPassword()), LocalDateTime.now());

        Set<String> reqRoles = signUpRequestUser.getRoles();
        Set<Role> roles = new HashSet<>();

        if (reqRoles == null) {
            Role userRole = roleRepoService
                    .findByName(RoleEnum.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error, Role USER is not found"));
            roles.add(userRole);
        } else {
            reqRoles.forEach(r -> {
                if ("admin".equals(r)) {
                    Role adminRole = roleRepoService
                            .findByName(RoleEnum.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Error, Role ADMIN is not found"));
                    roles.add(adminRole);
                } else {
                    Role userRole = roleRepoService
                            .findByName(RoleEnum.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Error, Role USER is not found"));
                    roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepoService.save(user);
        return ResponseEntity.ok(new MessageResponse("User CREATED"));
    }
}

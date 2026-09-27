package com.expenseplatform.auth.service;

import com.expenseplatform.auth.dto.AuthResponse;
import com.expenseplatform.auth.dto.LoginRequest;
import com.expenseplatform.auth.dto.RegisterRequest;
import com.expenseplatform.auth.entity.Role;
import com.expenseplatform.auth.entity.User;
import com.expenseplatform.auth.exception.InvalidCredentialsException;
import com.expenseplatform.auth.exception.UserAlreadyExistsException;
import com.expenseplatform.auth.repository.UserRepository;
import com.expenseplatform.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Rahul Kumar");
        registerRequest.setEmail("rahul@example.com");
        registerRequest.setPassword("secret123");
        registerRequest.setRole(Role.USER);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("rahul@example.com");
        loginRequest.setPassword("secret123");

        existingUser = User.builder()
                .id(1L)
                .name("Rahul Kumar")
                .email("rahul@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_shouldCreateUserAndReturnToken_whenEmailNotAlreadyRegistered() {
        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(jwtService.generateToken(1L, "rahul@example.com", "USER")).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("rahul@example.com");
        assertThat(response.getToken()).isEqualTo("fake-jwt-token");

        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(argThat(user -> user.getPassword().equals("hashed-password")));
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("rahul@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(1L, "rahul@example.com", "USER")).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getEmail()).isEqualTo("rahul@example.com");
    }

    @Test
    void login_shouldThrow_whenEmailDoesNotExist() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    void login_shouldThrow_whenPasswordIsWrong() {
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateToken(any(), anyString(), anyString());
    }
}

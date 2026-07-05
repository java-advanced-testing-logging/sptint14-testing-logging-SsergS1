package com.softserve.itacademy.service;

import com.softserve.itacademy.config.exception.NullEntityReferenceException;
import com.softserve.itacademy.dto.userDto.CreateUserDto;
import com.softserve.itacademy.dto.userDto.UpdateUserDto;
import com.softserve.itacademy.dto.userDto.UserDto;
import com.softserve.itacademy.dto.userDto.UserDtoConverter;
import com.softserve.itacademy.model.User;
import com.softserve.itacademy.model.UserRole;
import com.softserve.itacademy.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for UserService")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @InjectMocks
    private UserService userService;

    private User user;
    private User adminUser;
    private CreateUserDto createUserDto;
    private UpdateUserDto updateUserDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPassword("password123");
        user.setRole(UserRole.USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("admin123");
        adminUser.setRole(UserRole.ADMIN);

        createUserDto = new CreateUserDto();
        createUserDto.setFirstName("Jane");
        createUserDto.setLastName("Smith");
        createUserDto.setEmail("jane@example.com");
        createUserDto.setPassword("jane123");

        updateUserDto = new UpdateUserDto();
        updateUserDto.setId(1L);
        updateUserDto.setFirstName("Updated");
        updateUserDto.setLastName("Updated");
        updateUserDto.setEmail("updated@example.com");
        updateUserDto.setRole(UserRole.USER);
    }

    // ==================== READ BY ID TESTS ====================

    @Test
    @DisplayName("Should return user when found")
    void readById_ShouldReturnUser_WhenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.readById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found")
    void readById_ShouldThrowException_WhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.readById(999L));
        verify(userRepository, times(1)).findById(999L);
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("Should successfully save user when valid")
    void create_ShouldSaveUser_WhenValid() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.create(user);

        assertNotNull(result);
        assertEquals(user, result);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void create_ShouldThrowException_WhenEmailExists() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.create(user));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw NullEntityReferenceException when user is null")
    void create_ShouldThrowException_WhenUserIsNull() {
        assertThrows(NullEntityReferenceException.class, () -> userService.create(null));
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register user with USER role and {noop} password")
    void register_ShouldSetUserRoleAndAddNoopPrefix_WhenValid() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userDtoConverter.convertToUser(any(CreateUserDto.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.register(createUserDto);

        assertNotNull(result);
        assertEquals(UserRole.USER, createUserDto.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        String savedPassword = userCaptor.getValue().getPassword();
        assertTrue(savedPassword.startsWith("{noop}"));
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void register_ShouldThrowException_WhenEmailExists() {
        when(userDtoConverter.convertToUser(any(CreateUserDto.class))).thenReturn(user);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.register(createUserDto));
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update all fields when valid")
    void update_ShouldUpdateAllFields_WhenValid() {
        UserDto expectedUserDto = new UserDto();
        expectedUserDto.setId(1L);
        expectedUserDto.setFirstName("Updated");
        expectedUserDto.setLastName("Updated");
        expectedUserDto.setEmail("updated@example.com");
        expectedUserDto.setRole(UserRole.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userDtoConverter).fillFields(any(User.class), any(UpdateUserDto.class));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userDtoConverter.toDto(any(User.class))).thenReturn(expectedUserDto);

        UserDto result = userService.update(updateUserDto);

        assertNotNull(result);
        assertEquals(expectedUserDto, result);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should allow ADMIN to change role")
    void update_ShouldAllowAdminToChangeRole_WhenUserIsAdmin() {
        UpdateUserDto adminUpdateDto = new UpdateUserDto();
        adminUpdateDto.setId(2L);
        adminUpdateDto.setRole(UserRole.USER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        doNothing().when(userDtoConverter).fillFields(any(User.class), any(UpdateUserDto.class));
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(userDtoConverter.toDto(any(User.class))).thenReturn(new UserDto());

        userService.update(adminUpdateDto);

        assertEquals(UserRole.USER, adminUser.getRole());
        verify(userRepository, times(1)).save(adminUser);
    }

    @Test
    @DisplayName("Should NOT allow non-ADMIN to change role")
    void update_ShouldNotAllowNonAdminToChangeRole_WhenUserIsNotAdmin() {
        UpdateUserDto userUpdateDto = new UpdateUserDto();
        userUpdateDto.setId(1L);
        userUpdateDto.setRole(UserRole.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userDtoConverter).fillFields(any(User.class), any(UpdateUserDto.class));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userDtoConverter.toDto(any(User.class))).thenReturn(new UserDto());

        userService.update(userUpdateDto);

        assertEquals(UserRole.USER, user.getRole());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void update_ShouldThrowException_WhenUserNotFound() {
        UpdateUserDto nonExistentDto = new UpdateUserDto();
        nonExistentDto.setId(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.update(nonExistentDto));
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete user when exists")
    void delete_ShouldDeleteUser_WhenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(any(User.class));

        userService.delete(1L);

        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void delete_ShouldThrowException_WhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.delete(999L));
        verify(userRepository, never()).delete(any(User.class));
    }

    // ==================== FIND TESTS ====================

    @Test
    @DisplayName("Should find user by email when exists")
    void findByUsername_ShouldReturnUser_WhenExists() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("john@example.com");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void findByUsername_ShouldReturnEmpty_WhenNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("nonexistent@example.com");

        assertFalse(result.isPresent());
    }
}
package com.softserve.itacademy.controller;

import com.softserve.itacademy.dto.userDto.CreateUserDto;
import com.softserve.itacademy.dto.userDto.UpdateUserDto;
import com.softserve.itacademy.dto.userDto.UserDtoConverter;
import com.softserve.itacademy.model.User;
import com.softserve.itacademy.model.UserRole;
import com.softserve.itacademy.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDtoConverter userDtoConverter;

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("Should show create user form")
    void createGet_ShouldReturnCreateUserView() throws Exception {
        mockMvc.perform(get("/users/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-user"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("Should create user and redirect when valid")
    void createPost_ShouldRedirect_WhenValid() throws Exception {
        User user = new User();
        user.setId(1L);

        when(userService.register(any(CreateUserDto.class))).thenReturn(user);

        mockMvc.perform(post("/users/create")
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "john@example.com")
                .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos/all/users/1"));

        verify(userService, times(1)).register(any(CreateUserDto.class));
    }

    @Test
    @DisplayName("Should return form with errors when validation fails")
    void createPost_ShouldReturnFormWithErrors_WhenInvalid() throws Exception {
        mockMvc.perform(post("/users/create")
                .param("firstName", "")
                .param("lastName", "Doe")
                .param("email", "invalid-email")
                .param("password", "pass"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Should return form with errors when email already exists")
    void createPost_ShouldReturnFormWithErrors_WhenEmailExists() throws Exception {
        when(userService.register(any(CreateUserDto.class)))
                .thenThrow(new IllegalArgumentException("User with email 'john@example.com' already exists"));

        mockMvc.perform(post("/users/create")
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "john@example.com")
                .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-user"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("user", "email"));

        verify(userService, times(1)).register(any(CreateUserDto.class));
    }

    // ==================== READ TESTS ====================

    @Test
    @DisplayName("Should show user info when user exists")
    void read_ShouldReturnUserInfoView_WhenUserExists() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setRole(UserRole.USER);

        when(userService.readById(1L)).thenReturn(user);

        mockMvc.perform(get("/users/1/read"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-info"))
                .andExpect(model().attribute("user", user));

        verify(userService, times(1)).readById(1L);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should show edit user form when user exists")
    void updateGet_ShouldReturnUpdateUserView_WhenUserExists() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setRole(UserRole.USER);

        when(userService.readById(1L)).thenReturn(user);

        mockMvc.perform(get("/users/1/update"))
                .andExpect(status().isOk())
                .andExpect(view().name("update-user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"));

        verify(userService, times(1)).readById(1L);
    }

    @Test
    @DisplayName("Should update user and redirect when valid")
    void updatePost_ShouldRedirect_WhenValid() throws Exception {
        mockMvc.perform(post("/users/1/update")
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "john.updated@example.com")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/all"));

        verify(userService, times(1)).update(any(UpdateUserDto.class));
    }

    @Test
    @DisplayName("Should return form with errors when validation fails during update")
    void updatePost_ShouldReturnFormWithErrors_WhenInvalid() throws Exception {
        mockMvc.perform(post("/users/1/update")
                .param("firstName", "") // Empty - invalid
                .param("lastName", "Doe")
                .param("email", "invalid-email") // Invalid email
                .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("update-user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().hasErrors());
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete user and redirect when user exists")
    void delete_ShouldRedirect_WhenUserExists() throws Exception {
        mockMvc.perform(get("/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/all"));

        verify(userService, times(1)).delete(1L);
    }

    // ==================== LIST TESTS ====================

    @Test
    @DisplayName("Should show all users")
    void getAll_ShouldReturnUsersListView() throws Exception {
        User user1 = new User();
        user1.setId(1L);
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setEmail("john@example.com");
        user1.setRole(UserRole.USER);

        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setEmail("jane@example.com");
        user2.setRole(UserRole.ADMIN);

        when(userService.getAll()).thenReturn(Arrays.asList(user1, user2));

        mockMvc.perform(get("/users/all"))
                .andExpect(status().isOk())
                .andExpect(view().name("users-list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", Arrays.asList(user1, user2)));

        verify(userService, times(1)).getAll();
    }
}
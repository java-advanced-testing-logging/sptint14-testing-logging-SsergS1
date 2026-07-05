package com.softserve.itacademy.controller;

import com.softserve.itacademy.dto.userDto.CreateUserDto;
import com.softserve.itacademy.dto.userDto.UpdateUserDto;
import com.softserve.itacademy.model.User;
import com.softserve.itacademy.model.UserRole;
import com.softserve.itacademy.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/create")
    public String create(Model model) {
        log.debug("GET request to show create user form");
        model.addAttribute("user", new CreateUserDto());
        return "create-user";
    }

    @PostMapping("/create")
    public String create(@Validated @ModelAttribute("user") CreateUserDto userDto,
                         BindingResult result) {
        log.info("POST request to create user with email: {}", userDto.getEmail());

        if (result.hasErrors()) {
            log.warn("Validation errors in user creation: {}", result.getAllErrors());
            return "create-user";
        }

        try {
            User user = userService.register(userDto);
            log.info("User created successfully with id: {}", user.getId());
            return "redirect:/todos/all/users/" + user.getId();
        } catch (IllegalArgumentException e) {
            log.warn("Error creating user: {}", e.getMessage());
            result.rejectValue("email", "error.user", e.getMessage());
            return "create-user";
        }
    }

    @GetMapping("/{id}/read")
    public String read(@PathVariable("id") Long id, Model model) {
        log.debug("GET request to read user with id: {}", id);
        User user = userService.readById(id);
        model.addAttribute("user", user);
        return "user-info";
    }

    @GetMapping("/{id}/update")
    public String update(@PathVariable("id") Long id, Model model) {
        log.debug("GET request to show update form for user id: {}", id);

        User user = userService.readById(id);
        UpdateUserDto userDto = new UpdateUserDto();
        userDto.setId(user.getId());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEmail(user.getEmail());
        userDto.setRole(user.getRole());

        model.addAttribute("user", userDto);
        model.addAttribute("roles", UserRole.values());
        return "update-user";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @Validated @ModelAttribute("user") UpdateUserDto userDto,
                         BindingResult result,
                         Model model) {
        log.info("POST request to update user with id: {}", id);

        if (result.hasErrors()) {
            log.warn("Validation errors in user update: {}", result.getAllErrors());
            model.addAttribute("roles", UserRole.values());
            return "update-user";
        }

        userDto.setId(id);
        userService.update(userDto);
        log.info("User {} updated successfully", id);
        return "redirect:/users/all";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id) {
        log.info("GET request to delete user with id: {}", id);
        userService.delete(id);
        log.info("User {} deleted successfully", id);
        return "redirect:/users/all";
    }

    @GetMapping("/all")
    public String getAll(Model model) {
        log.debug("GET request to show all users");
        model.addAttribute("users", userService.getAll());
        return "users-list";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleEntityNotFoundException(EntityNotFoundException ex) {
        log.error("Entity not found: {}", ex.getMessage());
        ModelAndView modelAndView = new ModelAndView("error/404");
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }
}
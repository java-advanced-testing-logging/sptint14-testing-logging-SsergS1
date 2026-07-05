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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserDtoConverter userDtoConverter;

    @Transactional
    public User register(CreateUserDto createUserDto) {
        log.info("Registering new user with email: {}", createUserDto.getEmail());

        createUserDto.setRole(UserRole.USER);
        User user = userDtoConverter.convertToUser(createUserDto);
        user.setPassword("{noop}" + user.getPassword());

        User savedUser = create(user);
        log.info("User registered successfully with id: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public User create(User user) {
        log.debug("Creating user with email: {}", user != null ? user.getEmail() : "null");

        if (user != null) {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                log.warn("Attempt to create user with existing email: {}", user.getEmail());
                throw new IllegalArgumentException("User with email '" + user.getEmail() + "' already exists");
            }
            User savedUser = userRepository.save(user);
            log.debug("User created with id: {}", savedUser.getId());
            return savedUser;
        }
        log.error("Attempt to create null user");
        throw new NullEntityReferenceException("User cannot be 'null'");
    }

    @Transactional(readOnly = true)
    public User readById(long id) {
        log.debug("Reading user by id: {}", id);

        return userRepository.findById(id).orElseThrow(() -> {
            log.error("User with id {} not found", id);
            return new EntityNotFoundException("User with id " + id + " not found");
        });
    }

    @Transactional
    public UserDto update(UpdateUserDto updateUserDto) {
        log.info("Updating user with id: {}", updateUserDto.getId());

        User user = userRepository.findById(updateUserDto.getId()).orElseThrow(
                () -> new EntityNotFoundException("User with id " + updateUserDto.getId() + " not found"));

        if (updateUserDto.getRole() != null && user.getRole() == UserRole.ADMIN) {
            log.debug("Admin {} changing role to {}", user.getId(), updateUserDto.getRole());
            user.setRole(updateUserDto.getRole());
            updateUserDto.setRole(null);
        } else {
            updateUserDto.setRole(null);
        }

        userDtoConverter.fillFields(user, updateUserDto);
        userRepository.save(user);

        UserDto result = userDtoConverter.toDto(user);
        log.info("User {} updated successfully", user.getId());
        return result;
    }

    @Transactional
    public void delete(long id) {
        log.info("Deleting user with id: {}", id);

        User user = readById(id);
        userRepository.delete(user);

        log.info("User {} deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public List<User> getAll() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by email: {}", username);
        return userRepository.findByEmail(username);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findById(long id) {
        log.debug("Finding user DTO by id: {}", id);
        return userRepository.findById(id).map(userDtoConverter::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto findByIdThrowing(long id) {
        log.debug("Finding user DTO by id with throwing: {}", id);
        return userRepository.findById(id)
                .map(userDtoConverter::toDto)
                .orElseThrow(() -> {
                    log.error("User with id {} not found", id);
                    return new EntityNotFoundException("User with id " + id + " not found");
                });
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        log.debug("Fetching all users as DTOs");
        return userRepository.findAll().stream()
                .map(userDtoConverter::toDto)
                .toList();
    }
}

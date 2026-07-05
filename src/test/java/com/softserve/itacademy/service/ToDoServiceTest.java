package com.softserve.itacademy.service;

import com.softserve.itacademy.config.exception.NullEntityReferenceException;
import com.softserve.itacademy.model.ToDo;
import com.softserve.itacademy.model.User;
import com.softserve.itacademy.model.UserRole;
import com.softserve.itacademy.repository.ToDoRepository;
import com.softserve.itacademy.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ToDoService")
public class ToDoServiceTest {

    @Mock
    private ToDoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ToDoService toDoService;

    private ToDo todo;
    private User owner;
    private User collaborator;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setEmail("john@example.com");
        owner.setRole(UserRole.USER);

        collaborator = new User();
        collaborator.setId(2L);
        collaborator.setFirstName("Jane");
        collaborator.setLastName("Smith");
        collaborator.setEmail("jane@example.com");
        collaborator.setRole(UserRole.USER);

        todo = new ToDo();
        todo.setId(1L);
        todo.setTitle("Test ToDo");
        todo.setOwner(owner);
        todo.setCollaborators(new HashSet<>());
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("Should successfully create ToDo when title is unique")
    void create_ShouldSaveToDo_WhenTitleIsUnique() {
        when(todoRepository.existsByTitle(todo.getTitle())).thenReturn(false);
        when(todoRepository.save(any(ToDo.class))).thenReturn(todo);

        ToDo result = toDoService.create(todo);

        assertNotNull(result);
        assertEquals(todo, result);
        verify(todoRepository, times(1)).existsByTitle(todo.getTitle());
        verify(todoRepository, times(1)).save(todo);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when title already exists")
    void create_ShouldThrowException_WhenTitleExists() {
        when(todoRepository.existsByTitle(todo.getTitle())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            toDoService.create(todo);
        });

        assertEquals("ToDo with title '" + todo.getTitle() + "' already exists", exception.getMessage());
        verify(todoRepository, times(1)).existsByTitle(todo.getTitle());
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    @Test
    @DisplayName("Should throw NullEntityReferenceException when todo is null")
    void create_ShouldThrowException_WhenTodoIsNull() {
        assertThrows(NullEntityReferenceException.class, () -> {
            toDoService.create(null);
        });

        verify(todoRepository, never()).existsByTitle(anyString());
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    // ==================== READ BY ID TESTS ====================

    @Test
    @DisplayName("Should return ToDo when found")
    void readById_ShouldReturnToDo_WhenExists() {
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));

        ToDo result = toDoService.readById(1L);

        assertNotNull(result);
        assertEquals(todo, result);
        verify(todoRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when ToDo not found")
    void readById_ShouldThrowException_WhenNotFound() {
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            toDoService.readById(999L);
        });

        verify(todoRepository, times(1)).findById(999L);
    }

    // ==================== ADD COLLABORATOR TESTS ====================

    @Test
    @DisplayName("Should successfully add collaborator to ToDo")
    void addCollaborator_ShouldAddUser_WhenValid() {
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(collaborator));
        when(todoRepository.existsByTitleAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(todoRepository.save(any(ToDo.class))).thenReturn(todo);

        toDoService.addCollaborator(1L, 2L);

        assertTrue(todo.getCollaborators().contains(collaborator));
        assertEquals(1, todo.getCollaborators().size());

        verify(todoRepository, times(2)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(todoRepository, times(1)).save(todo);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when ToDo not found while adding collaborator")
    void addCollaborator_ShouldThrowException_WhenToDoNotFound() {
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            toDoService.addCollaborator(999L, 2L);
        });

        verify(todoRepository, times(1)).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when User not found while adding collaborator")
    void addCollaborator_ShouldThrowException_WhenUserNotFound() {
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            toDoService.addCollaborator(1L, 999L);
        });

        verify(todoRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(999L);
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    // ==================== REMOVE COLLABORATOR TESTS ====================

    @Test
    @DisplayName("Should successfully remove collaborator from ToDo")
    void removeCollaborator_ShouldRemoveUser_WhenValid() {
        todo.getCollaborators().add(collaborator);

        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(collaborator));
        when(todoRepository.existsByTitleAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(todoRepository.save(any(ToDo.class))).thenReturn(todo);

        toDoService.removeCollaborator(1L, 2L);

        assertFalse(todo.getCollaborators().contains(collaborator));
        assertEquals(0, todo.getCollaborators().size());

        verify(todoRepository, times(2)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(todoRepository, times(1)).save(todo);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when ToDo not found while removing collaborator")
    void removeCollaborator_ShouldThrowException_WhenToDoNotFound() {
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            toDoService.removeCollaborator(999L, 2L);
        });

        verify(todoRepository, times(1)).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when User not found while removing collaborator")
    void removeCollaborator_ShouldThrowException_WhenUserNotFound() {
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            toDoService.removeCollaborator(1L, 999L);
        });

        verify(todoRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(999L);
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should successfully update ToDo when title is unique")
    void update_ShouldSaveToDo_WhenTitleIsUnique() {
        when(todoRepository.existsByTitleAndIdNot(todo.getTitle(), todo.getId())).thenReturn(false);
        when(todoRepository.findById(todo.getId())).thenReturn(Optional.of(todo));
        when(todoRepository.save(any(ToDo.class))).thenReturn(todo);

        ToDo result = toDoService.update(todo);

        assertNotNull(result);
        assertEquals(todo, result);
        verify(todoRepository, times(1)).existsByTitleAndIdNot(todo.getTitle(), todo.getId());
        verify(todoRepository, times(1)).findById(todo.getId());
        verify(todoRepository, times(1)).save(todo);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when title already exists during update")
    void update_ShouldThrowException_WhenTitleExists() {
        when(todoRepository.existsByTitleAndIdNot(todo.getTitle(), todo.getId())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            toDoService.update(todo);
        });

        assertEquals("ToDo with title '" + todo.getTitle() + "' already exists", exception.getMessage());
        verify(todoRepository, times(1)).existsByTitleAndIdNot(todo.getTitle(), todo.getId());
        verify(todoRepository, never()).findById(anyLong());
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    @Test
    @DisplayName("Should throw NullEntityReferenceException when todo is null during update")
    void update_ShouldThrowException_WhenTodoIsNull() {
        assertThrows(NullEntityReferenceException.class, () -> {
            toDoService.update(null);
        });

        verify(todoRepository, never()).existsByTitleAndIdNot(anyString(), anyLong());
        verify(todoRepository, never()).findById(anyLong());
        verify(todoRepository, never()).save(any(ToDo.class));
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete ToDo when exists")
    void delete_ShouldDeleteToDo_WhenExists() {
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        doNothing().when(todoRepository).delete(any(ToDo.class));

        toDoService.delete(1L);

        verify(todoRepository, times(1)).findById(1L);
        verify(todoRepository, times(1)).delete(todo);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent ToDo")
    void delete_ShouldThrowException_WhenNotFound() {
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            toDoService.delete(999L);
        });

        verify(todoRepository, times(1)).findById(999L);
        verify(todoRepository, never()).delete(any(ToDo.class));
    }
}
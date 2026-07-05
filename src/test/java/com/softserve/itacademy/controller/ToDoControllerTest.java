package com.softserve.itacademy.controller;

import com.softserve.itacademy.dto.todoDto.CreateToDoDto;
import com.softserve.itacademy.dto.todoDto.ToDoDtoConverter;
import com.softserve.itacademy.dto.todoDto.UpdateToDoDto;
import com.softserve.itacademy.model.Task;
import com.softserve.itacademy.model.ToDo;
import com.softserve.itacademy.model.User;
import com.softserve.itacademy.model.UserRole;
import com.softserve.itacademy.service.TaskService;
import com.softserve.itacademy.service.ToDoService;
import com.softserve.itacademy.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToDoController.class)
public class ToDoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToDoService todoService;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserService userService;

    @MockBean
    private ToDoDtoConverter todoDtoConverter;

    // ==================== CREATE FORM TESTS ====================

    @Test
    @DisplayName("Should show create ToDo form")
    void createToDoForm_ShouldReturnCreateView() throws Exception {
        mockMvc.perform(get("/todos/create/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-todo"))
                .andExpect(model().attributeExists("todo"))
                .andExpect(model().attribute("ownerId", 1L));
    }

    // ==================== CREATE TESTS ====================

    @Test
    @DisplayName("Should create ToDo and redirect when valid")
    void createToDo_ShouldRedirect_WhenValid() throws Exception {
        User owner = new User();
        owner.setId(1L);

        ToDo todo = new ToDo();
        todo.setId(1L);
        todo.setTitle("Test ToDo");
        todo.setOwner(owner);

        when(userService.readById(1L)).thenReturn(owner);
        when(todoDtoConverter.toEntity(any(CreateToDoDto.class), any(User.class))).thenReturn(todo);
        when(todoService.create(any(ToDo.class))).thenReturn(todo);

        mockMvc.perform(post("/todos/create/users/1")
                .param("title", "Test ToDo")
                .param("ownerId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos/all/users/1"));

        verify(userService, times(1)).readById(1L);
        verify(todoService, times(1)).create(any(ToDo.class));
    }

    @Test
    @DisplayName("Should return form with errors when validation fails")
    void createToDo_ShouldReturnFormWithErrors_WhenInvalid() throws Exception {
        mockMvc.perform(post("/todos/create/users/1")
                .param("title", "") // Empty - invalid
                .param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-todo"))
                .andExpect(model().attributeExists("todo"))
                .andExpect(model().attribute("ownerId", 1L))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Should return form with errors when title already exists")
    void createToDo_ShouldReturnFormWithErrors_WhenTitleExists() throws Exception {
        User owner = new User();
        owner.setId(1L);

        ToDo todo = new ToDo();
        todo.setTitle("Test ToDo");

        when(userService.readById(1L)).thenReturn(owner);
        when(todoDtoConverter.toEntity(any(CreateToDoDto.class), any(User.class))).thenReturn(todo);
        when(todoService.create(any(ToDo.class)))
                .thenThrow(new IllegalArgumentException("ToDo with title 'Test ToDo' already exists"));

        mockMvc.perform(post("/todos/create/users/1")
                .param("title", "Test ToDo")
                .param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-todo"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("todo", "title"));

        verify(todoService, times(1)).create(any(ToDo.class));
    }

    // ==================== UPDATE FORM TESTS ====================

    @Test
    @DisplayName("Should show update ToDo form")
    void updateToDoForm_ShouldReturnUpdateView() throws Exception {
        User owner = new User();
        owner.setId(1L);

        ToDo todo = new ToDo();
        todo.setId(1L);
        todo.setTitle("Test ToDo");
        todo.setOwner(owner);

        UpdateToDoDto updateDto = UpdateToDoDto.builder()
                .id(1L)
                .title("Test ToDo")
                .ownerId(1L)
                .build();

        when(todoService.readById(1L)).thenReturn(todo);

        mockMvc.perform(get("/todos/1/update/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("update-todo"))
                .andExpect(model().attributeExists("todo"));

        verify(todoService, times(1)).readById(1L);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update ToDo and redirect when valid")
    void updateToDo_ShouldRedirect_WhenValid() throws Exception {
        User owner = new User();
        owner.setId(1L);

        ToDo todo = new ToDo();
        todo.setId(1L);
        todo.setTitle("Updated ToDo");
        todo.setOwner(owner);

        when(todoService.readById(1L)).thenReturn(todo);
        when(userService.readById(1L)).thenReturn(owner);
        when(todoService.update(any(ToDo.class))).thenReturn(todo);

        mockMvc.perform(post("/todos/1/update/users/1")
                        .param("id", "1")  // <-- ДОБАВИТЬ ЭТО!
                        .param("title", "Updated ToDo")
                        .param("ownerId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos/all/users/1"));

        verify(todoService, times(1)).readById(1L);
        verify(userService, times(1)).readById(1L);
        verify(todoService, times(1)).update(any(ToDo.class));
    }

    @Test
    @DisplayName("Should return form with errors when validation fails during update")
    void updateToDo_ShouldReturnFormWithErrors_WhenInvalid() throws Exception {
        mockMvc.perform(post("/todos/1/update/users/1")
                .param("title", "") // Empty - invalid
                .param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("update-todo"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Should return form with errors when title already exists during update")
    void updateToDo_ShouldReturnFormWithErrors_WhenTitleExists() throws Exception {
        User owner = new User();
        owner.setId(1L);

        ToDo todo = new ToDo();
        todo.setId(1L);
        todo.setTitle("Updated ToDo");
        todo.setOwner(owner);

        when(todoService.readById(1L)).thenReturn(todo);
        when(userService.readById(1L)).thenReturn(owner);
        doThrow(new IllegalArgumentException("ToDo with title 'Updated ToDo' already exists"))
                .when(todoService).update(any(ToDo.class));

        mockMvc.perform(post("/todos/1/update/users/1")
                        .param("id", "1")  // <-- ДОБАВИТЬ ЭТО!
                        .param("title", "Updated ToDo")
                        .param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("update-todo"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("todo", "title"));

        verify(todoService, times(1)).update(any(ToDo.class));
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete ToDo and redirect")
    void deleteToDo_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/todos/1/delete/users/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos/all/users/1"));

        verify(todoService, times(1)).delete(1L);
    }

    // ==================== LIST TESTS ====================

    @Test
    @DisplayName("Should show all ToDos for user")
    void getAll_ShouldReturnTodosUserView() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");

        ToDo todo1 = new ToDo();
        todo1.setId(1L);
        todo1.setTitle("ToDo 1");
        todo1.setOwner(owner);

        ToDo todo2 = new ToDo();
        todo2.setId(2L);
        todo2.setTitle("ToDo 2");
        todo2.setOwner(owner);

        when(userService.readById(1L)).thenReturn(owner);
        when(todoService.getByUserId(1L)).thenReturn(Arrays.asList(todo1, todo2));

        mockMvc.perform(get("/todos/all/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("todos-user"))
                .andExpect(model().attributeExists("todos"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", owner));

        verify(todoService, times(1)).getByUserId(1L);
        verify(userService, times(1)).readById(1L);
    }

    // ==================== TASKS WITH COLLABORATORS TESTS ====================

    @Test
    @DisplayName("Should show tasks with potential collaborators")
    void getTasks_ShouldReturnTodoTasksView() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Doe");

        User collaborator1 = new User();
        collaborator1.setId(2L);
        collaborator1.setFirstName("Jane");
        collaborator1.setLastName("Smith");

        User collaborator2 = new User();
        collaborator2.setId(3L);
        collaborator2.setFirstName("Bob");
        collaborator2.setLastName("Johnson");

        User otherUser = new User();
        otherUser.setId(4L);
        otherUser.setFirstName("Alice");
        otherUser.setLastName("Brown");

        ToDo todo = new ToDo();
        todo.setId(1L);
        todo.setTitle("Test ToDo");
        todo.setOwner(owner);
        todo.setCollaborators(new HashSet<>(Arrays.asList(collaborator1, collaborator2)));
        todo.setTasks(new HashSet<>());

        when(todoService.readById(1L)).thenReturn(todo);
        when(userService.getAll()).thenReturn(Arrays.asList(owner, collaborator1, collaborator2, otherUser));

        mockMvc.perform(get("/todos/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo-tasks"))
                .andExpect(model().attributeExists("todo"))
                .andExpect(model().attributeExists("tasks"))
                .andExpect(model().attributeExists("users"));

        verify(todoService, times(1)).readById(1L);
        verify(userService, times(1)).getAll();
    }

    // ==================== ADD COLLABORATOR TESTS ====================

    @Test
    @DisplayName("Should add collaborator and redirect")
    void addCollaborator_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/todos/1/add")
                .param("user_id", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos/1/tasks"));

        verify(todoService, times(1)).addCollaborator(1L, 2L);
    }

    // ==================== REMOVE COLLABORATOR TESTS ====================

    @Test
    @DisplayName("Should remove collaborator and redirect")
    void removeCollaborator_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/todos/1/remove")
                .param("user_id", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos/1/tasks"));

        verify(todoService, times(1)).removeCollaborator(1L, 2L);
    }
}
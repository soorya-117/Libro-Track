package com.example.librotrack.controller;

import com.example.librotrack.entity.Book;
import com.example.librotrack.entity.IssueRecord;
import com.example.librotrack.service.LibraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    // 1. Add book
    @PostMapping("/books")
    public ResponseEntity<Book> addBook(@Valid @RequestBody Book book) {
        Book savedBook = libraryService.addBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
    }

    // 2. Update book
    @PutMapping("/books/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @Valid @RequestBody Book book) {
        Book updatedBook = libraryService.updateBook(id, book);
        return ResponseEntity.ok(updatedBook);
    }

    // 3. Search books
    @GetMapping("/books/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String keyword) {
        String searchKeyword = (query != null) ? query : keyword;
        List<Book> books = libraryService.searchBooks(title, author, category, searchKeyword);
        return ResponseEntity.ok(books);
    }

    // 4. Issue book (JSON payload)
    @PostMapping(value = "/issues", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IssueRecord> issueBookJson(@RequestBody Map<String, Object> body) {
        Long bookId = null;
        Long studentId = null;

        if (body != null) {
            if (body.get("bookId") != null) {
                bookId = Long.valueOf(body.get("bookId").toString());
            } else if (body.get("book") instanceof Map) {
                Object idObj = ((Map<?, ?>) body.get("book")).get("id");
                if (idObj != null) bookId = Long.valueOf(idObj.toString());
            }

            if (body.get("studentId") != null) {
                studentId = Long.valueOf(body.get("studentId").toString());
            } else if (body.get("student") instanceof Map) {
                Object idObj = ((Map<?, ?>) body.get("student")).get("id");
                if (idObj != null) studentId = Long.valueOf(idObj.toString());
            }
        }

        if (bookId == null || studentId == null) {
            throw new IllegalArgumentException("Both bookId and studentId must be provided");
        }

        IssueRecord record = libraryService.issueBook(bookId, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    // 4. Issue book (Query or form parameters)
    @PostMapping("/issues")
    public ResponseEntity<IssueRecord> issueBookParams(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long studentId) {
        if (bookId == null || studentId == null) {
            throw new IllegalArgumentException("Both bookId and studentId must be provided");
        }
        IssueRecord record = libraryService.issueBook(bookId, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    // 5. Return book
    @PutMapping("/issues/{id}/return")
    public ResponseEntity<IssueRecord> returnBook(@PathVariable Long id) {
        IssueRecord record = libraryService.returnBook(id);
        return ResponseEntity.ok(record);
    }

    // 6. Get currently issued books for a student
    @GetMapping("/students/{studentId}/issues")
    public ResponseEntity<List<IssueRecord>> getCurrentlyIssuedBooksForStudent(@PathVariable Long studentId) {
        List<IssueRecord> records = libraryService.getCurrentlyIssuedBooksForStudent(studentId);
        return ResponseEntity.ok(records);
    }

    // Simple error handling within controller
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}

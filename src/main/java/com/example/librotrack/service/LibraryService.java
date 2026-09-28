package com.example.librotrack.service;

import com.example.librotrack.entity.Book;
import com.example.librotrack.entity.IssueRecord;
import com.example.librotrack.entity.Student;
import com.example.librotrack.repository.BookRepository;
import com.example.librotrack.repository.IssueRecordRepository;
import com.example.librotrack.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LibraryService {

    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final IssueRecordRepository issueRecordRepository;

    public LibraryService(BookRepository bookRepository,
                          StudentRepository studentRepository,
                          IssueRecordRepository issueRecordRepository) {
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.issueRecordRepository = issueRecordRepository;
    }

    // 1. Add book
    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    // 2. Update book
    public Book updateBook(Long id, Book updatedBook) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        book.setTitle(updatedBook.getTitle());
        book.setAuthor(updatedBook.getAuthor());
        book.setIsbn(updatedBook.getIsbn());
        book.setCategory(updatedBook.getCategory());
        book.setCopies(updatedBook.getCopies());

        return bookRepository.save(book);
    }

    // 3. Issue book
    public IssueRecord issueBook(Long bookId, Long studentId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        // Business Rule 1: A book cannot be issued if all copies are already checked out (copies == 0)
        if (book.getCopies() <= 0) {
            throw new RuntimeException("Book cannot be issued: all copies are already checked out.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(14);

        // Decrease available copies by 1
        book.setCopies(book.getCopies() - 1);
        bookRepository.save(book);

        IssueRecord issueRecord = new IssueRecord();
        issueRecord.setBook(book);
        issueRecord.setStudent(student);
        issueRecord.setIssueDate(issueDate);
        issueRecord.setDueDate(dueDate);
        issueRecord.setReturnDate(null);
        issueRecord.setFine(0.0);

        return issueRecordRepository.save(issueRecord);
    }

    // 4. Return book and calculate fine
    public IssueRecord returnBook(Long issueRecordId) {
        IssueRecord issueRecord = issueRecordRepository.findById(issueRecordId)
                .orElseThrow(() -> new RuntimeException("Issue record not found with id: " + issueRecordId));

        // Make sure it has not already been returned
        if (issueRecord.getReturnDate() != null) {
            throw new RuntimeException("Book has already been returned on: " + issueRecord.getReturnDate());
        }

        LocalDate returnDate = LocalDate.now();
        double fine = 0.0;

        // Business Rule 2: Fine is charged per day after the due date (₹5 per overdue day)
        if (returnDate.isAfter(issueRecord.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(issueRecord.getDueDate(), returnDate);
            fine = overdueDays * 5.0;
        }

        // Increase available copies by 1
        Book book = issueRecord.getBook();
        book.setCopies(book.getCopies() + 1);
        bookRepository.save(book);

        issueRecord.setReturnDate(returnDate);
        issueRecord.setFine(fine);

        return issueRecordRepository.save(issueRecord);
    }

    // 5. Search books by title, author, or category
    public List<Book> searchBooks(String title, String author, String category, String query) {
        if (title != null && !title.trim().isEmpty()) {
            return bookRepository.findByTitleContainingIgnoreCase(title.trim());
        }
        if (author != null && !author.trim().isEmpty()) {
            return bookRepository.findByAuthorContainingIgnoreCase(author.trim());
        }
        if (category != null && !category.trim().isEmpty()) {
            return bookRepository.findByCategoryContainingIgnoreCase(category.trim());
        }
        if (query != null && !query.trim().isEmpty()) {
            return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                    query.trim(), query.trim(), query.trim());
        }
        return bookRepository.findAll();
    }

    // 6. List currently issued books for a student
    public List<IssueRecord> getCurrentlyIssuedBooksForStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new RuntimeException("Student not found with id: " + studentId);
        }
        return issueRecordRepository.findByStudentIdAndReturnDateIsNull(studentId);
    }
}

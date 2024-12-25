package ru.hh.school.unittesting.homework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserService userService;

    private LibraryManager libraryManager;

    @BeforeEach
    void setUp() {
        libraryManager = new LibraryManager(notificationService, userService);
    }

    @Test
    void checkingAddNewBookToInventory() {
        String bookId = "book1";
        libraryManager.addBook("book1", 5);
        int availableCopies = libraryManager.getAvailableCopies(bookId);

        assertEquals(5, availableCopies, "Количество копий новой книги должно быть равно добавленному количеству.");

    }

    @Test
    void checkingAddExistingBookToInventory() {
        String bookId = "book1";
        libraryManager.addBook(bookId, 5);
        libraryManager.addBook(bookId, 2);
        int availableCopies = libraryManager.getAvailableCopies(bookId);

        assertEquals(7, availableCopies, "Количество копий новой книги должно быть равно добавленному количеству.");

    }

    @Test
    void testBorrowBookUserIsNotActive() {
        when(userService.isUserActive("user1")).thenReturn(false);
        String bookId = "book1";
        String userId = "user1";
        libraryManager.addBook(bookId, 5);

        assertFalse(libraryManager.borrowBook(bookId, userId));
    }

    @Test
    void testBorrowBookCountBookMoreZero() {
        when(userService.isUserActive("user1")).thenReturn(true);
        String bookId = "book1";
        String userId = "user1";
        libraryManager.addBook(bookId, 0);
        userService.isUserActive(userId);

        assertFalse(libraryManager.borrowBook(bookId, userId));
    }

    @ParameterizedTest
    @CsvSource({
            "book1, user1, 5, 4",
            "book2, user1, 10, 9",
            "book1, user2, 50, 49",
    })
    void testBorrowBookCountBookDecreasesByOne(String bookId, String userID, Integer quantity, int expectedQuantity) {
        when(userService.isUserActive(userID)).thenReturn(true);
        libraryManager.addBook(bookId, quantity);
        libraryManager.borrowBook(bookId, userID);

        assertEquals(expectedQuantity, libraryManager.getAvailableCopies(bookId));
    }

    @Test
    void testReturnBookShouldSucceedWhenBookIsBorrowedByUser() {
        String bookId = "book1";
        String userId = "user1";
        libraryManager.addBook(bookId, 1);
        when(userService.isUserActive(userId)).thenReturn(true);
        libraryManager.borrowBook(bookId, userId);
        boolean success = libraryManager.returnBook(bookId, userId);

        assertTrue(success, "Пользователь должен успешно вернуть книгу.");
        assertEquals(1, libraryManager.getAvailableCopies(bookId), "Количество доступных копий должно увеличиться.");
        verify(notificationService).notifyUser(userId, "You have returned the book: " + bookId);
    }

    @Test
    void testReturnBookShouldFailWhenBookIsNotBorrowedByUser() {
        boolean success = libraryManager.returnBook("book1", "user1");

        assertFalse(success, "Пользователь не должен вернуть книгу, которую не брал.");
    }

    @Test
    void testReturnBookShouldFailWhenBookisNoSuchBook() {
        libraryManager.addBook("book1", 1);
        when(userService.isUserActive("user2")).thenReturn(true);
        libraryManager.borrowBook("book1", "user2");
        boolean success = libraryManager.returnBook("book1", "user1");

        assertFalse(success, "Пользователь не должен вернуть книгу, которую не брал.");
    }

    @Test
    void testCalculateDynamicLateForNonBestsellerNonPremium() {
        double fee = libraryManager.calculateDynamicLateFee(5, false, false);

        assertEquals(2.5, fee);
    }

    @Test
    void testCalculateDynamicLateFeeBestsellerMultiplier() {
        double fee = libraryManager.calculateDynamicLateFee(5, true, false);

        assertEquals(3.75, fee);
    }

    @Test
    void testCalculateDynamicLateFeePremiumDiscount() {
        double fee = libraryManager.calculateDynamicLateFee(5, false, true);

        assertEquals(2.0, fee);
    }

    @Test
    void testCalculateDynamicLateFeePremiumDiscountBestsellerMultiplier() {
        double fee = libraryManager.calculateDynamicLateFee(5, true, true);

        assertEquals(3.0, fee);
    }

    @Test
    void testCalculateDynamicLateFeeThrowExceptionForNegativeDays() {
        assertThrows(IllegalArgumentException.class, () -> libraryManager.calculateDynamicLateFee(-1, false, false));
    }

    @Test
    void testCalculateDynamicLateFeeThrowExceptionForZeroDays() {
        double fee = libraryManager.calculateDynamicLateFee(0, false, false);

        assertEquals(0, fee);
    }

}

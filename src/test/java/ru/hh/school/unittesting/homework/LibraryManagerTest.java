package ru.hh.school.unittesting.homework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    when(userService.isUserActive("1")).thenReturn(true);
    libraryManager.addBook("1", 3);
    libraryManager.borrowBook("1", "1");
  }

  @Test
  void testBorrowingBookWithInactiveUser() {
    when(userService.isUserActive("1")).thenReturn(false);

    boolean bookBorrowResult = libraryManager.borrowBook("1", "1");

    verify(notificationService, times(1)).notifyUser("1", "Your account is not active.");
    assertFalse(bookBorrowResult);
  }

  @Test
  void testUnavailableBookBorrowing() {
    boolean bookBorrowResult = libraryManager.borrowBook("0", "1");

    assertFalse(bookBorrowResult);
  }

  @Test
  void testSuccessfulBookBorrowing() {
    boolean bookBorrowResult = libraryManager.borrowBook("1", "1");

    verify(notificationService, times(2)).notifyUser("1", "You have borrowed the book: 1");
    assertEquals(1, libraryManager.getAvailableCopies("1"));
    assertTrue(bookBorrowResult);
  }

  @ParameterizedTest
  @CsvSource({"0, 1", "1, 0"})
  void testUnsuccessfulBookReturning(String bookId, String userId) {
    boolean bookReturnResult = libraryManager.returnBook(bookId, userId);

    assertFalse(bookReturnResult);
  }

  @Test
  void testSuccessfulBookReturning() {
    boolean bookReturnResult = libraryManager.returnBook("1", "1");

    assertEquals(3, libraryManager.getAvailableCopies("1"));
    verify(notificationService, times(1)).notifyUser("1", "You have returned the book: 1");
    assertTrue(bookReturnResult);
  }

  @Test
  void testDynamicLateFeeCalculationException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, false, false)
    );

    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "0, false, false, 0",
      "10, false, false, 5.0",
      "20, true, false, 15.0",
      "30, true, false, 22.5",
      "40, true, true, 24.0",
  })
  void testDynamicLateFeeCalculation(int overdueDays, boolean isBestseller, boolean isPremiumMember, double expectedLateFee) {
    double dynamicLateFeeResult = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);

    assertEquals(expectedLateFee, dynamicLateFeeResult);
  }
}

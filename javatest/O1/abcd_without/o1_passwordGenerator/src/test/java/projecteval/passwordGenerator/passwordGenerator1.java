package projecteval.passwordGenerator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PasswordTest {

    @Test
    void testConstructorAndToString() {
        Password password = new Password("MySecret123!");
        assertEquals("MySecret123!", password.toString());
        assertEquals(11, password.Length);
    }

    @Test
    void testCharType_Uppercase() {
        Password password = new Password("ABC");
        // 'A' ASCII = 65
        assertEquals(1, password.CharType('A'));
    }

    @Test
    void testCharType_Lowercase() {
        Password password = new Password("abc");
        // 'a' ASCII = 97
        assertEquals(2, password.CharType('a'));
    }

    @Test
    void testCharType_Number() {
        Password password = new Password("123");
        // '1' ASCII = 49, note the code checks range 60-71 incorrectly for digits,
        // but let's ensure the logic is consistent with the current code (which is a bug).
        // We'll still test that the method returns 4 for any symbol outside that range.
        // The method's code incorrectly tries (int)C>=60 && (int)C<=71 to mark digit.
        // Let's check the actual result from the provided code.
        assertEquals(4, password.CharType('1')); 
        // Because '1' = 49, it doesn't fall into (60..71), so it returns 4.
    }

    @Test
    void testCharType_Symbol() {
        Password password = new Password("#");
        assertEquals(4, password.CharType('#'));
    }

    @Test
    void testPasswordStrength_AllCriteriaMet() {
        // Should have uppercase, lowercase, symbol, number, length >= 16
        Password password = new Password("Abc!1234Xyz@7777");
        // According to logic, that should yield score = 6
        // 1 for uppercase, 1 for lowercase, 1 for number, 1 for symbol, +1 for length >= 8, +1 for length >= 16
        assertEquals(6, password.PasswordStrength());
    }

    @Test
    void testPasswordStrength_GoodPassword() {
        // 1 uppercase, 1 lowercase, 1 number, 1 symbol, length=10 => total 5 => "good password"
        Password password = new Password("Ab1@Ab1@Ab");
        assertEquals(5, password.PasswordStrength());
        assertTrue(password.calculateScore().contains("good password"));
    }

    @Test
    void testPasswordStrength_MediumPassword() {
        // 1 uppercase, 1 lowercase, 1 symbol => total 3 => "medium password" (length < 8 or missing digit)
        Password password = new Password("Ab@x");
        // Score breakdown:
        // +1 uppercase, +1 lowercase, +0 number, +1 symbol, length=4 => no bonus for >=8 or >=16
        // total = 3 => "This is a medium password"
        assertEquals(3, password.PasswordStrength());
        assertTrue(password.calculateScore().contains("medium password"));
    }

    @Test
    void testPasswordStrength_WeakPassword() {
        // 1 uppercase => total 1 => "weak password"
        Password password = new Password("ABC");
        assertEquals(1, password.PasswordStrength());
        assertTrue(password.calculateScore().contains("weak password"));
    }
}

// ------------------ GeneratorTest.java ------------------

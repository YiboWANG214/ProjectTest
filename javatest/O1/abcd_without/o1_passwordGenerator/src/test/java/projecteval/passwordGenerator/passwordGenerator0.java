package projecteval.passwordGenerator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AlphabetTest {

    @Test
    void testAlphabetAllIncluded() {
        Alphabet alphabet = new Alphabet(true, true, true, true);
        String result = alphabet.getAlphabet();
        assertTrue(result.contains(Alphabet.UPPERCASE_LETTERS));
        assertTrue(result.contains(Alphabet.LOWERCASE_LETTERS));
        assertTrue(result.contains(Alphabet.NUMBERS));
        assertTrue(result.contains(Alphabet.SYMBOLS));
    }

    @Test
    void testAlphabetUpperOnly() {
        Alphabet alphabet = new Alphabet(true, false, false, false);
        String result = alphabet.getAlphabet();
        assertEquals(Alphabet.UPPERCASE_LETTERS, result);
    }

    @Test
    void testAlphabetLowerOnly() {
        Alphabet alphabet = new Alphabet(false, true, false, false);
        String result = alphabet.getAlphabet();
        assertEquals(Alphabet.LOWERCASE_LETTERS, result);
    }

    @Test
    void testAlphabetNumbersOnly() {
        Alphabet alphabet = new Alphabet(false, false, true, false);
        String result = alphabet.getAlphabet();
        assertEquals(Alphabet.NUMBERS, result);
    }

    @Test
    void testAlphabetSymbolsOnly() {
        Alphabet alphabet = new Alphabet(false, false, false, true);
        String result = alphabet.getAlphabet();
        assertEquals(Alphabet.SYMBOLS, result);
    }

    @Test
    void testAlphabetEmpty() {
        Alphabet alphabet = new Alphabet(false, false, false, false);
        String result = alphabet.getAlphabet();
        assertEquals("", result);
    }
}

// ------------------ PasswordTest.java ------------------

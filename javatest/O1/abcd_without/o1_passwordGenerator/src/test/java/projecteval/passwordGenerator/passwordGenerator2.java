package projecteval.passwordGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.InjectMocks;

@ExtendWith(MockitoExtension.class)
class GeneratorTest {

    @Mock
    private Alphabet mockAlphabet;

    @Test
    void testConstructorUsingBooleans() {
        Generator generator = new Generator(true, false, false, false);
        assertEquals(Alphabet.UPPERCASE_LETTERS, generator.alphabet.getAlphabet());
    }

    @Test
    void testGeneratePasswordFixedLength() throws Exception {
        Alphabet realAlphabet = new Alphabet(true, true, true, true);
        Generator generator = new Generator(true, true, true, true);
        // We want to generate a password of length 5
        // Because of random usage, let's just check the length
        // and confirm it's from the chosen alphabet set
        Password p = invokeGeneratePassword(generator, 5);
        assertNotNull(p);
        assertEquals(5, p.Value.length());
        for (char c : p.Value.toCharArray()) {
            assertTrue(realAlphabet.getAlphabet().indexOf(c) >= 0);
        }
    }

    // We use a little trick here to invoke the private method by reflection
    private Password invokeGeneratePassword(Generator generator, int length) throws Exception {
        java.lang.reflect.Method method = Generator.class.getDeclaredMethod("GeneratePassword", int.class);
        method.setAccessible(true);
        return (Password) method.invoke(generator, length);
    }

    @Test
    void testMockAlphabet() throws Exception {
        // Suppose the mock alphabet returns "ABC" for getAlphabet()
        when(mockAlphabet.getAlphabet()).thenReturn("ABC");
        Generator g = new Generator(false, false, false, false);
        g.alphabet = mockAlphabet;
        Password p = invokeGeneratePassword(g, 2);

        // The password should be 2 chars, both from "ABC"
        assertEquals(2, p.Value.length());
        for (char c : p.Value.toCharArray()) {
            assertTrue("ABC".indexOf(c) >= 0);
        }

        // Verify we used the mock's getAlphabet at least once
        verify(mockAlphabet, atLeastOnce()).getAlphabet();
    }
}

// ------------------ MainTest.java ------------------

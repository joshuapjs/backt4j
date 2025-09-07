import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import com.backt4j.Main;

public class AppTest {
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @Test
    public void shouldAnswerWithTrue() {
        try {

            System.setOut(new PrintStream(outputStreamCaptor));

            String[] inputStrings = {"5010", "5011"};
            Main.main(inputStrings);

            String output = outputStreamCaptor.toString().trim();
            assertEquals(output.contains("00112233445566778899"), true);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            System.setOut(standardOut);
        }
    }

    
}

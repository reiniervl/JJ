package com.rvlstudio;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        try(FileReader reader = new FileReader("test.json")) {
			JJ.parse(reader);
		} catch(IOException e) {
			System.out.println(e);
		}
        assertTrue( true );
    }
}

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
				JJ.JSONObject jo = JJ.parse(reader);
				System.out.println(((JJ.JSONObject)jo.getMember("query").getValue().getValue()).getMember("count"));
				System.out.println(jo.<JJ.JSONObject>get("query").<Double>get("count"));
			} catch(IOException e) {
				System.out.println(e);
			}
			assertTrue( true );
    }
}

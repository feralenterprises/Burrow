package com.feralenterprises.hoverlite;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Screenshot-grounded examples and explicitly synthetic adversarial cases. */
public class ExpandedCorpusTest
{
    @Test public void actionAndNameMatrix() throws Exception
    {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("auto-shorten-cases.tsv");
        assertNotNull("Missing auto-shorten regression corpus", stream);
        int checked = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.isEmpty() || line.startsWith("id\t")) continue;
                String[] cols = line.split("\t", -1);
                assertEquals("Malformed test input: " + line, 6, cols.length);
                HoverCategory category = HoverCategory.valueOf(cols[1]);
                assertEquals(cols[0] + " | " + cols[5], cols[4],
                    HoverText.format(HoverMode.SMART_SHORTEN, category, cols[2], cols[3]));
                checked++;
            }
        }
        assertTrue("Regression corpus unexpectedly small", checked >= 180);
    }
}

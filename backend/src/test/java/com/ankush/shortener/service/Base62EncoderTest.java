package com.ankush.shortener.service;

import com.ankush.shortener.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void generatesCodesOfConfiguredLength() {
        Base62Encoder enc = new Base62Encoder(new AppProperties(
                "http://x", 7,
                new AppProperties.CorsProps("*"),
                new AppProperties.SafetyProps(true, 0.75, "", ""),
                new AppProperties.RateLimitProps(10, 10)
        ));
        String code = enc.next();
        assertEquals(7, code.length());
        assertTrue(code.matches("[0-9A-Za-z]+"));
    }

    @Test
    void codesAreDistinct() {
        Base62Encoder enc = new Base62Encoder(new AppProperties(
                "http://x", 8,
                new AppProperties.CorsProps("*"),
                new AppProperties.SafetyProps(true, 0.75, "", ""),
                new AppProperties.RateLimitProps(10, 10)
        ));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) seen.add(enc.next());
        assertEquals(1000, seen.size());
    }
}

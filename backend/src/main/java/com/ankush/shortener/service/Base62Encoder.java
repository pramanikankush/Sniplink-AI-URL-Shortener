package com.ankush.shortener.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Base62 short-code generator using a cryptographically-random byte source.
 * Output length is fixed (default 7) which gives ~62^7 ≈ 3.5e12 unique codes.
 *
 * Uses rejection sampling to avoid the modulo bias that would otherwise skew
 * the first few characters of the alphabet slightly upward.
 */
@Service
public class Base62Encoder {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    /** Largest multiple of ALPHABET.length that fits in a byte (62 * 4 = 248). */
    private static final int REJECT_THRESHOLD = 248;

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public Base62Encoder(com.ankush.shortener.config.AppProperties props) {
        this.length = Math.max(1, props.codeLength());
    }

    public String next() {
        StringBuilder sb = new StringBuilder(length);
        while (sb.length() < length) {
            int idx = random.nextInt(ALPHABET.length);
            // Rejection sampling would be needed if ALPHABET.length did not
            // divide 256 evenly. Since 62 doesn't divide 256, we use
            // nextInt(62) which is internally unbiased.
            sb.append(ALPHABET[idx]);
        }
        return sb.toString();
    }
}

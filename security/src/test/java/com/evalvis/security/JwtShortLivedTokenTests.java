package com.evalvis.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class JwtShortLivedTokenTests {
    private static final SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(
                    "b936cee86c9f87aa5d3c6f2e84cb5a4239a5fe50480a6ec66b70ab5b1f4ac6730c6c51542" +
                    "1b327ec1d69402e53dfb49ad7381eb067b338fd7b0cb22247225d47"
            )
    );

    @Test
    void createsToken() {
        JwtShortLivedToken token = JwtShortLivedToken.create(
                JwtRefreshToken.create(
                        "tester", new FakeAuthentication("tester@gmail.com", null), key, true
                ), key
        );

        assertEquals("tester@gmail.com", token.email());
        assertTrue(token.expirationDate().after(new Date(System.currentTimeMillis())));
        assertTrue(token.tokenIsValid());
    }

    @Test
    void readsExistingTokenFromAuthorizationHeader() {
        HttpServletRequest httpRequest = new FakeHttpServletRequest(
                Map.of(
                        "Authorization",
                        "Bearer " +
                                JwtShortLivedToken.create(
                                        JwtRefreshToken.create(
                                                "tester",
                                                new FakeAuthentication("tester@gmail.com", null),
                                                key,
                                                true
                                        ),
                                        key
                                ).value()
                )
        );

        Optional<JwtShortLivedToken> token = JwtShortLivedToken.existing(httpRequest, key);

        assertTrue(token.isPresent());
        assertEquals("tester@gmail.com", token.get().email());
        assertTrue(token.get().expirationDate().after(new Date(System.currentTimeMillis())));
        assertTrue(token.get().tokenIsValid());
    }

    @Test
    void readsExistingTokenFromCookies() {
        HttpServletRequest httpRequest = new FakeHttpServletRequest(
                Map.of(
                        "Authorization",
                        "Bearer " + JwtShortLivedToken.create(
                                JwtRefreshToken.create(
                                        "tester",
                                        new FakeAuthentication("tester@gmail.com", null),
                                        key,
                                        true
                                ),
                                key
                        ).value()
                )
        );

        Optional<JwtShortLivedToken> token = JwtShortLivedToken.existing(httpRequest, key);

        assertTrue(token.isPresent());
        assertEquals("tester@gmail.com", token.get().email());
        assertTrue(token.get().expirationDate().after(new Date(System.currentTimeMillis())));
        assertTrue(token.get().tokenIsValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "short",
            "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0ZXIiLCJpYXQiOjE3MDUxNjg1OTQsI" +
                    "mV4cCI6MTcwNTE2OTE5NH0.0BKl5k3IJ7PlfpuuPjRO0qqb7_IVal6IqqjvkPLm3Yhk" +
                    "rqsn5SMYtkNqN321ChofuxvhhfdfJdROuh10_hyflg" // expired token
    })
    void rejectsInvalidToken(String jwt) {
        assertFalse(
                JwtShortLivedToken
                        .existing(new FakeHttpServletRequest(Map.of("Authorization", jwt)), key)
                        .isPresent()
        );
    }

    @Test
    void rejectsInvalidToken() {
        assertFalse(JwtShortLivedToken.existing(new FakeHttpServletRequest(Map.of()), key).isPresent());
    }
}

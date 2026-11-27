package com.example.backend.features.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Tokens de restablecimiento de contraseña. */
@Service
public class PasswordResetTokenService extends TokensDeUnUso {

    public PasswordResetTokenService(StringRedisTemplate redis,
            @Value("${app.auth.reset-ttl:PT30M}") Duration vigencia) {
        super(redis, "auth:reset:", vigencia);
    }
}

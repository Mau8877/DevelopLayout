package com.example.backend.features.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Tokens de invitación: con ellos la persona invitada elige su contraseña. */
@Service
public class InvitacionTokenService extends TokensDeUnUso {

    public InvitacionTokenService(StringRedisTemplate redis,
            @Value("${app.auth.invite-ttl:PT24H}") Duration vigencia) {
        super(redis, "auth:invite:", vigencia);
    }
}

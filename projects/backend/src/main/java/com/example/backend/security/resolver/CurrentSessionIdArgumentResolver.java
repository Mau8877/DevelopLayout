package com.example.backend.security.resolver;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.features.auth.constant.AuthErrorCodes;
import com.example.backend.security.annotation.CurrentSessionId;

/** Resuelve @CurrentSessionId: el `sid` (codigo de la sesión) del token ya validado por el filtro. */
public class CurrentSessionIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentSessionId.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof UUID codigo) {
            return codigo;
        }
        throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_07, "common.error.noAutenticado");
    }
}

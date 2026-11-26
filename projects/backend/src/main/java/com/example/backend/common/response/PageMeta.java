package com.example.backend.common.response;

public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {

    /** `page` sale 1-based, como la recibe el cliente. */
    public static PageMeta de(org.springframework.data.domain.Page<?> pagina) {
        return new PageMeta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}

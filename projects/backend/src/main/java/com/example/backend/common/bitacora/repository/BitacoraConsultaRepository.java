package com.example.backend.common.bitacora.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.backend.common.bitacora.enums.ResultadoBitacora;
import com.example.backend.common.bitacora.model.Bitacora;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Lectura de la bitácora. Solo consulta.
 *
 * * No usa JpaSpecificationExecutor a propósito: trae `delete` y `update`.
 * ! RLS solo deja leer con el contexto de administrador de la transacción.
 */
@Repository
@RequiredArgsConstructor
public class BitacoraConsultaRepository {

    private final EntityManager entityManager;

    /** Filtros opcionales; `desde` y `hasta` incluyen sus extremos. */
    public record Filtro(String accion, String modulo, ResultadoBitacora resultado, UUID usuarioCodigo,
            Instant desde, Instant hasta) {
    }

    /** Del más reciente al más antiguo. */
    public Page<Bitacora> buscar(Filtro filtro, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Bitacora> raizConteo = conteo.from(Bitacora.class);
        conteo.select(cb.count(raizConteo)).where(condiciones(cb, raizConteo, filtro));
        long total = entityManager.createQuery(conteo).getSingleResult();

        CriteriaQuery<Bitacora> consulta = cb.createQuery(Bitacora.class);
        Root<Bitacora> raiz = consulta.from(Bitacora.class);
        consulta.select(raiz).where(condiciones(cb, raiz, filtro))
                .orderBy(cb.desc(raiz.get("createdAt")), cb.desc(raiz.get("id")));
        List<Bitacora> filas = entityManager.createQuery(consulta)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(filas, pageable, total);
    }

    private static Predicate[] condiciones(CriteriaBuilder cb, Root<Bitacora> raiz, Filtro f) {
        List<Predicate> condiciones = new ArrayList<>();
        if (f.accion() != null) {
            condiciones.add(cb.equal(raiz.get("accion"), f.accion()));
        }
        if (f.modulo() != null) {
            condiciones.add(cb.equal(raiz.get("modulo"), f.modulo()));
        }
        if (f.resultado() != null) {
            condiciones.add(cb.equal(raiz.get("resultado"), f.resultado()));
        }
        if (f.usuarioCodigo() != null) {
            condiciones.add(cb.equal(raiz.get("usuarioCodigo"), f.usuarioCodigo()));
        }
        if (f.desde() != null) {
            condiciones.add(cb.greaterThanOrEqualTo(raiz.<Instant>get("createdAt"), f.desde()));
        }
        if (f.hasta() != null) {
            condiciones.add(cb.lessThanOrEqualTo(raiz.<Instant>get("createdAt"), f.hasta()));
        }
        return condiciones.toArray(Predicate[]::new);
    }
}

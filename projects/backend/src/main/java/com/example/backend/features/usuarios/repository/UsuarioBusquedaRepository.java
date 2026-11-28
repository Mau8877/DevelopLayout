package com.example.backend.features.usuarios.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.backend.features.usuarios.enums.EstadoUsuario;
import com.example.backend.features.usuarios.model.Usuario;
import com.example.backend.features.usuarios.model.UsuarioRol;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Búsqueda de usuarios. Solo consulta.
 *
 * ! RLS solo muestra todas las filas con el contexto de administrador.
 */
@Repository
@RequiredArgsConstructor
public class UsuarioBusquedaRepository {

    private static final char ESCAPE = '\\';

    private final EntityManager entityManager;

    /** Criterios opcionales que se combinan con Y; `texto` es parte del correo. */
    public record Filtro(String texto, EstadoUsuario estado, String rol) {
        // ! Datos personales: un record los incluiría en su toString.
        @Override
        public String toString() {
            return "Filtro[oculto]";
        }
    }

    /** Del alta más reciente a la más antigua. */
    public Page<Usuario> buscar(Filtro filtro, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> conteo = cb.createQuery(Long.class);
        Root<Usuario> raizConteo = conteo.from(Usuario.class);
        conteo.select(cb.count(raizConteo)).where(condiciones(cb, conteo, raizConteo, filtro));
        long total = entityManager.createQuery(conteo).getSingleResult();

        CriteriaQuery<Usuario> consulta = cb.createQuery(Usuario.class);
        Root<Usuario> raiz = consulta.from(Usuario.class);
        consulta.select(raiz).where(condiciones(cb, consulta, raiz, filtro))
                .orderBy(cb.desc(raiz.get("createdAt")), cb.desc(raiz.get("id")));
        List<Usuario> filas = entityManager.createQuery(consulta)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(filas, pageable, total);
    }

    private static Predicate[] condiciones(CriteriaBuilder cb, CriteriaQuery<?> consulta, Root<Usuario> raiz,
            Filtro filtro) {
        List<Predicate> condiciones = new ArrayList<>();
        if (filtro.texto() != null) {
            condiciones.add(cb.like(cb.lower(raiz.get("email")),
                    "%" + literal(filtro.texto().toLowerCase(Locale.ROOT)) + "%", ESCAPE));
        }
        if (filtro.estado() != null) {
            condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
        }
        if (filtro.rol() != null) {
            Subquery<Long> conElRol = consulta.subquery(Long.class);
            Root<UsuarioRol> asignacion = conElRol.from(UsuarioRol.class);
            conElRol.select(asignacion.get("id")).where(
                    cb.equal(asignacion.get("usuario"), raiz),
                    cb.equal(asignacion.get("rol").get("nombre"), filtro.rol()),
                    cb.isTrue(asignacion.get("isActive")),
                    cb.isTrue(asignacion.get("rol").get("isActive")));
            condiciones.add(cb.exists(conElRol));
        }
        return condiciones.toArray(Predicate[]::new);
    }

    // * `%` y `_` que escriba la persona se buscan como texto.
    private static String literal(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

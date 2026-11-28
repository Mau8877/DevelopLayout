package com.example.backend.features.bitacora.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.bitacora.model.Bitacora;
import com.example.backend.common.bitacora.repository.BitacoraConsultaRepository;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.features.bitacora.constant.BitacoraErrorCodes;
import com.example.backend.features.bitacora.dto.BitacoraResponse;
import com.example.backend.security.RlsContext;

@Service
@RequiredArgsConstructor
public class BitacoraQueryService {

    private final BitacoraConsultaRepository repositorio;
    private final RlsContext rlsContext;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public Page<BitacoraResponse> consultar(BitacoraConsultaRepository.Filtro filtro, Pageable pageable) {
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw new ValidationException(BitacoraErrorCodes.ERR_BIT_01,
                    "bitacora.error.rangoFechas");
        }
        rlsContext.usarAdmin();
        return repositorio.buscar(filtro, pageable).map(BitacoraQueryService::aRespuesta);
    }

    private static BitacoraResponse aRespuesta(Bitacora b) {
        return new BitacoraResponse(b.getCodigo(), b.getCreatedAt(), b.getUsuarioCodigo(), b.getAccion(),
                b.getModulo(), b.getEntidad(), b.getEntidadCodigo(), b.getResultado(), b.getIp(), b.getDetalle());
    }
}

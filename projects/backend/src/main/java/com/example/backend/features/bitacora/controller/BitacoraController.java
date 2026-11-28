package com.example.backend.features.bitacora.controller;

import com.example.backend.common.idioma.Mensajes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.bitacora.enums.ResultadoBitacora;
import com.example.backend.common.bitacora.repository.BitacoraConsultaRepository;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageMeta;
import com.example.backend.common.util.Paginacion;
import com.example.backend.features.bitacora.dto.BitacoraResponse;
import com.example.backend.features.bitacora.service.BitacoraQueryService;

/** Consulta de la bitácora. Solo lectura; la autorización la hace el service. */
@RestController
@RequestMapping("/api/v1/audit-log")
@RequiredArgsConstructor
public class BitacoraController {

    private final BitacoraQueryService bitacora;
    private final Mensajes mensajes;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BitacoraResponse>>> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[action]", required = false) String accion,
            @RequestParam(name = "filter[module]", required = false) String modulo,
            @RequestParam(name = "filter[result]", required = false) ResultadoBitacora resultado,
            @RequestParam(name = "filter[userCode]", required = false) UUID usuarioCodigo,
            @RequestParam(name = "from", required = false) Instant desde,
            @RequestParam(name = "to", required = false) Instant hasta) {
        Page<BitacoraResponse> pagina = bitacora.consultar(
                new BitacoraConsultaRepository.Filtro(accion, modulo, resultado, usuarioCodigo, desde, hasta),
                Paginacion.de(page, pageSize));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), mensajes.de("bitacora.exito.obtenida"),
                PageMeta.de(pagina)));
    }
}

package com.example.backend.features.bitacora.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.bitacora.ResultadoBitacora;
import com.example.backend.common.bitacora.repository.BitacoraConsultaRepository;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.PageMeta;
import com.example.backend.common.util.Paginacion;
import com.example.backend.features.bitacora.dto.BitacoraResponse;
import com.example.backend.features.bitacora.service.BitacoraQueryService;

/** Consulta de la bitácora. Solo lectura; la autorización la hace el service. */
@RestController
@RequestMapping("/api/v1/bitacora")
public class BitacoraController {

    private final BitacoraQueryService bitacora;

    public BitacoraController(BitacoraQueryService bitacora) {
        this.bitacora = bitacora;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BitacoraResponse>>> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(name = "filter[accion]", required = false) String accion,
            @RequestParam(name = "filter[modulo]", required = false) String modulo,
            @RequestParam(name = "filter[resultado]", required = false) ResultadoBitacora resultado,
            @RequestParam(name = "filter[usuarioCodigo]", required = false) UUID usuarioCodigo,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta) {
        Page<BitacoraResponse> pagina = bitacora.consultar(
                new BitacoraConsultaRepository.Filtro(accion, modulo, resultado, usuarioCodigo, desde, hasta),
                Paginacion.de(page, pageSize));
        return ResponseEntity.ok(ApiResponse.successList(pagina.getContent(), "Bitácora obtenida",
                PageMeta.de(pagina)));
    }
}

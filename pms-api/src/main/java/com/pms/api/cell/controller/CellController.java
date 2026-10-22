package com.pms.api.cell.controller;

import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.cell.dto.CellAllocationResponse;
import com.pms.api.cell.dto.CellDetailResponse;
import com.pms.api.cell.dto.CellMoveResponse;
import com.pms.api.cell.dto.CellResponse;
import com.pms.api.cell.dto.CreateCellRequest;
import com.pms.api.cell.service.CellService;
import com.pms.api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "4. Cell allocation and custody", description = "Manage cell inventory and inmate cell allocation.")
public class CellController {

    private final CellService cellService;

    public CellController(CellService cellService) {
        this.cellService = cellService;
    }

    @PostMapping("/cells")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a cell")
    public CellResponse createCell(@Valid @RequestBody CreateCellRequest request) {
        return cellService.createCell(request);
    }

    @PostMapping("/inmates/{inmateId}/cell-allocation")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Allocate an inmate to a cell")
    public CellAllocationResponse allocate(@PathVariable UUID inmateId,
                                           @Valid @RequestBody AllocateCellRequest request) {
        return cellService.allocate(inmateId, request);
    }

    @PutMapping("/inmates/{inmateId}/cell-allocation")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "Move an inmate to a different cell")
    public CellMoveResponse move(@PathVariable UUID inmateId,
                                @Valid @RequestBody AllocateCellRequest request) {
        return cellService.move(inmateId, request);
    }

    @GetMapping("/cells")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List cells")
    public PageResponse<CellResponse> listCells(@RequestParam(required = false) String blockName,
                                                @RequestParam(required = false) String classification,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size,
                                                @RequestParam(required = false) List<String> sort) {
        return cellService.listCells(blockName, classification, page, size, sort);
    }

    @GetMapping("/cells/{cellId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Get a cell")
    public CellDetailResponse getCell(@PathVariable UUID cellId) {
        return cellService.getCell(cellId);
    }
}

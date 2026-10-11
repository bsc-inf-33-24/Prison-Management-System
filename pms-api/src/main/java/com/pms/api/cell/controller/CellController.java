package com.pms.api.cell.controller;

import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.cell.dto.CellAllocationResponse;
import com.pms.api.cell.dto.CellDetailResponse;
import com.pms.api.cell.dto.CellMoveResponse;
import com.pms.api.cell.dto.CellResponse;
import com.pms.api.cell.dto.CreateCellRequest;
import com.pms.api.cell.service.CellService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class CellController {

    private final CellService cellService;

    public CellController(CellService cellService) {
        this.cellService = cellService;
    }

    @PostMapping("/cells")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CellResponse createCell(@Valid @RequestBody CreateCellRequest request) {
        return cellService.createCell(request);
    }

    @PostMapping("/inmates/{inmateId}/cell-allocation")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CellAllocationResponse allocate(@PathVariable Long inmateId,
                                           @Valid @RequestBody AllocateCellRequest request) {
        return cellService.allocate(inmateId, request);
    }

    @PutMapping("/inmates/{inmateId}/cell-allocation")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public CellMoveResponse move(@PathVariable Long inmateId,
                                @Valid @RequestBody AllocateCellRequest request) {
        return cellService.move(inmateId, request);
    }

    @GetMapping("/cells")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    public List<CellResponse> listCells() {
        return cellService.listCells();
    }

    @GetMapping("/cells/{cellId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    public CellDetailResponse getCell(@PathVariable Long cellId) {
        return cellService.getCell(cellId);
    }
}

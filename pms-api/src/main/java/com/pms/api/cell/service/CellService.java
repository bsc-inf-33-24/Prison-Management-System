package com.pms.api.cell.service;

import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.cell.dto.CellAllocationResponse;
import com.pms.api.cell.dto.CellDetailResponse;
import com.pms.api.cell.dto.CellInmateSummary;
import com.pms.api.cell.dto.CellMoveResponse;
import com.pms.api.cell.dto.CellResponse;
import com.pms.api.cell.dto.CreateCellRequest;
import com.pms.api.cell.entity.Cell;
import com.pms.api.cell.entity.CellAllocation;
import com.pms.api.cell.repository.CellAllocationRepository;
import com.pms.api.cell.repository.CellRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CellService {

    private final CellRepository cellRepository;
    private final CellAllocationRepository cellAllocationRepository;
    private final InmateRepository inmateRepository;
    private final PaginationProperties paginationProperties;

    public CellService(CellRepository cellRepository,
                       CellAllocationRepository cellAllocationRepository,
                       InmateRepository inmateRepository,
                       PaginationProperties paginationProperties) {
        this.cellRepository = cellRepository;
        this.cellAllocationRepository = cellAllocationRepository;
        this.inmateRepository = inmateRepository;
        this.paginationProperties = paginationProperties;
    }

    @Audited
    @Transactional
    public CellResponse createCell(CreateCellRequest request) {
        Cell cell = new Cell(
                request.blockName().trim(),
                request.cellNumber(),
                request.capacity(),
                request.classification().trim()
        );
        Cell saved = cellRepository.saveAndFlush(cell);
        return CellResponse.from(saved, 0);
    }

    @Audited
    @Transactional
    public CellAllocationResponse allocate(Long inmateId, AllocateCellRequest request) {
        Inmate inmate = inmateRepository.findById(inmateId)
                .orElseThrow(InmateNotFoundException::new);
        Cell cell = cellRepository.findByIdForUpdate(request.cellId())
                .orElseThrow(CellNotFoundException::new);

        validateAllocation(inmate, cell);

        long occupancy = cellAllocationRepository.countByCellId(cell.getId());
        if (occupancy >= cell.getCapacity()) {
            throw new CellCapacityExceededException();
        }

        try {
            CellAllocation allocation = cellAllocationRepository.saveAndFlush(
                    new CellAllocation(inmate, cell, Instant.now())
            );
            int updatedOccupancy = Math.toIntExact(cellAllocationRepository.countByCellId(cell.getId()));
            int availableSpaces = Math.max(cell.getCapacity() - updatedOccupancy, 0);
            return CellAllocationResponse.from(allocation, updatedOccupancy, availableSpaces);
        } catch (DataIntegrityViolationException exception) {
            throw new CellAlreadyAllocatedException();
        }
    }

    @Audited
    @Transactional
    public CellMoveResponse move(Long inmateId, AllocateCellRequest request) {
        Inmate inmate = inmateRepository.findById(inmateId)
                .orElseThrow(InmateNotFoundException::new);

        CellAllocation currentAllocation = cellAllocationRepository.findByInmateId(inmateId)
                .orElseThrow(CellAllocationNotFoundException::new);

        Cell currentCell = currentAllocation.getCell();
        Cell targetCell = cellRepository.findByIdForUpdate(request.cellId())
                .orElseThrow(CellNotFoundException::new);

        if (currentCell.getId().equals(targetCell.getId())) {
            throw new CellAlreadyAssignedToSameCellException();
        }

        validateAllocation(inmate, targetCell);

        int currentCellOccupancy = Math.toIntExact(cellAllocationRepository.countByCellId(currentCell.getId()));
        int targetCellOccupancy = Math.toIntExact(cellAllocationRepository.countByCellId(targetCell.getId()));

        if (targetCellOccupancy >= targetCell.getCapacity()) {
            throw new CellCapacityExceededException();
        }

        currentAllocation.moveTo(targetCell, Instant.now());
        cellAllocationRepository.saveAndFlush(currentAllocation);

        return new CellMoveResponse(
                inmateId,
                currentCell.getId(),
                targetCell.getId(),
                currentAllocation.getAllocatedAt(),
                currentCellOccupancy - 1,
                targetCellOccupancy + 1
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CellResponse> listCells(String blockName, String classification,
                                                 Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "blockName", "cellNumber", "capacity"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        Specification<Cell> specification = Specification.where(null);
        if (blockName != null && !blockName.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("blockName"), blockName.trim()));
        }
        if (classification != null && !classification.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("classification"), classification.trim()));
        }

        var cells = cellRepository.findAll(specification, pageable);
        List<Long> cellIds = cells.getContent().stream().map(Cell::getId).toList();
        Map<Long, Long> occupancyByCellId = new HashMap<>();
        if (!cellIds.isEmpty()) {
            cellAllocationRepository.countByCellIds(cellIds)
                    .forEach(row -> occupancyByCellId.put((Long) row[0], (Long) row[1]));
        }
        return PageResponse.from(cells.map(cell ->
                CellResponse.from(cell, occupancyByCellId.getOrDefault(cell.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public CellDetailResponse getCell(Long cellId) {
        Cell cell = cellRepository.findById(cellId).orElseThrow(CellNotFoundException::new);
        int occupancy = Math.toIntExact(cellAllocationRepository.countByCellId(cellId));
        List<CellInmateSummary> inmates = cellAllocationRepository.findByCell_IdOrderByAllocatedAtAsc(cellId)
                .stream()
                .map(allocation -> new CellInmateSummary(
                        allocation.getInmate().getId(),
                        allocation.getInmate().getInmateNumber(),
                        allocation.getInmate().getFirstName(),
                        allocation.getInmate().getLastName()))
                .toList();
        return CellDetailResponse.from(cell, occupancy, inmates);
    }

    private void validateAllocation(Inmate inmate, Cell cell) {
        if (!inmate.getClassification().equalsIgnoreCase(cell.getClassification())) {
            throw new CellClassificationMismatchException();
        }
    }

    public static class CellNotFoundException extends RuntimeException {
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class CellAllocationNotFoundException extends RuntimeException {
    }

    public static class CellCapacityExceededException extends RuntimeException {
    }

    public static class CellClassificationMismatchException extends RuntimeException {
    }

    public static class CellAlreadyAllocatedException extends RuntimeException {
    }

    public static class CellAlreadyAssignedToSameCellException extends RuntimeException {
    }
}

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
import java.util.UUID;
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
                request.capacity()
        );
        Cell saved = cellRepository.saveAndFlush(cell);
        return CellResponse.from(saved, 0);
    }

    @Audited
    @Transactional
    public CellAllocationResponse allocate(UUID inmateId, AllocateCellRequest request) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateId)
                .orElseThrow(InmateNotFoundException::new);
        Cell cell = cellRepository.findByPublicUuidForUpdate(request.cellId())
                .orElseThrow(CellNotFoundException::new);

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
    public CellMoveResponse move(UUID inmateId, AllocateCellRequest request) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateId)
                .orElseThrow(InmateNotFoundException::new);

        CellAllocation currentAllocation = cellAllocationRepository.findByInmateId(inmate.getId())
                .orElseThrow(CellAllocationNotFoundException::new);

        Cell currentCell = currentAllocation.getCell();
        Cell targetCell = cellRepository.findByPublicUuidForUpdate(request.cellId())
                .orElseThrow(CellNotFoundException::new);

        if (currentCell.getId().equals(targetCell.getId())) {
            throw new CellAlreadyAssignedToSameCellException();
        }

        int currentCellOccupancy = Math.toIntExact(cellAllocationRepository.countByCellId(currentCell.getId()));
        int targetCellOccupancy = Math.toIntExact(cellAllocationRepository.countByCellId(targetCell.getId()));

        if (targetCellOccupancy >= targetCell.getCapacity()) {
            throw new CellCapacityExceededException();
        }

        currentAllocation.moveTo(targetCell, Instant.now());
        cellAllocationRepository.saveAndFlush(currentAllocation);

        return new CellMoveResponse(
                inmate.getPublicUuid(),
                currentCell.getPublicUuid(),
                targetCell.getPublicUuid(),
                currentAllocation.getAllocatedAt(),
                currentCellOccupancy - 1,
                targetCellOccupancy + 1
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CellResponse> listCells(String blockName,
                                                 Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "blockName", "cellNumber", "capacity"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        Specification<Cell> specification = Specification.unrestricted();
        if (blockName != null && !blockName.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("blockName"), blockName.trim()));
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
    public CellDetailResponse getCell(UUID cellId) {
        Cell cell = cellRepository.findByPublicUuid(cellId).orElseThrow(CellNotFoundException::new);
        int occupancy = Math.toIntExact(cellAllocationRepository.countByCellId(cell.getId()));
        List<CellInmateSummary> inmates = cellAllocationRepository.findByCell_IdOrderByAllocatedAtAsc(cell.getId())
                .stream()
                .map(allocation -> new CellInmateSummary(
                        allocation.getInmate().getPublicUuid(),
                        allocation.getInmate().getInmateNumber(),
                        allocation.getInmate().getFirstName(),
                        allocation.getInmate().getLastName()))
                .toList();
        return CellDetailResponse.from(cell, occupancy, inmates);
    }

    public static class CellNotFoundException extends RuntimeException {
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class CellAllocationNotFoundException extends RuntimeException {
    }

    public static class CellCapacityExceededException extends RuntimeException {
    }

    public static class CellAlreadyAllocatedException extends RuntimeException {
    }

    public static class CellAlreadyAssignedToSameCellException extends RuntimeException {
    }
}

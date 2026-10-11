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
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CellService {

    private final CellRepository cellRepository;
    private final CellAllocationRepository cellAllocationRepository;
    private final InmateRepository inmateRepository;

    public CellService(CellRepository cellRepository,
                       CellAllocationRepository cellAllocationRepository,
                       InmateRepository inmateRepository) {
        this.cellRepository = cellRepository;
        this.cellAllocationRepository = cellAllocationRepository;
        this.inmateRepository = inmateRepository;
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

        CellAllocation currentAllocation = cellAllocationRepository.findByInmate_Id(inmateId)
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
    public List<CellResponse> listCells() {
        return cellRepository.findAllByOrderByIdAsc().stream()
                .map(cell -> CellResponse.from(cell, cellAllocationRepository.countByCellId(cell.getId())))
                .toList();
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

package com.pms.api.prison.service;

import com.pms.api.cell.entity.Cell;
import com.pms.api.cell.repository.CellRepository;
import com.pms.api.cell.repository.CellAllocationRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.prison.dto.CreateBlockRequest;
import com.pms.api.prison.dto.CreatePrisonCellRequest;
import com.pms.api.prison.dto.CreatePrisonRequest;
import com.pms.api.prison.dto.PrisonDetailResponse;
import com.pms.api.prison.dto.PrisonListResponse;
import com.pms.api.prison.dto.PrisonResponse;
import com.pms.api.prison.dto.UpdatePrisonRequest;
import com.pms.api.prison.dto.UpdateBlockRequest;
import com.pms.api.prison.entity.Block;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.BlockRepository;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.security.PrisonAccessService;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrisonService {
    private final PrisonRepository prisons;
    private final BlockRepository blocks;
    private final CellRepository cells;
    private final CellAllocationRepository allocations;
    private final PaginationProperties pagination;
    private final PrisonAccessService prisonAccess;

    public PrisonService(PrisonRepository prisons, BlockRepository blocks, CellRepository cells,
                         CellAllocationRepository allocations, PaginationProperties pagination,
                         PrisonAccessService prisonAccess) {
        this.prisons=prisons; this.blocks=blocks; this.cells=cells; this.allocations=allocations; this.pagination=pagination;
        this.prisonAccess=prisonAccess;
    }

    @Audited @Transactional
    public PrisonDetailResponse create(CreatePrisonRequest request) {
        String code=generateCode();
        validateStructure(request.blocks());
        Prison prison;
        try {
            prison=prisons.saveAndFlush(new Prison(code, request.name().trim(), clean(request.location())));
            for (CreateBlockRequest blockRequest : request.blocks()) {
                Block block=blocks.saveAndFlush(new Block(prison, blockRequest.name().trim()));
                List<Cell> definitions=blockRequest.cells().stream()
                        .map(c -> new Cell(block, c.cellNumber(), c.capacity())).toList();
                cells.saveAllAndFlush(definitions);
            }
        } catch (DataIntegrityViolationException exception) { throw new DuplicatePrisonStructureException(); }
        return detail(prison);
    }

    @Transactional(readOnly = true)
    public PageResponse<PrisonListResponse> list(String search, Boolean active, Integer page, Integer size, List<String> sort) {
        Pageable pageable=PageableValidator.create(page,size,sort,Set.of("id","code","name","location","active","createdAt"),
                Sort.by(Sort.Order.asc("name")),pagination);
        Specification<Prison> spec=Specification.unrestricted();
        if (active != null) spec=spec.and((root,query,cb)->cb.equal(root.get("active"),active));
        if (search != null && !search.isBlank()) {
            String term="%"+escape(search.trim().toLowerCase(Locale.ROOT))+"%";
            spec=spec.and((root,query,cb)->cb.or(cb.like(cb.lower(root.get("name")),term,'!'),
                    cb.like(cb.lower(root.get("code")),term,'!'),cb.like(cb.lower(root.get("location")),term,'!')));
        }
        return PageResponse.from(prisons.findAll(spec,pageable).map(PrisonListResponse::from));
    }

    @Transactional(readOnly = true)
    public PrisonDetailResponse get(UUID id) { prisonAccess.requirePrison(id); return detail(getPrison(id)); }

    @Audited @Transactional
    public PrisonResponse update(UUID id, UpdatePrisonRequest request) {
        Prison prison=getPrison(id); prison.update(request.name().trim(),clean(request.location()));
        return PrisonResponse.from(prison);
    }

    @Audited @Transactional
    public PrisonDetailResponse addBlock(UUID prisonId, CreateBlockRequest request) {
        Prison prison=getPrison(prisonId);
        if (!prison.isActive()) throw new InactivePrisonException();
        validateStructure(List.of(request));
        String name=request.name().trim();
        if (blocks.existsByPrison_IdAndNameIgnoreCase(prison.getId(),name)) {
            throw new DuplicatePrisonStructureException();
        }
        try {
            Block block=blocks.saveAndFlush(new Block(prison,name));
            cells.saveAllAndFlush(request.cells().stream()
                    .map(cell -> new Cell(block,cell.cellNumber(),cell.capacity())).toList());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePrisonStructureException();
        }
        return detail(prison);
    }

    @Audited @Transactional
    public PrisonDetailResponse updateBlock(UUID prisonId, UUID blockId, UpdateBlockRequest request) {
        Prison prison=getPrison(prisonId);
        if (!prison.isActive()) throw new InactivePrisonException();
        Block block=blocks.findByPublicUuidAndPrison_PublicUuid(blockId,prisonId)
                .orElseThrow(BlockNotFoundException::new);
        if (request.name()==null && request.cells()==null) {
            throw new IllegalArgumentException("Provide a block name or cells to add.");
        }
        String name=clean(request.name());
        if (name!=null && !name.equalsIgnoreCase(block.getName())
                && blocks.existsByPrison_IdAndNameIgnoreCase(prison.getId(),name)) {
            throw new DuplicatePrisonStructureException();
        }
        if (request.cells()!=null) validateNewCells(block,request.cells());
        try {
            if (name!=null) block.rename(name);
            if (request.cells()!=null) {
                cells.saveAllAndFlush(request.cells().stream()
                        .map(cell -> new Cell(block,cell.cellNumber(),cell.capacity())).toList());
            }
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePrisonStructureException();
        }
        return detail(prison);
    }

    private PrisonDetailResponse detail(Prison prison) {
        List<Block> prisonBlocks=blocks.findByPrison_IdOrderByNameAsc(prison.getId());
        List<Long> blockIds=prisonBlocks.stream().map(Block::getId).toList();
        List<Cell> prisonCells=blockIds.isEmpty() ? List.of() : cells.findAllByBlockIds(blockIds);
        List<Long> cellIds=prisonCells.stream().map(Cell::getId).toList();
        java.util.Map<Long,Long> occupancy=new java.util.HashMap<>();
        if (!cellIds.isEmpty()) allocations.countByCellIds(cellIds).forEach(row->occupancy.put((Long)row[0],(Long)row[1]));
        return PrisonDetailResponse.from(prison,prisonBlocks,prisonCells,occupancy);
    }
    private String generateCode() {
        String code;
        do {
            code = "PR-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 29).toUpperCase(Locale.ROOT);
        } while (prisons.existsByCodeIgnoreCase(code));
        return code;
    }
    private Prison getPrison(UUID id) { return prisons.findByPublicUuid(id).orElseThrow(PrisonNotFoundException::new); }
    private static void validateStructure(List<CreateBlockRequest> definitions) {
        Set<String> names=new HashSet<>();
        for (CreateBlockRequest block : definitions) {
            if (!names.add(block.name().trim().toLowerCase(Locale.ROOT))) throw new DuplicatePrisonStructureException();
            Set<Integer> numbers=new HashSet<>();
            for (CreatePrisonCellRequest cell : block.cells()) if (!numbers.add(cell.cellNumber())) throw new DuplicatePrisonStructureException();
        }
    }
    private void validateNewCells(Block block, List<CreatePrisonCellRequest> definitions) {
        Set<Integer> numbers=block.getCells().stream().map(Cell::getCellNumber)
                .collect(java.util.stream.Collectors.toSet());
        for (CreatePrisonCellRequest cell : definitions) {
            if (!numbers.add(cell.cellNumber())) {
                throw new DuplicatePrisonStructureException();
            }
        }
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String escape(String value) { return value.replace("!","!!").replace("%","!%").replace("_","!_"); }
    public static class PrisonNotFoundException extends RuntimeException { }
    public static class PrisonCodeAlreadyExistsException extends RuntimeException { }
    public static class DuplicatePrisonStructureException extends RuntimeException { }
    public static class InactivePrisonException extends RuntimeException { }
    public static class BlockNotFoundException extends RuntimeException { }
}

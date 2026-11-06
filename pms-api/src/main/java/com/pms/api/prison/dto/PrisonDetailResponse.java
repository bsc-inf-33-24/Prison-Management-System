package com.pms.api.prison.dto;

import com.pms.api.cell.dto.CellResponse;
import com.pms.api.cell.entity.Cell;
import com.pms.api.prison.entity.Block;
import com.pms.api.prison.entity.Prison;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record PrisonDetailResponse(UUID id, String name, String location, boolean active,
                                   List<BlockDetail> blocks) {
    public record BlockDetail(UUID id, String name, List<CellResponse> cells) { }
    public static PrisonDetailResponse from(Prison p, List<Block> blocks, List<Cell> cells,
                                            Map<Long, Long> occupancy) {
        Map<Long, List<Cell>> cellsByBlock = cells.stream()
                .collect(Collectors.groupingBy(cell -> cell.getBlock().getId()));
        return new PrisonDetailResponse(p.getPublicUuid(), p.getName(), p.getLocation(), p.isActive(),
                blocks.stream().map(b -> new BlockDetail(b.getPublicUuid(), b.getName(),
                        cellsByBlock.getOrDefault(b.getId(), List.of()).stream()
                                .map(c -> CellResponse.from(c, occupancy.getOrDefault(c.getId(), 0L))).toList())).toList());
    }
}

package gg.feral.burrow.experience;

import java.util.List;

/** @param sortApplied may differ from the request. {@code note} then says why */
public record ChronicleBrowseResult(List<BossPreview> rows, int totalMatches, int page, int limit,
                                    ChronicleBrowseRequest.Sort sortApplied, String note) {
    public ChronicleBrowseResult { rows = List.copyOf(rows); }
}

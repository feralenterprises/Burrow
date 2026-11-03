package gg.feral.burrow.experience;

import gg.feral.burrow.core.BossRegistry;

/** @param group null for all groups. Raids only appear when {@code includeRaids} is true */
public record ChronicleBrowseRequest(String query, BossRegistry.Group group, boolean includeRaids, Sort sort, int page, int limit) {
    public enum Sort { A_TO_Z, Z_TO_A, DIFFICULTY }
    public static final int MAX_LIMIT = 100;
    public ChronicleBrowseRequest {
        query = query == null ? "" : query.trim();
        sort = sort == null ? Sort.A_TO_Z : sort;
        page = Math.max(0, page);
        limit = limit <= 0 ? 25 : Math.min(limit, MAX_LIMIT);
    }
    public static ChronicleBrowseRequest all() { return new ChronicleBrowseRequest("", null, true, Sort.A_TO_Z, 0, MAX_LIMIT); }
}

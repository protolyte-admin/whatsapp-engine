package com.whatsapp.engine.contacts.dto;

import java.util.List;

public record ContactUploadResponse(
        int totalRows,
        int created,
        int updated,
        int failed,
        List<RowError> errors
) {

    public record RowError(
            int rowNumber,
            String reason
    ) {
    }
}

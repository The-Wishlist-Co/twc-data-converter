package au.twc.core.product.dto;

import lombok.Data;

import java.util.List;

@Data
public class CSVImportResponseDTO {

    private List<ProductResponse> response;

}


package au.twc.core.product.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResponseData {

    private List<ProductResponse> productResponses;
}

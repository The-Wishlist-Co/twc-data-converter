package au.twc.core.product.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProductResponse {


    private Set<String> successData;

    private Set<String> failedData;

    private Set<String> successVariantData;

    private Set<String> failedVariantData;

    private Set<String> successPriceData;

    private Set<String> failedPriceData;
}

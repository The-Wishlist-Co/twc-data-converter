package au.twc.core.product.domain;

import lombok.Data;

@Data
public class ProductVariantResponse {
    private String id;
    private String gtin;

    private String description;

    private String productRef;
    private String baseProductRef;
    private String color;

    private String title;

    private String brandName;

    private Variance variance;


}

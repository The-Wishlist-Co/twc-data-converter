package au.twc.core.product.domain;

import lombok.Data;

@Data
public class Product {

    private String gtin;

    private String description;

    private String productRef;

    private String color;

    private String title;

    private String brandName;

}

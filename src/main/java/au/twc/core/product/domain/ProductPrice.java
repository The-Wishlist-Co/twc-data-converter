package au.twc.core.product.domain;

import lombok.Data;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Data
public class ProductPrice {

    private Boolean active;

    private String priceRef;

    private String currencyCode;

    private String price;

    private Boolean sale;

    private String salePrice;

    private String productVariantId;

}

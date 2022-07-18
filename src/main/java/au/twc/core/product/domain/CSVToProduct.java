package au.twc.core.product.domain;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component("csvToProduct")
@Data
public class CSVToProduct {

    @CsvBindByName(column = "gtin")
    private String gtin;

    @CsvBindByName(column = "description")
    private String description;

    @CsvBindByName(column = "price")
    private String price;

    @CsvBindByName(column = "productRef")
    private String productRef;

    @CsvBindByName(column = "size")
    private String size;

    @CsvBindByName(column = "color")
    private String color;

    @CsvBindByName(column = "title")
    private String title;

    @CsvBindByName(column = "brandName")
    private String brandName;

}

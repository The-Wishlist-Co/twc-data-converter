package au.twc.core.product.web.rest;

import au.twc.core.product.domain.CSVToProduct;
import au.twc.core.product.dto.CSVImportResponseDTO;
import au.twc.core.product.dto.ResponseData;
import au.twc.core.product.service.ProductService;
import com.opencsv.exceptions.CsvException;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
public class ProductResource {

    private final ProductService productService;

    public ProductResource(ProductService productService) {
        this.productService = productService;
    }


    /**
     * @param response
     * @return
     */
    @PostMapping(path = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import CSV file", description = "Product Import CSV")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Success", response = CSVImportResponseDTO.class),
            @ApiResponse(code = 401, message = "Unauthorized"), @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 400, message = "Bad Request")})
    public ResponseEntity<CSVImportResponseDTO> importCSV(@RequestPart("file") MultipartFile file, final HttpServletResponse response) throws IOException, CsvException {
        CSVImportResponseDTO productList = productService.importProductCSV(file);
        return new ResponseEntity(productList, HttpStatus.OK);
    }


}

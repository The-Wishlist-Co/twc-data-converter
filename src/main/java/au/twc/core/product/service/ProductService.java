package au.twc.core.product.service;

import au.twc.core.product.domain.CSVToProduct;
import au.twc.core.product.dto.CSVImportResponseDTO;
import au.twc.core.product.dto.ResponseData;
import com.opencsv.exceptions.CsvException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    CSVImportResponseDTO importProductCSV(MultipartFile file) throws IOException, CsvException;

}

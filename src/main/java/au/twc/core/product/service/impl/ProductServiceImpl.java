package au.twc.core.product.service.impl;

import au.twc.core.product.domain.*;
import au.twc.core.product.dto.CSVImportResponseDTO;
import au.twc.core.product.dto.ProductResponse;
import au.twc.core.product.service.ProductService;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private Gson gson;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public CSVImportResponseDTO importProductCSV(MultipartFile file) throws IOException, CsvException {
        CSVImportResponseDTO responseData = convertCSV(file);
        return responseData;
    }

    private CSVImportResponseDTO convertCSV(MultipartFile file) throws IOException, CsvException {
        RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
        Reader reader = getReader(file);
//        testSuperCSV(reader);
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(1).withCSVParser(rfc4180Parser) // Skiping firstline as it is header
                .build();
//        getData(file);


        List<CSVToProduct> productsList = csvReader.readAll().stream().map(data -> {
            CSVToProduct csvToProduct = new CSVToProduct();
            csvToProduct.setGtin(data[0]);
            csvToProduct.setDescription(data[1]);
            csvToProduct.setPrice(data[2]);
            csvToProduct.setProductRef(StringEscapeUtils.unescapeCsv(data[3]));
            csvToProduct.setSize(data[4]);
            csvToProduct.setColor(data[5]);
            csvToProduct.setTitle(data[6]);
            csvToProduct.setBrandName(data[7]);
            return csvToProduct;
        }).collect(Collectors.toList());
        Map<String, List<CSVToProduct>> map = productsList.stream()
                .collect(Collectors.groupingBy(CSVToProduct::getProductRef));
        CSVImportResponseDTO products = getProducts(map);
        return products;
    }

    private List<CSVToProduct> getData(MultipartFile file) throws IOException {
        List<CSVToProduct> products = new ArrayList<>();
        Reader reader = getReader(file);
        CsvToBean<CSVToProduct> csvToProductMappings = new CsvToBeanBuilder<CSVToProduct>(reader)
                .withType(CSVToProduct.class).withIgnoreLeadingWhiteSpace(true).build();
        if (Objects.nonNull(csvToProductMappings)) {
            Iterator<CSVToProduct> csvToProductIterator = csvToProductMappings.iterator();
            while (csvToProductIterator.hasNext()) {
                products.add(csvToProductIterator.next());
            }
        }

        return products;
    }

    public String numberConversion(double value) //Got here 6.743240136E7 or something..
    {
        DecimalFormat formatter;

        if (value - (int) value > 0.0)
            formatter = new DecimalFormat("0.00"); //Here you can also deal with rounding if you wish..
        else
            formatter = new DecimalFormat("0");

        return formatter.format(value);
    }

    private CSVImportResponseDTO getProducts(Map<String, List<CSVToProduct>> map) {
        CSVImportResponseDTO csvImportResponseDTO = new CSVImportResponseDTO();
        List<ProductResponse> successData = new ArrayList<>();
        List<ProductResponse> failedData = new ArrayList<>();

        ProductResponse responseData = new ProductResponse();
        Set<String> successSet = new HashSet<>();
        Set<String> failedSet = new HashSet<>();
        responseData.setSuccessVariantData(new HashSet<>());
        responseData.setFailedVariantData(new HashSet<>());
        responseData.setSuccessPriceData(new HashSet<>());
        responseData.setFailedPriceData(new HashSet<>());
        List<ProductVariant> finalProductVariants = new ArrayList<>();
        List<ProductPrice> finalProductPrices = new ArrayList<>();
        map.entrySet().stream().forEach(products -> {
            List<CSVToProduct> list = products.getValue();
            Boolean isProductNotExists = true;//getProductByRef(list.get(0).getProductRef());
            if (isProductNotExists) {
                Product prdt = new Product();
                BeanUtils.copyProperties(list.get(0), prdt);
                String response = saveProducts(prdt, successSet, failedSet);
                responseData.setSuccessData(successSet);
                responseData.setFailedData(failedSet);
            }
            getProductVariantsList(list, responseData);
            successData.add(responseData);
        });
        csvImportResponseDTO.setResponse(successData);
//        String productJson = gson.toJson(finalProducts);
//        String productVariantJson = gson.toJson(finalProductVariants);
//        String productPriceJson = gson.toJson(finalProductPrices);
//        System.out.println(productJson);
        return csvImportResponseDTO;
    }

    private List<ProductPrice> getProductPrice(List<CSVToProduct> products) {
        List<ProductPrice> productPrices = products.stream().map(product -> {
            ProductPrice productNew = new ProductPrice();
            productNew.setActive(Boolean.TRUE);
            productNew.setPriceRef(product.getProductRef() + "_" + product.getColor() + "_" + product.getSize());
            productNew.setPrice(product.getPrice());
            productNew.setSalePrice(product.getPrice());
            productNew.setProductVariantId(""); //future implementation
            productNew.setSale(Boolean.TRUE);
            return productNew;
        }).collect(Collectors.toList());
        return productPrices;
    }

    private void getProductVariantsList(List<CSVToProduct> products, ProductResponse responseData) {
        products.stream().forEach(product -> {
            ProductVariant productNew = new ProductVariant();
            productNew.setGtin(product.getGtin());
            productNew.setDescription(product.getDescription());
            productNew.setProductRef(product.getProductRef() + "_" + product.getColor() + "_" + product.getSize());
            productNew.setBaseProductRef(product.getProductRef());
            productNew.setColor(product.getColor());
            productNew.setTitle(product.getTitle());
            productNew.setBrandName(product.getBrandName());
            Variance variance = new Variance();
            variance.setColor(product.getColor());
            variance.setSize(product.getSize());
            productNew.setVariance(variance);
            ProductVariantResponse productVariant = saveProductVariants(productNew, responseData.getSuccessVariantData(), responseData.getFailedVariantData());
            if (StringUtils.isNotEmpty(productVariant.getId())) {
//                System.out.println(productVariant.getId());
                ProductPrice productPrice = new ProductPrice();
                productPrice.setActive(Boolean.TRUE);
                productPrice.setPriceRef(product.getProductRef() + "_" + product.getColor() + "_" + product.getSize());
                productPrice.setPrice(product.getPrice());
                productPrice.setSalePrice(product.getPrice());
                productPrice.setProductVariantId(productVariant.getId());
                productPrice.setCurrencyCode("AUD");
                productPrice.setSale(Boolean.TRUE);
                saveProductPrices(productPrice, responseData.getSuccessPriceData(), responseData.getFailedPriceData());
            }
        });
    }

    private Reader getReader(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        return new InputStreamReader(new BOMInputStream(fileInputStream), StandardCharsets.UTF_8);
    }

    private Reader getReader(MultipartFile file) throws IOException {
        return new InputStreamReader(new BOMInputStream(file.getInputStream()), StandardCharsets.UTF_8);
    }

    private String getToken() {
        String url = "https://auth.au-sandbox.thewishlist.io/auth/realms/sunils_electronics/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        Map<String, Object> map = new HashMap<>();
        map.put("grant_type", "client_credentials");
        map.put("client_id", "twc_admin");
        map.put("client_secret", "20Z7jL7.Fai.4Xw1i.8fyF");
        // build the request
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);
        // send POST request
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        String responseObject = response.getBody();
        AuthToken authToken = gson.fromJson(responseObject, AuthToken.class);
        return authToken.getToken_type() + " " + authToken.getAccess_token();
    }

    private String saveProducts(Product product, Set<String> successList, Set<String> failedList) {
        String url = "https://api.au-sandbox.thewishlist.io/services/productsvc/api/v2/products";
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.set("X-TWC-Tenant", "sunils_electronics");
        headers.set("Authorization", getToken());
        // build the request
        HttpEntity<Product> entity = new HttpEntity<>(product, headers);
        ResponseEntity<String> response = null;
        String responseObject = "";
        // send POST request
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            responseObject = "Failed";
            failedList.add(product.getProductRef() + " - " + e.getMessage());
        }
        if (null == response) {
            responseObject = "Failed";
            failedList.add(product.getProductRef() + " - No response");
        } else if (response.getStatusCode().is4xxClientError()) {
            responseObject = "Failed";
            failedList.add(product.getProductRef() + " - Bad Request");
        } else {
            responseObject = response.getBody();
            successList.add(product.getProductRef());
        }

//        AuthToken authToken = gson.fromJson(responseObject,AuthToken.class);
        return responseObject;
    }

    private ProductVariantResponse saveProductVariants(ProductVariant productVariant, Set<String> successList, Set<String> failedList) {
        String url = "https://api.au-sandbox.thewishlist.io/services/productsvc/api/v2/products/variants";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.set("X-TWC-Tenant", "sunils_electronics");
        headers.set("Authorization", getToken());
        // build the request
        HttpEntity<ProductVariant> entity = new HttpEntity<>(productVariant, headers);
        // send POST request
        ResponseEntity<String> response = null;
        String responseObject = "";
        // send POST request
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            responseObject = "Failed";
            failedList.add(productVariant.getProductRef() + " - " + e.getMessage());
        }
        if (null == response) {
            responseObject = "Failed";
            failedList.add(productVariant.getProductRef() + " - No response");
        } else if (response.getStatusCode().is4xxClientError()) {
            responseObject = "Failed";
            failedList.add(productVariant.getProductRef() + " - Bad Request");
        } else {
            responseObject = response.getBody();
            successList.add(productVariant.getProductRef());
        }
        ProductVariantResponse variant = new ProductVariantResponse();
        if (!"Failed".equalsIgnoreCase(responseObject))
            variant = gson.fromJson(responseObject, ProductVariantResponse.class);
        return variant;
    }

    private String saveProductPrices(ProductPrice productPrice, Set<String> successList, Set<String> failedList) {
        String url = "https://api.au-sandbox.thewishlist.io/services/pricesvc/api/prices?novalidate=true";
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.set("X-TWC-Tenant", "sunils_electronics");
        headers.set("Authorization", getToken());
        // build the request
        HttpEntity<ProductPrice> entity = new HttpEntity<>(productPrice, headers);
        ResponseEntity<String> response = null;
        String responseObject = "";
        // send POST request
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            responseObject = "Failed";
            failedList.add(productPrice.getPriceRef() + " - " + e.getMessage());
        }
        if (null == response) {
            responseObject = "Failed";
            failedList.add(productPrice.getPriceRef() + " - No response");
        } else if (response.getStatusCode().is4xxClientError()) {
            responseObject = "Failed";
            failedList.add(productPrice.getPriceRef() + " - Bad Request");
        } else {
            responseObject = response.getBody();
            successList.add(productPrice.getPriceRef());
        }
        return responseObject;
    }

    private Boolean getProductByRef(String productRef) {
        String url = "https://api.au-sandbox.thewishlist.io/services/productsvc/api/v2/products/" + productRef + "/byref";
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.set("X-TWC-Tenant", "sunils_electronics");
        headers.set("Authorization", getToken());
        // build the request
        HttpEntity<ProductPrice> entity = new HttpEntity<>(headers);
        Boolean result = false;
        // send POST request
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseObject = response.getBody();
        } catch (Exception e) {
            result = true;
        }
//        String res gson.fromJson(responseObject,AuthToken.class);
        return result;
    }

    public void testSuperCSV(Reader reader) {

        try (ICsvMapReader listReader = new CsvMapReader(reader, CsvPreference.STANDARD_PREFERENCE)) {
            //First Column is header names
            final String[] headers = listReader.getHeader(true);
            final CellProcessor[] processors = getProcessors();

            Map<String, Object> fieldsInCurrentRow;
            while ((fieldsInCurrentRow = listReader.read(headers, processors)) != null) {
                System.out.println(fieldsInCurrentRow);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the processors used for the examples.
     */
    private static CellProcessor[] getProcessors() {


        final CellProcessor[] processors = new CellProcessor[]{
                new NotNull(new ParseLong()), // CustomerId
                new NotNull(), // CustomerName
                new NotNull(),
                new NotNull(), // CustomerName
                new NotNull(),
                new NotNull(),
                new NotNull(),
                new NotNull()
        };
        return processors;
    }

}

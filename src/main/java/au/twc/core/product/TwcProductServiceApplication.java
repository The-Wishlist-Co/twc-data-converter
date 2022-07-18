package au.twc.core.product;

import au.twc.core.product.config.ObjectToUrlEncodedConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
public class TwcProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TwcProductServiceApplication.class, args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();
        restTemplate.getMessageConverters().add(new ObjectToUrlEncodedConverter(mapper));
        return restTemplate;
    }
}

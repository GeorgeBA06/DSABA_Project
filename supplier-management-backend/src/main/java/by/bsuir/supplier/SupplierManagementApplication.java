package by.bsuir.supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Model-stage application: exposes only Actuator; business API follows in next stage. */
@SpringBootApplication
public class SupplierManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupplierManagementApplication.class, args);
    }
}

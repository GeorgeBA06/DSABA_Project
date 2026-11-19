package by.bsuir.supplier.service.common;

/** A violated domain invariant. Controllers will map this to a 4xx response in the API stage. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

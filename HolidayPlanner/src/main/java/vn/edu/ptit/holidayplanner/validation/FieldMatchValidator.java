package vn.edu.ptit.holidayplanner.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Objects;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String first;
    private String second;
    private String message;

    @Override
    public void initialize(FieldMatch annotation) {
        first = annotation.first();
        second = annotation.second();
        message = annotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object firstValue = wrapper.getPropertyValue(first);
        Object secondValue = wrapper.getPropertyValue(second);
        if (Objects.equals(firstValue, secondValue)) return true;

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(second)
                .addConstraintViolation();
        return false;
    }
}

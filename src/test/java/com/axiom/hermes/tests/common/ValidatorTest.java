package com.axiom.hermes.tests.common;

import com.axiom.hermes.common.validation.Validator;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.AssertTrue;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class ValidatorTest {

    @Test
    public void checkMobileNumber() {
        try {
            String mobile;
            mobile = Validator.validateMobile("+770560049270");
            assertTrue(mobile.equals("+77056004927"));
            mobile = Validator.validateMobile("+7(705)6004927");
            assertTrue(mobile.equals("+77056004927"));
            mobile = Validator.validateMobile("+7 705 600 49 27");
            assertTrue(mobile.equals("+77056004927"));
            mobile = Validator.validateMobile("+7 (705) 600-49-27");
            assertTrue(mobile.equals("+77056004927"));
            mobile = Validator.validateMobile("+7(705) 600-49-27");
            assertTrue(mobile.equals("+77056004927"));
            mobile = Validator.validateMobile("+7705600-49-27");
            assertTrue(mobile.equals("+77056004927"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkBusinessID() {
        try {
            Validator.validateBusinessID("123456789");
            Validator.validateBusinessID("1234567890");
            Validator.validateBusinessID("123456789012");
            Validator.validateBusinessID("12345678901234");
            Validator.validateBusinessID(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

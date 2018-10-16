package com.laika.laika_yellow_book;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class Tests {
    InputValidation validation;
    DataLine data;
    @Before
    public void loadTestVariables() {
        validation = new InputValidation();
        data = new DataLine();
        validation.setData(data);
    }
    //input validation tests
    @Test
    public void cowNumber_Validation() {
        assertTrue("validation returns error for invalid cow number", validation.validate("0",0)=="Cow number cannot be negative or zero"&&validation.validate("-5",0)=="Cow number cannot be negative or zero"&&validation.validate("299",0)==""&&validation.validate("3.5",0)=="Invalid integer, please try again.");
    }
}

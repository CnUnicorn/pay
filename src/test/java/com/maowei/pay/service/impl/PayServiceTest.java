package com.maowei.pay.service.impl;

import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.maowei.pay.PayApplicationTests;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class PayServiceTest extends PayApplicationTests {

    @Autowired
    private PayService payService;

    @Test
    public void create() {
        //new BigDecimal("0.01"),不要用new BigDecimal(0.01)
        payService.create("876459874553", BigDecimal.valueOf(0.01), BestPayTypeEnum.WXPAY_NATIVE);
    }
}
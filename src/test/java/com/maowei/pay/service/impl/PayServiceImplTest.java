package com.maowei.pay.service.impl;

import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.maowei.pay.PayApplicationTests;
import org.junit.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class PayServiceImplTest extends PayApplicationTests {

    @Autowired
    private PayServiceImpl payServiceImpl;

    @Autowired
    private AmqpTemplate amqpTemplate; // amqp是一种协议，很多中间件都实现了这个西医，RabbitMQ只是其中的一种

    @Test
    public void create() {
        //new BigDecimal("0.01"),不要用new BigDecimal(0.01)
        payServiceImpl.create("876459874553", BigDecimal.valueOf(0.01), BestPayTypeEnum.WXPAY_NATIVE);
    }

    @Test
    public void sendMQMsg() {
        amqpTemplate.convertAndSend("payNotify", "hello");
    }
}
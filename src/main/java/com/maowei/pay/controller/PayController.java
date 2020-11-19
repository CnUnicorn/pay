package com.maowei.pay.controller;

import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayResponse;
import com.maowei.pay.pojo.PayInfo;
import com.maowei.pay.service.impl.PayServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/pay")
public class PayController {

    private static final Logger logger = LoggerFactory.getLogger(PayController.class);

    @Autowired
    private PayServiceImpl payServiceImpl;

    @Autowired
    private WxPayConfig wxPayConfig;

    @GetMapping("/create")
    public ModelAndView create(@RequestParam("orderId") String orderId,
                         @RequestParam("amount") BigDecimal amount,
                         @RequestParam("payType")BestPayTypeEnum bestPayTypeEnum) {
        PayResponse response = payServiceImpl.create(orderId, amount, bestPayTypeEnum); // 发起请求

        Map<String, String> map = new HashMap<>();
        // 支付方式不同，渲染就不同。
        if (bestPayTypeEnum == BestPayTypeEnum.WXPAY_NATIVE) {
            map.put("codeUrl", response.getCodeUrl()); // 微信WXPAY_NATIVE，使用codeUrl；
            map.put("orderId", orderId);
            map.put("returnUrl", wxPayConfig.getReturnUrl());
            return new ModelAndView("createForWxPayNative", map);
        }else if (bestPayTypeEnum == BestPayTypeEnum.ALIPAY_PC) {
            map.put("body", response.getBody()); // 支付宝ALIPAY_PC，使用返回的body字段
            return new ModelAndView("createForAlipayPc", map);

        }

        throw new RuntimeException("暂不支持的支付类型"); // 如果采用的不是这两种支付方式，就抛出异常
    }

    @PostMapping("/notify")
    @ResponseBody  // 如果校验正确，说明订单支付成功，向微信发送消息不再通知
    public String asyncNotify(@RequestBody String notifyData) {
        return payServiceImpl.asyncNotify(notifyData);
    }

    @GetMapping("/queryByOrderId")
    @ResponseBody
    public PayInfo queryByOrderId(@RequestParam("orderId") String orderId) {
        logger.info("查询支付记录...");
        return payServiceImpl.queryByOrderId(orderId);
    }



}

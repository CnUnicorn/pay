package com.maowei.pay.service;

import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayResponse;
import com.maowei.pay.pojo.PayInfo;

import java.math.BigDecimal;

public interface IPayService {

    /**
     * 创建/发起支付
     * @param orderId 订单号
     * @param amount 订单价格
     */
    PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum);

    /**
     * 异步通知处理
     * @param notifyData 微信返回的异步通知数据
     */
    String asyncNotify(String notifyData);

    /**
     * 通过订单号查询订单信息
     */
    PayInfo queryByOrderId(String orderId);
}

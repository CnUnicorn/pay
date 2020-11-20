package com.maowei.pay.service.impl;

import com.google.gson.Gson;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import com.maowei.pay.dao.PayInfoMapper;
import com.maowei.pay.enums.PayPlatformEnum;
import com.maowei.pay.pojo.PayInfo;
import com.maowei.pay.service.IPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

//@Slf4j // 使用一个名为log的日志对象（log4j）
@Service // 表示是一个service组件，在IoC容器中注册bean
public class PayServiceImpl implements IPayService {
    // slf4j
    private  final Logger logger = LoggerFactory.getLogger(PayServiceImpl.class);

    private final static String QUEUE_PAY_NOTIFY = "payNotify";

    @Autowired
    private BestPayService bestPayService;  // 注入bean，所有方法复用bestPayService，不用重复new

    @Autowired
    private PayInfoMapper payInfoMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 创建新的订单并发起支付请求
     * @param orderId 订单号
     * @param amount 订单价格
     * @return
     */
    @Override
    public PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum) {
        // 订单写入数据库
        PayInfo payInfo = new PayInfo(Long.parseLong(orderId),
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(),
                OrderStatusEnum.NOTPAY.name(),
                amount);
        payInfoMapper.insertSelective(payInfo);

        PayRequest request = new PayRequest();         // 设置请求参数
        request.setOrderName("9000772-最好的支付SDK");
        request.setOrderId(orderId);
        request.setOrderAmount(amount.doubleValue());
        request.setPayTypeEnum(bestPayTypeEnum);

        PayResponse response = bestPayService.pay(request);  // 发起请求，返回预支付订单信息
        logger.info("发起支付Response={}", response);

        return response;
    }

    /**
     * 校验微信异步通知
     * @param notifyData 微信返回的异步通知数据
     */
    @Override
    public String asyncNotify(String notifyData) {
        // 1. 签名校验
        PayResponse response = bestPayService.asyncNotify(notifyData);
        logger.info("异步通知Response={}", response);

        // 2. 金额校验（通过订单号从数据库查询订单）
        // 从本地数据库中查出订单信息，和异步通知中的内容比较
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(response.getOrderId()));
        if (payInfo == null) {
            // 比较严重的情况，不应该查不到数据，需要告警：钉钉、微信
            throw new RuntimeException("通过orderNo查询到的订单信息为null");
        }

        // 如果支付状态不是支付成功，还未支付
        // 如果支付状态成功，就直接通知微信支付宝不要再发送异步通知了
        if (!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS.name())) {
            // 判断支付平台的支付金额和数据库的金额是否相同
            // Double类型由于精度的问题不好比较，所以使用BigDecimal类型
            if (payInfo.getPayAmount().compareTo(BigDecimal.valueOf(response.getOrderAmount())) != 0) {
                // 如果不相同，告警
                throw new RuntimeException("异步通知中的金额数据和数据库里的不一致,orderNo=" + response.getOrderId());
            }
            // 3. 金额相同，修改订单状态（是否完成支付）
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            payInfo.setPlatformNumber(response.getOutTradeNo()); // 设置支付平台流水号
//            payInfo.setUpdateTime(null); // 如果sql中有更新时间的语句，则需要将时间设置成null，以便mysql自动更新时间
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        // TODO pay发送MQ消息，mall接收MQ消息
        // 第一个参数是队列名称，第二个参数是需要发送的内容
        amqpTemplate.convertAndSend(QUEUE_PAY_NOTIFY, new Gson().toJson(payInfo));

        // 商户确认交易成功后返回给微信和支付宝的结束信息不同。
        if (response.getPayPlatformEnum() == BestPayPlatformEnum.WX) {
            // 4. 告诉微信不再通知
            return "<xml>\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
        }else if (response.getPayPlatformEnum() == BestPayPlatformEnum.ALIPAY) {
            // 4. 告诉支付宝不再通知
            return "success";
        }
        // 如果异步通知的请求平台既不是支付宝也不是微信，抛出此异常
        throw new RuntimeException("错误的支付平台");
    }

    @Override
    public PayInfo queryByOrderId(String orderId) {
        return payInfoMapper.selectByOrderNo(Long.parseLong(orderId));
    }
}

package com.inspiration_work.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspiration_work.web.domain.ResponseResult;
import com.inspiration_work.web.domain.SysPaymentType;
import com.inspiration_work.web.service.ISysPaymentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author RoyDowney
 * @date 2024/8/29
 */
@RestController
@RequestMapping("/payment")
public class SysPaymentController {

    @Resource
    private ISysPaymentService paymentService;

    /**
     * 分页查询PaymentType，条件 payId ，payName
     * @return
     */
    @GetMapping("/listPage/{pageNum}/{pageSize}")
    public IPage<SysPaymentType> list(@PathVariable("pageNum") Integer pageNum
            , @PathVariable("pageSize") Integer pageSize
            , SysPaymentType paymentType){

        QueryWrapper<SysPaymentType> queryWrapper = new QueryWrapper<>();
        //判断支付类型名称 不为空，则进行条件查询
        queryWrapper.lambda()
                .like(Objects.nonNull(paymentType.getPayName()),SysPaymentType::getPayName,paymentType.getPayName())
                .eq(Objects.nonNull(paymentType.getPayId()),SysPaymentType::getPayId,paymentType.getPayId());
        return paymentService.page(new Page<>(pageNum,pageSize),queryWrapper);

    }

    /**
     * 根据支付Id查询支付类型
     * @param payId
     * @return
     */
    @GetMapping("/findById/{payId}")
    public SysPaymentType findById(@PathVariable("payId") Long payId){
        return paymentService.getById(payId);
    }

    /**
     * 根据Id进行删除
     * @param payId
     * @return
     */
    @DeleteMapping("/removeById/{payId}")
    public ResponseResult removeById(@PathVariable("payId") Long payId){
        return ResponseResult.success(paymentService.removeById(payId));
    }
}

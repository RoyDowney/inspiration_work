# 2024 0829
# 创建sys_payment_type 支付类型表
CREATE TABLE `sys_payment_type`  (
                                     `pay_id` bigint(20) NOT NULL COMMENT '支付类型主键',
                                     `pay_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付类型名称',
                                     `remark` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
                                     PRIMARY KEY (`pay_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

# 插入数据
INSERT INTO `sys_payment_type` (`pay_id`, `pay_name`, `remark`) VALUES (1, '支付宝', '使用支付宝进行支付');
INSERT INTO `sys_payment_type` (`pay_id`, `pay_name`, `remark`) VALUES (2, '微信支付', '使用微信支付进行支付');
INSERT INTO `sys_payment_type` (`pay_id`, `pay_name`, `remark`) VALUES (3, '银行转账', '通过银行转账进行支付');


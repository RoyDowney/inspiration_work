package com.inspiration_work.listener;

import cn.hutool.core.collection.CollectionUtil;
import com.inspiration_work.model.CuEnterpriseFinancialReportDetail;
import com.inspiration_work.model.NewAppDic;
import com.inspiration_work.model.vo.FinancialTargetDataVo;
import org.springframework.stereotype.Component;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RoyDowney
 * @date 2025/2/10
 */

@Component
public class FinancialTarget {

    public List<FinancialTargetDataVo> financialTargetData(List<CuEnterpriseFinancialReportDetail> financialReportDetails) {
        List<FinancialTargetDataVo> result = new ArrayList<>();

        Map<String, CuEnterpriseFinancialReportDetail> reportDetailMap = financialReportDetails.stream()
                .filter(detail -> detail.getDicMkey() != null)
                .collect(Collectors.toMap(CuEnterpriseFinancialReportDetail::getDicMkey, detail -> detail));

        // 按照记录单条查询
        List<NewAppDic> accountReportAllDic = this.findDicListByLike("accountreport_");
        if (CollectionUtil.isEmpty(accountReportAllDic)) {
            return result;
        }
        // pkey 去重
        List<String> pkeyList = accountReportAllDic.stream().map(NewAppDic::getPkey).distinct().collect(Collectors.toList());

        // mkeyList
        Map<String, List<NewAppDic>> dicMap = accountReportAllDic.stream().collect(Collectors.groupingBy(NewAppDic::getMkey));

        // 合并 pkey 下所有的字典数据
        Map<String, List<NewAppDic>> pkeyMap = new HashMap<>();
        pkeyList.forEach(pkey -> {
            dicMap.forEach((mkey, dicList) -> {
                if (mkey.contains(pkey)) {
                    pkeyMap.merge(pkey, dicList, (existingList, newList) -> {
                        existingList.addAll(newList);
                        return existingList;
                    });
                }
            });
        });

        try {
            // 流动资产
            BigDecimal currentAssets = getSumByPkeyResult(pkeyMap.get("accountreport_balancesheet_currentassets"), reportDetailMap);

            // 流动负债
            BigDecimal currentLiabilities = getSumByPkeyResult(pkeyMap.get("accountreport_balancesheet_currentliabilities"), reportDetailMap);

            // 存货
            BigDecimal inventory = getEndMoneyFromReportDetail(reportDetailMap, "accountreport_balancesheet_currentassets_stock");

            // 总资产 = 流动资产 + 非流动资产
            BigDecimal totalAssets = BigDecimal.ZERO;
            // 非流动资产
            BigDecimal nonCurrentAssets = getSumByPkeyResult(pkeyMap.get("accountreport_balancesheet_noncurrentassets"), reportDetailMap);
            totalAssets = currentAssets.add(nonCurrentAssets);

            // 总负债 = 流动负债 + 非流动负债
            BigDecimal totalLiabilities = BigDecimal.ZERO;
            // 非流动负债
            BigDecimal nonCurrentLiabilities = getSumByPkeyResult(pkeyMap.get("accountreport_balancesheet_noncurrentliabilities"), reportDetailMap);
            totalLiabilities = currentLiabilities.add(nonCurrentLiabilities);

            // 所有者权益
            BigDecimal equity = getSumByPkeyResult(pkeyMap.get("accountreport_balancesheet_ownersequity"), reportDetailMap);

            // 利息收入
            BigDecimal interestIncome = getEndMoneyFromReportDetail(reportDetailMap, "accountreport_incomestatement_totaloperatingcost_financialexpenses_interestincome");

            // 利息支出
            BigDecimal interestExpense = getEndMoneyFromReportDetail(reportDetailMap, "accountreport_incomestatement_totaloperatingcost_interestexpenses");

            // 利息费用
            BigDecimal interestCost = getEndMoneyFromReportDetail(reportDetailMap, "accountreport_incomestatement_totaloperatingcost_financialexpenses_interestexpenses");

            // 利润总额
            BigDecimal totalProfit = getSumByPkeyResult(pkeyMap.get("accountreport_incomestatement_totalprofit"), reportDetailMap);

            // 营业利润
            BigDecimal operatingIncome = getEndMoneyFromReportDetail(reportDetailMap, "accountreport_incomestatement_operatingprofit_operatingprofitchild");

            // 净利润
            BigDecimal netProfit = getEndMoneyFromReportDetail(reportDetailMap, "accountreport_incomestatement_netprofit");

            // 净资产 = 总资产 - 总负债
            BigDecimal netAssets = totalAssets.subtract(totalLiabilities);

            // 主营业务收入
            BigDecimal mainBusinessIncome = getSumByPkeyResult(pkeyMap.get("accountreport_incomestatement_totaloperatingrevenue"), reportDetailMap);

            // 主营业务成本
            BigDecimal mainBusinessCost = getSumByPkeyResult(pkeyMap.get("accountreport_incomestatement_totaloperatingcost"), reportDetailMap);

            // 营业费用
            BigDecimal operatingExpenses = getEndMoneyFromReportDetail(reportDetailMap, "operatingExpenses");

            // 管理费用
            BigDecimal managementExpenses = getEndMoneyFromReportDetail(reportDetailMap, "accountreport_incomestatement_totaloperatingcost_managementexpenses");

            // 财务费用
            BigDecimal financialExpenses = getSumByPkeyResult(pkeyMap.get("accountreport_incomestatement_totaloperatingcost_financialexpenses"), reportDetailMap);

            // 平均总资产
            BigDecimal averageTotalAssets = getAverageByPkeyResult(pkeyMap.get("accountreport_balancesheet_currentassets"), reportDetailMap)
                    .add(getAverageByPkeyResult(pkeyMap.get("accountreport_balancesheet_noncurrentassets"), reportDetailMap));

            // 平均库存
            BigDecimal averageInventory = getAverageFromReportDetail(reportDetailMap, "accountreport_balancesheet_currentassets_stock");

            // 平均应收账款
            BigDecimal averageAccountsReceivable = getAverageFromReportDetail(reportDetailMap, "accountreport_balancesheet_currentassets_accountsreceivable");

            // 平均净资产
            BigDecimal averageEquity = getAverageByPkeyResult(pkeyMap.get("accountreport_balancesheet_noncurrentassets"), reportDetailMap)
                    .subtract(getAverageByPkeyResult(pkeyMap.get("accountreport_balancesheet_noncurrentliabilities"), reportDetailMap));


            // 计算流动比率
            FinancialTargetDataVo calculateCurrentRatioData = calculateRatio(currentAssets, currentLiabilities,
                    "流动比率", "calculateCurrentRatio",
                    "流动比率 = 流动资产 / 流动负债",
                    "衡量企业短期偿债能力，即企业流动资产变现以偿还短期债务的能力。");
            result.add(calculateCurrentRatioData);

            // 计算速动比率
            FinancialTargetDataVo calculateQuickRatioData = calculateRatio(currentAssets.subtract(inventory), currentLiabilities,
                    "速动比率", "calculateQuickRatio",
                    "速动比率 = (流动资产 - 存货) / 流动负债",
                    "衡量企业立即偿债能力，从流动资产中扣除存货后，与流动负债的比率。");
            result.add(calculateQuickRatioData);

            // 计算资产负债率
            FinancialTargetDataVo calculateDebtToAssetRatioData = calculateRatio(totalLiabilities, totalAssets,
                    "资产负债率", "calculateDebtToAssetRatio",
                    "资产与负债率 = 总负债 / 总资产",
                    "反映企业资产中有多少是由负债形成的。");
            result.add(calculateDebtToAssetRatioData);

            // 计算负债与所有者权益比例
            FinancialTargetDataVo calculateDebtToEquityRatioData = calculateRatio(totalLiabilities, equity,
                    "负债与所有者权益比例", "calculateDebtToEquityRatio",
                    "负债与所有者权益比例 = 总负债 / 所有者权益",
                    "表示企业负债与所有者权益之间的比例关系。");
            result.add(calculateDebtToEquityRatioData);

            // 计算利息保障倍数
            FinancialTargetDataVo calculateInterestCoverageRatioData = calculateRatio(totalProfit.add(interestCost), interestCost,
                    "利息保障倍数", "calculateInterestCoverageRatio",
                    "利息保障倍数 = (利润总额 + 利息费用) / 利息费用",
                    "表示企业支付利息的能力。");
            result.add(calculateInterestCoverageRatioData);

            // 计算毛利率
            FinancialTargetDataVo calculateGrossProfitMarginData = calculateRatio(mainBusinessIncome.subtract(mainBusinessCost), mainBusinessIncome,
                    "毛利率", "calculateGrossProfitMargin",
                    "毛利率 = (主营业务收入 - 主营业务成本) / 主营业务收入",
                    "反映企业销售收入的盈利水平。");
            result.add(calculateGrossProfitMarginData);

            // 计算营业利润率
            FinancialTargetDataVo calculateOperatingProfitMarginData = calculateRatio(operatingIncome, mainBusinessIncome,
                    "营业利润率", "calculateOperatingProfitMargin",
                    "营业利润率 = 营业利润 / 主营业务收入",
                    "反映企业通过销售获得的营业利润水平。");
            result.add(calculateOperatingProfitMarginData);

            // 计算销售利润率
            FinancialTargetDataVo calculateSalesProfitMarginData = calculateRatio(netProfit, mainBusinessIncome,
                    "销售利润率", "calculateSalesProfitMargin",
                    "销售利润率 = 净利润 / 主营业务收入",
                    "反映企业销售收入的最终盈利水平。");
            result.add(calculateSalesProfitMarginData);

            // 计算净利润率
            FinancialTargetDataVo calculateNetProfitMarginData = calculateRatio(netProfit, mainBusinessIncome,
                    "净利润率", "calculateNetProfitMargin",
                    "净利润率 = 净利润 / 主营业务收入",
                    "同销售利润率，不同行业或地区可能表述不同。");
            result.add(calculateNetProfitMarginData);

            // 计算净资产收益率
            FinancialTargetDataVo calculateReturnOnEquityData = calculateRatio(netProfit, netAssets,
                    "净资产收益率", "calculateReturnOnEquity",
                    "净资产收益率 = 净利润 / 净资产",
                    "反映股东权益的收益水平，即企业运用自有资本的效率。");
            result.add(calculateReturnOnEquityData);

            // 计算成本费用利润率
            BigDecimal totalCosts = mainBusinessCost.add(operatingExpenses).add(managementExpenses).add(financialExpenses);
            FinancialTargetDataVo calculateCostProfitMarginData = calculateRatio(totalProfit, totalCosts,
                    "成本费用利润率", "calculateCostProfitMargin",
                    "成本费用利润率 = 利润总额 / 成本费用总额",
                    "其中成本费用总额为：主营业务成本、营业费用、管理费用、财务费用之和，该指标反映企业经营耗费所带来的经营成果。");
            result.add(calculateCostProfitMarginData);

            // 计算总资产报酬率
            FinancialTargetDataVo calculateReturnOnTotalAssetsData = calculateRatio(totalProfit.add(interestExpense), averageTotalAssets,
                    "总资产报酬率", "calculateReturnOnTotalAssets",
                    "总资产报酬率 = (利润总额 + 利息支出) / 平均总资产",
                    "反映企业资产的盈利能力，即企业运用全部资产的总体获利能力。");
            result.add(calculateReturnOnTotalAssetsData);

            // 计算总资产周转率
            FinancialTargetDataVo calculateTotalAssetTurnoverData = calculateRatio(mainBusinessIncome, averageTotalAssets,
                    "总资产周转率", "calculateTotalAssetTurnover",
                    "总资产周转率 = 主营业务收入 / 平均总资产",
                    "反映企业全部资产的经营质量和利用效率。");
            result.add(calculateTotalAssetTurnoverData);

            // 计算存货周转率
            FinancialTargetDataVo calculateInventoryTurnoverData = calculateRatio(mainBusinessCost, averageInventory,
                    "存货周转率", "calculateInventoryTurnover",
                    "存货周转率 = 主营业务成本 / 平均存货",
                    "衡量企业存货的周转速度和销货能力。");
            result.add(calculateInventoryTurnoverData);

            // 计算应收账款周转率
            FinancialTargetDataVo calculateAccountsReceivableTurnoverData = calculateRatio(mainBusinessIncome, averageAccountsReceivable,
                    "应收账款周转率", "calculateAccountsReceivableTurnover",
                    "应收账款周转率 = 主营业务收入 / 平均应收账款",
                    "反映企业应收账款的周转速度和管理效率。");
            result.add(calculateAccountsReceivableTurnoverData);

            // 计算净资产周转率
            FinancialTargetDataVo calculateEquityTurnoverData = calculateRatio(mainBusinessIncome, averageEquity,
                    "净资产周转率", "calculateEquityTurnover",
                    "净资产周转率 = 主营业务收入 / 平均净资产",
                    "");
            result.add(calculateEquityTurnoverData);
        } catch (Exception e) {

            e.printStackTrace();
            return result;
        }
        return result;
    }

    private BigDecimal getSumByPkeyResult(List<NewAppDic> pkeyResult, Map<String, CuEnterpriseFinancialReportDetail> reportDetailMap) {
        return CollectionUtil.isNotEmpty(pkeyResult) ? sumMoneyByMatchPkeyDic(pkeyResult, reportDetailMap) : BigDecimal.ZERO;
    }

    private BigDecimal getAverageByPkeyResult(List<NewAppDic> pkeyResult, Map<String, CuEnterpriseFinancialReportDetail> reportDetailMap) {
        return CollectionUtil.isNotEmpty(pkeyResult) ? sumAvgMoneyByMatchPkeyDic(pkeyResult, reportDetailMap) : BigDecimal.ZERO;
    }

    private BigDecimal getEndMoneyFromReportDetail(Map<String, CuEnterpriseFinancialReportDetail> reportDetailMap, String key) {
        return reportDetailMap.containsKey(key)
                ? Optional.ofNullable(reportDetailMap.get(key).getEndMoney()).orElse(BigDecimal.ZERO)
                : BigDecimal.ZERO;
    }

    private BigDecimal getAverageFromReportDetail(Map<String, CuEnterpriseFinancialReportDetail> reportDetailMap, String key) {
        if (reportDetailMap.containsKey(key)) {
            CuEnterpriseFinancialReportDetail detail = reportDetailMap.get(key);
            return (detail.getStartMoney().add(detail.getEndMoney())).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private FinancialTargetDataVo calculateRatio(BigDecimal numerator, BigDecimal denominator, String resultName, String resultKey, String formula, String remark) {
        BigDecimal ratio = denominator.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : numerator.divide(denominator, 10, RoundingMode.HALF_UP);
        FinancialTargetDataVo dataVo = new FinancialTargetDataVo();
        dataVo.setResultName(resultName);
        dataVo.setResultKey(resultKey);
        dataVo.setFormula(formula);
        dataVo.setResultValue(ratio);
        dataVo.setRemark(remark);
        return dataVo;
    }

    private BigDecimal sumMoneyByMatchPkeyDic(List<NewAppDic> pkeyResult, Map<String, CuEnterpriseFinancialReportDetail> reportDetailMap) {
        Set<String> mkeySet = new HashSet<>();
        collectMkeys(pkeyResult, mkeySet);
        // 过滤掉 startMoney 或 endMoney 等于 0 的 mkey
        mkeySet.removeIf(mkey -> {
            CuEnterpriseFinancialReportDetail detail = reportDetailMap.get(mkey);
            return detail != null &&
                    ((detail.getStartMoney() != null && BigDecimal.ZERO.compareTo(detail.getStartMoney()) == 0) ||
                            (detail.getEndMoney() != null && BigDecimal.ZERO.compareTo(detail.getEndMoney()) == 0));
        });
        // 计算
        return mkeySet.stream()
                .map(reportDetailMap::get)
                .filter(Objects::nonNull)
                .map(CuEnterpriseFinancialReportDetail::getEndMoney)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumAvgMoneyByMatchPkeyDic(List<NewAppDic> pkeyResult, Map<String, CuEnterpriseFinancialReportDetail> reportDetailMap) {
        Set<String> mkeySet = new HashSet<>();
        collectMkeys(pkeyResult, mkeySet);
        // 过滤掉 startMoney 或 endMoney 等于 0 的 mkey
        mkeySet.removeIf(mkey -> {
            CuEnterpriseFinancialReportDetail detail = reportDetailMap.get(mkey);
            return detail != null &&
                    ((detail.getStartMoney() != null && BigDecimal.ZERO.compareTo(detail.getStartMoney()) == 0) ||
                            (detail.getEndMoney() != null && BigDecimal.ZERO.compareTo(detail.getEndMoney()) == 0));
        });

        return mkeySet.stream()
                .map(reportDetailMap::get)
                .filter(Objects::nonNull)
                .filter(detail -> detail.getStartMoney() != null && detail.getEndMoney() != null)
                .map(detail -> detail.getStartMoney().add(detail.getEndMoney()).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void collectMkeys(List<NewAppDic> dicList, Set<String> mkeySet) {
        if (CollectionUtil.isEmpty(dicList)) {
            return;
        }
        for (NewAppDic dic : dicList) {
            mkeySet.add(dic.getMkey());
            collectMkeys(dic.getChildren(), mkeySet);
        }
    }

    private List<NewAppDic> findDicListByLike(String pkey) {
        //
        return null;
    }

}

package com.xuxiang.smartfast_rearend.controller;

import com.alibaba.fastjson.JSONObject;
import com.xuxiang.smartfast_rearend.pojo.BscContracts;
import com.xuxiang.smartfast_rearend.pojo.EthContracts;
import com.xuxiang.smartfast_rearend.pojo.SmartfastContracts;
import com.xuxiang.smartfast_rearend.service.BscContractAuditService;
import com.xuxiang.smartfast_rearend.service.EthContractAuditService;
import com.xuxiang.smartfast_rearend.service.SmartFastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
public class SmartFastController {
    @Autowired
    SmartFastService smartFastService;

    @Autowired
    EthContractAuditService ethereumContractAuditService;

    @Autowired
    BscContractAuditService bscContractAuditService;

    @Value("${true.path}")
    private String dataPath;



    // 从前端接收检测合约和版本号，执行检测服务，生产检测结果pdf，从中提取关键信息，返回json文本给前端
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/smartFast")
    public JSONObject smartFastAudit(@RequestParam String solcVersion, @RequestParam String  code) {

        long startTime = System.currentTimeMillis(); //记录方法开始执行时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String auditStartTime = dateFormat.format(new Date(startTime));

        // 1. 从前端接收合约文本和版本号, 保存合约文件本地文件和版本号，同时将检测信息存储进数据库
        String result1 = smartFastService.saveContractAndSolcVersion(solcVersion, code);
        System.out.println(result1);
        SmartfastContracts contracts = smartFastService.getCurrentContract();

        // 2. 执行脚本文件, 对存入的合约文件进行检测, 生成检测报告
        String result2 = smartFastService.executeScript();
        System.out.println(result2);

        // 3. 对检测报告进行提取产生漏洞信息json
        JSONObject auditResultJson = smartFastService.pdfTest(contracts.getContractName());
        System.out.println(auditResultJson);
        // return auditResultJson;

        // 4. 将漏洞信息json文件写入数据库
        System.out.println(smartFastService.replenishVulnerabilityInfoInDB(auditResultJson));


        long endTime = System.currentTimeMillis(); // 方法执行结束时间
        long executedTime = endTime-startTime;
        // 5. 整合，从数据库中获取，当前检测合约的合约名、版本、报告名
        JSONObject resultJson = smartFastService.getAuditResult(auditResultJson, auditStartTime,executedTime);
        System.out.println(resultJson);
        return resultJson;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/smartFast/EthAudit")
    public EthContracts ethAudit(@RequestParam String address) {
        // 1.从前端获取待检测ethereum待检测合约源码，并存储到本地
        ethereumContractAuditService.ethereumContractSave(address);
        System.out.println("存储合约成功");
        EthContracts currentEthContract = ethereumContractAuditService.getCurrentContract();

        // 判定代码是否开源
        if (currentEthContract.getOpenSource()) {
            // 开源则执行检测脚本，生成pdf报告链接
            System.out.println(smartFastService.executeScript());
            // 添加报告链接
            System.out.println(ethereumContractAuditService.addReportLink());
            // 从检测报告中提取漏洞信息json
            JSONObject vulnerabilityInfoJson =  smartFastService.pdfTest(currentEthContract.getContractName());
            System.out.println(vulnerabilityInfoJson);
            // 将漏洞信息json填入数据库
            System.out.println(ethereumContractAuditService.replenishVulnerabilityInfoInDB(vulnerabilityInfoJson));
            System.out.println("合约检测成功");
        }
        return currentEthContract;

    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/smartFast/BscAudit")
    public BscContracts bscAudit(@RequestParam String address) {
        // 1.从前端获取待检测bsc待检测合约源码，并存储到本地和数据库
        bscContractAuditService.bscContractSave(address);
        System.out.println("存储合约成功");
        BscContracts currentBscContract = bscContractAuditService.getCurrentContract();

        // 判定代码是否开源
        if (bscContractAuditService.isOpenSource()) {
            // 开源则执行检测脚本，生成pdf报告链接
            System.out.println(smartFastService.executeScript());
            // 添加报告链接
            System.out.println(bscContractAuditService.addReportLink());
            // 从检测报告中提取漏洞信息json
            JSONObject vulnerabilityInfoJson =  smartFastService.pdfTest(currentBscContract.getContractName());
            System.out.println(vulnerabilityInfoJson);
            // 将漏洞信息json填入数据库
            System.out.println(bscContractAuditService.replenishVulnerabilityInfoInDB(vulnerabilityInfoJson));
            System.out.println("合约检测成功");
        }
        return currentBscContract;
    }

    // 检测记录，返回数据库中所有对象信息
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/record/listAuditRecord")
    public List<SmartfastContracts> AuditRecord() {
        return smartFastService.getAllContractsRecord();
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/query")
    public List<SmartfastContracts> queryByContractNameOrSolcVersion(
            @RequestParam(required = false) String contractName,
            @RequestParam(required = false) String solcVersion
    ) {
        return smartFastService.queryByContractNameOrSolcVersion(contractName, solcVersion);
    }


    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/test/saveContractAndSolcVersion")
    public String contractTest(@RequestParam String solcVersion, @RequestParam String  code) {
       return smartFastService.saveContractAndSolcVersion(solcVersion,code);
    }

    @PostMapping("/smartFast/EthAuditTest")
    public String ethAuditTest(@RequestParam String address) {
        ethereumContractAuditService.ethereumContractSave(address);
        return "success!";
    }
}

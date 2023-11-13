package com.xuxiang.smartfast_rearend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.xuxiang.smartfast_rearend.service.BscContractAuditService;
import com.xuxiang.smartfast_rearend.service.EthContractAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicChainToolController {
    @Autowired
    EthContractAuditService ethContractAuditService;
    @Autowired
    BscContractAuditService bscContractAuditService;

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/publicChain/getEthGasTracker")
    public JsonNode getEthGasTracker() {
        return ethContractAuditService.gasTracker();
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/publicChain/estimateConfirmTime")
    public int estimateConfirmTime(int gasPrice) {
        return ethContractAuditService.estimateConfirmTime(gasPrice);
    }
}

package com.xuxiang.smartfast_rearend.controller;

import com.alibaba.fastjson.JSONObject;
import com.xuxiang.smartfast_rearend.service.OyenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class OyenteController {

    @Autowired
    OyenteService oyenteService;

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/oyente")
    public JSONObject oyenetAudit(@RequestParam String solcVersion, @RequestParam String  code) throws IOException {

        long startTime = System.currentTimeMillis(); //记录方法开始执行时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String auditStartTime = dateFormat.format(new Date(startTime));

       /* // test DIR
        String contractDir = "/Users/xuxiang/workplace/testfile/contract/"; //"/home/xuxiang/data/Oyente/data/contract/";
        String jsonDir = "/Users/xuxiang/workplace/testfile/json/"; //"/home/xuxiang/data/Oyente/data/json/";*/

        // FWC dir
        String contractDir = "/home/xuxiang/data/Oyente/data/contract/";
        String jsonDir = "/home/xuxiang/data/Oyente/data/json/";
        //1. 从前端获取合约文本和合约版本
        String saveResult = oyenteService.saveContractAndSolcVersion(solcVersion, code, jsonDir, contractDir);
        System.out.println(saveResult);

        //2. 执行检测脚本获取检测结果
        String scriptResult = oyenteService.executeScript();
        System.out.println(scriptResult);

        //3. 读取检测结果json文件并输出
        return oyenteService.readDetectJson();
    }
}

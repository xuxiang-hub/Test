package com.xuxiang.smartfast_rearend.controller;

import com.alibaba.fastjson.JSONObject;
import com.xuxiang.smartfast_rearend.service.TODetectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class TODetectorController {

    @Autowired
    TODetectorService toDetectorService;

    @PostMapping("/toDetector")
    @ResponseBody // 将Controller的方法返回的对象，通过适当的转换器转换为指定的格式之后，写入到response对象的body区，通常用来返回JSON数据或者是XML数据。
    public JSONObject toDetectorAudit(@RequestParam String solcVersion, @RequestParam String code) throws IOException {
        // 1. 从前端接收合约文本和版本号, 保存合约文件本地文件和版本号，同时将检测信息存储进数据库
        String jsonDir = "/home/xuxiang/data/TODetector/data/json/";
        String contractDir = "/home/xuxiang/data/TODetector/data/contract/";
        String result1 = toDetectorService.saveContractAndSolcVersion(solcVersion, code, jsonDir, contractDir);
        System.out.println(result1);
        // return result1;

        // 2. 执行脚本文件, 对存入的合约文件进行检测，生产检测结果json文件
        String result2 = toDetectorService.executeScript();
        System.out.println(result2);

        //3. 读取检测结果json文件并输出
        return toDetectorService.readDetectJson();
    }

    @GetMapping("/test/readJson")
    public JSONObject Test() throws IOException {
        return toDetectorService.readDetectJson();
    }

}

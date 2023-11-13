package com.xuxiang.smartfast_rearend.controller;

import com.alibaba.fastjson.JSONObject;
import com.xuxiang.smartfast_rearend.pojo.Packets;
import com.xuxiang.smartfast_rearend.pojo.Session;
import com.xuxiang.smartfast_rearend.pojo.TrafficFile;
import com.xuxiang.smartfast_rearend.service.TrafficDetectionService;
import org.pcap4j.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@RestController
public class TrafficDetectionController {

    @Autowired
    TrafficDetectionService trafficDetectionService;

    // 流量文件上传
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/uploadTrafficFile")
    public ResponseEntity<String> uploadTrafficFile(@RequestParam("file") MultipartFile file) {
        /**
         * 处理文件上传请求的方法。
         *
         * @param file 上传的文件
         * @return ResponseEntity 包含上传结果的响应
         */
        try {
            String result = trafficDetectionService.handleFileUpload(file);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            return new ResponseEntity<>("fail upload file!", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /*
    * 分析与检测流程
    * 对数据库中最新的流量文件进行按会话拆分
    * 执行检测脚本
    * 获取检测结果
    * 解析检测结果，返回json文件
    * */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/trafficDetect")
    public List<Session> trafficDetect() throws NotOpenException, EOFException, PcapNativeException, TimeoutException {
        // 获取上传的pcap文件并拆分存储
        trafficDetectionService.pcapSplitAnalyze();
        // 执行脚本文件检测, 得到result.txt(所有session和检测结果)
        trafficDetectionService.trafficDetect();
        // 检测结果输出，将每个traffic的session文件和检测结果都存储到sessio字段
        List<JSONObject> resultJson = trafficDetectionService.detectResult();
        System.out.println(resultJson);
        // 从trafficfile的session字段提取信息存入session表
        List<Session> detectResult = trafficDetectionService.analyzeSession();
        // 包分析
        String packetAnalyzeResult = trafficDetectionService.packetAnalyzeFromTraffic();
        System.out.println(packetAnalyzeResult);
        return detectResult;
    }


    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/pcap/detect")
    public String pacpDetect() {
        return trafficDetectionService.trafficDetect();
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/pcap/result")
    public List<JSONObject> pacpResult() {
        return trafficDetectionService.detectResult();
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/pcap/splitAndAnalyze")
    public String splitPcap() throws PcapNativeException, NotOpenException, EOFException, TimeoutException {
        trafficDetectionService.pcapSplitAnalyze();
        return "success";
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/pcap/test")
    public List<Session> test() {
        return trafficDetectionService.analyzeSession();
        //trafficDetectionService.sessionAnalyze();
        // return "success";
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/pcap/listDetectRecord")
    public List<TrafficFile> listAllDetectRecord() {
        return trafficDetectionService.listAllTrafficRecord();
    }

    // 根据trafficid找到所有session
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/pcap/listSessionByTrafficId")
    public List<Session> getSessionByTrafficId(@RequestParam("traffic_id") int trafficId) {
        return trafficDetectionService.listSessionByTrafficId(trafficId);
    }

    // 根据sessionid找到所有packets
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @PostMapping("/pcap/listPacketsBySessionId")
    public List<Packets> getPacketsBySessionId(@RequestParam("session_id") int sessionId) {
        return trafficDetectionService.listPacketsBySessionId(sessionId);
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/pcap/sessiontest")
     public String sessionTest(){
        return trafficDetectionService.packetAnalyzeFromTraffic();
    }



}

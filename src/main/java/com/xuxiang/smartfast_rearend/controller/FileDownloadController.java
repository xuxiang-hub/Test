package com.xuxiang.smartfast_rearend.controller;

import com.xuxiang.smartfast_rearend.dao.SmartfastContractDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class FileDownloadController {
    // private String filePath = "/home/xuxiang/data/report/contract-report_main.pdf";

    @Value("${report.path}")
    private String reportPath;

    @Autowired
    SmartfastContractDAO contractDAO;

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST}) // 设置允许跨域
    @GetMapping("/download")
    public ResponseEntity<UrlResource> downloadFile() {

        // 获取最新的合约名
        String contractName = contractDAO.findLatestContract().getContractName(); // contract-20230905171903
        // 裁剪成为报告名
        String reportName = contractName + "-report_main.pdf";  // contract-20230905171903-report_main.pdf
        String filePath =  reportPath+reportName;
        System.out.println(filePath);

        try {
            // 指定要下载的固定文件的路径
            File file = new File(filePath);

            if (file.exists()) {
                // 设置响应头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", "report.pdf");

                // 创建文件资源
                Path path = Paths.get(file.getAbsolutePath());
                UrlResource resource = new UrlResource(path.toUri());

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}

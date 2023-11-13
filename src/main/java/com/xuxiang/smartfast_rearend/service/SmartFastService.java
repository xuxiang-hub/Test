package com.xuxiang.smartfast_rearend.service;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuxiang.smartfast_rearend.dao.SmartfastContractDAO;
import com.xuxiang.smartfast_rearend.pojo.SmartfastContracts;
import com.xuxiang.smartfast_rearend.tool.Tool;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SmartFastService {
    @Autowired
    SmartfastContractDAO contractDAO;

    // 从前端接收合约文本存储为合约文件，并存入数据库，同时接收合约版本
    public String saveContractAndSolcVersion(String solcVersion, String code) {

        // 产生合约名 contract+时间戳
        String formattedDate = Tool.getFormattedDate();
        String contractName = "contract-" + formattedDate;

        // 获取存储文件路径
        String jsonPath = "/home/xuxiang/data/smartfast/data/json/test.json"; // 将合约名和版本号存入json文件供使用
        String contractPath = "/home/xuxiang/data/smartfast/data/contract/" + contractName + ".sol";
        // 测试用
        //String jsonPath =  "/Users/xuxiang/workplace/testfile/json/test.json"; // 将合约名和版本号存入json文件供使用
        //String contractPath =  "/Users/xuxiang/workplace/testfile/contract/" + contractName + ".sol";
        System.out.println("存储合约名和版本号json文件位置：" + jsonPath);
        System.out.println("待检测合约存储位置为：" + contractPath);


        // 将合约名和solc版本写入json文件
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put("solidityVersion", solcVersion);
        keyValueMap.put("contractName", contractName);

        ObjectMapper objectMapper = new ObjectMapper();

        // 进行反转义
        String unescapedCode = Tool.unescapeJava(code);
        System.out.println("待检测合约为：\n" + unescapedCode);
        System.out.println("合于版本为：" + solcVersion);

        // 生成检测报告链接（pdf文件）
        String reportLink = "http://42.194.184.32:5055/" + contractName + "-report_main.pdf";

        //合约名，版本号存入数据库
        SmartfastContracts contract = new SmartfastContracts();
        contract.setContractName(contractName);
        contract.setSolcVersion(solcVersion);
        contract.setCreatedAt(formattedDate);
        contract.setCode(unescapedCode);
        contract.setReportLink(reportLink);
        contractDAO.save(contract);

        try {
            //保存json文件
            objectMapper.writeValue(new File(jsonPath), keyValueMap);

            //保存合约文件
            BufferedWriter writer = new BufferedWriter(new FileWriter(contractPath));
            writer.write(unescapedCode);
            writer.close();
            return "合约存储成功，文本已保存到文件：" + contractPath;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error uploading file.";
        }
    }


    // 执行运行脚本，对生成的合约文件进行审查，脚本产生新的pdf报告
    public String executeScript() {
        try {
            // 脚本路径
            String scriptPath = "/home/xuxiang/data/smartfast/script/smartfastTest.sh";
            //构建外部命令
            ProcessBuilder processBuilder = new ProcessBuilder(scriptPath);
            //启动进程
            Process process = processBuilder.start();

            //获取脚本输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            //直到最后行为空，一直获取脚本输出
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 等待进程结束
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "脚本执行成功：\n" + output.toString();
            } else {
                return "脚本执行失败，退出码" + exitCode + ":\n" + output.toString();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error executing script.";
        }
    }


    // 传入检测的合约名， 提取最新的pdf检测文件（即当前检测合约的报告）进行提取目标文字并转换为json
    public JSONObject pdfTest(String contractName) {
        // 测试用
        // String pdfFilePath = "/home/xuxiang/data/report/contract-report_main.pdf";
        // 裁剪成为报告名
        String reportName = contractName + "-report_main.pdf";  // contract-20230905171903-report_main.pdf
        String pdfFilePath = "/home/xuxiang/data/smartfast/data/report/" + reportName;
        System.out.println("当前检测合约报告路径为：" + pdfFilePath);

        String searchTextStart = "This security audit found";
        String searchTextEnd = "that need attention";


        try {
            // 加载PDF文件
            PDDocument document = PDDocument.load(new File(pdfFilePath));
            PDFTextStripper textStripper = new PDFTextStripper();

            // 查找搜索文本的起始位置和结束位置
            String extractedText = textStripper.getText(document);
            int startIndex = extractedText.indexOf(searchTextStart);
            int endIndex = extractedText.indexOf(searchTextEnd, startIndex);

            if (startIndex != -1 && endIndex != -1) {
                // 截取包含目标文本的部分
                String extractedSection = extractedText.substring(startIndex, endIndex + searchTextEnd.length());
                System.out.println(extractedSection);
                JSONObject json = Tool.convertTextToJson(extractedSection);
                // 关闭PDF文档
                document.close();
                return json;
            } else {
                // 如果文本未找到，将错误信息添加到结果Map中
                // 关闭PDF文档
                document.close();
                JSONObject errorJson = new JSONObject();
                errorJson.put("error", "Text not found in the specified range.");
                return errorJson; // 返回包含错误信息的JSONObject
            }
        } catch (IOException e) {
            // 处理可能的异常情况，并将错误信息添加到结果中
            JSONObject errorJson = new JSONObject();
            errorJson.put("error", "Error extracting text from PDF: " + e.getMessage());
            return errorJson; // 返回包含错误信息的JSONObject
        }
    }

    // 将漏洞信息填入数据库
    public String replenishVulnerabilityInfoInDB(JSONObject json) {
        SmartfastContracts currentContracts = contractDAO.findLatestContract();
        // 将漏洞情况添加到数据库
        currentContracts.setHighLevelVulnerability(Integer.parseInt(json.get("high").toString()));
        currentContracts.setMediumLevelVulnerability(Integer.parseInt(json.get("medium").toString()));
        currentContracts.setLowLevelVulnerability(Integer.parseInt(json.get("low").toString()));
        currentContracts.setWarning(Integer.parseInt(json.get("need attention").toString()));
        currentContracts.setNeedOpt(Integer.parseInt(json.get("opt").toString()));
        contractDAO.save(currentContracts);
        return "数据库添加漏洞信息成功！";
    }

    // 将审计信息加入到审计结果的json文件中
    public JSONObject getAuditResult(JSONObject AuditResult, String auditStartTime, long executedTime) {
        // 获取当前检测的合约对象，即id最大的
        SmartfastContracts auditContract = contractDAO.findLatestContract();
        String auditContractName = auditContract.getContractName() + ".sol";
        String auditContractReport = auditContractName + "-report_main.pdf";
        String auditContractVersion = auditContract.getSolcVersion();

        AuditResult.put("ContractName", auditContractName);
        AuditResult.put("ContractVersion", auditContractVersion);
        AuditResult.put("ContractReport", auditContractReport);
        AuditResult.put("AuditTime", auditStartTime);
        AuditResult.put("ExecutedTime", executedTime + "ms");

        return AuditResult;
    }

    // 返回最新的Contracts对象
    public SmartfastContracts getCurrentContract() {
        return contractDAO.findLatestContract();
    }

    // 获取合约表中的所有合约对象
    public List<SmartfastContracts> getAllContractsRecord() {
        return contractDAO.findAll();
    }

    // 根据合约名或者合约版本进行查询
    public List<SmartfastContracts> queryByContractNameOrSolcVersion(String contractName, String solcVersion) {
        if (contractName != null && solcVersion != null) {
            return contractDAO.findByContractNameAndSolcVersion(contractName, solcVersion);
        } else if (contractName != null) {
            return contractDAO.findByContractName(contractName);
        } else if (solcVersion != null) {
            return contractDAO.findBySolcVersion(solcVersion);
        } else {
            return contractDAO.findAll();
        }
    }

}

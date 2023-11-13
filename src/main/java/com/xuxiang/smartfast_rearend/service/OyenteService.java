package com.xuxiang.smartfast_rearend.service;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuxiang.smartfast_rearend.dao.OyenetContractDAO;
import com.xuxiang.smartfast_rearend.pojo.OyenetContracts;
import com.xuxiang.smartfast_rearend.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OyenteService {
    @Autowired
    OyenetContractDAO oyenetContractDAO;

    // 获取合约表中的所有合约对象
    public List<OyenetContracts> getAllContractsRecord() {
        return oyenetContractDAO.findAll();
    }

    // 执行运行脚本，对生成的合约文件进行审查
    public String executeScript() {
        try {
            // 脚本路径 测试用
            //String scriptPath = "/Users/xuxiang/workplace/testfile/script/test.sh";
            String scriptPath = "/home/xuxiang/data/Oyente/script/oyente.sh";
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

    // 从前端接收合约文本存储为合约文件，并存入数据库，同时接收合约版本
    public String saveContractAndSolcVersion(String solcVersion, String code, String jsonDir, String contractDir) {

        // 产生合约名 contract+时间戳
        String formattedDate = Tool.getFormattedDate();
        String contractName = "contract-" + formattedDate;

        // 获取存储文件路径
        String jsonPath = jsonDir + "test.json"; // 将合约名和版本号存入json文件供使用
        String contractPath = contractDir + contractName + ".sol";
        System.out.println("存储合约名和版本号json文件位置：" + jsonPath);
        System.out.println("待检测合约存储位置为：" + contractPath);


        //合约名，版本号存入数据库
        OyenetContracts oyenetContracts = new OyenetContracts();
        oyenetContracts.setContractName(contractName);
        oyenetContracts.setSolcVersion(solcVersion);
        oyenetContracts.setCreatedAt(formattedDate);


        // 将合约名和solc版本写入json文件
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put("solcVersion", solcVersion);
        keyValueMap.put("contractName", contractName);

        ObjectMapper objectMapper = new ObjectMapper();

        // 进行反转义
        String unescapedCode = Tool.unescapeJava(code);
        System.out.println("待检测合约为：\n" + unescapedCode);
        System.out.println("合于版本为：" + solcVersion);
        oyenetContracts.setCode(unescapedCode);
        oyenetContractDAO.save(oyenetContracts);

        try {
            //保存json文件
            objectMapper.writeValue(new File(jsonPath), keyValueMap);

            //保存合约文件
            BufferedWriter writer = new BufferedWriter(new FileWriter(contractPath));
            writer.write(unescapedCode);
            writer.close();
            System.out.println();
            return "合约存储成功，文本已保存到文件：" + contractPath;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error uploading file.";
        }
    }

    // 读取当前检测合约的json检测结果并返回
    public JSONObject readDetectJson() throws IOException {
        OyenetContracts currentContract = oyenetContractDAO.findLatestContract();
        // 获取expected_json文件夹中当前检测合约的检测结果json文件
        File folder = new File("/home/xuxiang/data/Oyente/data/tests/expected_json");
        File[] files = folder.listFiles();
        // 遍历expected_json中的所有文件
        for (File file : files) {
            // 获取当前检测合约的检测结果json文件
            String currentContractName = currentContract.getContractName();
            if (file.getName().startsWith(currentContractName)) {
                System.out.println(file.getName());
                // 提取合约类型
                String contractType = Tool.extractContractType(currentContractName, file.getName());
                // 存入数据库
                currentContract.setContractType(contractType);
                oyenetContractDAO.save(currentContract);

                //输出json文件
                //使用jacksonAPI读取json文件
                ObjectMapper objectMapper = new ObjectMapper();
                //将json文件转换成Map对象
                Map<String, Object> map = objectMapper.readValue(file, Map.class);
                //将Map对象转换成JSON对象
                JSONObject json = new JSONObject(map);
                //打印或返回JSON对象
                System.out.println(json);
                json.put("contract_type", contractType);
                return json;
            }
        }
        return null;
    }

}

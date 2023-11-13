package com.xuxiang.smartfast_rearend.service;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuxiang.smartfast_rearend.dao.BscContractDAO;
import com.xuxiang.smartfast_rearend.pojo.BscContracts;
import com.xuxiang.smartfast_rearend.tool.Tool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class BscContractAuditService {

    @Autowired
    private BscContractDAO bscContractDAO;

    // 从Etherscan api 根据合约地址获取相应的合约源码和相关信息，并将合约源码和版本号保存至本地，相关信息保存至数据库，为后续检测做准备
    public void bscContractSave(String address) {

        // 获取检测时间
        long auditTime = System.currentTimeMillis(); //记录方法开始执行时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String auditTimeStamp = dateFormat.format(new Date(auditTime));

        // 新建EthContracts对象存储当前查询合约信息
        BscContracts bscContracts = new BscContracts();
        bscContracts.setContractAddress(address);
        bscContracts.setTimestamp(auditTimeStamp);
        bscContracts.setChain("Bsc MainNet");

        // 定义保存合约的文件夹
        String contractDir = "/home/xuxiang/data/smartfast/data/contract/";
        String jsonPath = "/home/xuxiang/data/smartfast/data/json/test.json";
//        String contractDir = "/Users/xuxiang/workplace/testfile/contract/";
//        String jsonPath = "/Users/xuxiang/workplace/testfile/json/test.json";

        // 开启代理
        Tool.setProxy();

        // 创建一个OkHttpClient对象
        OkHttpClient client = new OkHttpClient();

        String module = "contract";
        String action = "getsourcecode";
        //String address = "0xdAC17F958D2ee523a2206206994597C13D831ec7";
        String apikey = "NNS334QSKH381SJT36TUSXI91MGGAVBIZF";

        // 构建api请求URL
        String getContractCodeByAddressUrl = "https://api.bscscan.com/api"
                + "?module=" + module
                + "&action=" + action
                + "&address=" + address
                + "&apikey=" + apikey;
        String getContractCreationByAddressUrl = "https://api.bscscan.com/api"
                + "?module=" + module
                + "&action=" + "getcontractcreation"
                + "&contractaddresses=" + address
                + "&apikey=" + apikey;
        System.out.println("getsourcecode API请求的URL为：" + getContractCodeByAddressUrl);
        System.out.println("getcontractcreation API请求的URL为：" + getContractCodeByAddressUrl);

        // 创建request对象，请求getContractCodeByAddressUrl
        Request request1 = new Request.Builder()
                .url(getContractCodeByAddressUrl)
                .build();

        // 发送getContractCode请求并获取响应
        try (Response response = client.newCall(request1).execute()){
                    // 判定是否响应成功
                    if (response.isSuccessful()) {
                        // 获取响应的JSON字符串
                        String json = response.body().string();
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(json);
                        JsonNode sourceCodeNode = node.get("result").get(0).get("SourceCode");
                        //System.out.println(json);
                        String contractCode = sourceCodeNode.asText();
                        String contractName = node.get("result").get(0).get("ContractName").asText();
                        String compilerVersion = node.get("result").get(0).get("CompilerVersion").asText();
                        String solcVersion = Tool.cutSolcVersion(compilerVersion);
                        if (Tool.isGreaterThanVersion(solcVersion)) {
                            //判定如果待检测合约超过0.8.0，添加备注，并将合约写入test.json的版本重置为0.8.0
                            String remark = "The contract version is over 0.8.0 and can only perform simple xml analysis!";
                            bscContracts.setRemark(remark);
                            System.out.println(remark);
                            solcVersion = "0.8.0";
                        }
                        bscContracts.setOpenSource(true);
                        bscContracts.setContractName(contractName);
                        bscContracts.setCode(contractCode);
                        bscContracts.setCompilerVersion(compilerVersion);
                        bscContracts.setAbi(node.get("result").get(0).get("ABI").asText());
                        bscContracts.setEvmVersion(node.get("result").get(0).get("EVMVersion").asText());
                        bscContracts.setLibrary(node.get("result").get(0).get("Library").asText());
                        bscContracts.setLicenseType(node.get("result").get(0).get("LicenseType").asText());
                        bscContracts.setOptimizationUsed(node.get("result").get(0).get("OptimizationUsed").asText());
                        bscContracts.setRuns(node.get("result").get(0).get("Runs").asText());
                        bscContracts.setImplementation(node.get("result").get(0).get("Implementation").asText());
                        bscContracts.setConstructorArguments(node.get("result").get(0).get("ConstructorArguments").asText());
                        bscContracts.setProxy(node.get("result").get(0).get("Proxy").asText());
                        bscContracts.setSwarmSource(node.get("result").get(0).get("SwarmSource").asText());
                        System.out.println("合约名：" + contractName + "      " + "编译版本：" + compilerVersion +
                        "\n" + contractCode);
                        if (contractCode == "") {
                            bscContracts.setOpenSource(false);
                            System.out.println("该合约未开源");
                        } else {
                            // 将合约名和solc版本写入json文件
                            Map<String, String> keyValueMap = new HashMap<>();
                            keyValueMap.put("solidityVersion", solcVersion);
                            keyValueMap.put("contractName", contractName);
                            ObjectMapper objectMapper = new ObjectMapper();
                            //保存json文件
                            objectMapper.writeValue(new File(jsonPath), keyValueMap);
                            String contractPath = contractDir + contractName + ".sol";
                            //保存合约文件
                            BufferedWriter writer = new BufferedWriter(new FileWriter(contractPath));
                            writer.write(contractCode);
                            writer.close();
                            System.out.println("合约保存成功，本地路径为：" + contractPath);
                        }
            } else {
                System.out.println("请求失败" + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // 创建request对象，请求getContractCodeByAddressUrl
        Request request2 = new Request.Builder()
                .url(getContractCreationByAddressUrl)
                .build();
        // 发送getContractCode请求并获取响应
        try (Response response = client.newCall(request2).execute()) {
            // 判定是否响应成功
            if (response.isSuccessful()) {
                // 获取响应的JSON字符串
                String json = response.body().string();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);
                String contractCreator = node.get("result").get(0).get("contractCreator").asText();
                String createTxHash = node.get("result").get(0).get("txHash").asText();
                bscContracts.setContractCreator(contractCreator);
                bscContracts.setCreateTxHash(createTxHash);
                System.out.println(contractCreator + "\n" + createTxHash);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        bscContractDAO.save(bscContracts);
    }

    // 判定当前新上传的合约是否开源
    public boolean isOpenSource(){
        return bscContractDAO.findLatestContract().getOpenSource();
    }

    // 添加检测报告链接
    public String addReportLink(){
        BscContracts currentBscContracts = bscContractDAO.findLatestContract();
        String reportLink = "http://42.194.184.32:5055/"+currentBscContracts.getContractName()+"-report_main.pdf";
        currentBscContracts.setReportLink(reportLink);
        bscContractDAO.save(currentBscContracts);
        System.out.println("检测报告链接为：" + reportLink);
        return "添加检测报告链接成功";
    }

    // 返回最新的Contracts对象
    public BscContracts getCurrentContract(){
        return bscContractDAO.findLatestContract();
    }

    // 将漏洞信息填入数据库
    public String replenishVulnerabilityInfoInDB(JSONObject VulnerabilityInfoJson){
        BscContracts currentBscContract = getCurrentContract();
        currentBscContract.setHighLevelVulnerability(Integer.parseInt(VulnerabilityInfoJson.get("high").toString()));
        currentBscContract.setMediumLevelVulnerability(Integer.parseInt(VulnerabilityInfoJson.get("medium").toString()));
        currentBscContract.setLowLevelVulnerability(Integer.parseInt(VulnerabilityInfoJson.get("low").toString()));
        currentBscContract.setWarning(Integer.parseInt(VulnerabilityInfoJson.get("need attention").toString()));
        currentBscContract.setNeedOpt(Integer.parseInt(VulnerabilityInfoJson.get("opt").toString()));
        bscContractDAO.save(currentBscContract);
        return "数据库添加漏洞信息成功！";
    }


    /**
     * @return
     * "result":{
     *       "LastBlock":"11506521",  // 最新的区块编号
     *       "SafeGasPrice":"5",
     *       "ProposeGasPrice":"5",
     *       "FastGasPrice":"15.972",
     *       "UsdPrice":"441.52" // BNB 的当前美元价格
     *    }
     */
    public JsonNode gasTracker(){
        // 开启代理
        Tool.setProxy();
        // 创建一个OkHttpClient对象
        OkHttpClient client = new OkHttpClient();

        String module = "gastracker";
        String action = "gasoracle";
        //String address = "0xdAC17F958D2ee523a2206206994597C13D831ec7";
        String apikey = "NNS334QSKH381SJT36TUSXI91MGGAVBIZF";

        // 构建api请求URL
        String gasTrackerUrl = "https://api.bscscan.com/api"
                + "?module=" + module
                + "&action=" + action
                + "&apikey=" + apikey;

        // 创建request对象，请求getContractCodeByAddressUrl
        Request request1 = new Request.Builder()
                .url(gasTrackerUrl)
                .build();

        // 发送getContractCode请求并获取响应
        try (Response response = client.newCall(request1).execute()){
            // 判定是否响应成功
            if (response.isSuccessful()) {
                // 获取响应的JSON字符串
                String json = response.body().string();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);
                return node.get("result");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }




}

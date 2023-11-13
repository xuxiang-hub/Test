package com.xuxiang.smartfast_rearend.service;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuxiang.smartfast_rearend.dao.EthContractDAO;
import com.xuxiang.smartfast_rearend.pojo.EthContracts;
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
public class EthContractAuditService {

    @Autowired
    private EthContractDAO ethContractDAO;



    // 从Etherscan api 根据合约地址获取相应的合约源码和相关信息，并将合约源码和版本号保存至本地，相关信息保存至数据库，为后续检测做准备
    public void ethereumContractSave(String address) {

        // 获取检测时间
        long auditTime = System.currentTimeMillis(); //记录方法开始执行时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String auditTimeStamp = dateFormat.format(new Date(auditTime));

        // 新建EthContracts对象存储当前查询合约信息
        EthContracts ethContracts = new EthContracts();
        ethContracts.setContractAddress(address);
        ethContracts.setTimestamp(auditTimeStamp);
        ethContracts.setChain("Eth MainNet");

        // 定义保存合约的文件夹
        String contractDir = "/home/xuxiang/data/smartfast/data/contract/";
        String jsonPath = "/home/xuxiang/data/smartfast/data/json/test.json";
//        String contractDir = "/Users/xuxiang/workplace/testfile/contract/";
//        String jsonPath = "/Users/xuxiang/workplace/testfile/json/test.json";

        // 设置代理
        Tool.setProxy();

        // 创建一个OkHttpClient对象
        OkHttpClient client = new OkHttpClient();

        String module = "contract";
        String action = "getsourcecode";
        //String address = "0xdAC17F958D2ee523a2206206994597C13D831ec7";
        String apikey = "4BWXPSCVZXJJDF1MR2N3444MR1JFB1JEA7";

        // 构建api请求URL
        String getContractCodeByAddressUrl = "https://api.etherscan.io/api"
                + "?module=" + module
                + "&action=" + action
                + "&address=" + address
                + "&apikey=" + apikey;
        String getContractCreationByAddressUrl = "https://api.etherscan.io/api"
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
                            ethContracts.setRemark(remark);
                            System.out.println(remark);
                            solcVersion = "0.8.0";
                        }
                        ethContracts.setOpenSource(true);
                        ethContracts.setContractName(contractName);
                        ethContracts.setCode(contractCode);
                        ethContracts.setCompilerVersion(compilerVersion);
                        ethContracts.setAbi(node.get("result").get(0).get("ABI").asText());
                        ethContracts.setEvmVersion(node.get("result").get(0).get("EVMVersion").asText());
                        ethContracts.setLibrary(node.get("result").get(0).get("Library").asText());
                        ethContracts.setLicenseType(node.get("result").get(0).get("LicenseType").asText());
                        ethContracts.setOptimizationUsed(node.get("result").get(0).get("OptimizationUsed").asText());
                        ethContracts.setRuns(node.get("result").get(0).get("Runs").asText());
                        ethContracts.setImplementation(node.get("result").get(0).get("Implementation").asText());
                        ethContracts.setConstructorArguments(node.get("result").get(0).get("ConstructorArguments").asText());
                        ethContracts.setProxy(node.get("result").get(0).get("Proxy").asText());
                        ethContracts.setSwarmSource(node.get("result").get(0).get("SwarmSource").asText());
                        System.out.println("合约名：" + contractName + "      " + "编译版本：" + compilerVersion +
                        "\n" + contractCode);
                        if (contractCode == "") {
                            ethContracts.setOpenSource(false);
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
                ethContracts.setContractCreator(contractCreator);
                ethContracts.setCreateTxHash(createTxHash);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        ethContractDAO.save(ethContracts);
    }

    // 判定当前新上传的合约是否开源
    public boolean isOpenSource(){
        return ethContractDAO.findLatestContract().getOpenSource();
    }

    // 添加检测报告链接
    public String addReportLink(){
        EthContracts currentEthContract = ethContractDAO.findLatestContract();
        String reportLink = "http://42.194.184.32:5055/"+currentEthContract.getContractName()+"-report_main.pdf";
        currentEthContract.setReportLink(reportLink);
        ethContractDAO.save(currentEthContract);
        System.out.println("检测报告链接为：" + reportLink);
        return "添加检测报告链接成功";
    }

    // 返回最新的Contracts对象
    public EthContracts getCurrentContract(){
        return ethContractDAO.findLatestContract();
    }

    // 将漏洞信息填入数据库
    public String replenishVulnerabilityInfoInDB(JSONObject VulnerabilityInfoJson){
        EthContracts currentEthContract = getCurrentContract();
        currentEthContract.setHighLevelVulnerability(Integer.parseInt(VulnerabilityInfoJson.get("high").toString()));
        currentEthContract.setMediumLevelVulnerability(Integer.parseInt(VulnerabilityInfoJson.get("medium").toString()));
        currentEthContract.setLowLevelVulnerability(Integer.parseInt(VulnerabilityInfoJson.get("low").toString()));
        currentEthContract.setWarning(Integer.parseInt(VulnerabilityInfoJson.get("need attention").toString()));
        currentEthContract.setNeedOpt(Integer.parseInt(VulnerabilityInfoJson.get("opt").toString()));
        ethContractDAO.save(currentEthContract);
        return "数据库添加漏洞信息成功！";
    }


    /**
     * @return
     * "result":{
     *       "LastBlock":"13053741",   // 以太坊网络上最新的区块编号
     *       "SafeGasPrice":"20",      // 推荐的 gas 价格，如果您使用这个价格发送交易，那么您的交易有很高的概率在 30 分钟内被确认。
     *       "ProposeGasPrice":"22",   // 推荐的 gas 价格，如果您使用这个价格发送交易，那么您的交易有很高的概率在 5 分钟内被确认。
     *       "FastGasPrice":"24",      // 推荐的 gas 价格，如果您使用这个价格发送交易，那么您的交易有很高的概率在 2 分钟内被确认。
     *       "suggestBaseFee":"19.230609716",  // 建议的基础费用，它是根据区块的满载程度动态调整的。如果您使用 EIP-1559 的交易格式，那么您需要支付这个基础费用，以及一个可选的小费，来提高您的交易优先级。
     *       "gasUsedRatio":"0.370119078777807,0.8954731,0.550911766666667,0.212457033333333,0.552463633333333" // 表示了最近 5 个区块的 gas 使用率，也就是每个区块中使用的 gas 与可用的 gas 的比例。这个比例越高，说明网络越拥堵，交易费用也越高。
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
        String apikey = "4BWXPSCVZXJJDF1MR2N3444MR1JFB1JEA7";

        // 构建api请求URL
        String getContractCodeByAddressUrl = "https://api.etherscan.io/api"
                + "?module=" + module
                + "&action=" + action
                + "&apikey=" + apikey;

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
                return node.get("result");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 根据愿意花费（wei单位）预估，返回该交易预估的确认时间（s）
    public int estimateConfirmTime(int gasPrice) {
        // 开启代理
        Tool.setProxy();
        // 创建一个OkHttpClient对象
        OkHttpClient client = new OkHttpClient();

        String module = "gastracker";
        String action = "gasestimate";
        //String address = "0xdAC17F958D2ee523a2206206994597C13D831ec7";
        String apikey = "4BWXPSCVZXJJDF1MR2N3444MR1JFB1JEA7";

        // 构建api请求URL
        String gasestimateUrl = "https://api.etherscan.io/api"
                + "?module=" + module
                + "&action=" + action
                + "&gasprice=" + gasPrice
                + "&apikey=" + apikey;

        // 创建request对象，请求getContractCodeByAddressUrl
        Request request1 = new Request.Builder()
                .url(gasestimateUrl)
                .build();
        try (Response response = client.newCall(request1).execute()){
            if (response.isSuccessful()) {
                // 获取响应的JSON字符串
                String json = response.body().string();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);
                return node.get("result").asInt();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}

package com.xuxiang.smartfast_rearend.tool;

import com.alibaba.fastjson.JSONObject;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tool {

    // 提取文本中的关键信息
    public static JSONObject convertTextToJson(String text) {
        // 定义一个json对象，用于存储键值对
        JSONObject json = new JSONObject();
        // 定义一个正则表达式，用于匹配数字
        Pattern pattern = Pattern.compile("\\d+");
        // 创建一个匹配器，用于在文本中查找数字
        Matcher matcher = pattern.matcher(text);
        // 定义一个字符串数组，用于存储键的名字
        String[] keys = {"high", "medium", "low", "opt", "need attention"};
        // 定义一个变量，用于记录当前的键的索引
        int index = 0;
        // 循环遍历匹配器，找到所有的数字
        while (matcher.find()) {
            // 获取当前匹配到的数字
            String num = matcher.group();
            // 获取当前对应的键的名字
            String key = keys[index];
            // 将键值对放入json对象中
            json.put(key, num);
            // 索引加一，指向下一个键的名字
            index++;
        }
        // 返回json对象
        return json;
    }

    public static String getFormattedDate() {
        // 获取当前时间戳（毫秒级别）
        long timestamp = System.currentTimeMillis();
        // 创建 SimpleDateFormat 对象，用于将日期格式化为指定的字符串格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        // 使用 SimpleDateFormat 格式化时间戳为日期字符串
        String formattedDate = sdf.format(new Date(timestamp));
        // 输出
        return formattedDate;
    }


    // 转义代码字符
    public static String unescapeJava(String escaped) {
        StringBuilder result = new StringBuilder();
        int length = escaped.length();
        for (int i = 0; i < length; i++) {
            char currentChar = escaped.charAt(i);
            if (currentChar == '\\') {
                if (i < length - 1) {
                    char nextChar = escaped.charAt(i + 1);
                    switch (nextChar) {
                        case 'n':
                            result.append('\n');
                            i++; // 跳过下一个字符
                            break;
                        case 't':
                            result.append('\t');
                            i++; // 跳过下一个字符
                            break;
                        case '\\':
                            result.append('\\');
                            i++; // 跳过下一个字符
                            break;
                        // 处理其他转义字符
                        // ...
                        default:
                            // 如果没有匹配的转义字符，则保留原字符
                            result.append(currentChar);
                            break;
                    }
                } else {
                    // 如果转义字符在字符串末尾，则保留原字符
                    result.append(currentChar);
                }
            } else {
                result.append(currentChar);
            }
        }
        return result.toString();
    }

    // 获取纯数字版本号
    public static String cutSolcVersion(String input) {
        String pattern = "v(\\d+\\.\\d+\\.\\d+)";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(input);
        if (m.find()) {
            String solcVersion = m.group(1);
            return solcVersion;
        } else {
            return null;
        }
    }

    // 版本比较是否超过0.8.0
    public static boolean isGreaterThanVersion (String solcVersion) {
        ComparableVersion version1 = new ComparableVersion (solcVersion);
        ComparableVersion version2 = new ComparableVersion ("0.8.0");
        return version1.compareTo (version2) > 0;
    }

    // 工具方法提取合约类型contract-20231029211927.sol:TraceabilityContract.json
    public static String extractContractType(String prefix, String jsonName) {
        // 使用变量值构建正则表达式，动态匹配
        String regex = Pattern.quote(prefix) + ".sol:(.*?)\\.json";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jsonName);

        if (matcher.find()) {
            String extractedSubstring = matcher.group(1);
            return extractedSubstring;
        } else {
            return "UnknownType";
        }
    }

    // 封装代理设置的方法
    public static void setProxy() {
        // 设置代理服务器的IP和端口号
        // 设置代理
        String proxyHost = "127.0.0.1";
        String proxyPort = "7890";
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);
    }
}

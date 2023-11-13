package com.xuxiang.smartfast_rearend.service;

import com.alibaba.fastjson.JSONObject;
import com.xuxiang.smartfast_rearend.dao.PacketsDao;
import com.xuxiang.smartfast_rearend.dao.SessionDAO;
import com.xuxiang.smartfast_rearend.dao.TrafficFileDao;
import com.xuxiang.smartfast_rearend.pojo.Packets;
import com.xuxiang.smartfast_rearend.pojo.Session;
import com.xuxiang.smartfast_rearend.pojo.TrafficFile;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.IpNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class TrafficDetectionService {

    @Autowired
    TrafficFileDao trafficFileDao;

    @Autowired
    SessionDAO sessionDAO;

    @Autowired
    PacketsDao packetsDao;

    @Value("${traffic.path}")
    private String trafficFileDir;

    // 接收上传的文件并保存到指定文件夹，并添加到数据库
    public String handleFileUpload(MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please select a traffic file to upload.");
        }

        // 测试用
        //String trafficFileDir = "/Users/xuxiang/workplace/testfile/trafficTest/";

        // 创建以时间戳命名的文件夹
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String createdTime = dateFormat.format(new Date()); // 创建文件上传时戳
        String trafficFilePath = trafficFileDir + createdTime;
        File dir = new File(trafficFilePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存上传的文件到文件夹中
        String fileName = file.getOriginalFilename();
        String filePath = trafficFilePath + File.separator + fileName;
        File dest = new File(filePath);
        file.transferTo(dest);

        // 相关信息存入数据库
        TrafficFile trafficFile = new TrafficFile();
        trafficFile.setTrafficFileName(fileName);
        trafficFile.setPath(filePath);
        trafficFile.setCreatedTime(createdTime);
        trafficFileDao.save(trafficFile);

        // 保存成功响应
        return "file upload success!";
    }


    // 1. 读取最新pcap文件，按会话拆分，获取每个会话的五元组
    public void pcapSplitAnalyze() throws PcapNativeException, NotOpenException, EOFException, TimeoutException {
        // 获取待检测pcap文件对象
        TrafficFile currentPcapFile = trafficFileDao.findLatestContract();
        // 获取最新pcap文件路径
        String pcapFilePath = currentPcapFile.getPath(); ///Users/xuxiang/workplace/testfile/trafficTest/20230925_153229/ffc514357ce868ea57de238512bab7de.pcap
        System.out.println(pcapFilePath);
        Path path = Paths.get(pcapFilePath);
        Path parentPath = path.getParent();
//        System.out.println(parentPath);
        // 定义输出的每个会话pcap文件位置和名称名
        String outputDir = parentPath + "/sessions/";
        String prefix = "session_";
        String suffix = ".pcap";

        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 使用handle打开pcap文件进行解析
        PcapHandle handle = Pcaps.openOffline(pcapFilePath);

        // 设置过滤器，只捕获TCP数据包
        handle.setFilter("tcp or udp", BpfProgram.BpfCompileMode.OPTIMIZE);

        // 创建Map用于存储不同会话的PcapDumper对象
        Map<String, PcapDumper> dumpersMap = new HashMap<>();

        // 会话计数器
        int counter = 0;
        // 包计数器
        int packetIndex = 0;
        //存储该流量包的所有session
        String sessionString = "";

        // 遍历pcap文件中的每个包，并加入相应的会话
        while (true) {
            try {
                Packet packet = handle.getNextPacketEx();
                // 获取每个包的sessionKey, 会话标识符为源地址:源端口-目的地址:目的端口-协议名
                String sessionKey = getSessionKey(packet);
                //System.out.println(++packetIndex + " " + sessionKey);

                // 检查Map中是否已经存在该会话的PcapDumper对象
                PcapDumper dumper = dumpersMap.get(sessionKey);

                // 如果不存在，创建一个新的PcapDumper对象，并将其存入Map中
                if (dumper == null) {
                    System.out.println(prefix+counter+"--"+sessionKey);
                    // 所有会话写入对应的TrafficFile session字段
//                    String tmp = currentPcapFile.getSession();
//                    currentPcapFile.setSession(tmp + prefix+counter+"--"+sessionKey + "\n");
//                    trafficFileDao.save(currentPcapFile);
                    // 生成一个每个会话的新文件名
                    String sessionPcapName = outputDir+prefix+counter+"-"+sessionKey+suffix;
                    System.out.println(sessionPcapName);
                    sessionString += counter + "." +sessionKey + "\n";
                    counter++;

                    // 创建一个新的PcapDumper对象，用于写入数据包
                    dumper = handle.dumpOpen(sessionPcapName);
                    // 将PcapDumper对象存入Map中
                    dumpersMap.put(sessionKey, dumper);
                }
                // 将数据包写入PcapDumper对象
                dumper.dump(packet, handle.getTimestamp());
            } catch (PcapNativeException | TimeoutException | NotOpenException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (EOFException e) {
                System.out.println("reach last packet");
                break;
                // throw new RuntimeException(e);
            }
        }
        // 关闭所有PcapDumper对象和PcapHandle对象
        for (PcapDumper dumper : dumpersMap.values()) {
            dumper.close();
        }
        handle.close();
        // currentPcapFile.setSession(sessionString);
        System.out.println("当前traffic file的所有session为：" + sessionString);
        currentPcapFile.setSessionNum(counter);
        trafficFileDao.save(currentPcapFile);
    }

    // 2. 流量检测，执行检测脚本
    public String trafficDetect() {
        String scriptInput = null;
        try {
//            // 创建processbuilder并设置执行的命令
//            ProcessBuilder processBuilder = new ProcessBuilder("python3",
//                    "/Users/xuxiang/workplace/testfile/script/trafficTest.py");

            // 脚本路径
            String scriptPath = "/home/xuxiang/data/trafficdetection/script/trafficDectect.sh";
            //构建外部命令
            ProcessBuilder processBuilder = new ProcessBuilder(scriptPath);

            // 启动子进程
            Process process = processBuilder.start();

            // 从子进程中读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                scriptInput += line + "\n";
                System.out.println(line);
            }

            // 等待子进程执行完毕
            int exitCode = process.waitFor();
            System.out.println("exitCode:" + exitCode);
        } catch (IOException e) {
            System.out.println("an I/O error occurs");
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("the current thread is interrupted by another thread while it is waiting, then the wait is ended and an InterruptedException is thrown.");
            throw new RuntimeException(e);
        }
        return scriptInput;
    }

    // 检测结果输出，将每个traffic的session文件和检测结果都存储到sessio字段
    public List<JSONObject> detectResult() {
        List<JSONObject> resultJson = new ArrayList<>();
        TrafficFile currentPcapFile = trafficFileDao.findLatestContract();
        String tmp = "";

        // 读取文件并将数据转换为JSON对象
        try (BufferedReader br = new BufferedReader(new FileReader("/home/xuxiang/data/trafficdetection/data/result/result.txt"))) {
            String line;
            // 读文本直到最后一行
            while ((line = br.readLine()) != null) {
                // 将每行按逗号拆分
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String key = parts[0];
                    String value = parts[1];
                    System.out.println(key + ": " + value) ;
                    currentPcapFile.setSession(tmp + key + ": " + value + "\n");
                    tmp = currentPcapFile.getSession();
                    trafficFileDao.save(currentPcapFile);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(key,value);
                    resultJson.add(jsonObject);
                }
            }
        } catch (IOException e) {
            System.out.println("an I/O error occurs");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return resultJson;
    }



    // 获取每个包的session key
    public static String getSessionKey(Packet packet) {
        // Get the IPv4 header
        IpPacket.IpHeader ipHeader = packet.get(IpPacket.class).getHeader();
        // Get the source and destination IP addresses
        String srcIp = ipHeader.getSrcAddr().getHostAddress();
        String dstIp = ipHeader.getDstAddr().getHostAddress();
        // Get the protocol type
        IpNumber protocol = ipHeader.getProtocol();
        // Get the source and destination ports
        String srcPort = null;
        String dstPort = null;
        if (protocol == IpNumber.TCP) {
            TcpPacket.TcpHeader tcpHeader = packet.get(TcpPacket.class).getHeader();
            srcPort = tcpHeader.getSrcPort().valueAsString();
            dstPort = tcpHeader.getDstPort().valueAsString();
        } else if (protocol == IpNumber.UDP) {
            UdpPacket.UdpHeader udpHeader = packet.get(UdpPacket.class).getHeader();
            srcPort = udpHeader.getSrcPort().valueAsString();
            dstPort = udpHeader.getDstPort().valueAsString();
        } else {
            // If the protocol is not TCP or UDP, return null
            return null;
        }
        // Concatenate the IP addresses, ports and protocol type to form the session key
        return srcIp + ":" + srcPort + "-" + dstIp + ":" + dstPort + "-" + protocol.name();
    }


    // 从trafficfile的session字段提取信息存入数据库
    public List<Session> analyzeSession() {
        TrafficFile currentTrafficFile = trafficFileDao.findLatestContract();
        // 获取当前traffic目录
        String currentTrafficFilePath = currentTrafficFile.getPath();
        Path path = Paths.get(currentTrafficFilePath);
        Path parentPath = path.getParent();
        String outputDir = parentPath + "/sessions/";
        System.out.println(outputDir);

        // 获取所有的session
        String sessions = currentTrafficFile.getSession();
        List<Session> sessionList = new ArrayList<>();
        String[] lines = sessions.split("\n");
        for (String line : lines) {
            System.out.println(line);
            String sessionName = extractSessionName(line);
            Session session = extractSessionInfo(line);
            session.setTrafficfileName(currentTrafficFile.getTrafficFileName());
            session.setTrafficFile(currentTrafficFile);
            session.setPath(outputDir+sessionName);
            if (session != null) {
                sessionDAO.save(session);
                sessionList.add(session);
            }
        }
        return sessionList;
    }

    // 从每行的session中提取session文件名
    public String extractSessionName(String line) {
        Pattern pattern = Pattern.compile("^(.*?\\.pcap):");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String sessionName = matcher.group(1);
            return sessionName;
        }
        return null;
    }

    // 提取session信息，从每行的session文件名中
    private static Session extractSessionInfo(String line) {
        // 使用正则匹配
        Pattern pattern = Pattern.compile("session_(\\d+)-(.*):(\\d+)-(.*):(\\d+)-(\\w+)\\.pcap:\\s+(\\d+)");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            Session session = new Session();
            session.setSessionName("session_" + matcher.group(1));
            session.setSourceIp(matcher.group(2));
            session.setSourcePort(matcher.group(3));
            session.setDestinationIp(matcher.group(4));
            session.setDestinationPort(matcher.group(5));
            session.setProtocol(matcher.group(6).toLowerCase());
            session.setDetectResult(Integer.parseInt(matcher.group(7)));
            return session;
        }
        return null;
    }



    // 找到当前traffic文件的所有session.pcap文件并检测所有流量包信息
    public String packetAnalyzeFromTraffic() {
        // 找到当前处理的流量文件
        TrafficFile currentTrafficFile = trafficFileDao.findLatestContract();
        // 根据当前的traffic找到对应的所有session
        List<Session> sessions = sessionDAO.findByTrafficId(currentTrafficFile.getId());
        // 循环处理每个session
        for(Session session : sessions) {
            String sessionPath = session.getPath();
            if (sessionPath == null) {continue;} // session路径为空进入下一次循环
            // 应该是每个包一个packets对象
            String sessionName = session.getSessionName();
            packetAnalyzeFromSession(sessionPath,  session, sessionName);
        }
        return "finished packet analyze";
    }


    // 分析每个会话中的每个流量包信息
    public void packetAnalyzeFromSession(String sessionPath, Session session, String sessionName) {
        try {
            // 使用pcaphandle打开pcap文件
            PcapHandle handle = Pcaps.openOffline(sessionPath);
            // 使用Packet对象，存储每个包
            Packet packet = null;
            int packetIndex = 0;
            // 循环处理每个包
            while (true) {
                // 为每个包创建一个packets对象
                Packets packetInfo = new Packets();
                packetInfo.setSessionName(sessionName);
                packetInfo.setSession(session);
                try {
                    packet = handle.getNextPacketEx();
                    System.out.println("timestamp: " + handle.getTimestamp());
                    System.out.println("length: " + packet.length());
                    packetInfo.setPacketLength(packet.length());
                    packetInfo.setTimestamp(handle.getTimestamp());
                    System.out.println(packet);

                    System.out.println("Ethernet Header Information:");
                    //Ethernet Header
                    EthernetPacket ethernetPacket = packet.get(EthernetPacket.class); // 获取以太网报文
                    if (ethernetPacket != null) {
                        EthernetPacket.EthernetHeader ethernetHeader = ethernetPacket.getHeader(); // 获取以太网报文头部
                        int ethernetHeaderSize = ethernetHeader.length();
                        String dstMacAddr = ethernetHeader.getDstAddr().toString();
                        String srcMacAddr = ethernetHeader.getSrcAddr().toString();
                        String type = ethernetHeader.getType().toString();
                        packetInfo.setEthernetHeaderSize(ethernetHeaderSize);
                        packetInfo.setDstMacAddr(dstMacAddr);
                        packetInfo.setSrcMacAddr(srcMacAddr);
                        packetInfo.setType(type);
                        System.out.println(ethernetHeaderSize +"-"+ dstMacAddr +"-"+ srcMacAddr +"-"+ type);
                    }

                    //IpV4Header
                    System.out.println("IpV4Header Header Information:");
                    IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
                    if (ipV4Packet != null) {
                        IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();
                        String ipVersion = ipV4Header.getVersion().valueAsString();
                        String dstIpAddr = ipV4Header.getDstAddr().toString();
                        String srcIpAddr = ipV4Header.getSrcAddr().toString();
                        String protocol = ipV4Header.getProtocol().valueAsString();
                        int ttl = ipV4Header.getTtlAsInt();
                        int ipv4HeaderSize = ipV4Header.length();
                        packetInfo.setIpVersion(ipVersion);
                        packetInfo.setDstIpAddr(dstIpAddr);
                        packetInfo.setSrcIpAddr(srcIpAddr);
                        packetInfo.setProtocol(protocol);
                        packetInfo.setTtl(ttl);
                        packetInfo.setIpv4HeaderSize(ipv4HeaderSize);
                        System.out.println(ipVersion +"-"+ dstIpAddr +"-"+ srcIpAddr +"-"+ ttl +"-"+ ipv4HeaderSize);
                    }

                    // tcpHeader
                    System.out.println("tcpHeader Header Information:");
                    TcpPacket tcpPacket = packet.get(TcpPacket.class);
                    if (tcpPacket != null) {
                        TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
                        int dstPort = tcpHeader.getDstPort().value();
                        int srcPort = tcpHeader.getSrcPort().value();
                        packetInfo.setDstPort(dstPort);
                        packetInfo.setSrcPort(srcPort);
                        System.out.println(dstPort +"-"+ srcPort);
                    }
                } catch (NotOpenException e) {
                    throw new RuntimeException(e);
                } catch (EOFException e) {
                    System.out.println("read last packet");
                    break;
                    // throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
                packetsDao.save(packetInfo);
            }
            // 关闭PcapHandle对象
            handle.close();
        } catch (PcapNativeException e) {
            System.out.println("an error occurs in the pcap native library.\n");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // 流量检测记录，查询所有流量检测文件
    public List<TrafficFile> listAllTrafficRecord() {
        return trafficFileDao.findAll();
    }

    // 根据trafficid找到所有session
    public List<Session> listSessionByTrafficId(int trafficId) {
        return sessionDAO.findByTrafficId(trafficId);
    }

    // 根据sessionid找到所有packet
    public List<Packets> listPacketsBySessionId(int sessionId) {
        return packetsDao.findBySessionId(sessionId);
    }
}

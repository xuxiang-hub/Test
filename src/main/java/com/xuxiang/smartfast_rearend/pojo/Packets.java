package com.xuxiang.smartfast_rearend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "packets")
//前后端使用的是json格式，使用jpa做实体类的持久化，jpa 默认会使用 hibernate, 在 jpa 工作过程中，就会创造代理类来继承 User ，
// 并添加 handler 和 hibernateLazyInitializer 这两个无须 json 化的属性，
// 所以这里需要用 JsonIgnoreProperties 把这两个属性忽略掉。
@JsonIgnoreProperties({"handler","hibernateLazyInitializer"})
public class Packets {
    // 主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "packet_id")
    int packetId;

    @Column(name = "timestamp")
    Timestamp timestamp;

    @Column(name = "packet_length")
    int packetLength;

    @Column(name = "ethernet_header_size")
    int ethernetHeaderSize;

    @Column(name = "dst_mac_addr")
    String dstMacAddr;

    @Column(name = "src_mac_addr")
    String srcMacAddr;

    @Column(name = "type")
    String type;

    @Column(name = "ip_version")
    String ipVersion;

    @Column(name = "dst_ip_addr")
    String dstIpAddr;

    @Column(name = "src_ip_addr")
    String srcIpAddr;

    @Column(name = "protocol")
    String protocol;

    @Column(name = "ttl")
    int ttl;

    @Column(name = "ipv4_header_size")
    int ipv4HeaderSize;
    @Column(name = "dst_port")
    int dstPort;
    @Column(name = "src_port")
    int srcPort;
    @Column(name = "session")
    String sessionName;

    // 外键，多对一
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    Session session;
}

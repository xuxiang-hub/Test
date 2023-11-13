package com.xuxiang.smartfast_rearend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "session_info")
//前后端使用的是json格式，使用jpa做实体类的持久化，jpa 默认会使用 hibernate, 在 jpa 工作过程中，就会创造代理类来继承 User ，
// 并添加 handler 和 hibernateLazyInitializer 这两个无须 json 化的属性，
// 所以这里需要用 JsonIgnoreProperties 把这两个属性忽略掉。
@JsonIgnoreProperties({"handler","hibernateLazyInitializer"})
public class Session {

    // 主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    int sessionId;

    @Column(name = "session_name")
    String sessionName;

    @Column(name = "source_ip")
    String sourceIp;

    @Column(name = "source_port")
    String sourcePort;

    @Column(name = "destination_ip")
    String destinationIp;

    @Column(name = "destination_port")
    String destinationPort;

    @Column(name = "protocol")
    String protocol;

    @Column(name = "detect_result")
    Integer detectResult;

    // 外键，多对一
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traffic_id")
    TrafficFile trafficFile;

    @Column(name = "trafficfile_name")
    String trafficfileName;

    @Column(name = "path")
    String path;

}

package com.xuxiang.smartfast_rearend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "traffic_file")
//前后端使用的是json格式，使用jpa做实体类的持久化，jpa 默认会使用 hibernate, 在 jpa 工作过程中，就会创造代理类来继承 User ，
// 并添加 handler 和 hibernateLazyInitializer 这两个无须 json 化的属性，
// 所以这里需要用 JsonIgnoreProperties 把这两个属性忽略掉。
@JsonIgnoreProperties({"handler","hibernateLazyInitializer"})
public class TrafficFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    int id; // 唯一标识主键

    @Column(name = "traffic_file_name")
    String trafficFileName;

    @Column(name = "created_time")
    String createdTime;

    @Column(name = "session")
    String session;

    @Column(name = "path")
    String path;

    @Column(name = "session_number")
    int sessionNum;
}

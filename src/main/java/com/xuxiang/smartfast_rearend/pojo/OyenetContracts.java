package com.xuxiang.smartfast_rearend.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "oyenet_contracts")
//前后端使用的是json格式，使用jpa做实体类的持久化，jpa 默认会使用 hibernate, 在 jpa 工作过程中，就会创造代理类来继承 User ，
// 并添加 handler 和 hibernateLazyInitializer 这两个无须 json 化的属性，
// 所以这里需要用 JsonIgnoreProperties 把这两个属性忽略掉。
@JsonIgnoreProperties({"handler","hibernateLazyInitializer"})
public class OyenetContracts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    int id; // 唯一标识主键

    @Column(name = "solc_version")
    String solcVersion;
    //java 属性一般采用小驼峰写法，但JPA 会自动将小驼峰命名转换为下划线命名
    @Column(name = "contract_name")
    String contractName;
    @Column(name = "created_at")
    String createdAt;

    @Column(name = "code")
    String code;

    @Column(name = "contract_type")
    String contractType;
//    @Column(name = "report_link") // 当前合约的检测报告链接
//    String reportLink;
}

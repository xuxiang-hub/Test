package com.xuxiang.smartfast_rearend.dao;

import com.xuxiang.smartfast_rearend.pojo.SmartfastContracts;
import com.xuxiang.smartfast_rearend.pojo.OyenetContracts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

//Data Access Object（数据访问对象，DAO）即用来操作数据库的对象
//DAO直接与数据库交互，定义增删改查等操作
//继承JpaRepository构建DAO
//使用jpa无需手动构建SQL语句
public interface OyenetContractDAO extends JpaRepository<OyenetContracts, Integer> {

    // 查询最新的contract对象，即id最大的contract对象
    @Query("SELECT c FROM OyenetContracts c WHERE c.id = (SELECT MAX(c2.id) FROM OyenetContracts c2)")
    OyenetContracts findLatestContract();

    // 通过合约名或solc版本进行查询
    List<OyenetContracts> findByContractName(String contractName);
    List<OyenetContracts> findBySolcVersion(String solcVersion);
    List<OyenetContracts> findByContractNameAndSolcVersion(String contractName, String solcVersion);
}

package com.xuxiang.smartfast_rearend.dao;

import com.xuxiang.smartfast_rearend.pojo.BscContracts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

//Data Access Object（数据访问对象，DAO）即用来操作数据库的对象
//DAO直接与数据库交互，定义增删改查等操作
//继承JpaRepository构建DAO
//使用jpa无需手动构建SQL语句
public interface BscContractDAO extends JpaRepository<BscContracts, Integer> {

    // 查询最新的contract对象，即id最大的contract对象
    @Query("SELECT c FROM BscContracts c WHERE c.id = (SELECT MAX(c2.id) FROM BscContracts c2)")
    BscContracts findLatestContract();
}

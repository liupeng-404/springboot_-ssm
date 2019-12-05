package com.imooc.junit;

import com.imooc.Application;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.service.impl.UserServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.junit.Assert;

import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class TestServerImpl {


    @Autowired
    private UserServiceImpl userService;

    @Before
    public void init() {
        System.out.println("开始测试-----------------");
    }


    @Test
    public void test(){

        String userId="191028G50DGXAMRP";
        List<MyFriendsVO> myFriendsVOList=userService.queryMyFriends(userId);
        System.out.println("朋友人数为："+myFriendsVOList.size());
        Assert.assertNotEquals(0,myFriendsVOList.size());
    }

    @After
    public void after() {
        System.out.println("测试结束-----------------");
    }

}


package com.fhs.test.service;

import com.fhs.common.utils.JsonUtils;
import com.fhs.test.pojo.Student;
import com.fhs.trans.service.impl.DictionaryTransService;
import com.fhs.trans.service.impl.TransService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TeacherServiceTest {

    @Autowired
    private  DictionaryTransService dictionaryTransService;

    @Autowired
    private TransService transService;

    @Test
    public void transOne(){
        Student student = new Student();
        student.setStudentName("张三");
        student.setTeacherId("1");
        student.setEnglishteacherId("2");
        student.setSex(1);
        transService.transOne(student);
        transService.transOne(student);
        System.out.println(JsonUtils.bean2json(student));
    }

    @Before
    public void init(){
        Map<String,String> transMap = new HashMap<>();
        transMap.put("0","男");
        transMap.put("1","女");
        dictionaryTransService.refreshCache("sex",transMap);
    }
}
package com.fhs.test.service;

import com.fhs.core.trans.anno.AutoTrans;
import com.fhs.core.trans.vo.VO;
import com.fhs.test.pojo.Teacher;
import com.fhs.trans.service.AutoTransAble;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@AutoTrans(namespace = "teacher",fields = "name",defaultAlias = "teacher",useCache = true,useRedis = true)
public class TeacherService implements AutoTransAble {
    @Override
    public List findByIds(List ids) {
        List list = new ArrayList();
        Teacher t  = null;
        for (Object id : ids) {
            t  = new Teacher();
            t.setTeacherId((String)id);
            t.setName("老师名字" + id);
            list.add(t);
        }
        System.out.println("teacher service的findByIds 进来了");
        return list;
    }

    @Override
    public List select() {
        System.out.println("teacher service的select 进来了");
        return findByIds(Arrays.asList("1","2","3","4"));
    }

    @Override
    public VO selectById(Object primaryValue) {
        System.out.println("teacher service的selectById 进来了");
        Teacher t  = new Teacher();
        t.setTeacherId((String)primaryValue);
        t.setName("老师名字" + primaryValue);
        return t;
    }
}

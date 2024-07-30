package com.fhs.test.pojo;

import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.VO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Student implements VO {

    private String studentName;

    @Trans(type = TransType.AUTO_TRANS,key = "teacher")
    private String teacherId;

    @Trans(type = TransType.AUTO_TRANS,key = "teacher#english")
    private String englishteacherId;

    @Trans(type = TransType.DICTIONARY,key = "sex")
    private Integer sex;

    public Map<String,String> transMap = new HashMap<>();

    @Override
    public Map<String, String> getTransMap() {
        return transMap;
    }
}

package com.fhs.trans.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.BeforeFilter;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.fhs.core.trans.vo.VO;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransFastJsonHttpMessageConverter extends FastJsonHttpMessageConverter {

    @Override
    protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        OutputStream out = outputMessage.getBody();
        String text = JSON.toJSONString(obj, new SerializeFilter[]{
                new TransFormatterFilter(),new TransPropertyPreFilter()
        }, super.getFastJsonConfig().getSerializerFeatures());
        byte[] bytes = text.getBytes(super.getFastJsonConfig().getCharset());
        out.write(bytes);
    }
}
//过滤掉transMap
class TransPropertyPreFilter extends SimplePropertyPreFilter{
    @Override
    public boolean apply(JSONSerializer serializer, Object source, String name) {
        //扁平化的时候不需要
        if(source!=null && source instanceof VO && "transMap".equals(name)){
            return false;
        }
        return true;
    }
}
//把transmap 的内容也写进去
class TransFormatterFilter extends BeforeFilter {
    public TransFormatterFilter() {

    }

    @Override
    public void writeBefore(Object o) {
        if (o instanceof Collection) {
            Collection<?> objs = (Collection<?>) o;
            for (Object obj : objs) {
                writeObject(o);
            }
            return;
        }
        writeObject(o);
    }

    /**
     * 写一个obejct
     *
     * @param obj obj
     */
    private void writeObject(Object obj) {
        if (obj == null) {
            return;
        }
        boolean isSuperBean = obj instanceof VO;
        if (!isSuperBean) {
            return;
        }
        VO vo = (VO) obj;
        if (vo.getTransMap() != null) {
            for (String key : vo.getTransMap().keySet()) {
                super.writeKeyValue(key, vo.getTransMap().get(key));
            }
        }
    }
}

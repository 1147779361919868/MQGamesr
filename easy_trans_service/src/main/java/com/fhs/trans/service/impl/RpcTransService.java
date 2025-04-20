package com.fhs.trans.service.impl;

import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.VO;
import lombok.Data;

import java.util.List;

/**
 * 远程翻译服务
 */
@Data
public class RpcTransService extends SimpleTransService {


    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.RPC, this);
    }

    @Override
    public List<? extends VO> findByIds(List<String> ids, Trans tempTrans) {
        return super.findByIds(ids, tempTrans);
    }

    @Override
    public VO findById(String id, Trans tempTrans) {
        return super.findById(id, tempTrans);
    }
}

package com.coding.core.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.core.mapper.ResourceRecordMapper;
import com.coding.core.model.entity.ResourceRecordDO;
import com.coding.core.repository.ResourceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 文件信息Repository实现类
 */
@Repository
@RequiredArgsConstructor
public class ResourceRecordRepositoryImpl implements ResourceRecordRepository {

    private final ResourceRecordMapper resourceRecordMapper;

    @Override
    public ResourceRecordDO getByMd5(String md5) {
        LambdaQueryWrapper<ResourceRecordDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ResourceRecordDO::getFileMd5, md5);
        return resourceRecordMapper.selectOne(queryWrapper);
    }

    @Override
    public Long save(ResourceRecordDO resourceRecordDO) {
        int result = resourceRecordMapper.insert(resourceRecordDO);
        return result > 0 ? resourceRecordDO.getId() : null;
    }

    @Override
    public ResourceRecordDO getById(Long id) {
        return resourceRecordMapper.selectById(id);
    }
}

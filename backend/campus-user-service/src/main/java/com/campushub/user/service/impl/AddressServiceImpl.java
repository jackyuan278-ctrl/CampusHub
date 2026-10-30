package com.campushub.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.common.BizException;
import com.campushub.common.interceptor.UserContext;
import com.campushub.user.domain.dto.AddressDTO;
import com.campushub.user.domain.po.Address;
import com.campushub.user.domain.vo.AddressVO;
import com.campushub.user.mapper.AddressMapper;
import com.campushub.user.service.IAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

    @Override
    public List<AddressVO> queryMyAddresses() {
        Long userId = UserContext.getUser();
        List<Address> addressList = query().eq("user_id", userId).orderByDesc("is_default").list();
        List<AddressVO> addressVOList = new ArrayList<>();
        for (Address address : addressList) {
            addressVOList.add(BeanUtil.copyProperties(address, AddressVO.class));
        }
        return addressVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAddress(AddressDTO addressDTO) {
        Long userId = UserContext.getUser();
        Address address = BeanUtil.copyProperties(addressDTO, Address.class);
        address.setUserId(userId);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        // 首个地址自动设为默认；非首个但标记默认时，先清掉旧默认
        if (query().eq("user_id", userId).count() == 0) {
            address.setIsDefault(1);
        } else if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            update().eq("user_id", userId).set("is_default", 0).update();
        }
        save(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long id, AddressDTO addressDTO) {
        Long userId = UserContext.getUser();
        Address address = query().eq("id", id).one();
        if (address == null) {
            throw new BizException(400, "地址不存在");
        }
        if (!userId.equals(address.getUserId())) {
            throw new BizException(400, "无权限操作该地址");
        }
        // 仅当把目标地址提升为默认时才先清同用户旧默认
        if (addressDTO.getIsDefault() != null && addressDTO.getIsDefault() == 1) {
            update().eq("user_id", userId).set("is_default", 0).update();
        }
        update().eq("id", id)
                .set(addressDTO.getCity() != null, "city", addressDTO.getCity())
                .set(addressDTO.getReceiver() != null, "receiver", addressDTO.getReceiver())
                .set(addressDTO.getPhone() != null, "phone", addressDTO.getPhone())
                .set(addressDTO.getDetail() != null, "detail", addressDTO.getDetail())
                .set(addressDTO.getIsDefault() != null, "is_default", addressDTO.getIsDefault())
                .set(addressDTO.getProvince() != null, "province", addressDTO.getProvince())
                .set(addressDTO.getDistrict() != null, "district", addressDTO.getDistrict())
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAddress(Long id) {
        Long userId = UserContext.getUser();
        Address address = query().eq("id", id).one();
        if (address == null) {
            throw new BizException(400, "地址不存在");
        }
        if (!userId.equals(address.getUserId())) {
            throw new BizException(400, "无权限操作该地址");
        }
        removeById(id);
        // 删的是默认地址：把最早创建的一条顶为默认
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            List<Address> addressList = query().eq("user_id", userId).orderByAsc("create_time").list();
            if (!addressList.isEmpty()) {
                update().eq("id", addressList.get(0).getId()).set("is_default", 1).update();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Long userId = UserContext.getUser();
        Address address = query().eq("id", id).one();
        if (address == null) {
            throw new BizException(400, "地址不存在");
        }
        if (!userId.equals(address.getUserId())) {
            throw new BizException(400, "无权限操作该地址");
        }
        update().eq("user_id", userId).set("is_default", 0).update();
        update().eq("id", id).set("is_default", 1).update();
    }
}





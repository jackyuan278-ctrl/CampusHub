package com.campushub.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.user.domain.dto.AddressDTO;
import com.campushub.user.domain.po.Address;
import com.campushub.user.domain.vo.AddressVO;

import java.util.List;

public interface IAddressService extends IService<Address> {

    List<AddressVO> queryMyAddresses();

    void saveAddress(AddressDTO addressDTO);

    void updateAddress(Long id, AddressDTO addressDTO);

    void removeAddress(Long id);

    void setDefault(Long id);
}





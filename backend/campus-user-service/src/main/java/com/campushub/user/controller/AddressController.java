package com.campushub.user.controller;

import com.campushub.common.Result;
import com.campushub.user.domain.dto.AddressDTO;
import com.campushub.user.domain.vo.AddressVO;
import com.campushub.user.service.IAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final IAddressService addressService;

    @GetMapping
    public Result<List<AddressVO>> queryMyAddresses() {
        return Result.success(addressService.queryMyAddresses());
    }

    @PostMapping
    public Result<Void> saveAddress(@RequestBody @Validated AddressDTO addressDTO) {
        addressService.saveAddress(addressDTO);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateAddress(@PathVariable("id") Long id, @RequestBody @Validated AddressDTO addressDTO) {
        addressService.updateAddress(id, addressDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> removeAddress(@PathVariable("id") Long id) {
        addressService.removeAddress(id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable("id") Long id) {
        addressService.setDefault(id);
        return Result.success();
    }
}





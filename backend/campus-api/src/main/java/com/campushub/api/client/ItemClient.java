package com.campushub.api.client;

import com.campushub.api.dto.ItemDTO;
import com.campushub.api.dto.OrderDetailDTO;
import com.campushub.common.Result;
import com.campushub.common.domain.PageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "campus-secondhand-service", path = "/items")
public interface ItemClient {

    @GetMapping("/{id}")
    Result<ItemDTO> queryItemById(@PathVariable("id") Long id);
    @GetMapping
    Result<PageDTO<ItemDTO>> queryItemPage(@RequestParam("page") Integer page,
                                           @RequestParam("pageSize") Integer pageSize);

    @PutMapping("/stock/deduct")
    Result<Void> deductStock(@RequestBody List<OrderDetailDTO> details);

    @PutMapping("/stock/restore")
    Result<Void> restoreStock(@RequestBody List<OrderDetailDTO> details);
}





